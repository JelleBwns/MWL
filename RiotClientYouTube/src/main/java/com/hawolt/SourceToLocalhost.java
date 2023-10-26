package com.hawolt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class SourceToLocalhost extends SocketConnection {
    public SourceToLocalhost(Socket in, Socket out) {
        super(in, out);
    }

    @Override
    public void run() {
        try (InputStream inputStream = in.getInputStream()) {
            try (OutputStream outputStream = out.getOutputStream()) {
                int code;
                while (in.isConnected() && out.isConnected() && (code = inputStream.read()) != -1) {
                    byte[] b = read(inputStream, code, inputStream.available());
                    System.out.println("< " + new String(b, StandardCharsets.UTF_8));
                    outputStream.write(b);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
