package com.mycompany.mavenproject6;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ServidorEsclavo3 {

    public static void main(String[] args) {
        int puerto = 8003; // Cambiar por 8001, 8002 o 8003
        String nombreDirectorio = "esclavo3"; // Cambiar por "esclavo1", "esclavo2" o "esclavo3"

        try (ServerSocket server = new ServerSocket(puerto)) {
            System.out.println("🟡 Esclavo esperando conexión en puerto " + puerto);

            Socket maestro = server.accept();
            System.out.println("🔗 Conectado al maestro");

            DataInputStream entrada = new DataInputStream(new BufferedInputStream(maestro.getInputStream()));
            DataOutputStream salida = new DataOutputStream(new BufferedOutputStream(maestro.getOutputStream()));

            // Enviar confirmación al maestro
            salida.writeUTF("Esclavo listo");
            salida.flush();

            while (true) {
                System.out.println("⌛ Esperando orden...");

                String orden = entrada.readUTF();
                System.out.println("📨 Orden recibida: " + orden);

                Path directorio = Paths.get(nombreDirectorio, "archivos_guardados");
                Files.createDirectories(directorio);

                switch (orden.toLowerCase()) {

                    case "archivo": {
                        String nombreArchivo = entrada.readUTF();
                        long tamano = entrada.readLong();

                        byte[] contenido = new byte[(int) tamano];
                        entrada.readFully(contenido);

                        Path ruta = directorio.resolve(nombreArchivo);
                        Files.write(ruta, contenido);

                        System.out.println("📥 Archivo guardado: " + nombreArchivo + " (" + tamano + " bytes)");
                        salida.writeUTF("Archivo recibido exitosamente.");
                        salida.flush();
                        break;
                    }

                    case "solicitar": {
                        String nombreArchivoSolicitado = entrada.readUTF();
                        Path rutaArchivo = directorio.resolve(nombreArchivoSolicitado);

                        if (Files.exists(rutaArchivo)) {
                            byte[] contenido = Files.readAllBytes(rutaArchivo);
                            salida.writeLong(contenido.length);
                            salida.write(contenido);
                            salida.flush();

                            System.out.println("📤 Archivo enviado: " + nombreArchivoSolicitado);
                        } else {
                            salida.writeLong(0);
                            salida.flush();
                            System.out.println("⚠️ Archivo no encontrado: " + nombreArchivoSolicitado);
                        }
                        break;
                    }

           case "listar": {
    File[] archivos = directorio.toFile().listFiles((dir, name) -> name.matches(".*\\.part\\d+$"));
    if (archivos != null) {
        salida.writeInt(archivos.length);
        for (File archivo : archivos) {
            salida.writeUTF(archivo.getName());
            salida.writeLong(archivo.length());
            salida.writeLong(archivo.lastModified()); // milisegundos
        }
    } else {
        salida.writeInt(0);
    }
    salida.flush();
    System.out.println("📃 Lista de archivos enviada.");
    break;
}

                    case "renombrar": {
    String originalBase = entrada.readUTF();
    String nuevoBase = entrada.readUTF();

    File[] archivos = directorio.toFile().listFiles();
    boolean renombrado = false;

    if (archivos != null) {
        for (File archivo : archivos) {
            String nombre = archivo.getName();

            if (nombre.startsWith(originalBase) && nombre.matches(".*\\.part\\d+$")) {
                // Extraer la extensión antes de .partX
                int indexPart = nombre.lastIndexOf(".part");
                int indexPunto = nombre.lastIndexOf('.', indexPart - 1);

                String extension = "";
                if (indexPunto != -1) {
                    extension = nombre.substring(indexPunto + 1, indexPart);
                }

                String sufijo = nombre.substring(indexPart); // .partX
                String nuevoNombre = nuevoBase + "." + extension + sufijo;

                Path origen = archivo.toPath();
                Path destino = directorio.resolve(nuevoNombre);
                Files.move(origen, destino);
                System.out.println("✏️ Renombrado: " + nombre + " → " + nuevoNombre);
                renombrado = true;
            }
        }
    }

    salida.writeUTF(renombrado ? "✅ Partes renombradas correctamente." : "❌ No se encontraron partes para renombrar.");
    salida.flush();
    break;
}  

                    case "eliminar": {
                        String baseEliminar = entrada.readUTF();

                        File[] archivos = directorio.toFile().listFiles();
                        boolean eliminado = false;

                        if (archivos != null) {
                            for (File archivo : archivos) {
                                String nombre = archivo.getName();
                                if (nombre.startsWith(baseEliminar) && nombre.matches(".*\\.part\\d+$")) {
                                    if (archivo.delete()) {
                                        System.out.println("🗑️ Eliminado: " + nombre);
                                        eliminado = true;
                                    } else {
                                        System.out.println("⚠️ No se pudo eliminar: " + nombre);
                                    }
                                }
                            }
                        }
                        salida.writeUTF(eliminado ? "✅ Partes eliminadas correctamente." : "❌ No se encontraron partes para eliminar.");
                        salida.flush();
                        break;
                    }

                    default:
                        System.out.println("⚠️ Orden desconocida: " + orden);
                        salida.writeUTF("Orden no reconocida.");
                        salida.flush();
                        break;
                }
            }

        } catch (IOException e) {
            System.err.println("❌ Error en esclavo: " + e.getMessage());
        }
    }
}
