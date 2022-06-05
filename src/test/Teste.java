package test;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import util.Json;

public class Teste {

    public static void main(String[] args) {
        Gson json = Json.getInstance();
        var x = new X("ola");
        System.out.println(json.toJson(x));
        System.out.println(JsonParser.parseString(json.toJson(x)).getAsJsonObject().get("path").getAsString());

    }

    record X(String path) {
    }

}
