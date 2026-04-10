package com.br.lrgm.aplicaoprovisionamento.http;

import android.content.Context;
import android.util.Log;

import com.br.lrgm.aplicaoprovisionamento.R;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpRequests {
    private String  endpoint;
    private Context context;
    String  token;
    public HttpRequests(Context context) {
        this.context = context;
        this.endpoint=context.getString(R.string.endpoint_url);

    }
    public void login(String username, String password) {

        ExecutorService executorService= Executors.newSingleThreadExecutor();

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
                }
            } catch (Exception e) {

                throw new RuntimeException(e);
            }

        });
    }
    private void obterParametros(){

    }
    private void alterarWifi(){

    }
    private void alterarPppoe(){

    }
}
