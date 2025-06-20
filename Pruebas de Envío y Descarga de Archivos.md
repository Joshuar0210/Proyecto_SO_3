# Pruebas de Envío y Descarga de Archivos

Este documento presenta los resultados de las pruebas realizadas para validar el correcto funcionamiento del sistema distribuido de fragmentación de archivos, incluyendo la subida, descarga, renombrado y eliminación de archivos tanto de texto como binarios.

## Archivos de Prueba

Se utilizaron los siguientes archivos de ejemplo:

- **PDF:**
  - `Abraham-Silberschatz-Operating-System-Concepts-10th-2018.pdf`
  - Tamaño: 29,6 MB
  - Contenido: texto 

- **Binario:**
  - `imagen.jpg`
  - Tamaño: aproximadamente 220 KB
  - Contenido: imagen común para testeo

## Escenarios Probados

### 1. Subida de Archivos

**Procedimiento:**

- El usuario selecciona un archivo desde la interfaz JavaFX.
- El archivo se divide automáticamente en tres partes.
- Cada parte se envía a un servidor esclavo distinto.

**Resultados esperados y observados:**

- Los fragmentos `.part1`, `.part2` y `.part3` se almacenan en las carpetas correspondientes (`esclavo1`, `esclavo2`, `esclavo3`).
- La barra de progreso refleja el avance de la operación.
- El sistema notifica que la subida fue exitosa.

---

### 2. Descarga de Archivos

**Procedimiento:**

- El usuario selecciona un archivo desde la lista en la GUI.
- El cliente solicita al servidor maestro la reconstrucción del archivo.
- El maestro recopila las partes desde los esclavos.
- El archivo completo se reconstruye y guarda localmente.

**Resultados esperados y observados:**

- El archivo descargado es idéntico al original.
- La barra de progreso muestra el estado de la descarga.
- Se muestra una notificación confirmando la descarga.

---

### 3. Renombrado de Archivos

**Procedimiento:**

- El usuario escribe un nuevo nombre y selecciona un archivo.
- El cliente envía la solicitud al maestro.
- El maestro ordena a cada esclavo renombrar sus fragmentos.

**Resultados esperados y observados:**

- Todos los fragmentos `.partX` cambian su nombre base.
- El archivo aparece en la lista con el nuevo nombre.
- Se muestra un mensaje confirmando el renombrado.

---

### 4. Eliminación de Archivos

**Procedimiento:**

- El usuario selecciona un archivo y pulsa el botón "Eliminar".
- El maestro solicita a los esclavos borrar los fragmentos.

**Resultados esperados y observados:**

- Los tres fragmentos del archivo se eliminan exitosamente.
- El archivo desaparece de la tabla del cliente.
- Se muestra un mensaje de confirmación.

