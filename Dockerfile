FROM eclipse-temurin:21-jdk-jammy

# Install only what we need
RUN apt-get update && apt-get install -y sqlite3 && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY build/libs/ktor-ptbox-server-all.jar /app/app.jar

# Create directory for harvester results
RUN mkdir -p /app/harvester_results

VOLUME /data
ENV DB_PATH=/data/scans.db
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]