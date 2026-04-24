package com.br.lrgm.aplicaoprovisionamento;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.br.lrgm.aplicaoprovisionamento.http.ArmazenamentoLocal;
import com.br.lrgm.aplicaoprovisionamento.http.HttpEscutas;
import com.br.lrgm.aplicaoprovisionamento.http.HttpRequests;
import com.br.lrgm.aplicaoprovisionamento.http.Parametros;

public class PrincipalActivity extends AppCompatActivity {



    private LinearLayout layout5g, mainLayout;

    ProgressBar
            loadingLayout;
    private Button botaoEnviarWifi1,botaoEnviarWifi2,botaoEnviarPppoe, botaoLogout;

    private EditText ssid1, ssid2, password1, password2;

    private EditText pppoe, passpppoe;

    private Parametros parametros;

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

                dialog.dismiss();

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
        this.botaoEnviarWifi1 = findViewById(R.id.botaoEnviarWifi1);
        this.botaoEnviarWifi2 = findViewById(R.id.botaoEnviarWifi2);
        this.botaoEnviarPppoe = findViewById(R.id.botaoEnviarPppoe);
        this.ssid1 = findViewById(R.id.ssid1);
        this.password1 = findViewById(R.id.password1);
        this.ssid2 = findViewById(R.id.ssid2);
        this.password2 = findViewById(R.id.password2);
        this.pppoe = findViewById(R.id.pppoe);
        this.passpppoe = findViewById(R.id.passpppoe);
        this.loadingLayout = findViewById(R.id.progressBar);
        this.botaoLogout = findViewById(R.id.btnLogout);

        //
        this.botaoLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Admin.logout(PrincipalActivity.this);
            }
        });

        this.botaoEnviarWifi1.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                alterarWifi(0);

            }
        });

        this.botaoEnviarWifi2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                alterarWifi(1);

            }
        });

        this.botaoEnviarPppoe.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                alterarPppoe();

            }
        });


        mostrarLayout(loadingLayout,true);
        mostrarLayout(mainLayout,false);

        try {
            Bundle bundle = getIntent().getExtras();
            String dados = bundle.getString("dados","");
            this.parametros=Parametros.extrair(dados);
            this.pppoe.setText(parametros.internet.pppoe);
            this.passpppoe.setText(parametros.internet.password);

            for(Wifi wifi:parametros.listaWifi){
                if (wifi.band != null && wifi.band.equalsIgnoreCase("2.4GHz")) {
                    preencherCampos(this.ssid1, this.password1, wifi.ssid, wifi.password);
                }
                else if (wifi.band != null && wifi.band.equalsIgnoreCase("5GHz")) {
                    preencherCampos(this.ssid2, this.password2, wifi.ssid, wifi.password);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();
            //throw new RuntimeException(e);
        }
        verificarAcesso();


        SwipeRefreshLayout  sw=findViewById(R.id.swiperefresh);
        sw.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                obterParametros();
                sw.setRefreshing(false);
            }
        });
    }

    private void
    verificarAcesso() {
        Handler
                handler = new Handler(Looper.getMainLooper());

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String token = ArmazenamentoLocal.obterToken(PrincipalActivity.this);
                if (token.isEmpty()){
                    Admin.logout(PrincipalActivity.this);
                }else{
                    mostrarLayout(loadingLayout,false);
                    mostrarLayout(mainLayout,true);

                }
            }
        }, 3000);


    }

    private void alterarWifi(int network){
        String ssid;
        String password;
        if (network == 0){
            ssid = this.ssid1.getText().toString();
            password = this.password1.getText().toString();
        }else{
            ssid = this.ssid2.getText().toString();
            password = this.password2.getText().toString();
        }
            ProgressDialog progressDialog = new ProgressDialog(PrincipalActivity.this);
            progressDialog.setTitle("Alterando Parametros...");
        progressDialog.setMessage("Aguarde...");
        progressDialog.setCancelable(false);

        String ip = this.parametros.ip;
            String porta = this.parametros.porta;

        new HttpRequests(this).alterarWifi(ip,porta,ssid,password,network, new HttpEscutas() {

            @Override
            public void quandoInicia() {
                executing=true;
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
                        Toast.makeText(PrincipalActivity.this, error, Toast.LENGTH_LONG).show();

                    }
                });
            }

            @Override
            public void quandoSucesso(String mensagem) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(PrincipalActivity.this, mensagem, Toast.LENGTH_LONG).show();

                    }
                });
            }

            @Override
            public void quandoLoginExpirado() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Admin.irParaTelaLogin(PrincipalActivity.this);
                    }
                });
            }

            @Override
            public void quandoFinaliza() {
                executing=false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                    }
                });
            }
        });
    }

    private void alterarPppoe(){
            String pppoe = this.pppoe.getText().toString();
            String passpppoe = this.passpppoe.getText().toString();
        ProgressDialog progressDialog = new ProgressDialog(PrincipalActivity.this);
        progressDialog.setTitle("Alterando Parametros...");
        progressDialog.setMessage("Aguarde...");
        progressDialog.setCancelable(false);

        String ip = this.parametros.ip;
        String porta = this.parametros.porta;

        new HttpRequests(this).alterarPppoe(ip,porta,pppoe,passpppoe, new HttpEscutas() {

            @Override
            public void quandoInicia() {
                executing=true;
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
                        Toast.makeText(PrincipalActivity.this, error, Toast.LENGTH_LONG).show();

                    }
                });
            }

            @Override
            public void quandoSucesso(String mensagem) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(PrincipalActivity.this, mensagem, Toast.LENGTH_LONG).show();

                    }
                });
            }

            @Override
            public void quandoLoginExpirado() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Admin.irParaTelaLogin(PrincipalActivity.this);
                    }
                });
            }

            @Override
            public void quandoFinaliza() {
                executing=false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                    }
                });
            }
        });
    }

    private void obterParametros(){
        String ip = this.parametros.ip;
        String porta = this.parametros.porta;
        ProgressDialog progressDialog = new ProgressDialog(PrincipalActivity.this);
        progressDialog.setTitle("Obtendo Parametros");
        progressDialog.setMessage("Aguarde...");
        progressDialog.setCancelable(false);

        new HttpRequests(this).obterParametros(ip, porta, new HttpEscutas() {

            @Override
            public void quandoInicia() {
                executing=true;
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
                        Toast.makeText(PrincipalActivity.this, error, Toast.LENGTH_LONG).show();

                    }
                });
            }

            @Override
            public void quandoSucesso(String mensagem) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(PrincipalActivity.this, mensagem, Toast.LENGTH_LONG).show();

                    }
                });
            }

            @Override
            public void quandoLoginExpirado() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Admin.irParaTelaLogin(PrincipalActivity.this);
                    }
                });
            }

            @Override
            public void quandoFinaliza() {
                executing=false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                    }
                });
            }
        });
    }

    boolean executing=false;

    @Override
    public void onBackPressed() {
        if(!executing)  super.onBackPressed();
    }

    @Override
    public boolean onNavigateUp() {
        return false;
    }
}