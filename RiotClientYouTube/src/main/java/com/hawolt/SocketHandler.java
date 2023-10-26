package com.hawolt;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketHandler implements Runnable {

    private ExecutorService service = Executors.newCachedThreadPool();

    private ServerSocket serverSocket;

    @Override
    public void run() {
        try {
            // incoming connection from the league client
            serverSocket = new ServerSocket(52741);
            System.out.println("waiting for connection");
            Socket in = serverSocket.accept();
            System.out.println("connection accepted");

            // outgoing connection to riot games
            Socket out = SSLSocketFactory.getDefault().createSocket("euw1.chat.si.riotgames.com", 5223);
            System.out.println("outgoing connection created");

            // handle both connection ways client <> us <> riot
            service.execute(new SourceToLocalhost(out, in));
            service.execute(new ClientToLocalhost(in, out));

            System.out.println("connection proxy initialized");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
