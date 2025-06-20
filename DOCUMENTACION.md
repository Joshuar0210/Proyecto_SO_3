## 3. Protocolo de Comunicación y Diagrama

### Protocolo de Comunicación

El sistema utiliza sockets TCP para establecer la comunicación entre el cliente, el servidor maestro y los servidores esclavos. Todos los mensajes se envían mediante `DataInputStream` y `DataOutputStream`.

#### Cliente → Servidor Maestro

| Comando     | Descripción                                      |
|-------------|--------------------------------------------------|
| `UPLOAD`    | Envía archivo completo para ser fragmentado      |
| `DOWNLOAD`  | Solicita la reconstrucción y descarga del archivo|
| `LISTAR`    | Solicita el listado completo de archivos         |
| `RENOMBRAR` | Solicita cambio de nombre del archivo            |
| `ELIMINAR`  | Solicita eliminación del archivo                 |

#### Servidor Maestro → Servidores Esclavos

| Comando       | Descripción                                 |
|---------------|---------------------------------------------|
| `archivo`     | Envía fragmento de archivo (`.partX`)       |
| `solicitar`   | Solicita fragmento para reconstrucción      |
| `listar`      | Solicita lista de fragmentos disponibles    |
| `renombrar`   | Renombra fragmentos del archivo             |
| `eliminar`    | Elimina fragmentos del archivo              |

Los archivos se dividen en tres partes: `archivo.part1`, `archivo.part2`, `archivo.part3`.

### Diagrama de Comunicación

```plaintext
┌─────────────┐         TCP         ┌──────────────┐
│   Cliente   │────────────────────►│  Servidor    │
│  JavaFX UI  │                     │   Maestro    │
└─────────────┘◄────────────────────┘              │
    ▲ UPLOAD / DOWNLOAD / LISTAR /                │
    │ RENOMBRAR / ELIMINAR                        ▼
    │        ┌──────────────┐     TCP     ┌──────────────┐
    │        │  Esclavo 1   │◄────────────│  Esclavo 2   │
    │        │  puerto 8001 │             │  puerto 8002 │
    ▼        └──────────────┘────────────►└──────────────┘
        TCP   ▲                TCP                ▼
            ┌──────────────┐       TCP     ┌──────────────┐
            │  Esclavo 3   │◄──────────────│   Maestro    │
            │  puerto 8003 │               └──────────────┘
            └──────────────┘
