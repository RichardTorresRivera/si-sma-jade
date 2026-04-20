# Tarea - SMA - JADE

Repositorio para la entrega de la tarea "SMA - JADE" del curso de Software Inteligente

## Requisitos

- [Docker](https://www.docker.com/)
- [Java](https://www.java.com/es/download/)
- [Apache Maven](https://maven.apache.org/)
- [Xming X Server for Windows](https://sourceforge.net/projects/xming/)

## Configuracion de XLaunch

Cargar el archivo de configuracion `config.xlaunch` al hacer doble click, o seleccionar manualmente la siguiente configuracion:

- Multiple windows
- Display number = 0
- Start no client
- Clipboard enabled
- NoAccessControl

## Ejecución

```bash
# Compilar el proyecto
mvn clean package
# Ejecutar los contenedores
docker compose up --build -d
# Volver a ejecutar
docker compose down && docker compose up --build -d
```

## Integrantes

- Chávez Ccahuana, Álvaro Andrés
- Obando Salinas, Enmanuel José
- Torres Rivera, Richard Maycol
- Vera Alva, Miguel Angel
