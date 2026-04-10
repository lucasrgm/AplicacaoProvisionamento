package com.br.lrgm.aplicaoprovisionamento;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.br.lrgm.aplicaoprovisionamento.http.HttpRequests;

public class PrincipalActivity extends AppCompatActivity {

    private int contador;

    private LinearLayout layout5g, mainLayout;

    ProgressBar
            loadingLayout;
    private Button botaoEnviar;

    private EditText ssid1, ssid2, password1, password2;

    private EditText pppoe, passpppoe;

    private void preencherCampos(EditText campo1, EditText campo2, String valorCampo1, String valorCampo2) {
        campo1.setText(valorCampo1);
        campo2.setText(valorCampo2);
    }

    private void preencherCampos(EditText campo1, String valorCampo1) {
        campo1.setText(valorCampo1);

    }

    private void mostrarLayout(View v, boolean mostrar) {
        v.setVisibility(mostrar ? View.VISIBLE : View.GONE);
    }

    private void mostrarErro(String mensagem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(mensagem);
        builder.setTitle("Atenção!");
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                PrincipalActivity.this.finish();
            }
        });
        builder.setPositiveButton("Tentar novamente", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                contador++;
                dialog.dismiss();
                simular();
            }
        });

        builder.create().show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal);
        this.mainLayout = findViewById(R.id.mainLayout);
        this.layout5g = findViewById(R.id.layout_wifi2);
        this.botaoEnviar = findViewById(R.id.botaoEnviar);
        this.ssid1 = findViewById(R.id.ssid1);
        this.password1 = findViewById(R.id.password1);
        this.ssid2 = findViewById(R.id.ssid2);
        this.password2 = findViewById(R.id.password2);
        this.pppoe = findViewById(R.id.pppoe);
        this.passpppoe = findViewById(R.id.passpppoe);
        this.loadingLayout = findViewById(R.id.progressBar);

        //

        this.botaoEnviar.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                new HttpRequests(PrincipalActivity.this).login("Lucas", "12345");

            }
        });

        mostrarLayout(loadingLayout,false);
        mostrarLayout(mainLayout,true);

    }

    private void
    simular() {
        Handler
                handler = new Handler(Looper.getMainLooper());

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (contador < 3){
                    mostrarLayout(loadingLayout,true);
                    mostrarLayout(mainLayout,false);
                    mostrarErro("Erro,tente novamente! Tentativas: "+contador);
                } else{
                    contador=0;
                    mostrarLayout(loadingLayout,false);
                    mostrarLayout(mainLayout,true);
                }
            }
        }, 3000);
    }
}