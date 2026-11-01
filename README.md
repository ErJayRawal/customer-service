# Customer Service

Customer Service owns customer profile data and maps authenticated OIDC identities to immutable customer UUIDs. An external identity is identified by the pair `(issuer, subject)` from the validated access token; the email address is profile data, not an identity key.

## API

All endpoints require a bearer access token whose issuer matches the configured OIDC issuer and whose audience contains `customer-service`.

## Authentication and authorization model

The client application (for example, a web frontend) authenticates users with Keycloak using Authorization Code with PKCE and sends the resulting access token to the target API. Services do not request a new user token from Keycloak per API call. Each service validates incoming tokens locally: it verifies the signature using Keycloak's JWKS endpoint, then checks issuer, audience, expiry, and subject. Quarkus refreshes signing keys as needed.

Authorization is enforced by the service that owns the resource. Customer Service derives the caller identity from the validated `(issuer, subject)` and restricts `/me` operations to that identity; callers cannot supply another customer's ID. When backend-to-backend operations are introduced, the calling service should obtain a separate client-credentials token for the target audience and narrowly scoped permission. Propagate a user's token only when a downstream operation must explicitly act on that user's behalf; do not use service credentials as a substitute for user ownership checks.

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/customers/me` | Retrieve the caller's profile; returns `404` until the profile is created |
| `PUT` | `/api/v1/customers/me` | Create or update the caller's profile; returns `201` on creation and `200` on update |

Example request body:

```json
{
  "fullName": "Ada Lovelace",
  "email": "ada@example.com"
}
```

The server derives ownership exclusively from the validated token claims. The request cannot choose a customer ID, issuer, or subject. Database uniqueness on `(issuer, subject)` prevents one identity from mapping to multiple customer records. A future customer merge or identity-linking flow must be an explicit, audited operation; this API never reassigns identities.

## Run and test locally

Requirements: JDK 21, Maven Wrapper, and Docker Desktop. Dev Services starts PostgreSQL for local development; tests use an isolated in-memory H2 database while applying the same Flyway migration.

```powershell
.\mvnw.cmd test
.\mvnw.cmd quarkus:dev
```

The API listens on `http://localhost:8080`; OpenAPI is available at `/q/openapi`, Swagger UI at `/q/swagger-ui/`, and health checks at `/q/health`.

In dev mode, Quarkus Dev Services starts PostgreSQL when Docker is available. For production mode, configure `DB_JDBC_URL`, `DB_USERNAME`, and `DB_PASSWORD`; all are required. Use a dedicated database and provide credentials through the platform's secret manager. Flyway applies migrations from `src/main/resources/db/migration`; Hibernate validates the schema rather than modifying it.

Configure OIDC discovery with `OIDC_AUTH_SERVER_URL`, the JWKS endpoint path with `OIDC_JWKS_PATH` (default `/protocol/openid-connect/certs`), and the externally asserted issuer with `OIDC_TOKEN_ISSUER`. These may differ when the application reaches Keycloak through a cluster-internal address but tokens contain a public issuer URL or discovery advertises a host-local JWKS URL. The access token must include the `customer-service` audience. Do not disable issuer, signature, subject, or audience validation outside tests.

## Deploy to Docker Desktop Kubernetes

This local setup uses a dedicated PostgreSQL StatefulSet and PVC in the shared `e-commerce-platform` namespace. It is a single-node development deployment, not a highly available production setup. Create the local database Secret once without committing credentials:

```powershell
$passwordFile = [System.IO.Path]::GetTempFileName()
try {
  $bytes = New-Object byte[] 32
  [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
  [System.IO.File]::WriteAllText($passwordFile, [Convert]::ToBase64String($bytes))
  kubectl create namespace e-commerce-platform --dry-run=client -o yaml | kubectl apply -f -
  kubectl create secret generic customer-db-credentials --namespace e-commerce-platform `
    --from-literal=username=customer_service --from-file=password=$passwordFile
}
finally {
  Remove-Item $passwordFile -ErrorAction SilentlyContinue
  $bytes = $null
}
```

Build and deploy the independent service:

```powershell
.\mvnw.cmd verify
docker build -f src/main/docker/Dockerfile.jvm -t customer-service:local .
kubectl apply -k k8s
kubectl rollout status statefulset/customer-postgres --namespace e-commerce-platform
kubectl rollout status deployment/customer-service --namespace e-commerce-platform
kubectl port-forward service/customer-service 8090:80 --namespace e-commerce-platform
```

Customer Service is available at `http://localhost:8090`. It discovers Keycloak inside the shared namespace; the required external token issuer remains `http://localhost:8180/realms/orders`. The deployment expects the Keycloak access token to contain the `customer-service` audience.

The local Keycloak development client issues both the `order-service` and `customer-service` audiences so its tokens can be used with either API. The realm import configures both audience mappers for fresh deployments; when updating an existing realm, add the `customer-service` audience mapper through Keycloak's Admin Console or Admin REST API because realm imports do not reconcile persisted realms. Keep the two service repositories independent; the Customer Service UUID mapping is not yet wired into Order Service, which continues using its current customer identifier until a coordinated migration is designed and tested.

## Data ownership and integration boundary

Customer Service is the source of truth for profile fields and identity-to-customer mapping. Order Service remains responsible for order lifecycle and does not synchronously depend on this service yet. When integrating the internal customer UUID into orders, define the backfill and dual-read/write transition before changing existing `customer_id` values; do not reinterpret existing OIDC subjects as UUIDs.
