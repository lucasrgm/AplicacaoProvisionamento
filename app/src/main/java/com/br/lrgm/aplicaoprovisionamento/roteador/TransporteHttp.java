package com.br.lrgm.aplicaoprovisionamento.roteador;

import java.io.IOException;
import java.util.Map;

/**
 * Envio das requisições HTTP ao roteador. Separado da lógica do roteador para que ela
 * possa ser testada sem rede (ver os testes em src/test).
 */
public interface TransporteHttp {

    /** Faz um GET e devolve o corpo. Status diferente de 2xx gera IOException. */
    String get(String url, Map<String, String> cabecalhos) throws IOException;

    /** Faz um POST com o corpo em texto e devolve o corpo da resposta. */
    String post(String url, String corpo, Map<String, String> cabecalhos) throws IOException;
}
