package com.br.lrgm.aplicaoprovisionamento;

import java.io.Serializable;

/** Rede sem fio lida do roteador. */
public class Wifi implements Serializable {
    private static final long serialVersionUID = 1L;

    public String ssid = "", band = "", password = "";

    /**
     * Identificação da instância da rede no roteador (ex.: "1,1,0,0,0,0").
     * É usada para alterar exatamente a rede que foi lida.
     */
    public String pilha = "";

    public boolean eh5GHz() {
        return band != null && band.replace(" ", "").equalsIgnoreCase("5GHz");
    }
}
