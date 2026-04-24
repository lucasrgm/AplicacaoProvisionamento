package com.br.lrgm.aplicaoprovisionamento.http;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.br.lrgm.aplicaoprovisionamento.R;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpRequests {
    private String  endpoint;
    private Activity context;
    String  token;
    public HttpRequests(Activity context) {
        this.context = context;
        this.endpoint=context.getString(R.string.endpoint_url);
        this.token = ArmazenamentoLocal.obterToken(context);
    }
    public void login(String username, String password,HttpEscutas escutas) {

        ExecutorService executorService= Executors.newSingleThreadExecutor();

        escutas.quandoInicia();

        executorService.execute(()-> {


            OkHttpClient Client = new OkHttpClient();

            try {
                RequestBody body = RequestBody.create("{\"username\":\"" + username + "\", \"password\":\"" + password + "\"}", MediaType.get("application/json"));
                Request request = new Request.Builder().url(endpoint + "/login")
                        //.addHeader("token_autenticacao",this.token)
                        .post(body)
                        .build();
                try (Response response = Client.newCall(request).execute()) {
                    String resultado = response.body().string();
                    Log.i("retornohttp", resultado + "");
                    if(resultado.contains("error")){

                        escutas.quandoErro(resultado);
                    }else{
                        //extrair o token
                        try {
                            org.json.JSONObject json = new org.json.JSONObject(resultado);

                            if (json.has("token")) {
                                this.token = json.getString("token");
                                Log.i("TOKEN", this.token);
                                escutas.quandoSucesso(this.token);
                            } else {
                                Log.e("TOKEN", "Token não encontrado na resposta");
                                throw new Exception("Token não encontrado. Não foi possível autenticar!");
                            }

                        } catch (Exception e) {
                            Log.e("TOKEN", "Erro ao extrair token", e);
                            escutas.quandoErro(e.getMessage());
                        }


                    }
                }
            } catch (Exception e) {
                Log.e("TOKEN", e.getMessage());
                escutas.quandoErro("Erro interno, tente novamente!");
                //throw new RuntimeException(e);
            }

            escutas.quandoFinaliza();

        });
    }

    private void filtroRequest(OkHttpClient client, Request request, HttpEscutas escutas)throws Exception{
        try (Response response = client.newCall(request).execute()) {
            if(response.code()==401) {
                escutas.quandoLoginExpirado();
            }else{

                String resultado = response.body().string();
                Log.i("retornohttp_parametros", resultado + "");
                escutas.quandoSucesso(resultado);
            }
        }
    }
    public void obterParametros(String ip, String porta, HttpEscutas escutas){

        escutas.quandoInicia();

       if(!validarAcesso(ip,porta,escutas)) return;

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        executorService.execute(() -> {

            OkHttpClient client = new OkHttpClient();

            try {
                Request request = new Request.Builder()
                        .url(endpoint + "/obterParametros?ip="+ip+"&porta="+porta)
                        .addHeader("token_autenticacao", this.token)
                        .get()
                        .build();

                    filtroRequest(client, request, escutas);

            } catch (Exception e) {
                escutas.quandoErro(e.getMessage());
                //throw new RuntimeException(e);
            }
            escutas.quandoFinaliza();
        });
    }
    private boolean validarAcesso(String ip, String porta,HttpEscutas   escutas){
        if(!ip.matches("^\\d+.\\d+.\\d+.\\d+")){

            escutas.quandoErro("Endereço ip inválido!");
            escutas.quandoFinaliza();
            return  false;
        }

        if(!porta.matches("^\\d{0,5}")){

            escutas.quandoErro("Porta inválida!");
            escutas.quandoFinaliza();
            return  false;
        }

        if (this.token.isEmpty()) {
            Log.e("http", "Token não definido!");
            escutas.quandoLoginExpirado();
            escutas.quandoFinaliza();
            return  false;
        }

        return true;
    }
    public void alterarWifi(String  ip,String porta,String ssid, String password,int network, HttpEscutas escutas){

        escutas.quandoInicia();

        if(!validarAcesso(ip,porta,escutas))    return;

        if (ssid.isEmpty()){
            escutas.quandoErro("SSID não pode ser vazio!");
            escutas.quandoFinaliza();
            return;
        }

        if (password.isEmpty() || password.length() <  8){
            escutas.quandoErro("Senha precisa ter no mínimo 8 dígitos!");
            escutas.quandoFinaliza();
            return;
        }

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        executorService.execute(() -> {

            OkHttpClient client = new OkHttpClient.Builder().connectTimeout(36000,TimeUnit.MILLISECONDS).callTimeout(36000, TimeUnit.MILLISECONDS).readTimeout(36000, TimeUnit.MILLISECONDS).build();

            try {
                String json = "{ \"network\": \"" + network + "\" , \"ssid\": \"" + ssid + "\", \"senha\": \"" + password + "\", \"ip\": \"" + ip + "\", \"porta\": \"" + porta + "\" }";

                RequestBody body = RequestBody.create(
                        json,
                        MediaType.get("application/json")
                );

                Request request = new Request.Builder()
                        .url(endpoint + "/alterarWifi")
                        .addHeader("token_autenticacao", this.token)
                        .post(body)
                        .build();

                filtroRequest(client, request, escutas);

            } catch (Exception e) {
                escutas.quandoErro(e.getMessage());
                //throw new RuntimeException(e);
            }
            escutas.quandoFinaliza();
        });
    }

    public void alterarPppoe(String ip, String porta, String username, String password, HttpEscutas escutas){

        escutas.quandoInicia();

        if(!validarAcesso(ip,porta,escutas)) return;

        if (username.isEmpty()){
            escutas.quandoErro("Usuário não pode ser vazio!");
            escutas.quandoFinaliza();
            return;
        }

        if (password.isEmpty() || password.length() <  8){
            escutas.quandoErro("Senha precisa ter no mínimo 8 dígitos!");
            escutas.quandoFinaliza();
            return;
        }

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        executorService.execute(() -> {

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(36000,TimeUnit.MILLISECONDS)
                    .callTimeout(36000, TimeUnit.MILLISECONDS)
                    .readTimeout(36000, TimeUnit.MILLISECONDS)
                    .build();

            try {
                String json = "{ \"usuario\": \"" + username + "\", \"senha\": \"" + password + "\", \"ip\": \"" + ip + "\", \"porta\": \"" + porta + "\" }";

                RequestBody body = RequestBody.create(
                        json,
                        MediaType.get("application/json")
                );

                Request request = new Request.Builder()
                        .url(endpoint + "/alterarPppoe")
                        .addHeader("token_autenticacao", this.token)
                        .post(body)
                        .build();

                filtroRequest(client, request, escutas);

            } catch (Exception e) {
                escutas.quandoErro(e.getMessage());
            }

            escutas.quandoFinaliza();
        });
    }
}
