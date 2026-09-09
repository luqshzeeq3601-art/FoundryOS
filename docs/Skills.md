# Permitted Development Commands

These commands are specifications for future implementation. The application, scripts, Maven Wrapper and Compose files must first be created under Epic 1. This document does not authorize executing implementation commands during a planning-only request.

## Frontend — from frontend/

```powershell
npm ci
npm run lint
npm run typecheck
npm run test -- --run
npm run build
npx --no-install playwright install chromium
npx --no-install playwright test
```

- Define these scripts and commit Playwright as a development dependency.
- Foundation setup must select compatible exact versions and create the lockfile before `npm ci` works.
- New dependencies require explicit package/version choices within the implementation task; do not install floating latest versions blindly.
- Consult [.agents/skills/design-taste-frontend](../.agents/skills/design-taste-frontend/SKILL.md) and [.agents/skills/industrial-brutalist-ui](../.agents/skills/industrial-brutalist-ui/SKILL.md) for frontend visual design and component styling.

## Backend — from backend/

```powershell
.\mvnw.cmd dependency:go-offline
.\mvnw.cmd spotless:check
.\mvnw.cmd test
.\mvnw.cmd verify
.\mvnw.cmd package -DskipTests
```

- Configure Maven Failsafe so verify runs integration tests against Testcontainers PostgreSQL.
- Skipping tests for packaging is acceptable only after the same revision passed verification.

## Development containers — from repository root

```powershell
docker compose --env-file .env.development config --quiet
docker compose --env-file .env.development up -d postgres
docker compose --env-file .env.development build
docker compose --env-file .env.development up -d
docker compose --env-file .env.development logs --tail 200 backend
docker compose --env-file .env.development down
```

The Compose dependency/startup flow must run the specified one-shot migration mode before launching a backend against a new database. Compose maps environment values explicitly; `--env-file` alone does not configure Spring or Maven.

## Migrations — from backend/

```powershell
.\mvnw.cmd flyway:info
.\mvnw.cmd flyway:validate
.\mvnw.cmd flyway:migrate
```

- Configure the Flyway Maven plugin explicitly to read `FACTORYOS_MIGRATION_JDBC_URL`, `FACTORYOS_MIGRATION_USER`, and `FACTORYOS_MIGRATION_PASSWORD` from the process environment. Never assume environment-file auto-loading.
- Flyway is the sole migration tool; no Liquibase. Commit versioned migrations; never edit applied migrations.
- Use a migration identity with schema privileges; runtime credentials lack DDL privileges.
- Disable Flyway clean. Production migration follows the authorized release process.

## Performance — from repository root

```powershell
k6 run tests/performance/mvp.js
```

Provide the target URL and test-account credentials through the process environment. The script must implement the PRD dataset/workload and thresholds. Do not load-test production without explicit authorization.

## Safeguards

- No destructive database resets, Flyway clean, or `docker compose down -v` by default.
- Never put credentials in command arguments or logs.
- Routine reversible checks do not need repeated confirmation when already authorized.
- Deployment and destructive changes require applicable authorization; a command's inclusion here does not grant it.
