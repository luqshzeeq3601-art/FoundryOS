# FactoryOS Architecture Essentials

These rules are the short, implementation-facing contract. Detailed rationale and lifecycle behavior are in `ARCHITECTURE.md`; schema details are in `Data.md`.

- Deploy one organization and one plant per deployment.
- Use a Java 21 Spring Boot 3+ modular monolith, React 18+/TypeScript, and PostgreSQL.
- MVP is online-only. Do not add microservices, Kafka, MQTT, Kubernetes, WebSockets, Redis, or an offline write queue.
- Organize backend domains under `com.factoryos.modules.<domain>` with `api`, `application`, `domain`, and `infrastructure` packages.
- Modules own their tables and expose application services. Never call another module's repository.
- The table-free operations module orchestrates cross-domain commands; domain modules never call it or each other. See ARCHITECTURE.md for the explicit dependency direction.
- Application services own transactions; controllers only validate transport input and delegate.
- Backend authorization is mandatory. Hiding a UI control is not authorization.
- A user has exactly one active role in MVP. Never remove or deactivate the final active Admin.
- Access JWTs are short-lived. Refresh credentials are opaque, rotated, hashed, and held in an HTTP-only cookie.
- Store UUID v4 identifiers and UTC `timestamptz` values. Render plant-local time using the configured IANA timezone.
- Machine statuses are `IDLE`, `RUNNING`, and `DOWN`.
- `DOWN` means exactly one open downtime event. Entering `DOWN` opens it atomically; resolving it closes the event and sets `IDLE` atomically.
- An active machine cannot have two `IN_PROGRESS` Production Orders.
- Starting an order requires `IDLE` and makes the machine `RUNNING`; downtime preserves the active order.
- Completing maintenance never implicitly resolves downtime.
- Use optimistic numeric versions and `expectedVersion`; stale updates return `409` without partial changes.
- Lock machine rows before related work/order/event rows; eligibility user locks precede machine locks when needed.
- Assignment/account operations lock affected users in UUID order before machine/work rows; last-Admin changes first lock the ADMIN role row. Block account ineligibility while nonterminal assigned work exists.
- Persist consequential business audit events in the same transaction as the mutation. Audit insertion failure rolls back the mutation.
- Preserve historical references. MVP exposes archive operations for users and machines; no business hard-delete API exists.
- Validate at API boundaries, use explicit DTO fields, reject unknown fields, allowlist sort fields, and never expose entities.
- Flyway is the sole migration system. A migration-only backend image runs Flyway and exits; the runtime image only validates the schema. Hibernate runs in `validate` mode and must not create production tables.
- PostgreSQL constraints are final enforcement for uniqueness and lifecycle invariants.
- Integration tests use real PostgreSQL through Testcontainers; do not replace them with H2.
- Dashboard definitions must follow the documented formulas. Do not invent KPI data or call basic uptime OEE.
- Keep management endpoints private and logs free of passwords, hashes, tokens, cookies, and full request bodies.
- Required operational checks include auth, role denials, concurrent downtime/order attempts, audit rollback, stale versions, timezone boundaries, and archived history.
