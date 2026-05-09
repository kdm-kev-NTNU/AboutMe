# Build stage
FROM node:22-alpine AS builder

WORKDIR /app

# Copy package files
COPY package.json package-lock.json ./
ENV CYPRESS_INSTALL_BINARY=0
# npm ci can fail on Alpine when the lockfile omits a nested peer resolution (e.g. typescript 5.9.x)
# that local npm materialized differently; install respects the lock for hoisted deps and fills gaps.
RUN npm install --no-audit --no-fund

# Copy source and build
COPY . .
ENV NODE_ENV=production

# Vite bakes VITE_* env vars at build time; receive them as Docker build args
ARG VITE_POSTHOG_KEY
ARG VITE_POSTHOG_HOST
ARG VITE_POSTHOG_ENABLED
ENV VITE_POSTHOG_KEY=$VITE_POSTHOG_KEY
ENV VITE_POSTHOG_HOST=$VITE_POSTHOG_HOST
ENV VITE_POSTHOG_ENABLED=$VITE_POSTHOG_ENABLED

RUN npm run build-only

# Production stage
FROM nginx:alpine

# Copy built assets
COPY --from=builder /app/dist /usr/share/nginx/html

COPY nginx.conf /etc/nginx/templates/default.conf.template
CMD sh -c "envsubst '\$BACKEND_URL' < /etc/nginx/templates/default.conf.template > /etc/nginx/conf.d/default.conf && nginx -g 'daemon off;'"


EXPOSE 80
