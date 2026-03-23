package com.br.lrgm.aplicaoprovisionamento;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PrincipalActivity extends AppCompatActivity {

    private ImageView imagem;
    private TextView texto;
    private EditText campoTexto;

    private void atualizarTexto(){
        String valor = this.campoTexto.getText().toString().toLowerCase().trim();

        this.texto.setText(valor);

        switch(valor){
            case "roxo":
                this.imagem.setImageResource(R.drawable.roxo);
                break;

            case "amarelo":
                this.imagem.setImageResource(R.drawable.amarelo);
                break;

            case "verde":
                this.imagem.setImageResource(R.drawable.verde);
                break;

            default:
                Toast.makeText(this, "Cor inválida!", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal);

        this.imagem = (ImageView) this.findViewById(R.id.imagemRoxa);
        this.texto = (TextView) this.findViewById(R.id.txt_hello);
        this.campoTexto = (EditText) this.findViewById(R.id.campoTexto);

        Button btn2 = (Button) this.findViewById(R.id.button3);

        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                atualizarTexto();


            }
        });

//        String cor = "roxo";
//        switch(cor){
//            case "roxo":
//                //muda pra roxo;
//                break;
//            case "amarelo":
//                //muda pra amarelo
//                break;
//            default:
//                break;
//        }
    }
}