# Usamos una imagen de Java ligera
FROM openjdk:27-ea-17-slim-trixie

# Instalamos X11 para la GUI
# RUN apt-get update && apt-get install -y libx11-6 xauth

# Directorio de trabajo
WORKDIR /usr/src/app

# Copiamos el jar que generaste con Maven
COPY target/si-sma-jade-1.0-SNAPSHOT.jar app.jar

# Variable para permitir la conexión gráfica
# ENV DISPLAY=host.docker.internal:0

# Comando por defecto
CMD ["java", "-cp", "app.jar", "jade.Boot"]