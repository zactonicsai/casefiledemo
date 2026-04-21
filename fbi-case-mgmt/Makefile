.PHONY: up down logs ps backend-build frontend-build verify clean rebuild

up:
	docker compose up -d --build

down:
	docker compose down

logs:
	docker compose logs -f --tail=100

ps:
	docker compose ps

backend-build:
	cd backend && mvn -B clean package -DskipTests

frontend-build:
	cd frontend && npm install && npx ng build

verify:
	@echo "[verify] Checking service health..."
	@curl -sf http://localhost:8080/realms/sentinel/.well-known/openid-configuration >/dev/null && echo "  keycloak OK"  || echo "  keycloak NOT READY"
	@curl -sf http://localhost:9200/_cluster/health >/dev/null && echo "  elasticsearch OK" || echo "  elasticsearch NOT READY"
	@curl -sf http://localhost:4566/_localstack/health >/dev/null && echo "  localstack OK"  || echo "  localstack NOT READY"
	@curl -sf http://localhost:8081/actuator/health >/dev/null && echo "  backend OK"      || echo "  backend NOT READY"
	@curl -sf http://localhost:4200/ >/dev/null && echo "  frontend OK"                    || echo "  frontend NOT READY"

rebuild: down
	docker compose build --no-cache
	docker compose up -d

clean:
	docker compose down -v
	rm -rf backend/target frontend/node_modules frontend/dist

# ─── Demo helpers ─────────────────────────────────────────────────────────────
seed-localstack:
	docker exec cms-localstack bash /etc/localstack/init/ready.d/init.sh || true
	docker exec cms-localstack awslocal s3 ls

seed-keycloak:
	bash infra/init-scripts/seed-keycloak.sh
