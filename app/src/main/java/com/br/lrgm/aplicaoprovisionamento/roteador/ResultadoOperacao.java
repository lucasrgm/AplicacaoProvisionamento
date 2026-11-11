package com.br.lrgm.aplicaoprovisionamento.roteador;

/** Resultado de uma alteração de configuração no roteador. */
public class ResultadoOperacao {
    /** true quando o roteador aceitou a configuração. */
    public final boolean sucesso;
    /** true quando o valor foi lido de volta do roteador e confere com o enviado. */
    public final boolean confirmado;
    public final String mensagem;

    public ResultadoOperacao(boolean sucesso, boolean confirmado, String mensagem) {
        this.sucesso = sucesso;
        this.confirmado = confirmado;
        this.mensagem = mensagem;
    }
}
