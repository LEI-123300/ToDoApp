package com.example.examplefeature;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

public class CurrencyService {

    public double convert(String from, String to, double amount) {
        String url = "https://api.exchangerate-api.com/v4/latest/" + from;

        HttpResponse<JsonNode> response = Unirest.get(url).asJson();
        if (response.getStatus() == 200) {
            JSONObject rates = response.getBody().getObject().getJSONObject("rates");
            double rate = rates.getDouble(to);
            return amount * rate;
        } else {
            throw new RuntimeException("Erro ao obter taxa de câmbio: " + response.getStatus());
        }
    }
}