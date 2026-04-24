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
import com.br.lrgm.aplicaoprovisionamento.http.HttpEscutas;
import com.br.lrgm.aplicaoprovisionamento.http.HttpRequests;
import com.br.lrgm.aplicaoprovisionamento.databinding.ActivityBuscarDispositivoBinding;
import com.br.lrgm.aplicaoprovisionamento.http.Parametros;

public class BuscarDispositivoActivity extends AppCompatActivity {
    private EditText enderecoIp, porta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityBuscarDispositivoBinding binding = ActivityBuscarDispositivoBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        enderecoIp = binding.enderecoIp;
        porta = binding.enderecoPorta;

        binding.botaoBuscar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                obterParametros();
            }
        });

        binding.btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Admin.logout(BuscarDispositivoActivity.this);
            }
        });
    }

    private void abrirActivityPrincipal(String mensagem) {
        Intent intent = new Intent(BuscarDispositivoActivity.this, PrincipalActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("dados", mensagem);
        intent.putExtras(bundle);
        startActivity(intent);
    }
    private void obterParametros(){
        String ip = this.enderecoIp.getText().toString();
        String porta = this.porta.getText().toString();
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Obtendo Parametros");
        progressDialog.setMessage("Aguarde...");

        new HttpRequests(this).obterParametros(ip, porta, new HttpEscutas() {

            @Override
            public void quandoInicia() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.show();
                    }
                });

            }

            @Override
            public void quandoErro(String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(BuscarDispositivoActivity.this, error, Toast.LENGTH_LONG).show();

                    }
                });
            }

            @Override
            public void quandoSucesso(String mensagem) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        abrirActivityPrincipal(mensagem);
                    }
                });
            }

            @Override
            public void quandoLoginExpirado() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(BuscarDispositivoActivity.this, "Login expirado, refaça!", Toast.LENGTH_LONG).show();
                        Admin.irParaTelaLogin(BuscarDispositivoActivity.this);
                    }
                });
            }

            @Override
            public void quandoFinaliza() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                    }
                });
            }
        });
    }
}