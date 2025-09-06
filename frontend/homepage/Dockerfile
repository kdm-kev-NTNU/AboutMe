# Build stage
FROM node:22-alpine AS builder

# Enable corepack and prepare pnpm
RUN corepack enable && corepack prepare pnpm@10.10.0 --activate

WORKDIR /app

# Copy package files
COPY package.json package-lock.json ./

# Install dependencies (use npm ci to respect lockfile)
RUN npm ci

# Copy source
COPY . .

# Build the application (ignore type errors at build-time similar to base)
ENV NODE_ENV=production
RUN npm run build-only

# Production stage
FROM nginx:alpine

# Copy built assets
COPY --from=builder /app/dist /usr/share/nginx/html

# Default nginx config serves single-page app
RUN printf "server {\n  listen 80;\n  server_name _;\n  root /usr/share/nginx/html;\n  location / {\n    try_files $uri $uri/ /index.html;\n  }\n}\n" > /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]


