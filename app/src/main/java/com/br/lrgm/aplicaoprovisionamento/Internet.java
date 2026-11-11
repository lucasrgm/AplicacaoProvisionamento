package com.br.lrgm.aplicaoprovisionamento;

import java.io.Serializable;

/** Dados de acesso à internet (WAN) lidos do roteador. */
public class Internet implements Serializable {
    private static final long serialVersionUID = 1L;

    public String transportType = "", pppoe = "", password = "";

    public String getTransportType() {
        return transportType;
    }

    public String getPppoe() {
        return pppoe;
    }

    public String getPassword() {
        return password;
    }
}
