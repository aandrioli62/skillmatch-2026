#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# deploy-k8s.sh — Apply the SkillMatch Kubernetes manifests in the right order.
#
# Prerequisites (from infra/scripts/setup-oracle-vm.sh):
#   - K3s running, kubectl configured against it
#   - Nginx Ingress Controller + cert-manager installed (only needed for
#     infra/k8s/ingress.yaml)
#   - GHCR packages (ghcr.io/aandrioli62/skillmatch/*) set to Public, so K3s
#     can pull them without credentials
#
# Usage: ./infra/scripts/deploy-k8s.sh
# Run from the repository root.
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

cd "$(dirname "$0")/../.."

echo "Regenerating ConfigMaps from source files..."
kubectl create configmap postgres-init \
  --from-file=init-databases.sql=infra/init-databases.sql \
  --namespace skillmatch --dry-run=client -o yaml > infra/k8s/postgres/init-configmap.yaml
kubectl create configmap keycloak-realm \
  --from-file=skillmatch-realm.json=infra/keycloak/skillmatch-realm.json \
  --namespace skillmatch --dry-run=client -o yaml > infra/k8s/keycloak/realm-configmap.yaml

echo "Applying namespace and shared config..."
kubectl apply -f infra/k8s/namespace.yaml
kubectl apply -f infra/k8s/config.yaml

echo "Applying data stores and message broker..."
kubectl apply -f infra/k8s/postgres/
kubectl apply -f infra/k8s/mongodb/
kubectl apply -f infra/k8s/rabbitmq/

echo "Applying Keycloak..."
kubectl apply -f infra/k8s/keycloak/

echo "Applying microservices..."
kubectl apply -f infra/k8s/api-gateway/
kubectl apply -f infra/k8s/user-service/
kubectl apply -f infra/k8s/project-service/
kubectl apply -f infra/k8s/contract-service/
kubectl apply -f infra/k8s/payment-service/
kubectl apply -f infra/k8s/feedback-service/
kubectl apply -f infra/k8s/notification-service/

echo "Applying ingress (requires nginx-ingress + cert-manager)..."
kubectl apply -f infra/k8s/ingress.yaml

echo "Done. Check status with: kubectl get pods -n skillmatch"
