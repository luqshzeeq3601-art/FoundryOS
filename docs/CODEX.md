# FactoryOS Engineering Standards

This is an implementation specification, not evidence that the application or pipeline exists. Agent orchestration and task-based effort are defined in [AGENTS.md](../AGENTS.md).

## Generated code and comments

- For generated or edited source-code comments, read and apply [$antislop-code](C:/Users/luqma/.codex/skills/antislop-code/SKILL.md) together with its core [antislop](C:/Users/luqma/.codex/skills/antislop/SKILL.md).
- Keep comments that explain non-obvious intent, constraints, edge cases, security, performance, protocols, API behavior, or workarounds.
- Remove comments that only narrate obvious code, add decorative separators or emoji, use empty labels, mark vague future work, or mark the end of a block.
- This check is comment-only. Do not change executable code, identifiers, formatting, or behavior to satisfy it.

## Steps 11–12 — Stack and standards

### Frontend

- React 18+, TypeScript strict mode, Vite, Tailwind CSS.
- UI Design & Styling: Apply [.agents/skills/design-taste-frontend](../.agents/skills/design-taste-frontend/SKILL.md) and [.agents/skills/industrial-brutalist-ui](../.agents/skills/industrial-brutalist-ui/SKILL.md) for anti-slop visual discipline, intentional typography, clean industrial aesthetics, and accessible touch targets.
- React Router for navigation; TanStack Query for server state.
- React Hook Form and Zod for forms.
- Structure: `src/features/<domain>/{components,hooks,services,types}`.
- Shared UI, API client, and authorization helpers live outside domain features.
- Keep access tokens in memory, never browser local storage.
- Pin compatible exact dependency versions during foundation setup and commit the lockfile. Version ranges here are constraints, not claims of current release support.

### Backend

- Java 21, Spring Boot 3+, Spring Security, Spring Data JPA, Maven Wrapper, Flyway, PostgreSQL.
- Base package: `com.factoryos.modules.<domain>`; modules follow [Architecture](ARCHITECTURE.md).
- Explicit DTOs and Jakarta validation at boundaries; constructor injection.
- Inject `Clock` for deterministic time-sensitive tests.
- Hibernate uses `validate`; Flyway exclusively owns schema changes.
- No controller business logic, cross-module repository calls, or JPA entities returned as API responses.

### Repository layout

- `frontend/`: React application.
- `backend/`: Java application and migrations.
- `docs/`: specifications.
- Root: AGENTS.md, README.md, Compose, environment templates, CI workflows.
- `tests/performance/mvp.js`: k6 acceptance scenario.

### Environment configuration

- `.env.development`, `.env.test`, `.env.production`: ignored, generated locally or by deployment tooling.
- `.env.example`: committed safe placeholders with descriptions, never working secrets.
- Compose selects values with `--env-file`; these files do not automatically load into Spring.
- Explicit Compose environment mappings feed Spring profiles `development`, `test`, `production`.
- Backend settings include JDBC URL, database user/password, JWT key mount paths, allowed origins, plant timezone, management binding, and one-time bootstrap credentials.
- Production secrets come from secret storage or restricted mounted files. Fail startup when secrets are absent or insecure defaults remain.
- Frontend public build configuration uses `VITE_API_BASE_URL=/api/v1`; never expose secrets through Vite variables. The relative API URL lets the same frontend artifact move between environments.
- Default plant timezone is `Asia/Kuala_Lumpur`; storage remains UTC.

## Step 13 — Git workflow

- `main` is production-ready; `develop` feeds staging.
- `feature/<domain>-<name>` and `fix/<issue-name>` branch from `develop`.
- Protected branches require passing checks and review.
- Conventional Commits: `feat:`, `fix:`, `test:`, `refactor:`, `docs:`.
- Release PR merges develop into main. Urgent fixes may branch from main and must merge back into develop.
- Never commit credentials, signing keys, generated secrets, or populated environment files.

## Step 14 — Testing

| Layer | Tools | Responsibility |
|---|---|---|
| Backend unit | JUnit 5, Mockito | Lifecycles, permissions, calculations |
| Integration | Testcontainers PostgreSQL | Real constraints, transactions, migrations and queries |
| Architecture | ArchUnit | Module ownership and dependency cycles |
| Frontend | Vitest, React Testing Library | Forms, permissions, loading/error states |
| E2E | Playwright | Six-role workflows and tablet/desktop behavior |
| Performance | k6 | PRD load profile and latency/error targets |

Required scenarios:

- Every allowed/denied role permission and Technician assignment restriction.
- Concurrent downtime opens and competing Production Order starts.
- Failed audit insert rolls back the business mutation.
- Stale versions return 409 without partial changes.
- JWT expiry, refresh rotation/replay, password changes and deactivated accounts.
- Dashboard open-event clipping, timezone boundaries, empty ranges and archived references.
- Cumulative output corrections and network-lost response retries without double counting.
- Last active Admin protection under concurrent administration.
- Concurrent Technician deactivation/reassignment and assignment/null/status coupling.
- Keyboard focus, labeled controls, 768 px tablet and 1280 px desktop layout.

## Step 15 — Containers and CI/CD

### Runtime design

- Frontend multi-stage image: Node asset build then non-root static web server.
- Backend multi-stage image: Maven/Java 21 build then non-root Java 21 runtime.
- Compose services: frontend, backend, postgres; no Redis service in MVP.
- PostgreSQL has a named persistent volume. Database and management ports are private.
- Frontend proxies `/api` to backend; production TLS terminates at a trusted edge.
- Define a one-shot migration mode in the backend image: run Flyway and exit without starting HTTP serving. Runtime application mode validates schema and does not run migrations.
- Use separate migration and runtime database identities. Migration execution must complete before application promotion.

### Pipeline

`Push/PR → Lint → Unit tests → Integration tests → Artifact build → Docker image build`

- Include TypeScript checks, backend formatting checks and architecture tests.
- Integration tests run Testcontainers PostgreSQL, never H2 as a substitute.
- Build frontend assets and backend JAR, then images.
- Publish immutable commit-tagged images only from trusted branches.
- Scan dependencies/images; unresolved critical/high findings block promotion unless documented exception is approved.
- Deploy develop to staging. Production promotion follows release approval.
- Promote the exact tested images; do not rebuild per environment.

## Step 19 — Release readiness

1. Freeze the candidate and deploy immutable images to staging.
2. Validate migrations on an empty database and a restored copy of the previous schema.
3. Use additive, backward-compatible migrations; never edit applied migrations.
4. Confirm backup freshness and restore to an isolated database.
5. Run authorization, operational E2E, tablet and load acceptance checks.
6. Confirm TLS, secrets, CORS, private ports and management restrictions.
7. Obtain deployment authorization; use an announced maintenance window.
8. Run migrations once before application promotion.
9. Smoke-test login, machine reads, downtime, maintenance and dashboard reconciliation.
10. Roll back the application image if needed, retaining compatible additive schema changes. Destructive recovery requires a separate restoration decision.

Release blockers: authorization bypass, inconsistent machine/downtime state, missing audit records, failed restoration, broken critical workflows, or an unmet performance target without approved exception.

### Backup and operations baseline

- Target RPO ≤15 minutes and RTO ≤4 hours, proven by restoration drills.
- PostgreSQL daily base backups plus continuous WAL archiving to encrypted off-host storage; retain at least 30 days.
- Monitor archival freshness and backup failures. Restrict backup access and test that credentials/keys required for recovery are available to the designated system owner.
- System owner coordinates incidents, restores and release decisions; assign a named person before production release.
- Validate 99.5% monthly availability objective excluding announced maintenance.

## Step 20 — Post-release validation

- Continuously monitor readiness, errors, latency, disk capacity and backups.
- Review security/operational signals daily for the first week.
- Reconcile dashboard samples against operational records after first plant use.
- Collect Operator/Technician feedback on touch controls and failed submissions.
- Review incidents and write conflicts weekly during the first month.
- Run quarterly restoration drills and access reviews.
- Admit Version 2 only after operational stability and KPI reconciliation are demonstrated.

## Lifecycle coverage

| Step | Specification |
|---|---|
| 1 Problem and goals | PRD.md |
| 2 Users and RBAC | PRD.md |
| 3 Phased scope | PRD.md |
| 4 Requirements | PRD.md |
| 5 Use cases | PRD.md, BACKLOG.md |
| 6 Architecture | ARCHITECTURE.md |
| 7 Database | Data.md |
| 8 API | API-DESIGN.md |
| 9 Security | Security.md |
| 10 UI/UX | PRD.md |
| 11 Stack | CODEX.md |
| 12 Standards | CODEX.md, ARCHITECTURE-ESSENTIALS.md, ../AGENTS.md |
| 13 Git | CODEX.md |
| 14 Tests | CODEX.md |
| 15 CI/CD | CODEX.md |
| 16 Authentication | Security.md |
| 17 Observability | ARCHITECTURE.md |
| 18 Backlog | BACKLOG.md |
| 19 Release | CODEX.md, BACKLOG.md |
| 20 Post-release | CODEX.md, BACKLOG.md |
