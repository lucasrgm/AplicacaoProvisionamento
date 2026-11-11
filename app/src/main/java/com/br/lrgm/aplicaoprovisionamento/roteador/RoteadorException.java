package com.br.lrgm.aplicaoprovisionamento.roteador;

/** Erro de comunicação ou operação recusada pelo roteador. A mensagem é exibida ao técnico. */
public class RoteadorException extends Exception {
    private static final long serialVersionUID = 1L;

    public RoteadorException(String mensagem) {
        super(mensagem);
    }

    public RoteadorException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
