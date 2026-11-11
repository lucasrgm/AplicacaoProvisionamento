package com.br.lrgm.aplicaoprovisionamento;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.br.lrgm.aplicaoprovisionamento.databinding.ActivityTelaLoginBinding;
import com.br.lrgm.aplicaoprovisionamento.http.ArmazenamentoLocal;
import com.br.lrgm.aplicaoprovisionamento.http.HttpEscutas;
import com.br.lrgm.aplicaoprovisionamento.http.HttpRequests;

public class LoginActivity extends AppCompatActivity {

    private static final int MINIMO_CARACTERES = 4;

    private ProgressDialog progressDialog;
    private ActivityTelaLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityTelaLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Carregando");
        progressDialog.setMessage("Aguarde...");
        progressDialog.setCancelable(false);

        binding.acessar.setOnClickListener(v -> {
            String valorUsuario = binding.user.getText().toString().trim();
            String valorSenha = binding.password.getText().toString();

            if (valorUsuario.length() >= MINIMO_CARACTERES && valorSenha.length() >= MINIMO_CARACTERES) {
                fazerLogin(valorUsuario, valorSenha);
            } else {
                Toast.makeText(this, "Acesso inválido! Usuário e senha precisam ter no mínimo "
                        + MINIMO_CARACTERES + " caracteres.", Toast.LENGTH_LONG).show();
            }
        });

        // Se já existe uma sessão válida, vai direto para a busca do roteador.
        if (!ArmazenamentoLocal.obterToken(this).isEmpty()) {
            validarSessaoExistente();
        }
    }

    private void validarSessaoExistente() {
        new HttpRequests(this).validarSessao(new HttpEscutas() {
            @Override
            public void quandoInicia() {
                runOnUiThread(() -> binding.acessar.setEnabled(false));
            }

            @Override
            public void quandoErro(String error) {
            }

            @Override
            public void quandoSucesso(String mensagem) {
                runOnUiThread(() -> irParaBuscaDoRoteador());
            }

            @Override
            public void quandoLoginExpirado() {
                runOnUiThread(() -> Toast.makeText(LoginActivity.this,
                        "Sessão expirada, faça login novamente.", Toast.LENGTH_LONG).show());
            }

            @Override
            public void quandoFinaliza() {
                runOnUiThread(() -> binding.acessar.setEnabled(true));
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
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show());
            }

            @Override
            public void quandoSucesso(String token) {
                runOnUiThread(() -> irParaBuscaDoRoteador());
            }

            @Override
            public void quandoLoginExpirado() {
            }

            @Override
            public void quandoFinaliza() {
                runOnUiThread(() -> {
                    if (progressDialog.isShowing()) progressDialog.dismiss();
                });
            }
        });
    }

    private void irParaBuscaDoRoteador() {
        if (isFinishing()) return;
        startActivity(new Intent(this, BuscarDispositivoActivity.class));
        finish();
    }
}
