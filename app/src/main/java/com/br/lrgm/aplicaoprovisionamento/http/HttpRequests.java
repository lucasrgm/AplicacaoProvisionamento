package com.br.lrgm.aplicaoprovisionamento.http;

import android.app.Activity;
import android.util.Log;

import com.br.lrgm.aplicaoprovisionamento.R;
import com.br.lrgm.aplicaoprovisionamento.roteador.RedeLocal;
import com.br.lrgm.aplicaoprovisionamento.roteador.ResultadoOperacao;
import com.br.lrgm.aplicaoprovisionamento.roteador.Roteador;
import com.br.lrgm.aplicaoprovisionamento.roteador.RoteadorException;
import com.br.lrgm.aplicaoprovisionamento.roteador.TLWR840N;
import com.br.lrgm.aplicaoprovisionamento.roteador.TransporteOkHttp;
import com.br.lrgm.aplicaoprovisionamento.roteador.TravaRoteador;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Ponto único de acesso às operações do app:
 * <ul>
 *   <li>backend (API REST/JSON): login e validação do token JWT;</li>
 *   <li>roteador: leitura e alteração dos parâmetros, feitas diretamente no equipamento.</li>
 * </ul>
 */
public class HttpRequests {

    private static final String TAG = "HttpRequests";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private final String endpoint;
    private final Activity context;
    private String token;

    public HttpRequests(Activity context) {
        this.context = context;
        this.endpoint = context.getString(R.string.endpoint_url);
        this.token = ArmazenamentoLocal.obterToken(context);
    }

    // ------------------------------------------------------------------
    // Backend
    // ------------------------------------------------------------------

    public void login(String username, String password, HttpEscutas escutas) {
        escutas.quandoInicia();

        EXECUTOR.execute(() -> {
            try {
                JSONObject corpo = new JSONObject();
                corpo.put("username", username);
                corpo.put("password", password);

                Request request = new Request.Builder()
                        .url(endpoint + "/login")
                        .post(RequestBody.create(corpo.toString(), JSON))
                        .build();

                try (Response response = clienteBackend().newCall(request).execute()) {
                    JSONObject json = lerJson(response);
                    String novoToken = json.optString("token", "");

                    if (response.isSuccessful() && !novoToken.isEmpty()) {
                        this.token = novoToken;
                        ArmazenamentoLocal.salvarToken(context, novoToken);
                        escutas.quandoSucesso(novoToken);
                    } else {
                        escutas.quandoErro(json.optString("error",
                                "Não foi possível fazer login (HTTP " + response.code() + ")."));
                    }
                }
            } catch (IOException e) {
                escutas.quandoErro("Não foi possível conectar ao servidor. Verifique a conexão!");
            } catch (Exception e) {
                Log.e(TAG, "Erro no login", e);
                escutas.quandoErro("Erro interno, tente novamente!");
            } finally {
                escutas.quandoFinaliza();
            }
        });
    }

    /**
     * Confere se a sessão do técnico ainda vale. Primeiro pela data de expiração do token
     * e, se o servidor estiver acessível, pela rota protegida /validarToken.
     * Sem conexão com o servidor, vale a data de expiração do próprio token.
     */
    public void validarSessao(HttpEscutas escutas) {
        escutas.quandoInicia();

        if (!TokenJwt.valido(token)) {
            ArmazenamentoLocal.limparToken(context);
            escutas.quandoLoginExpirado();
            escutas.quandoFinaliza();
            return;
        }

        EXECUTOR.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(endpoint + "/validarToken")
                        .header("token_autenticacao", token)
                        .get()
                        .build();

                try (Response response = clienteBackend().newCall(request).execute()) {
                    if (response.code() == 401) {
                        ArmazenamentoLocal.limparToken(context);
                        escutas.quandoLoginExpirado();
                    } else {
                        escutas.quandoSucesso("Sessão válida");
                    }
                }
            } catch (IOException e) {
                // Servidor fora de alcance (ex.: celular na rede do roteador sem internet).
                escutas.quandoSucesso("Sessão válida (sem conexão com o servidor)");
            } finally {
                escutas.quandoFinaliza();
            }
        });
    }

    // ------------------------------------------------------------------
    // Roteador (comunicação direta com o equipamento)
    // ------------------------------------------------------------------

    public void obterParametros(String ip, String porta, String senhaRoteador,
                                EscutasRoteador<Parametros> escutas) {
        executarNoRoteador(ip, porta, senhaRoteador, escutas, Roteador::obterParametros);
    }

    public void alterarWifi(String ip, String porta, String senhaRoteador, String pilha,
                            String ssid, String senhaWifi,
                            EscutasRoteador<ResultadoOperacao> escutas) {
        executarNoRoteador(ip, porta, senhaRoteador, escutas,
                roteador -> exigirSucesso(roteador.alterarWifi(pilha, ssid, senhaWifi)));
    }

    public void alterarPppoe(String ip, String porta, String senhaRoteador,
                             String usuarioPppoe, String senhaPppoe,
                             EscutasRoteador<ResultadoOperacao> escutas) {
        executarNoRoteador(ip, porta, senhaRoteador, escutas,
                roteador -> exigirSucesso(roteador.alterarPppoe(usuarioPppoe, senhaPppoe)));
    }

    /** Cria a implementação do modelo de roteador. Novos modelos entram aqui. */
    protected Roteador criarRoteador(String ip, String porta, String senhaRoteador) {
        TransporteOkHttp transporte = new TransporteOkHttp(RedeLocal.socketFactory(context));
        return new TLWR840N(ip, porta, TLWR840N.USUARIO_PADRAO, senhaRoteador, transporte);
    }

    private interface Operacao<T> {
        T executar(Roteador roteador) throws RoteadorException;
    }

    private <T> void executarNoRoteador(String ip, String porta, String senhaRoteador,
                                        EscutasRoteador<T> escutas, Operacao<T> operacao) {
        escutas.quandoInicia();

        String erro = Validacao.validarEndereco(ip, porta);
        if (erro == null && (senhaRoteador == null || senhaRoteador.isEmpty())) {
            erro = "Informe a senha do roteador!";
        }
        if (erro != null) {
            escutas.quandoErro(erro);
            escutas.quandoFinaliza();
            return;
        }

        // RF02: sessão expirada leva o técnico de volta ao login.
        if (!TokenJwt.valido(token)) {
            ArmazenamentoLocal.limparToken(context);
            escutas.quandoLoginExpirado();
            escutas.quandoFinaliza();
            return;
        }

        // CT08: uma operação por vez no mesmo roteador.
        String chave = TravaRoteador.chave(ip, porta);
        if (!TravaRoteador.travar(chave)) {
            escutas.quandoErro("Roteador ocupado! Aguarde a operação em andamento terminar.");
            escutas.quandoFinaliza();
            return;
        }

        EXECUTOR.execute(() -> {
            try {
                Roteador roteador = criarRoteador(ip, porta, senhaRoteador);
                escutas.quandoSucesso(operacao.executar(roteador));
            } catch (RoteadorException e) {
                escutas.quandoErro(e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Erro na operação com o roteador", e);
                escutas.quandoErro("Erro inesperado na comunicação com o roteador.");
            } finally {
                TravaRoteador.liberar(chave);
                escutas.quandoFinaliza();
            }
        });
    }

    private static ResultadoOperacao exigirSucesso(ResultadoOperacao resultado)
            throws RoteadorException {
        if (!resultado.sucesso) throw new RoteadorException(resultado.mensagem);
        return resultado;
    }

    // ------------------------------------------------------------------
    // Utilitários
    // ------------------------------------------------------------------

    private static OkHttpClient clienteBackend() {
        return new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .callTimeout(20, TimeUnit.SECONDS)
                .build();
    }

    private static JSONObject lerJson(Response response) throws IOException {
        String texto = response.body() == null ? "" : response.body().string();
        try {
            return new JSONObject(texto);
        } catch (JSONException e) {
            return new JSONObject();
        }
    }
}
