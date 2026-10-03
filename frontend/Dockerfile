# syntax=docker/dockerfile:1
# =====================================================================
# ShopSphere frontend image - multi-stage build
#   Stage 1 (build):   Node 22 -> npm ci -> vite build (tests run in CI)
#   Stage 2 (runtime): nginx-unprivileged (non-root, port 8080) serving static files
# Build:  docker build -t shopsphere-frontend:local \
#           --build-arg VITE_BUILD_SHA=$(git rev-parse --short=12 HEAD) ./frontend
# Run:    docker run --rm -p 3000:8080 -e BACKEND_URL=http://host.docker.internal:8080 shopsphere-frontend:local
# =====================================================================

FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
ARG VITE_API_URL=/api
ARG VITE_APP_VERSION=1.0.0
ARG VITE_BUILD_SHA=local
ENV VITE_API_URL=$VITE_API_URL VITE_APP_VERSION=$VITE_APP_VERSION VITE_BUILD_SHA=$VITE_BUILD_SHA
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.27-alpine AS runtime
LABEL org.opencontainers.image.title="shopsphere-frontend" \
      org.opencontainers.image.source="https://github.com/rahmanuddinmd/shopsphere-ai-devops" \
      org.opencontainers.image.licenses="MIT"
# The default site is replaced by our template, rendered to conf.d/default.conf at start-up
USER 0
RUN rm -f /etc/nginx/conf.d/default.conf && chown -R 101:101 /etc/nginx/conf.d
USER 101
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
ENV BACKEND_URL=http://backend:8080
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD ["wget", "-qO-", "http://127.0.0.1:8080/healthz"]
