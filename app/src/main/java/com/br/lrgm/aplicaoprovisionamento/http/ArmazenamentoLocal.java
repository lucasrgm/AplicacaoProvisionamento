package com.br.lrgm.aplicaoprovisionamento.http;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

/** Dados guardados no celular: token da sessão e o último roteador acessado. */
public class ArmazenamentoLocal {
    private static final String ARQUIVO = "TOKENFILE";
    private static final String TOKENKEY = "TOKENKEY";
    private static final String ROTEADOR_IP = "ROTEADOR_IP";
    private static final String ROTEADOR_PORTA = "ROTEADOR_PORTA";
    private static final String ROTEADOR_SENHA = "ROTEADOR_SENHA";

    private static SharedPreferences preferencias(Activity context) {
        return context.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE);
    }

    public static void salvarToken(Activity context, String token) {
        preferencias(context).edit().putString(TOKENKEY, token == null ? "" : token).apply();
    }

    public static String obterToken(Activity context) {
        return preferencias(context).getString(TOKENKEY, "");
    }

    public static void limparToken(Activity context) {
        preferencias(context).edit().remove(TOKENKEY).apply();
    }

    public static void salvarRoteador(Activity context, String ip, String porta, String senha) {
        preferencias(context).edit()
                .putString(ROTEADOR_IP, ip)
                .putString(ROTEADOR_PORTA, porta)
                .putString(ROTEADOR_SENHA, senha)
                .apply();
    }

    public static String obterIpRoteador(Activity context, String padrao) {
        return preferencias(context).getString(ROTEADOR_IP, padrao);
    }

    public static String obterPortaRoteador(Activity context, String padrao) {
        return preferencias(context).getString(ROTEADOR_PORTA, padrao);
    }

    public static String obterSenhaRoteador(Activity context) {
        return preferencias(context).getString(ROTEADOR_SENHA, "");
    }
}
