# syntax=docker/dockerfile:1

# ── Build stage ────────────────────────────────────────────────────────────────
FROM node:20-alpine AS build
WORKDIR /build

COPY frontend/package.json frontend/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci

COPY frontend/ .

# Vite inlines these into the JS bundle at build time — they cannot be changed
# at container runtime, unlike the backend services' env vars. ARGs alone
# aren't visible to the `npm run build` subprocess, so re-export as ENV.
ARG VITE_KEYCLOAK_URL
ARG VITE_KEYCLOAK_REALM
ARG VITE_KEYCLOAK_CLIENT_ID
ARG VITE_API_BASE_URL
ENV VITE_KEYCLOAK_URL=$VITE_KEYCLOAK_URL \
    VITE_KEYCLOAK_REALM=$VITE_KEYCLOAK_REALM \
    VITE_KEYCLOAK_CLIENT_ID=$VITE_KEYCLOAK_CLIENT_ID \
    VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN npm run build

# ── Runtime stage ───────────────────────────────────────────────────────────────
FROM nginx:1.27-alpine
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html

EXPOSE 80
