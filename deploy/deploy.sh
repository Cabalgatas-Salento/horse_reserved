#!/bin/bash
set -e

AWS_REGION=us-east-1
AWS_ACCOUNT=950876115395
ECR_REPO=horse-reserved
APP_RUNNER_ARN=arn:aws:apprunner:us-east-1:950876115395:service/horse-reserved/d5633e78424a4a27b196ce87dfac0755

IMAGE_URI=$AWS_ACCOUNT.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPO

echo "==> Login ECR"
aws ecr get-login-password --region $AWS_REGION \
  | docker login --username AWS --password-stdin $AWS_ACCOUNT.dkr.ecr.$AWS_REGION.amazonaws.com

echo "==> Build imagen Docker"
cd "$(dirname "$0")/.."
docker build -t $IMAGE_URI:latest .

echo "==> Push a ECR"
docker push $IMAGE_URI:latest

echo "==> Trigger App Runner deployment"
OPERATION_ID=$(aws apprunner start-deployment \
  --service-arn $APP_RUNNER_ARN \
  --query 'OperationId' \
  --output text \
  --region $AWS_REGION)
echo "OperationId: $OPERATION_ID"

echo "==> Esperando que App Runner quede RUNNING (máx 15 min)..."
for i in $(seq 1 30); do
  STATUS=$(aws apprunner describe-service \
    --service-arn $APP_RUNNER_ARN \
    --query 'Service.Status' \
    --output text \
    --region $AWS_REGION)
  echo "  [$i/30] Status: $STATUS"
  if [ "$STATUS" = "RUNNING" ]; then
    echo "Deployment exitoso."
    exit 0
  fi
  sleep 30
done

echo "TIMEOUT: 15 minutos sin alcanzar RUNNING."
exit 1