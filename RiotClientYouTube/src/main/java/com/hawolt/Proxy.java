package com.hawolt;

import io.javalin.Javalin;
import io.javalin.http.Handler;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Proxy {
    public static Map<Object, String> map = new HashMap<>();

    static {
        map.put(15471, "https://clientconfig.rpg.riotgames.com");
    }

    public static void create(int port, String destination) {
        if (map.containsKey(port)) return;
        map.put(port, destination);
        Javalin javalin = Javalin.create()
                .get("*", Proxy.HANDLER)
                .put("*", Proxy.HANDLER)
                .post("*", Proxy.HANDLER)
                .start(port);
    }

    public static final Handler HANDLER = context -> {
        System.out.println(context.method() + " " + context.fullUrl());
        int port = Integer.parseInt(context.fullUrl().split(":")[2].split("/")[0]);
        // construct url the riot client wants to reach
        String url = String.format(
                "%s%s%s",
                map.get(port),
                context.path(),
                context.queryString() == null ? "" : "?" + context.queryString()
        );
        System.out.println(context.method() + " " + url);
        byte[] body = context.bodyAsBytes();
        // open a connection to the url we constructed
        HttpsURLConnection request = (HttpsURLConnection) new URL(url).openConnection();
        // set correct request method
        request.setRequestMethod(context.method().toString());
        // use all headers the riot client wanted to use
        for (Map.Entry<String, String> entry : context.headerMap().entrySet()) {
            request.setRequestProperty(entry.getKey(), entry.getValue());
            System.out.println(entry.getKey() + " " + entry.getValue());
        }
        // write content
        if (body != null && body.length > 0) {
            request.setDoOutput(true);
            try (OutputStream stream = request.getOutputStream()) {
                stream.write(body);
                stream.flush();
            }
            System.out.println("> " + new String(body, StandardCharsets.UTF_8));
        }


        // read the data the riot client would normally receive
        byte[] response = request.getInputStream().readAllBytes();

        // handle encoding
        List<String> contentEncoding = request.getHeaderFields().get("Content-Encoding");
        String encoding = (contentEncoding == null || contentEncoding.isEmpty()) ? null : contentEncoding.get(0);

        String plainText = null;
        if (encoding == null) {
            plainText = new String(response, StandardCharsets.UTF_8);
        } else {
            switch (encoding) {
                case "gzip" -> {
                    byte[] bytes = new GZIPInputStream(new ByteArrayInputStream(response)).readAllBytes();
                    plainText = new String(bytes, StandardCharsets.UTF_8);
                }
            }
        }

        System.out.println("< " + plainText);

        int responseCode = request.getResponseCode();
        // return all the data to the riot client
        for (Map.Entry<String, List<String>> entry : request.getHeaderFields().entrySet()) {
            if (entry.getKey() == null || entry.getValue().isEmpty() || entry.getValue().get(0) == null) {
                continue;
            }
            context.header(entry.getKey(), entry.getValue().get(0));
        }
        context.status(responseCode);

        if (encoding == null) {
            response = plainText.getBytes(StandardCharsets.UTF_8);
        } else {
            switch (encoding) {
                case "gzip" -> {
                    ByteArrayOutputStream obj = new ByteArrayOutputStream();
                    GZIPOutputStream gzip = new GZIPOutputStream(obj);
                    gzip.write(plainText.getBytes("UTF-8"));
                    gzip.flush();
                    gzip.close();
                    response = obj.toByteArray();
                }
            }
        }
        context.header("Content-Length", String.valueOf(response.length));
        context.result(response);
    };
}