package com.br.lrgm.aplicaoprovisionamento;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;

import com.br.lrgm.aplicaoprovisionamento.http.ArmazenamentoLocal;

public class Admin {

    /** Apaga o token e volta para a tela de login, fechando as outras telas. */
    public static void irParaTelaLogin(Activity activity) {
        ArmazenamentoLocal.limparToken(activity);
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    public static void logout(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("Confirmação")
                .setMessage("Deseja sair da sua conta?")
                .setPositiveButton("SIM", (dialog, which) -> {
                    dialog.dismiss();
                    Admin.irParaTelaLogin(activity);
                })
                .setNegativeButton("NÃO", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }
}
