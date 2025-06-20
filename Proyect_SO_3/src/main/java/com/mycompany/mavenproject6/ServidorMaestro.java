package com.mycompany.mavenproject6;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.*;
import model.FileInfo;

public class ServidorMaestro {

    private static final int PUERTO_MAESTRO = 8000;
    private static final int NUM_ESCLAVOS = 3;

    private static final String[] HOSTS = {"localhost", "localhost", "localhost"};
    private static final int[] PUERTOS = {8001, 8002, 8003};

    private Thread currentTaskThread;

    private static final List<Socket> esclavos = new ArrayList<>(NUM_ESCLAVOS);

    private static final ExecutorService pool = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        conectarConEsclavos();
        try (ServerSocket ss = new ServerSocket(PUERTO_MAESTRO)) {
            log("Maestro escuchando en %d", PUERTO_MAESTRO);
            while (true) {
                Socket cliente = ss.accept();
                pool.execute(() -> manejarCliente(cliente));
            }
        } catch (IOException ex) {
            logErr("Error maestro: %s", ex.getMessage());
        }
    }

    // ------------------------------------------------ conexión esclavos ------
    private static void conectarConEsclavos() {
        esclavos.clear();
        for (int i = 0; i < NUM_ESCLAVOS; i++) {
            esclavos.add(null);
        }
        for (int i = 0; i < NUM_ESCLAVOS; i++) {
            reconectarEsclavo(i);
        }
    }

    private static void reconectarEsclavo(int idx) {
        try {
            Socket s = new Socket(HOSTS[idx], PUERTOS[idx]);
            esclavos.set(idx, s);
            DataInputStream in = new DataInputStream(s.getInputStream());
            log(" Esclavo %d dice: %s", idx + 1, in.readUTF());
        } catch (IOException ex) {
            esclavos.set(idx, null);
            logErr(" No se reconectó esclavo %d: %s", idx + 1, ex.getMessage());
        }
    }

    private static void manejarCliente(Socket cli) {
        try (cli; DataInputStream in = new DataInputStream(cli.getInputStream()); DataOutputStream out = new DataOutputStream(cli.getOutputStream())) {
            while (true) {
                String cmd;
                try {
                    cmd = in.readUTF();
                } catch (EOFException e) {
                    return;
                }
                switch (cmd.toUpperCase()) {
                    case "UPLOAD":
                        handleUpload(in, out);
                        break;
                    case "DOWNLOAD":
                        handleDownload(in, out);
                        break;
                    case "LISTAR":
                        handleListar(out);
                        break;
                    case "RENOMBRAR":
                        handleRenombrar(in, out);
                        break;
                    case "ELIMINAR":
                        handleEliminar(in, out);
                        break;
                    default:
                        out.writeUTF("Cmd no válido");
                        out.flush();
                        break;
                }
            }
        } catch (IOException ex) {
            logErr("Cliente error: %s", ex.getMessage());
        }
    }

    private static void handleUpload(DataInputStream in, DataOutputStream out) throws IOException {
        String nombre = in.readUTF();
        long len = in.readLong();
        byte[] data = in.readNBytes((int) len);

        int slice = data.length / NUM_ESCLAVOS;
        sendToSlave(0, nombre + ".part1", Arrays.copyOfRange(data, 0, slice));
        sendToSlave(1, nombre + ".part2", Arrays.copyOfRange(data, slice, 2 * slice));
        sendToSlave(2, nombre + ".part3", Arrays.copyOfRange(data, 2 * slice, data.length));

        out.writeUTF("Subido");
        out.flush();
    }

    private static void handleDownload(DataInputStream in, DataOutputStream out) throws IOException {
        String base = in.readUTF();
        byte[] p1 = requestFromSlave(0, base + ".part1");
        byte[] p2 = requestFromSlave(1, base + ".part2");
        byte[] p3 = requestFromSlave(2, base + ".part3");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.writeBytes(p1);
        baos.writeBytes(p2);
        baos.writeBytes(p3);
        byte[] full = baos.toByteArray();
        out.writeUTF(base);
        out.writeLong(full.length);
        out.write(full);
        out.flush();
    }
private static void handleListar(DataOutputStream out) throws IOException {
    Map<String, FileInfo> archivos = new TreeMap<>();

    for (int i = 0; i < NUM_ESCLAVOS; i++) {
        Socket s = ensureSlave(i);
        if (s == null) continue;

        try {
            synchronized (s) {
                DataOutputStream os = new DataOutputStream(s.getOutputStream());
                DataInputStream is = new DataInputStream(s.getInputStream());

                os.writeUTF("listar");
                os.flush();

                int cantidad = is.readInt();
                for (int j = 0; j < cantidad; j++) {
                    String nombre = is.readUTF();
                    long tamano = is.readLong();
                    long fecha = is.readLong();

                    if (nombre.contains(".part")) {
                        String base = nombre.substring(0, nombre.lastIndexOf(".part"));
                        archivos.putIfAbsent(base, new FileInfo(base, 0, FileTime.fromMillis(fecha)));
                        archivos.get(base).setSize((long) (archivos.get(base).getSize() + tamano));
                    }
                }
            }
        } catch (IOException e) {
            logErr("Fallo esclavo %d: %s", i + 1, e.getMessage());
        }
    }

    out.writeInt(archivos.size());
    for (FileInfo info : archivos.values()) {
        out.writeUTF(info.getName());
        out.writeLong((long) info.getSize());
        out.writeLong(info.getLastModified().toMillis());
    }
    out.flush();
}



    private static void handleRenombrar(DataInputStream in, DataOutputStream out) throws IOException {
        String o = in.readUTF();
        String n = in.readUTF();
        boolean ok = true;
        for (int i = 0; i < NUM_ESCLAVOS; i++) {
            ok &= renameOnSlave(i, o, n);
        }
        out.writeUTF(ok ? "Renombrado" : "Falló renombrar");
        out.flush();
    }

    private static void handleEliminar(DataInputStream in, DataOutputStream out) throws IOException {
        String base = in.readUTF();
        boolean ok = true;
        for (int i = 0; i < NUM_ESCLAVOS; i++) {
            ok &= deleteOnSlave(i, base);
        }
        out.writeUTF(ok ? "Eliminado" : "Falló eliminar");
        out.flush();
    }

    private static synchronized void sendToSlave(int idx, String name, byte[] data) {
        try {
            Socket s = ensureSlave(idx);
            if (s == null) {
                return;
            }
            DataOutputStream os = new DataOutputStream(s.getOutputStream());
            DataInputStream is = new DataInputStream(s.getInputStream());
            os.writeUTF("archivo");
            os.writeUTF(name);
            os.writeLong(data.length);
            os.write(data);
            os.flush();
            is.readUTF(); 
        } catch (IOException ex) {
            logErr("Send esclavo %d: %s", idx + 1, ex.getMessage());
        }
    }

    private static synchronized byte[] requestFromSlave(int idx, String name) {
        try {
            Socket s = ensureSlave(idx);
            if (s == null) {
                return new byte[0];
            }
            DataOutputStream os = new DataOutputStream(s.getOutputStream());
            DataInputStream is = new DataInputStream(s.getInputStream());
            os.writeUTF("solicitar");
            os.writeUTF(name);
            os.flush();
            long len = is.readLong();
            return is.readNBytes((int) len);
        } catch (IOException ex) {
            logErr("Request esclavo %d: %s", idx + 1, ex.getMessage());
            return new byte[0];
        }
    }

    private static synchronized boolean renameOnSlave(int idx, String o, String n) {
        try {
            Socket s = ensureSlave(idx);
            if (s == null) {
                return false;
            }
            DataOutputStream os = new DataOutputStream(s.getOutputStream());
            DataInputStream is = new DataInputStream(s.getInputStream());
            os.writeUTF("renombrar");
            os.writeUTF(o);
            os.writeUTF(n);
            os.flush();
            return is.readUTF().startsWith("Partes renombradas correctamente");
        } catch (IOException ex) {
            logErr("Rename esclavo %d: %s", idx + 1, ex.getMessage());
            return false;
        }
    }

    private static synchronized boolean deleteOnSlave(int idx, String b) {
        try {
            Socket s = ensureSlave(idx);
            if (s == null) {
                return false;
            }
            DataOutputStream os = new DataOutputStream(s.getOutputStream());
            DataInputStream is = new DataInputStream(s.getInputStream());
            os.writeUTF("eliminar");
            os.writeUTF(b);
            os.flush();
            return is.readUTF().startsWith("Partes");
        } catch (IOException ex) {
            logErr("eliminar esclavo %d: %s", idx + 1, ex.getMessage());
            return false;
        }
    }

    private static Socket ensureSlave(int i) {
        Socket s = esclavos.get(i);
        if (s == null || s.isClosed()) {
            reconectarEsclavo(i);
            s = esclavos.get(i);
        }
        return s;
    }

    private static void log(String fmt, Object... args) {
        System.out.printf("[INFO] " + fmt + "%n", args);
    }

    private static void logErr(String fmt, Object... args) {
        System.err.printf("[ERROR] " + fmt + "%n", args);
    }
}
