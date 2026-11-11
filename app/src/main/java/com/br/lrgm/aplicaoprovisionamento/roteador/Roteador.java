package com.br.lrgm.aplicaoprovisionamento.roteador;

import com.br.lrgm.aplicaoprovisionamento.Internet;
import com.br.lrgm.aplicaoprovisionamento.Wifi;
import com.br.lrgm.aplicaoprovisionamento.http.Parametros;

import java.util.List;

/**
 * Operações esperadas de um roteador. Cada modelo suportado tem a sua implementação
 * (hoje, apenas o TP-Link TL-WR840N), o que permite incluir outros modelos depois.
 */
public interface Roteador {

    /** Lê as redes Wi-Fi e os dados de PPPoE em uma única sessão. */
    Parametros obterParametros() throws RoteadorException;

    List<Wifi> obterWifi() throws RoteadorException;

    Internet obterInternet() throws RoteadorException;

    /**
     * Altera nome e senha de uma rede Wi-Fi.
     *
     * @param pilha identificação da rede lida em {@link Wifi#pilha}
     */
    ResultadoOperacao alterarWifi(String pilha, String ssid, String senha) throws RoteadorException;

    ResultadoOperacao alterarPppoe(String usuario, String senha) throws RoteadorException;
}
