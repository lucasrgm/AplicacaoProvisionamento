package com.br.lrgm.aplicaoprovisionamento.roteador;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** Requisições HTTP ao roteador com a biblioteca OkHttp, guardando os cookies da sessão. */
public class TransporteOkHttp implements TransporteHttp {

    private static final MediaType TIPO_FORM =
            MediaType.get("application/x-www-form-urlencoded; charset=UTF-8");

    private final OkHttpClient cliente;

    /**
     * @param socketFactory sockets da rede Wi-Fi/Ethernet do roteador, ou null para a rede
     *                      padrão do celular (ver {@code RedeLocal}).
     */
    public TransporteOkHttp(SocketFactory socketFactory) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .callTimeout(45, TimeUnit.SECONDS)
                .cookieJar(new CookiesEmMemoria());
        if (socketFactory != null) builder.socketFactory(socketFactory);
        this.cliente = builder.build();
    }

    @Override
    public String get(String url, Map<String, String> cabecalhos) throws IOException {
        Request.Builder rb = new Request.Builder().url(url).get();
        return executar(rb, cabecalhos);
    }

    @Override
    public String post(String url, String corpo, Map<String, String> cabecalhos) throws IOException {
        RequestBody body = RequestBody.create(corpo == null ? "" : corpo, TIPO_FORM);
        Request.Builder rb = new Request.Builder().url(url).post(body);
        return executar(rb, cabecalhos);
    }

    private String executar(Request.Builder rb, Map<String, String> cabecalhos) throws IOException {
        for (Map.Entry<String, String> c : cabecalhos.entrySet()) {
            rb.header(c.getKey(), c.getValue());
        }
        try (Response resposta = cliente.newCall(rb.build()).execute()) {
            if (!resposta.isSuccessful()) {
                throw new IOException("O roteador respondeu HTTP " + resposta.code());
            }
            ResponseBody corpo = resposta.body();
            return corpo == null ? "" : corpo.string();
        }
    }

    /** Guarda os cookies por host enquanto o transporte existir (uma operação). */
    static class CookiesEmMemoria implements CookieJar {
        private final Map<String, List<Cookie>> porHost = new HashMap<>();

        @Override
        public synchronized void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            List<Cookie> lista = porHost.get(url.host());
            if (lista == null) {
                lista = new ArrayList<>();
                porHost.put(url.host(), lista);
            }
            for (Cookie novo : cookies) {
                Iterator<Cookie> it = lista.iterator();
                while (it.hasNext()) {
                    if (it.next().name().equals(novo.name())) it.remove();
                }
                lista.add(novo);
            }
        }

        @Override
        public synchronized List<Cookie> loadForRequest(HttpUrl url) {
            List<Cookie> lista = porHost.get(url.host());
            return lista == null ? new ArrayList<>() : new ArrayList<>(lista);
        }
    }
}
