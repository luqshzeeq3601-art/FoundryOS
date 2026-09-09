# FactoryOS Product Backlog (Version 1.0.0 GA)

> [!NOTE]
> **Status: RELEASED & COMPLETED (v1.0.0 GA)**  
> All Sprint 1 through Sprint 5 epics and user stories (E1-S1 through E3-S4) have been implemented, verified, and tagged in release `v1.0.0`.
> For the upcoming multi-plant, IIoT, and predictive maintenance roadmap, see [Version 2 Implementation Backlog](file:///docs/BACKLOG_V2.md).

The backlog below details the implemented and verified single-plant, online-only MVP: identity,
RBAC, machines, basic Production Orders, linked downtime, maintenance,
dashboard, and essential audit records.

## Epics and actionable stories

### Epic 1 — Identity, RBAC and Core Setup

#### E1-S1 — Reproducible application foundation

**Story:** As a developer, I want reproducible environments so that changes
behave consistently across development, test, and production.

**Acceptance criteria**

- Fresh setup builds frontend/backend and starts the development stack.
- Missing production secrets prevent startup.
- CI executes the documented stages.
- Actuator readiness reflects database availability.
- Root `AGENTS.md` documents the task-based astra-orchestrator workflow and
  difficulty-based effort selection; `CODEX.md` links to it.

**Tasks**

- [x] Create root `AGENTS.md` with concise communication rules, task
  classification, delegation contract, model ownership, completion gate, and
  task-difficulty-based effort levels.
- [x] Establish backend module and frontend feature structure.
- [x] Add environment templates, container builds, Compose, and CI.
- [x] Configure Flyway and separate migration/runtime credentials.
- [x] Add structured logs, health checks, and test foundations.
- [x] Link `AGENTS.md`, `CODEX.md`, `ARCHITECTURE-ESSENTIALS.md`, and the
  astra-orchestrator skill in the implementation workflow.

#### E1-S2 — Secure sessions

**Story:** As a user, I want secure login and session renewal so that I can
access authorized operational tools.

**Acceptance criteria**

- Valid users authenticate; invalid credentials return a generic `401`.
- Refresh tokens rotate; replay revokes the family.
- Password changes and deactivation prevent further refresh.
- Temporary-password users cannot access operational endpoints.

**Tasks**

- [x] Implement users, roles, refresh sessions, and bootstrap.
- [x] Implement JWT verification, CSRF, cookies, and rate limits.
- [x] Build login, password-change, and session-expiry flows.
- [x] Test token expiry, concurrent refresh, replay, and logout.

#### E1-S3 — User administration and permissions

**Story:** As an Admin, I want to manage accounts and roles so that plant
access stays controlled.

**Acceptance criteria**

- Only Admins manage users.
- The final active Admin cannot be removed.
- Forbidden API operations return `403`.
- Role changes affect subsequent protected requests.

**Tasks**

- [x] Implement user-management contracts and screens.
- [x] Centralize permission checks.
- [x] Add the complete role authorization test matrix.
- [x] Add append-only audit persistence and mutation coverage.

### Epic 2 — Machine Registry and Downtime Tracking Engine

#### E2-S1 — Machine registry

**Story:** As an Engineer, I want a machine registry so that everyone uses
consistent equipment identities.

**Acceptance criteria**

- Serial uniqueness is case-normalized and enforced by PostgreSQL.
- Lists support approved filters and pagination.
- Version conflicts do not overwrite newer changes.
- Machines with ongoing operational records cannot be archived.

**Tasks**

- [x] Add schema, indexes, DTOs, and APIs.
- [x] Build list/detail/edit views.
- [x] Enforce RBAC, archive rules, and auditing.
- [x] Test uniqueness and concurrent edits.

#### E2-S2 — Linked downtime

**Story:** As an Operator, I want breakdown reporting to update machine status
immediately so that the plant sees consistent downtime information.

**Acceptance criteria**

- Entering `DOWN` creates one event atomically.
- Concurrent submissions cannot create two open events.
- Resolution closes the event and sets `IDLE`.
- Historical intervals cannot be backdated through MVP APIs.

**Tasks**

- [x] Implement the shared transition coordinator.
- [x] Add row locks, unique constraints, and conflict handling.
- [x] Build tablet report/resolve flows.
- [x] Test rollback, races, repeat requests, and permissions.

#### E2-S3 — Basic production execution

**Story:** As a Production Manager, I want basic Production Orders so that
planned work and reported output are visible.

**Acceptance criteria**

- Managers create, release, and cancel orders.
- Operators start, report, and complete orders.
- A machine cannot run two active orders.
- Output updates replace cumulative totals and reject stale versions.
- Downtime preserves the active order for later resumption.

**Tasks**

- [x] Implement order schema and lifecycle APIs.
- [x] Coordinate order start/closure with machine state.
- [x] Build planning and execution screens.
- [x] Test competing starts, output corrections, and downtime interactions.

### Epic 3 — Maintenance and Operational Dashboard

#### E3-S1 — Maintenance requests and ownership

**Story:** As an Operator, I want to request maintenance so that equipment
problems reach the responsible team.

**Acceptance criteria**

- Operational roles may create requests.
- Only Admin, Production Manager, or Engineer can assign work or change priority.
- Linked downtime must belong to the same machine.
- Technicians see and can filter their assignments.

**Tasks**

- [x] Add maintenance schema, contracts, filters, and views.
- [x] Enforce assignee and related-machine validation.
- [x] Add audit records and authorization tests.

#### E3-S2 — Maintenance execution

**Story:** As a Technician, I want to update assigned work so that managers
can track repair progress.

**Acceptance criteria**

- Technicians update only their own assigned work.
- Completion requires notes.
- Terminal records reject further updates.
- Completion does not silently resolve downtime.

**Tasks**

- [x] Implement transitions and field-level permissions.
- [x] Build tablet work execution.
- [x] Test reassignment, conflicts, completion, and cancellation.

#### E3-S3 — Operational KPIs

**Story:** As a Production Manager, I want a reconciled dashboard so that I
can assess current conditions and recent output.

**Acceptance criteria**

- Metrics match documented database calculations.
- Current-state and interval metrics are clearly labeled.
- Open downtime is clipped at the response snapshot time.
- Completed-order output is not presented as per-unit production timing.
- Viewer access is read-only.

**Tasks**

- [x] Implement indexed reporting queries and summary API.
- [x] Build dashboard, filters, drill-down links, and polling.
- [x] Test timezone boundaries, empty data, and archived references.
- [x] Run the performance acceptance profile.

#### E3-S4 — Release and operational readiness

**Story:** As the system owner, I want a recoverable, monitored release so that
plant operations can depend on FactoryOS.

**Acceptance criteria**

- Backup restoration meets RPO/RTO objectives.
- Release smoke tests pass for all six roles.
- Alerts, rollback instructions, and incident ownership are documented.
- No unresolved release-blocking defects remain.

**Tasks**

- [x] Complete release readiness and post-release validation.
- [x] Rehearse migrations and rollback compatibility.
- [x] Perform restore and operational walkthroughs.
- [x] Prepare release evidence and administrator instructions.

## Delivery sequence

| Sprint | Primary outcome |
|---|---|
| 1 | E1-S1 and E1-S2: environment and secure sessions |
| 2 | E1-S3 and E2-S1: permissions, auditing, machine registry |
| 3 | E2-S2 and E2-S3: downtime and production execution |
| 4 | E3-S1 and E3-S2: maintenance lifecycle |
| 5 | E3-S3 and E3-S4: dashboard, performance, release readiness |

Sprints are sequencing units, not delivery-date commitments. Estimate duration
after confirming team capacity.

## Agent effort guidance

Use the lowest effort sufficient for reliable completion, based on task
difficulty rather than agent role. The root agent sets the initial effort for
each delegated task and raises it only when evidence shows the initial level is
insufficient. Split an overly broad task before increasing effort.

| Difficulty | Effort | Typical FactoryOS work |
|---|---|---|
| Simple | `low` | Locate files, small documentation edits, routine checks |
| Moderate | `medium` | Trace one module, standard CRUD, focused tests |
| Complex | `high` | Cross-module changes, concurrency, authentication |
| Very complex | `xhigh` | Ambiguous failures, security analysis, architecture tradeoffs |
| Exceptional | `max` | Persistent reasoning blockers after narrower attempts |

Routine explorers, workers, testers, and researchers use GPT-5.6 Luna. The
root agent retains its configured model. Use GPT-6 Astra for independent review
when the astra-orchestrator workflow determines that review materially helps.

## Definition of done

- [x] Acceptance criteria demonstrated.
- [x] Permissions enforced and tested.
- [x] API, schema, and documentation agree.
- [x] Audit and failure behavior verified.
- [x] Relevant automated checks pass.
- [x] Desktop/tablet workflow checked.
- [x] No secrets or unresolved critical/high security findings.
- [x] Migration and operational impact reviewed.

## Lifecycle coverage

| Step | Document or backlog location |
|---|---|
| 1 Problem and goals | `docs/PRD.md` |
| 2 Users and RBAC | `docs/PRD.md` |
| 3 Phased scope | `docs/PRD.md` |
| 4 Requirements | `docs/PRD.md` |
| 5 Use cases | `docs/PRD.md`, this backlog |
| 6 Architecture | `docs/ARCHITECTURE.md` |
| 7 Database design | `docs/Data.md` |
| 8 API contracts | `docs/API-DESIGN.md` |
| 9 Security design | `docs/Security.md` |
| 10 UI/UX | `docs/PRD.md` |
| 11 Technology stack | `docs/CODEX.md` |
| 12 Coding standards | `docs/CODEX.md`, `docs/ARCHITECTURE-ESSENTIALS.md` |
| 13 Git workflow | `docs/CODEX.md` |
| 14 Testing | `docs/CODEX.md` |
| 15 CI/CD | `docs/CODEX.md` |
| 16 Authentication controls | `docs/Security.md` |
| 17 Observability | `docs/ARCHITECTURE.md` |
| 18 Task breakdown | this backlog |
| 19 Release readiness | E3-S4 and `docs/CODEX.md` |
| 20 Post-release validation | E3-S4 and `docs/CODEX.md` |

