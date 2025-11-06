# Etapa 1: build (opcional se já tiver o JAR pronto)
# FROM maven:3.9.6-eclipse-temurin-21 AS builder
# WORKDIR /app
# COPY pom.xml .
# RUN mvn dependency:go-offline
# COPY src ./src
# RUN mvn clean package -DskipTests -Dfile.encoding=UTF-8

# Etapa 2: runtime
FROM eclipse-temurin:21-jre
WORKDIR /app

# Argumentos e variáveis de ambiente
ARG JAR_FILE=Bellory-1.0-SNAPSHOT.jar
ARG SERVER_PORT=8080
ARG SPRING_PROFILE=prod

ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILE}
ENV SERVER_PORT=${SERVER_PORT}

# Copia o JAR
COPY ${JAR_FILE} app.jar

# Expõe a porta configurada
EXPOSE ${SERVER_PORT}

# Comando de inicialização
ENTRYPOINT ["sh", "-c", "java --enable-preview -jar app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE} --server.port=${SERVER_PORT}"]
