package com.hawolt;

import io.javalin.http.Handler;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ClientConfig {

    static {
        Proxy.map.put(15471, "https://clientconfig.rpg.riotgames.com");
    }

    public static final Handler HANDLER = context -> {
        System.out.println(context.fullUrl());
        int port = Integer.parseInt(context.fullUrl().split(":")[2].split("/")[0]);
        // construct url the riot client wants to reach
        String url = String.format("%s%s?%s", Proxy.map.get(port), context.path(), context.queryString());
        // open a connection to the url we constructed
        HttpsURLConnection request = (HttpsURLConnection) new URL(url).openConnection();
        // use all headers the riot client wanted to use
        for (Map.Entry<String, String> entry : context.headerMap().entrySet()) {
            request.setRequestProperty(entry.getKey(), entry.getValue());
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

        System.out.println(plainText);
        JSONObject plainSettings = new JSONObject(plainText);
        plainSettings = modifyLeagueSettings(plainSettings);
        plainSettings = modifyChatSettings(plainSettings);

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
            response = plainSettings.toString().getBytes(StandardCharsets.UTF_8);
        } else {
            switch (encoding) {
                case "gzip" -> {
                    ByteArrayOutputStream obj = new ByteArrayOutputStream();
                    GZIPOutputStream gzip = new GZIPOutputStream(obj);
                    gzip.write(plainSettings.toString().getBytes("UTF-8"));
                    gzip.flush();
                    gzip.close();
                    response = obj.toByteArray();
                }
            }
        }
        context.header("Content-Length", String.valueOf(response.length));
        context.result(response);
    };

    private static JSONObject modifyLeagueSettings(JSONObject plainSettings) {
        if (plainSettings.has("lol.client_settings.player_platform_edge.url")) {
            Proxy.create(16987, plainSettings.getString("lol.client_settings.player_platform_edge.url"));
            plainSettings.put("lol.client_settings.player_platform_edge.url", "http://127.0.0.1:16987");
        }
        if (plainSettings.has("lol.client_settings.league_edge.url")) {
            Proxy.create(16989, plainSettings.getString("lol.client_settings.league_edge.url"));
            plainSettings.put("lol.client_settings.league_edge.url", "http://127.0.0.1:16989");
        }
        if (plainSettings.has("lol.game_client_settings.redge_urls.public")) {
            JSONObject settings = plainSettings.getJSONObject("lol.game_client_settings.redge_urls.public");
            int start = 15472;
            for (String key : settings.keySet()) {
                String link = settings.getString(key);
                //15471
                //key = loadouts
                //link = https://euw-red.lol.sgp.pvp.net
                //link = http://127.0.0.1:15472
                settings.put(key, "http://127.0.0.1:" + start);
                Proxy.create(start += 1, link);
            }
            System.err.println(settings.toString(5));
            plainSettings.put("lol.game_client_settings.redge_urls.public", settings);
        }
        return plainSettings;
    }

    private static JSONObject modifyChatSettings(JSONObject jsonObject) {
        if (jsonObject.has("chat.allow_bad_cert.enabled")) {
            jsonObject.put("chat.allow_bad_cert.enabled", true);
        }
        if (jsonObject.has("chat.use_tls.enabled")) {
            jsonObject.put("chat.use_tls.enabled", false);
        }
        if (jsonObject.has("chat.affinities")) {
            JSONObject affinities = jsonObject.getJSONObject("chat.affinities");
            for (String affinity : affinities.keySet()) {
                affinities.put(affinity, "127.0.0.1");
            }
        }
        if (jsonObject.has("chat.port")) {
            jsonObject.put("chat.port", 52742);
        }
        return jsonObject;
    }
}
