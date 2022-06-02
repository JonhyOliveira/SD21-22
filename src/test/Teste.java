package test;

import com.google.gson.Gson;

public class Teste {

    public static void main(String[] args) {
        Gson json = new Gson();
        var x = new X("ola");
        System.out.println(json.toJson(x));
        System.out.printf("{\"path\":\"%s\"}%n", x.path());

        System.out.println("ola".toUpperCase());

    }

    record X(String path) {}

}
