package com.br.lrgm.aplicaoprovisionamento.http;

// {"data":{"wifi":{"Wifi1":{"SSID":"USUARO po","band":"2.4GHz"},"Wifi2":{"SSID":"lucas","band":"5GHz"}},"internet":{"transportType":"PPPoE","username":"mnbmnabsmnd","password":"123456wsd"}}}

import android.util.Log;

import androidx.annotation.NonNull;

import com.br.lrgm.aplicaoprovisionamento.Internet;
import com.br.lrgm.aplicaoprovisionamento.Wifi;

import java.util.ArrayList;
import java.util.List;



public class Parametros {
    public List <Wifi> listaWifi=new ArrayList<>();
    public  Internet internet = new Internet();
    public String ip, porta;
    public Internet getInternet() {
        return internet;
    }

    public static Parametros extrair(String data) throws Exception {
        Parametros parametros = new Parametros();
        try {
            org.json.JSONObject json = new org.json.JSONObject(data);

            if (json.has("data")) {
                org.json.JSONObject dados = json.getJSONObject("data");
                org.json.JSONObject internet = dados.getJSONObject("internet");
                org.json.JSONObject wifi = dados.getJSONObject("wifi");

                String ip = dados.getString("ip");
                String porta = dados.getString("porta");
                parametros.ip = ip;
                parametros.porta = porta;

                Wifi wifi1 = new Wifi();
                wifi1.password = wifi.getJSONObject("Wifi1").getString("password");
                wifi1.band = wifi.getJSONObject("Wifi1").getString("band");
                wifi1.ssid = wifi.getJSONObject("Wifi1").getString("SSID");

                Wifi wifi2 = new Wifi();
                wifi2.password = wifi.getJSONObject("Wifi2").getString("password");
                wifi2.band = wifi.getJSONObject("Wifi2").getString("band");
                wifi2.ssid = wifi.getJSONObject("Wifi2").getString("SSID");

                parametros.listaWifi.add(wifi1);
                parametros.listaWifi.add(wifi2);

                parametros.internet.pppoe = internet.getString("username");
                parametros.internet.transportType = internet.getString("transportType");
                parametros.internet.password = internet.getString("password");

                return parametros;
            } else {
                throw new Exception("Dados não encontrados!");
            }

        } catch (Exception e) {
            Log.e("PARAMETROS", e.getMessage());
            throw new Exception("Erro ao extrair dados");
        }
    }



}
