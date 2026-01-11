FROM mcr.microsoft.com/devcontainers/java:1-17-bookworm

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Verify installation
RUN mvn --version
