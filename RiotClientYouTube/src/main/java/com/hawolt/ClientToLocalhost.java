package com.hawolt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientToLocalhost extends SocketConnection {
    public ClientToLocalhost(Socket in, Socket out) {
        super(in, out);
    }

    @Override
    public void run() {
        try (InputStream inputStream = in.getInputStream()) {
            try (OutputStream outputStream = out.getOutputStream()) {
                int code;
                while (in.isConnected() && out.isConnected() && (code = inputStream.read()) != -1) {
                    byte[] b = read(inputStream, code, inputStream.available());
                    String data = new String(b, StandardCharsets.UTF_8);
                    data = data.replaceAll("<show>(.*?)</show>", "<show>dnd</show>");
                    data = data.replaceAll("<st>(.*?)</st>", "<st>dnd</st>");
                    data = data.replaceAll("&quot;22&quot;", "&quot;501&quot;");
                    System.out.println("> " + data);
                    outputStream.write(data.getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
