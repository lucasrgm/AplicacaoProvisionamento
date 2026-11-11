package com.br.lrgm.aplicaoprovisionamento;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.br.lrgm.aplicaoprovisionamento.databinding.ActivityBuscarDispositivoBinding;
import com.br.lrgm.aplicaoprovisionamento.http.ArmazenamentoLocal;
import com.br.lrgm.aplicaoprovisionamento.http.EscutasRoteador;
import com.br.lrgm.aplicaoprovisionamento.http.HttpRequests;
import com.br.lrgm.aplicaoprovisionamento.http.Parametros;
import com.br.lrgm.aplicaoprovisionamento.http.TokenJwt;

public class BuscarDispositivoActivity extends AppCompatActivity {

    public static final String EXTRA_PARAMETROS = "parametros";
    public static final String EXTRA_SENHA_ROTEADOR = "senhaRoteador";

    private ActivityBuscarDispositivoBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBuscarDispositivoBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Preenche com o último roteador usado (ou com o padrão do TL-WR840N).
        binding.enderecoIp.setText(ArmazenamentoLocal.obterIpRoteador(this,
                getString(R.string.roteador_ip_padrao)));
        binding.enderecoPorta.setText(ArmazenamentoLocal.obterPortaRoteador(this,
                getString(R.string.roteador_porta_padrao)));
        binding.senhaRoteador.setText(ArmazenamentoLocal.obterSenhaRoteador(this));

        binding.botaoBuscar.setOnClickListener(v -> obterParametros());
        binding.btnLogout.setOnClickListener(v -> Admin.logout(BuscarDispositivoActivity.this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!TokenJwt.valido(ArmazenamentoLocal.obterToken(this))) {
            Toast.makeText(this, "Sessão expirada, faça login novamente.", Toast.LENGTH_LONG).show();
            Admin.irParaTelaLogin(this);
        }
    }

    private void abrirActivityPrincipal(Parametros parametros, String senhaRoteador) {
        Intent intent = new Intent(this, PrincipalActivity.class);
        intent.putExtra(EXTRA_PARAMETROS, parametros);
        intent.putExtra(EXTRA_SENHA_ROTEADOR, senhaRoteador);
        startActivity(intent);
    }

    private void obterParametros() {
        String ip = binding.enderecoIp.getText().toString().trim();
        String porta = binding.enderecoPorta.getText().toString().trim();
        String senhaRoteador = binding.senhaRoteador.getText().toString();

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Obtendo parâmetros");
        progressDialog.setMessage("Aguarde...");
        progressDialog.setCancelable(false);

        new HttpRequests(this).obterParametros(ip, porta, senhaRoteador, new EscutasRoteador<Parametros>() {

            @Override
            public void quandoInicia() {
                runOnUiThread(progressDialog::show);
            }

            @Override
            public void quandoErro(String erro) {
                runOnUiThread(() -> Toast.makeText(BuscarDispositivoActivity.this, erro,
                        Toast.LENGTH_LONG).show());
            }

            @Override
            public void quandoSucesso(Parametros parametros) {
                ArmazenamentoLocal.salvarRoteador(BuscarDispositivoActivity.this, ip, porta, senhaRoteador);
                runOnUiThread(() -> abrirActivityPrincipal(parametros, senhaRoteador));
            }

            @Override
            public void quandoLoginExpirado() {
                runOnUiThread(() -> {
                    Toast.makeText(BuscarDispositivoActivity.this, "Login expirado, refaça!",
                            Toast.LENGTH_LONG).show();
                    Admin.irParaTelaLogin(BuscarDispositivoActivity.this);
                });
            }

            @Override
            public void quandoFinaliza() {
                runOnUiThread(() -> {
                    if (progressDialog.isShowing()) progressDialog.dismiss();
                });
            }
        });
    }
}
