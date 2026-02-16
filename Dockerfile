## ESTÁGIO 1: Build - Usando Maven e JDK 21 para compilar o projeto
#FROM maven:3.9.6-eclipse-temurin-21 AS builder
#WORKDIR /app
#
## Otimização de cache: copia primeiro o pom.xml para baixar dependências
#COPY pom.xml .
#RUN mvn dependency:go-offline
#
## Copia o código-fonte e empacota a aplicação
#COPY src ./src
#RUN mvn clean package -DskipTests -Dfile.encoding=UTF-8
#
## ESTÁGIO 2: Runtime - Usando uma imagem JRE leve para rodar
#FROM eclipse-temurin:21-jre
#WORKDIR /app
#
## Copia apenas o .jar final do estágio de build
#COPY --from=builder /app/target/Bellory-1.0-SNAPSHOT.jar app.jar
#
## Expõe a porta da aplicação
#EXPOSE 8080
#
## Comando para iniciar a aplicação
#ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]
# Imagem JRE leve
FROM eclipse-temurin:21-jre
WORKDIR /app

# Criar diretórios de uploads dentro do container
RUN mkdir -p /var/bellory/dev/uploads /var/bellory/prod/uploads && \
    chmod -R 755 /var/bellory/dev/uploads /var/bellory/prod/uploads

# Copia o JAR existente
COPY app.jar app.jar

# Expõe a porta da aplicação
EXPOSE 8081

# Comando para iniciar a aplicação
ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]
