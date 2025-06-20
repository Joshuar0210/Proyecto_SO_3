# Proyecto_SO_3

# Sistema de Fragmentación Distribuida de Archivos

Este proyecto implementa un sistema distribuido de almacenamiento y manipulación de archivos. El sistema está compuesto por una interfaz cliente desarrollada con JavaFX, un servidor maestro y múltiples servidores esclavos. Los archivos son divididos en partes y distribuidos entre los servidores esclavos para almacenamiento redundante y eficiente.

## Características

- Subida de archivos con fragmentación automática
- Descarga de archivos reconstruidos desde partes distribuidas
- Renombrado y eliminación de archivos distribuidos
- Listado dinámico de archivos disponibles
- Cliente gráfico intuitivo en JavaFX
- Comunicación basada en sockets TCP

## Componentes del sistema

### 1. Cliente (`PrimaryController.java`)
- Interfaz JavaFX que permite:
  - Subir archivos al sistema
  - Descargar archivos reconstruidos
  - Renombrar y eliminar archivos existentes
  - Buscar archivos por nombre o fecha
  - Cancelar operaciones activas
  - Visualizar progreso mediante una barra de carga

### 2. Servidor Maestro (`ServidorMaestro.java`)
- Recibe solicitudes del cliente
- Fragmenta los archivos en 3 partes
- Coordina la comunicación con los 3 servidores esclavos
- Gestiona operaciones de subida, descarga, listado, renombrado y eliminación

### 3. Servidores Esclavos (`ServidorEsclavo1.java`, etc.)
- Cada uno escucha en su propio puerto (ej. 8001, 8002, 8003)
- Almacenan fragmentos de archivos (e.g., `.part1`, `.part2`, `.part3`)
- Ejecutan operaciones locales sobre los fragmentos: guardar, enviar, renombrar, eliminar

## Requisitos

- Java 17 o superior
- JavaFX (compatible con tu versión de JDK)
- Maven (opcional, si deseas empaquetar con `pom.xml`)
- Conexión TCP entre los componentes

## Estructura del Proyecto

/Proyect_SO_3
│
├── com.mycompany.mavenproject6
│ ├── PrimaryController.java
│ ├── ServidorMaestro.java
│ ├── ServidorEsclavo1.java
│ ├── ServidorEsclavo2.java
| ├── ServidorEsclavo3.java
├── model
│ └── FileInfo.java
├── esclavo1/
│ └── archivos_guardados/
├── esclavo2/
│ └── archivos_guardados/
├── esclavo3/
│ └── archivos_guardados/


## Cómo ejecutar

1. **Iniciar los servidores esclavos:**
   Se deben ejecutar los  3 servidores esclavo utilizando puertos disponible como:
   - `puerto` → `8001`, `8002`, `8003`

2. **Ejecutar el servidor maestro:**
   Se ejecuta el servidor maestro para aceptar las conexiones de los servidores esclavos y de clientes
