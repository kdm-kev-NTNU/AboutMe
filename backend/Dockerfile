# syntax=docker/dockerfile:1
# Targets: runtime (default for docker compose), publish (CI with build-context jar)

# --- ONNX cross-encoder for RAG reranking (bundled under /app/rerank; no HF token needed at runtime) ---
FROM python:3.12-slim-bookworm AS rerank-model
WORKDIR /export
ENV PIP_NO_CACHE_DIR=1 \
    PIP_ROOT_USER_ACTION=ignore
RUN apt-get update \
    && apt-get install -y --no-install-recommends git ca-certificates \
    && rm -rf /var/lib/apt/lists/*
# CPU wheels only; needed to load weights during optimum export.
# Pin torch below 2.6: unpinned installs can pull 2.6+ where ONNX symbols diverge from optimum's imports
# (e.g. ImportError: cannot import name '_attention_scale' from torch.onnx.symbolic_opset14).
# Use torch==2.5.1 (not 2.5.1+cpu): the PyTorch CPU index exposes different local-version tags per arch;
# linux/arm64 lists 2.5.1 while amd64 may list 2.5.1+cpu, so an exact +cpu pin breaks multi-platform builds.
RUN pip install --no-cache-dir "torch==2.5.1" --index-url https://download.pytorch.org/whl/cpu
RUN pip install --no-cache-dir \
    "optimum[onnxruntime]>=1.23.0,<2" \
    "onnx>=1.16" \
    "onnxruntime>=1.19" \
    "transformers>=4.38.0,<5" \
    sentencepiece \
    protobuf
# Use an HF model whose Hub API allows anonymous reads (some cross-encoder/* checkpoints return 401 without auth).
# Do not forward a stale host HF token into the build.
RUN sh -c 'unset HF_TOKEN HUGGINGFACE_HUB_TOKEN HF_HUB_TOKEN; exec optimum-cli export onnx \
    --model cross-encoder/ms-marco-MiniLM-L-12-v2 \
    --task text-classification \
    /export/out'
# Flatten to predictable paths for the JVM reranker (tokenizer dir = /app/rerank).
RUN set -eux; \
    ls -la /export/out; \
    onnx="$(find /export/out -name '*.onnx' -type f ! -path '*/.git/*' | head -n1)"; \
    test -n "$onnx"; \
    if [ "$onnx" != "/export/out/model.onnx" ]; then cp "$onnx" /export/out/model.onnx; fi; \
    if [ ! -f /export/out/tokenizer.json ]; then \
      tok="$(find /export/out -name tokenizer.json -type f | head -n1)"; \
      test -n "$tok"; \
      cp "$tok" /export/out/tokenizer.json; \
    fi; \
    test -f /export/out/tokenizer.json

# Build stage (local full image)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
ENV MAVEN_OPTS="-Dmaven.wagon.http.retryHandler.count=5 -Dmaven.wagon.http.retryHandler.requestSentEnabled=true -Dmaven.wagon.httpconnectionManager.ttlSeconds=120 -Daether.connector.http.retryHandler.count=5"
COPY pom.xml .
RUN --mount=type=cache,id=s/e6f61e16-913e-443d-8379-19475d5675a7-/root/.m2/repository,target=/root/.m2/repository,sharing=locked \
    mvn -B --no-transfer-progress dependency:go-offline
COPY src ./src
RUN --mount=type=cache,id=s/e6f61e16-913e-443d-8379-19475d5675a7-/root/.m2/repository,target=/root/.m2/repository,sharing=locked \
    mvn -B --no-transfer-progress clean package -DskipTests -Dmaven.test.skip=true -Dmaven.compiler.fork=true -Dmaven.compiler.maxmem=512m

# Production runtime (docker compose default)
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
RUN mkdir -p data/docs
COPY --from=build /app/target/*.jar app.jar
COPY --from=rerank-model /export/out /app/rerank
ENV PORTFOLIO_RETRIEVAL_RERANK_ENABLED=true \
    PORTFOLIO_RETRIEVAL_ONNX_MODEL_PATH=/app/rerank/model.onnx \
    PORTFOLIO_RETRIEVAL_TOKENIZER_PATH=/app/rerank
EXPOSE 8080
ENTRYPOINT ["java", "-Djava.net.preferIPv6Addresses=true", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

# CI publish: pre-built JAR via BuildKit context "jar" (see .github/workflows/docker-publish.yml)
FROM eclipse-temurin:21-jre AS publish
WORKDIR /app
RUN mkdir -p data/docs
COPY --from=jar *.jar /tmp/jars/
RUN set -eux; \
    j="$(find /tmp/jars -maxdepth 1 -type f -name '*.jar' ! -name '*.original' | head -n1)"; \
    test -n "$j"; \
    cp "$j" /app/app.jar; \
    rm -rf /tmp/jars
COPY --from=rerank-model /export/out /app/rerank
ENV PORTFOLIO_RETRIEVAL_RERANK_ENABLED=true \
    PORTFOLIO_RETRIEVAL_ONNX_MODEL_PATH=/app/rerank/model.onnx \
    PORTFOLIO_RETRIEVAL_TOKENIZER_PATH=/app/rerank
EXPOSE 8080
ENTRYPOINT ["java", "-Djava.net.preferIPv6Addresses=true", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
