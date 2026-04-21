#!/bin/bash
set -e

EC2_HOST="${EC2_HOST:?Falta EC2_HOST (ej: ec2-x-x-x-x.compute-1.amazonaws.com)}"
EC2_KEY="${EC2_KEY:?Falta EC2_KEY (ruta al archivo .pem)}"
NEON_DB_URL="${NEON_DB_URL:?Falta NEON_DB_URL (postgresql://user:pass@host/db?sslmode=require)}"
APP_RUNNER_HOST="${APP_RUNNER_HOST:?Falta APP_RUNNER_HOST (solo el hostname, sin https://)}"

REMOTE="ec2-user@$EC2_HOST"
SSH="ssh -i $EC2_KEY -o StrictHostKeyChecking=no $REMOTE"
SCP="scp -i $EC2_KEY -o StrictHostKeyChecking=no"

cd "$(dirname "$0")/.."

echo "==> Preparando directorios en EC2..."
$SSH "mkdir -p ~/horse-reserved/monitoring ~/horse-reserved/docker"

echo "==> Copiando archivos de monitoring..."
$SCP -r monitoring/prometheus monitoring/grafana monitoring/postgres-exporter \
  "$REMOTE:~/horse-reserved/monitoring/"

echo "==> Copiando docker-compose files..."
$SCP docker/docker-compose.yml docker/docker-compose.prod.yml \
  "$REMOTE:~/horse-reserved/docker/"

echo "==> Configurando hostname de App Runner en prometheus.prod.yml..."
$SSH "sed -i 's|\${APP_RUNNER_HOST}|$APP_RUNNER_HOST|g' \
  ~/horse-reserved/monitoring/prometheus/prometheus.prod.yml"

echo "==> Verificando Docker en EC2..."
$SSH "which docker > /dev/null 2>&1 || (sudo dnf install -y docker && sudo systemctl enable --now docker && sudo usermod -aG docker ec2-user)"
$SSH "docker compose version > /dev/null 2>&1 || sudo dnf install -y docker-compose-plugin"

echo "==> Levantando monitoring stack..."
$SSH "cd ~/horse-reserved/docker && \
  NEON_DB_URL='$NEON_DB_URL' \
  docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --pull missing"

echo ""
echo "==> Deploy completado."
echo "    Grafana:    http://$EC2_HOST:3000  (admin / admin123)"
echo "    Prometheus: http://$EC2_HOST:9090/targets"
