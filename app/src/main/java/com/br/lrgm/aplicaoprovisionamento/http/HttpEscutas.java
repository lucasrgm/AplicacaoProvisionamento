package com.br.lrgm.aplicaoprovisionamento.http;

public interface HttpEscutas {
    abstract void quandoInicia();
    abstract void quandoErro(String error);
    abstract void quandoSucesso(String mensagem);
    abstract void quandoLoginExpirado();
    abstract void quandoFinaliza();

}
