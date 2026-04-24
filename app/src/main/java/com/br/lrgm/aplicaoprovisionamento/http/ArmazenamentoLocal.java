package com.br.lrgm.aplicaoprovisionamento.http;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class ArmazenamentoLocal {
    private static String TOKENKEY = "TOKENKEY";
    public static void salvarToken(Activity context, String token){
        try {
            SharedPreferences sharedPref = context.getSharedPreferences("TOKENFILE",Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            Log.e("TOKEN", token);
            editor.putString(TOKENKEY, token);
            editor.apply();
        } catch (Exception e) {
            Log.e("TOKEN", e.getMessage());
            //throw new RuntimeException(e);
        }
    }
    public static String obterToken(Activity context){
        SharedPreferences sharedPref = context.getSharedPreferences("TOKENFILE",Context.MODE_PRIVATE);
        String token = sharedPref.getString(TOKENKEY, "");
        Log.e("TOKEN", token);
        return token;
    }
}
