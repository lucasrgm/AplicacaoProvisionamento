package com.br.lrgm.aplicaoprovisionamento;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.br.lrgm.aplicaoprovisionamento.databinding.ActivityTelaLoginBinding;
import com.br.lrgm.aplicaoprovisionamento.http.ArmazenamentoLocal;
import com.br.lrgm.aplicaoprovisionamento.http.HttpEscutas;
import com.br.lrgm.aplicaoprovisionamento.http.HttpRequests;

public class LoginActivity extends AppCompatActivity {

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityTelaLoginBinding binding = ActivityTelaLoginBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Carregando");
        progressDialog.setMessage("Aguarde...");

        EditText usuario = binding.user;
        EditText password = binding.password;
        Button acessar = binding.acessar;

        acessar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String valorUsuario = usuario.getText().toString();
                String valorSenha = password.getText().toString();

                if (valorUsuario.length() > 3 && valorSenha.length() > 3) {
                    fazerLogin(valorUsuario, valorSenha);
                } else {
                    Toast.makeText(LoginActivity.this, "Acesso inválido! Falta de caracteres, mínimo 3.", Toast.LENGTH_LONG).show();
                }
            }
        });


    }


    private void fazerLogin(String usuario, String senha) {

        new HttpRequests(this).login(usuario, senha, new HttpEscutas() {
            @Override
            public void quandoInicia() {
                progressDialog.show();
            }

            @Override
            public void quandoErro(String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();

                    }
                });
            }

            @Override
            public void quandoSucesso(String token) {
                ArmazenamentoLocal.salvarToken(LoginActivity.this, token);
                irParaTelaPrincipal();
            }

            @Override
            public void quandoLoginExpirado() {

            }

            @Override
            public void quandoFinaliza() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        progressDialog.dismiss();
                    }
                });
            }
        });
    }

    private void irParaTelaPrincipal(){
        Intent intent = new Intent(this, BuscarDispositivoActivity.class);
        startActivity(intent);
        finish();
    }
}