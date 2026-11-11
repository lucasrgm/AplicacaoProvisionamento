package com.br.lrgm.aplicaoprovisionamento;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.br.lrgm.aplicaoprovisionamento.http.ArmazenamentoLocal;
import com.br.lrgm.aplicaoprovisionamento.http.EscutasRoteador;
import com.br.lrgm.aplicaoprovisionamento.http.HttpRequests;
import com.br.lrgm.aplicaoprovisionamento.http.Parametros;
import com.br.lrgm.aplicaoprovisionamento.http.TokenJwt;
import com.br.lrgm.aplicaoprovisionamento.roteador.ResultadoOperacao;

/** Tela de configuração: Wi-Fi e PPPoE lidos do roteador, cada grupo com o seu botão de envio. */
public class PrincipalActivity extends AppCompatActivity {

    private LinearLayout layout5g, mainLayout;
    private ProgressBar loadingLayout;
    private SwipeRefreshLayout swipeRefresh;
    private Button botaoEnviarWifi1, botaoEnviarWifi2, botaoEnviarPppoe, botaoLogout;
    private EditText ssid1, ssid2, password1, password2;
    private EditText pppoe, passpppoe;

    private Parametros parametros;
    private String senhaRoteador;

    /** Evita sair da tela no meio de uma operação no roteador. */
    private volatile boolean executando = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal);

        mainLayout = findViewById(R.id.mainLayout);
        layout5g = findViewById(R.id.layout_wifi2);
        botaoEnviarWifi1 = findViewById(R.id.botaoEnviarWifi1);
        botaoEnviarWifi2 = findViewById(R.id.botaoEnviarWifi2);
        botaoEnviarPppoe = findViewById(R.id.botaoEnviarPppoe);
        ssid1 = findViewById(R.id.ssid1);
        password1 = findViewById(R.id.password1);
        ssid2 = findViewById(R.id.ssid2);
        password2 = findViewById(R.id.password2);
        pppoe = findViewById(R.id.pppoe);
        passpppoe = findViewById(R.id.passpppoe);
        loadingLayout = findViewById(R.id.progressBar);
        botaoLogout = findViewById(R.id.btnLogout);
        swipeRefresh = findViewById(R.id.swiperefresh);

        botaoLogout.setOnClickListener(v -> Admin.logout(this));
        botaoEnviarWifi1.setOnClickListener(v -> alterarWifi(false));
        botaoEnviarWifi2.setOnClickListener(v -> alterarWifi(true));
        botaoEnviarPppoe.setOnClickListener(v -> alterarPppoe());

        swipeRefresh.setOnRefreshListener(this::obterParametros);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (executando) {
                    Toast.makeText(PrincipalActivity.this,
                            "Aguarde a operação terminar.", Toast.LENGTH_SHORT).show();
                } else {
                    finish();
                }
            }
        });

        senhaRoteador = getIntent().getStringExtra(BuscarDispositivoActivity.EXTRA_SENHA_ROTEADOR);
        parametros = getIntent().getSerializableExtra(BuscarDispositivoActivity.EXTRA_PARAMETROS,
                Parametros.class);

        if (parametros == null) {
            Toast.makeText(this, "Não foi possível abrir os parâmetros do roteador.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!TokenJwt.valido(ArmazenamentoLocal.obterToken(this))) {
            Admin.irParaTelaLogin(this);
            return;
        }

        preencherCampos();
        mostrar(loadingLayout, false);
        mostrar(mainLayout, true);
    }

    private void preencherCampos() {
        Wifi rede24 = parametros.obterWifi(false);
        Wifi rede5 = parametros.obterWifi(true);

        ssid1.setText(rede24 == null ? "" : rede24.ssid);
        password1.setText(rede24 == null ? "" : rede24.password);

        // O TL-WR840N tem apenas a rede de 2,4 GHz: o bloco de 5 GHz só aparece
        // em modelos que informarem essa faixa.
        mostrar(layout5g, rede5 != null);
        if (rede5 != null) {
            ssid2.setText(rede5.ssid);
            password2.setText(rede5.password);
        }

        pppoe.setText(parametros.internet.pppoe);
        passpppoe.setText(parametros.internet.password);
    }

    private void mostrar(View v, boolean visivel) {
        v.setVisibility(visivel ? View.VISIBLE : View.GONE);
    }

    private ProgressDialog criarProgresso(String titulo) {
        ProgressDialog dialogo = new ProgressDialog(this);
        dialogo.setTitle(titulo);
        dialogo.setMessage("Aguarde...");
        dialogo.setCancelable(false);
        return dialogo;
    }

    private void alterarWifi(boolean cincoGHz) {
        Wifi rede = parametros.obterWifi(cincoGHz);
        if (rede == null) {
            Toast.makeText(this, "Este roteador não tem essa rede.", Toast.LENGTH_LONG).show();
            return;
        }

        String ssid = (cincoGHz ? ssid2 : ssid1).getText().toString();
        String senha = (cincoGHz ? password2 : password1).getText().toString();

        new HttpRequests(this).alterarWifi(parametros.ip, parametros.porta, senhaRoteador,
                rede.pilha, ssid, senha, new EscutasAlteracao("Alterando Wi-Fi...") {
                    @Override
                    void aoConcluir(ResultadoOperacao resultado) {
                        rede.ssid = ssid;
                        rede.password = senha;
                    }
                });
    }

    private void alterarPppoe() {
        String usuario = pppoe.getText().toString();
        String senha = passpppoe.getText().toString();

        new HttpRequests(this).alterarPppoe(parametros.ip, parametros.porta, senhaRoteador,
                usuario, senha, new EscutasAlteracao("Alterando PPPoE...") {
                    @Override
                    void aoConcluir(ResultadoOperacao resultado) {
                        parametros.internet.pppoe = usuario;
                        parametros.internet.password = senha;
                    }
                });
    }

    private void obterParametros() {
        new HttpRequests(this).obterParametros(parametros.ip, parametros.porta, senhaRoteador,
                new EscutasRoteador<Parametros>() {
                    @Override
                    public void quandoInicia() {
                        executando = true;
                    }

                    @Override
                    public void quandoErro(String erro) {
                        runOnUiThread(() -> Toast.makeText(PrincipalActivity.this, erro,
                                Toast.LENGTH_LONG).show());
                    }

                    @Override
                    public void quandoSucesso(Parametros novos) {
                        runOnUiThread(() -> {
                            parametros = novos;
                            preencherCampos();
                            Toast.makeText(PrincipalActivity.this, "Parâmetros atualizados!",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void quandoLoginExpirado() {
                        runOnUiThread(() -> Admin.irParaTelaLogin(PrincipalActivity.this));
                    }

                    @Override
                    public void quandoFinaliza() {
                        executando = false;
                        runOnUiThread(() -> swipeRefresh.setRefreshing(false));
                    }
                });
    }

    /** Tratamento comum das alterações: progresso, mensagem de sucesso ou de falha. */
    private abstract class EscutasAlteracao implements EscutasRoteador<ResultadoOperacao> {
        private final ProgressDialog progresso;

        EscutasAlteracao(String titulo) {
            progresso = criarProgresso(titulo);
        }

        /** Chamado na thread da tela quando o roteador aceitou a configuração. */
        abstract void aoConcluir(ResultadoOperacao resultado);

        @Override
        public void quandoInicia() {
            executando = true;
            runOnUiThread(progresso::show);
        }

        @Override
        public void quandoErro(String erro) {
            runOnUiThread(() -> new AlertDialog.Builder(PrincipalActivity.this)
                    .setTitle("Falha")
                    .setMessage(erro)
                    .setPositiveButton("OK", null)
                    .show());
        }

        @Override
        public void quandoSucesso(ResultadoOperacao resultado) {
            runOnUiThread(() -> {
                aoConcluir(resultado);
                if (resultado.confirmado) {
                    Toast.makeText(PrincipalActivity.this, resultado.mensagem,
                            Toast.LENGTH_LONG).show();
                } else {
                    new AlertDialog.Builder(PrincipalActivity.this)
                            .setTitle("Atenção")
                            .setMessage(resultado.mensagem)
                            .setPositiveButton("OK", null)
                            .show();
                }
            });
        }

        @Override
        public void quandoLoginExpirado() {
            runOnUiThread(() -> Admin.irParaTelaLogin(PrincipalActivity.this));
        }

        @Override
        public void quandoFinaliza() {
            executando = false;
            runOnUiThread(() -> {
                if (progresso.isShowing()) progresso.dismiss();
            });
        }
    }
}
