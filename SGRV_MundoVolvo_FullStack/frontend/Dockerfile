FROM node:22.16-alpine AS builder
WORKDIR /app
COPY package.json ./
RUN npm install --no-audit --no-fund
COPY . .
RUN npm run build
FROM nginx:1.27-alpine
# Valores por defecto (docker-compose local). En Railway se sobrescriben con variables.
ENV PORT=80 BACKEND_HOST=backend BACKEND_PORT=8080
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=builder /app/dist/sgrv-ui/browser /usr/share/nginx/html
EXPOSE 80
