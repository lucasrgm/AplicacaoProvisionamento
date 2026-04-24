package com.br.lrgm.aplicaoprovisionamento;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

import com.br.lrgm.aplicaoprovisionamento.http.ArmazenamentoLocal;

public class Admin {

    public static void irParaTelaLogin(Activity activity){
        ArmazenamentoLocal.salvarToken(activity, "");
        Intent intent = new Intent(activity, LoginActivity.class);
        activity.startActivity(intent);
        activity.finish();
    }
    public static void logout(Activity activity){
        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
        alert.setTitle("Confirmação");
        alert.setMessage("Deseja sair da sua conta?");
        alert.setPositiveButton("SIM", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Admin.irParaTelaLogin(activity);
                dialog.dismiss();
            }
        });
        alert.setNegativeButton("NÃO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        alert.create().show();
    }


}
