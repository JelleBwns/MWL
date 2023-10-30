package com.hawolt;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public abstract class SocketConnection implements Runnable {
    protected Socket in, out;

    public SocketConnection(Socket in, Socket out) {
        this.out = out;
        this.in = in;
    }

    protected byte[] read(InputStream stream, int code, int available) throws IOException {
        byte[] b = new byte[available];
        stream.read(b, 0, b.length);
        byte[] raw = new byte[b.length + 1];
        raw[0] = (byte) code;
        System.arraycopy(b, 0, raw, 1, b.length);
        return raw;
    }
}
