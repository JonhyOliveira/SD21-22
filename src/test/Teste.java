package test;

import com.google.gson.Gson;
import com.google.gson.JsonParser;

public class Teste {

    public static void main(String[] args) {
        Gson json = new Gson();
        var x = new X("ola");
        System.out.println(json.toJson(x));
        System.out.println(JsonParser.parseString(json.toJson(x)).getAsJsonObject().get("path").getAsString());

    }

    record X(String path) {}

}
