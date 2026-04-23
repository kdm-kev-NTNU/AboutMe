# syntax=docker/dockerfile:1
# Enable BuildKit

# --- ONNX cross-encoder for RAG reranking (bundled under /app/rerank; no HF token needed at runtime) ---
FROM python:3.12-slim-bookworm AS rerank-model
WORKDIR /export
ENV PIP_NO_CACHE_DIR=1 \
    PIP_ROOT_USER_ACTION=ignore
RUN apt-get update \
    && apt-get install -y --no-install-recommends git ca-certificates \
    && rm -rf /var/lib/apt/lists/*
# CPU wheels only; needed to load weights during optimum export.
RUN pip install --no-cache-dir torch --index-url https://download.pytorch.org/whl/cpu
RUN pip install --no-cache-dir \
    "optimum[onnxruntime]>=1.23.0,<2" \
    "onnx>=1.16" \
    "onnxruntime>=1.19" \
    "transformers>=4.38.0,<5" \
    sentencepiece \
    protobuf
RUN optimum-cli export onnx \
    --model cross-encoder/multilingual-MiniLM-L12-v2 \
    --task text-classification \
    /export/out
# Flatten to predictable paths for the JVM reranker (tokenizer dir = /app/rerank).
RUN set -eux; \
    ls -la /export/out; \
    onnx="$(find /export/out -name '*.onnx' -type f ! -path '*/.git/*' | head -n1)"; \
    test -n "$onnx"; \
    cp "$onnx" /export/out/model.onnx; \
    if [ ! -f /export/out/tokenizer.json ]; then \
      tok="$(find /export/out -name tokenizer.json -type f | head -n1)"; \
      test -n "$tok"; \
      cp "$tok" /export/out/tokenizer.json; \
    fi; \
    test -f /export/out/tokenizer.json

# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -Dmaven.test.skip=true -Dmaven.compiler.fork=true -Dmaven.compiler.maxmem=512m

# Production stage
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN mkdir -p data/docs

RUN touch .env

COPY --from=build /app/target/*.jar app.jar
COPY --from=rerank-model /export/out /app/rerank

# Defaults for containers (Railway, docker run). Override or set PORTFOLIO_RETRIEVAL_RERANK_ENABLED=false to disable.
ENV PORTFOLIO_RETRIEVAL_RERANK_ENABLED=true \
    PORTFOLIO_RETRIEVAL_ONNX_MODEL_PATH=/app/rerank/model.onnx \
    PORTFOLIO_RETRIEVAL_TOKENIZER_PATH=/app/rerank

EXPOSE 8080
# Prefer IPv6 for DNS (Railway private networking over *.railway.internal).
ENTRYPOINT ["java", "-Djava.net.preferIPv6Addresses=true", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
