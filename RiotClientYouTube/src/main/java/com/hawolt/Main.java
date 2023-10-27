package com.hawolt;

import io.javalin.Javalin;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        Javalin javalin = Javalin.create()
                .get("*", ClientConfig.HANDLER)
                .start(15471);
        ProcessBuilder builder = new ProcessBuilder();
        builder.command(
                "C:\\Riot Games\\Riot Client\\RiotClientServices.exe",
                "--launch-product=league_of_legends",
                "--launch-patchline=live",
                "--client-config-url=http://localhost:15471"
        );
        Process process = builder.start();

        System.out.println("start socket handler");
        // create an instance of our socket manager
        SocketHandler socketHandler = new SocketHandler();
        // start the socket manager and wait for league client to connect and initialize proxy
        socketHandler.run();
    }
}