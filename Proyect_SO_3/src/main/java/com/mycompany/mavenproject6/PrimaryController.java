package com.mycompany.mavenproject6;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.input.MouseEvent;
import model.FileInfo;

import java.io.*;
import java.net.Socket;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

public class PrimaryController {

    @FXML private Button btnAddFile;
    @FXML private Button btnGetFile;
    @FXML private Button btnUpdate;
    @FXML private Button btnDeleteFile;
    @FXML private TextField txtFileName;
    @FXML private ProgressBar prgBar;
    @FXML private Label lblNotification;
    @FXML private TableView<FileInfo> tableFiles;
    @FXML private TableColumn<FileInfo, String> colNombre;
    @FXML private TableColumn<FileInfo, Long> colTamano;
    @FXML private TableColumn<FileInfo, String> colFecha;
    @FXML private Button btnRefresh;
    @FXML private TextField txtBuscar;
    @FXML private Button btnCancele;

    private final String servidorHost = "localhost";
    private final int servidorPuerto = 8000;

    private ObservableList<FileInfo> listaArchivos = FXCollections.observableArrayList();
    private Thread currentTaskThread;

    public void initialize() {
        colNombre.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colTamano.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getSize()).asObject());
        colFecha.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFormattedDate()));

        prgBar.setProgress(0);
        lblNotification.setText("BIENVENIDO AL SERVIDOR MAS SEGURO!");

        FilteredList<FileInfo> filteredData = new FilteredList<>(listaArchivos, p -> true);
        txtBuscar.textProperty().addListener((obs, oldValue, newValue) -> {
            filteredData.setPredicate(file -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return file.getName().toLowerCase().contains(lowerCaseFilter)
                        || file.getFormattedDate().toLowerCase().contains(lowerCaseFilter);
            });
        });

        SortedList<FileInfo> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableFiles.comparatorProperty());
        tableFiles.setItems(sortedData);

        actualizarListaArchivos();
    }

    @FXML
    private void HandleAddFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecciona un archivo para subir");
        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            String nombreOriginal = file.getName();
            prgBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            currentTaskThread = new Thread(() -> {
                try {
                    subirArchivo(file, nombreOriginal);
                    Platform.runLater(() -> {
                        lblNotification.setText("Archivo subido: " + nombreOriginal);
                        prgBar.setProgress(1.0);
                        actualizarListaArchivos();
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        lblNotification.setText("Error al subir archivo: " + e.getMessage());
                        prgBar.setProgress(0);
                    });
                }
            });
            currentTaskThread.start();
        }
    }

    @FXML
    private void HandleGetFile(ActionEvent event) {
        txtFileName.setText("");
        FileInfo seleccionado = tableFiles.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            lblNotification.setText("Seleccione un archivo para descargar.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar archivo descargado");
        fileChooser.setInitialFileName(seleccionado.getName());
        File destino = fileChooser.showSaveDialog(null);

        if (destino != null) {
            currentTaskThread = new Thread(() -> {
                try {
                    descargarArchivo(seleccionado.getName(), destino);
                    Platform.runLater(() -> lblNotification.setText("Archivo descargado: " + destino.getName()));
                } catch (IOException e) {
                    Platform.runLater(() -> lblNotification.setText("Error al descargar: " + e.getMessage()));
                }
            });
            currentTaskThread.start();
        }
    }

    @FXML
    private void HandleUpdate(ActionEvent event) {
        FileInfo seleccionado = tableFiles.getSelectionModel().getSelectedItem();
        String nuevoNombre = txtFileName.getText().trim();

        if (seleccionado == null || nuevoNombre.isEmpty()) {
            lblNotification.setText("Seleccione un archivo y escriba un nuevo nombre.");
            return;
        }

        currentTaskThread = new Thread(() -> {
            try {
                boolean exito = renombrarArchivo(seleccionado.getName(), nuevoNombre);
                Platform.runLater(() -> {
                    if (exito) {
                        lblNotification.setText("Archivo renombrado a: " + nuevoNombre);
                        actualizarListaArchivos();
                    } else {
                        lblNotification.setText("Error renombrando archivo.");
                    }
                });
            } catch (IOException e) {
                Platform.runLater(() -> lblNotification.setText("Error: " + e.getMessage()));
            }
        });
        currentTaskThread.start();
    }

    @FXML
    private void HandleDeleteFile(ActionEvent event) {
        txtFileName.setText("");
        prgBar.setProgress(0);
        FileInfo seleccionado = tableFiles.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            lblNotification.setText("Seleccione un archivo para eliminar.");
            return;
        }

        currentTaskThread = new Thread(() -> {
            try {
                boolean exito = eliminarArchivo(seleccionado.getName());
                Platform.runLater(() -> {
                    if (exito) {
                        lblNotification.setText("Archivo eliminado: " + seleccionado.getName());
                        actualizarListaArchivos();
                    } else {
                        lblNotification.setText("Error eliminando archivo.");
                    }
                });
            } catch (IOException e) {
                Platform.runLater(() -> lblNotification.setText("Error: " + e.getMessage()));
            }
        });
        currentTaskThread.start();
    }

    private void actualizarListaArchivos() {
        txtFileName.setText("");
        prgBar.setProgress(0);
        currentTaskThread = new Thread(() -> {
            try (Socket socket = new Socket(servidorHost, servidorPuerto);
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                 DataInputStream in = new DataInputStream(socket.getInputStream())) {

                out.writeUTF("LISTAR");
                out.flush();

                int cantidad = in.readInt();
                List<FileInfo> archivos = new ArrayList<>();
                for (int i = 0; i < cantidad; i++) {
                    String nombre = in.readUTF();
                    long size = in.readLong();
                    FileTime modified = FileTime.fromMillis(in.readLong());
                    archivos.add(new FileInfo(nombre, size, modified));
                }

                Platform.runLater(() -> listaArchivos.setAll(archivos));

            } catch (IOException e) {
                Platform.runLater(() -> lblNotification.setText("Error listando archivos: " + e.getMessage()));
            }
        });
        currentTaskThread.start();
    }
private void subirArchivo(File archivo, String nombreOriginal) throws IOException {
    Socket socket = null;
    try {
        socket = new Socket(servidorHost, servidorPuerto);
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        DataInputStream in = new DataInputStream(socket.getInputStream());
        FileInputStream fis = new FileInputStream(archivo);

        out.writeUTF("UPLOAD");
        out.writeUTF(nombreOriginal);
        long fileSize = archivo.length();
        out.writeLong(fileSize);

        byte[] buffer = new byte[100];
        int bytesRead;
        long totalSent = 0;

        while ((bytesRead = fis.read(buffer)) != -1) {
            if (Thread.currentThread().isInterrupted()) {
                fis.close();
                out.close();
                in.close();
                socket.close();

                eliminarArchivo(nombreOriginal); 
                throw new IOException("Transferencia cancelada por el usuario. Fragmentos eliminados en servidor.");
            }

            out.write(buffer, 0, bytesRead);
            totalSent += bytesRead;
            double progress = (double) totalSent / fileSize;
            double finalProgress = progress;
            Platform.runLater(() -> prgBar.setProgress(finalProgress));
        }

        out.flush();
        String respuesta = in.readUTF();
        if (!respuesta.startsWith("Subido")) {
            throw new IOException(respuesta);
        }

        fis.close();
        out.close();
        in.close();
        socket.close();

    } catch (IOException e) {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        throw e;
    }
}

 private void descargarArchivo(String nombreArchivo, File destino) throws IOException {
    try (Socket socket = new Socket(servidorHost, servidorPuerto);
         DataOutputStream out = new DataOutputStream(socket.getOutputStream());
         DataInputStream in = new DataInputStream(socket.getInputStream());
         FileOutputStream fos = new FileOutputStream(destino)) {

        out.writeUTF("DOWNLOAD");
        out.writeUTF(nombreArchivo);
        out.flush();

        String nombreRecibido = in.readUTF();
        long tamano = in.readLong();

        byte[] buffer = new byte[100];
        long totalRead = 0;
        int bytesRead;

        while (totalRead < tamano &&
                (bytesRead = in.read(buffer, 0, (int) Math.min(buffer.length, tamano - totalRead))) != -1) {
            if (Thread.currentThread().isInterrupted()) {
                fos.close(); 
                destino.delete(); 
                throw new IOException("Descarga cancelada. Archivo parcial eliminado.");
            }
            fos.write(buffer, 0, bytesRead);
            totalRead += bytesRead;
            double progress = (double) totalRead / tamano;
            double finalProgress = progress;
            Platform.runLater(() -> prgBar.setProgress(finalProgress));
        }
        fos.flush();
    } catch (IOException e) {
        destino.delete(); 
        throw e;
    }
}

    private boolean renombrarArchivo(String original, String nuevo) throws IOException {
        prgBar.setProgress(0);
        try (Socket socket = new Socket(servidorHost, servidorPuerto);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            out.writeUTF("RENOMBRAR");
            out.writeUTF(original);
            out.writeUTF(nuevo);
            out.flush();

            String respuesta = in.readUTF();
            return respuesta.startsWith("Renombrado");
        }
    }

    private boolean eliminarArchivo(String nombre) throws IOException {
        prgBar.setProgress(0);
        try (Socket socket = new Socket(servidorHost, servidorPuerto);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            out.writeUTF("ELIMINAR");
            out.writeUTF(nombre);
            out.flush();

            String respuesta = in.readUTF();
            return respuesta.startsWith("Eliminado");
        }
    }

    @FXML
    private void HandleItemSelected(MouseEvent event) {
        FileInfo selected = tableFiles.getSelectionModel().getSelectedItem();
        if (selected != null) {
            txtFileName.setText(selected.getName());
            lblNotification.setText("Archivo seleccionado: " + selected.getName());
        }
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        actualizarListaArchivos();
    }

    @FXML
    private void HandleCancele(ActionEvent event) {
        if (currentTaskThread != null && currentTaskThread.isAlive()) {
            currentTaskThread.interrupt();
            lblNotification.setText("Operación cancelada por el usuario.");
            prgBar.setProgress(0);
        } else {
            lblNotification.setText("No hay operación en curso.");
        }
    }
    
    
}
