package com.br.lrgm.aplicaoprovisionamento.http;

/** Retorno das operações no roteador. Os métodos são chamados fora da thread da tela. */
public interface EscutasRoteador<T> {
    void quandoInicia();

    void quandoErro(String erro);

    void quandoSucesso(T resultado);

    void quandoLoginExpirado();

    void quandoFinaliza();
}
