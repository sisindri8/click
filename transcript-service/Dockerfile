# Stage 1: Build the Java app
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

# Stage 2: Runtime image with Java + Python + Whisper + ffmpeg
# Using eclipse-temurin:21-jre-jammy as base since it's Ubuntu-based
# and apt-get is available for installing ffmpeg and Python.
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Install Python3, pip, and ffmpeg
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

# Install Python dependencies
COPY python/requirements.txt ./python/requirements.txt
RUN pip3 install --no-cache-dir openai-whisper yt-dlp

# Copy the Python transcription script
COPY python/fetch_transcript.py ./python/fetch_transcript.py

# Copy the built Java jar
COPY --from=build /app/target/*.jar app.jar

# Whisper model downloads to ~/.cache/whisper on first run.
# Pre-download the small model during image build so the first request
# isn't slow waiting for a 460MB download.
RUN python3 -c "import whisper; whisper.load_model('small')"

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
