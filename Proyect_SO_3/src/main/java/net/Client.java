package com.mycompany.mavenproject6.net;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Cliente de alto nivel que se conecta al ServidorMaestro y expone
 * operaciones CRUD sobre archivos distribuidos.
 */
public class Client implements Closeable {

    public interface ProgressCallback {
        void update(long bytesDone, long bytesTotal);
    }

    private final String host;
    private final int    port;

    private Socket           socket;
    private DataInputStream  in;
    private DataOutputStream out;

    public Client(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        connect();
    }

    private void connect() throws IOException {
        socket = new Socket(host, port);
        in  = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        out = new DataOutputStream(socket.getOutputStream());
    }

    // ----------------------------------------------------------------- API ---
    public List<String> listFiles() throws IOException {
        ensureConnected();
        out.writeUTF("LISTAR");
        out.flush();
        int n = in.readInt();
        List<String> names = new ArrayList<>(n);
        for (int i = 0; i < n; i++) names.add(in.readUTF());
        return names;
    }

    public void upload(Path file, ProgressCallback cb) throws IOException {
        ensureConnected();
        byte[] data = Files.readAllBytes(file);
        out.writeUTF("UPLOAD");
        out.writeUTF(file.getFileName().toString());
        out.writeLong(data.length);
        sendInChunks(data, cb);
        out.flush();
        in.readUTF(); // consume "✅ …"
    }

    public void download(String name, Path destDir, ProgressCallback cb) throws IOException {
        ensureConnected();
        out.writeUTF("DOWNLOAD");
        out.writeUTF(name);
        out.flush();
        String fileName = in.readUTF();
        long length = in.readLong();
        Path target = destDir.resolve(fileName);
        try (OutputStream fos = Files.newOutputStream(target)) {
            final int CHUNK = 4 * 1024;
            long rec = 0;
            byte[] buf = new byte[CHUNK];
            while (rec < length) {
                int r = in.read(buf, 0, (int) Math.min(CHUNK, length - rec));
                fos.write(buf, 0, r);
                rec += r;
                if (cb != null) cb.update(rec, length);
            }
        }
    }

    public boolean rename(String o, String n) throws IOException {
        ensureConnected();
        out.writeUTF("RENOMBRAR");
        out.writeUTF(o);
        out.writeUTF(n);
        out.flush();
        return in.readUTF().startsWith("✅");
    }

    public boolean delete(String base) throws IOException {
        ensureConnected();
        out.writeUTF("ELIMINAR");
        out.writeUTF(base);
        out.flush();
        return in.readUTF().startsWith("✅");
    }

    // ------------------------------------------------------------ helpers ----
    private void sendInChunks(byte[] data, ProgressCallback cb) throws IOException {
        final int CHUNK = 4 * 1024;
        int sent = 0;
        while (sent < data.length) {
            int len = Math.min(CHUNK, data.length - sent);
            out.write(data, sent, len);
            sent += len;
            if (cb != null) cb.update(sent, data.length);
        }
    }

    private void ensureConnected() throws IOException {
        if (socket == null || socket.isClosed()) connect();
    }

    @Override public void close() throws IOException { if (socket != null) socket.close(); }
}