# FactoryOS Security Design

## Security scope

FactoryOS is an online-only, single-plant web application. The backend is the
security boundary: frontend permission checks improve usability but never
replace backend authorization. All traffic is HTTPS in deployed environments;
the production frontend and API share an origin.

## Passwords and account lifecycle

- Hash passwords with BCrypt cost factor 12. Never store, return, or log plaintext passwords.
- Require at least 12 Unicode characters and at most 72 UTF-8 bytes to avoid BCrypt truncation. Do not impose composition rules; password-manager-generated values are valid.
- Admin-created temporary passwords require a password change before operational API access.
- Password changes, Admin resets, role changes, deactivation, and archive operations revoke the user's refresh sessions.
- Never return a password hash or a previously assigned temporary password.
- Bootstrap the first Admin using a one-time deployment secret. Disable bootstrap after the account is created.
- Keep historical actor references when a user is archived; archived users cannot authenticate.
- Prevent deactivation, demotion, or deletion of the final active Admin.
- Block deactivation/archive or removal of the Technician role while nonterminal assigned maintenance exists; reassign or cancel it first. Serialize assignment and eligibility changes on affected user rows. Serialize last-Admin mutations on the ADMIN role row before user locks.

## Authorization model

Each active user has exactly one role: Admin, Production Manager, Engineer,
Technician, Operator, or Viewer. Every protected request verifies that the
user remains active and loads the current role. JWT claims do not override a
current database role or account state.

- Admins manage users and roles and can read restricted audit operations.
- Production Managers manage production planning and maintenance assignment.
- Engineers manage the machine registry and maintenance planning.
- Technicians execute assigned maintenance work.
- Operators execute Production Orders and report/resolve downtime.
- Viewers have read-only operational and dashboard access.

Enforce both endpoint permissions and field-level permissions in application
services. Use explicit request DTOs and reject unknown or unauthorized fields
to prevent mass assignment. Every permission-matrix denial must have an
automated backend authorization test.

## JWT access tokens

- Access tokens expire after 10 minutes and are signed with RS256.
- Required claims are `sub`, `iss`, `aud`, `iat`, `exp`, and `jti`.
- Validate the signature, allowed algorithm, issuer, audience, expiry, and subject.
- Keep private signing keys outside the repository and support rotation with `kid`.
- Keep access tokens in frontend memory; never store credentials in local storage.
- Do not use a server-side HTTP session.
- An issued access token may remain valid for its remaining lifetime after logout; refresh is revoked immediately.

## Refresh sessions and replay detection

Refresh credentials are 256-bit cryptographically random opaque values. Store
only a SHA-256 token hash in `refresh_sessions`.

- Rotate the refresh token transactionally on every refresh.
- A refresh family has a seven-day absolute lifetime; rotation does not extend it.
- Track `family_id`, `consumed_at`, `revoked_at`, `replacement_id`, and expiry.
- Reuse of a consumed refresh token revokes the entire family.
- Logout revokes the refresh family.
- Password changes, resets, role changes, and account deactivation revoke all sessions.
- Coordinate refresh requests across browser tabs and in-flight API requests so parallel refresh calls do not cause accidental rotation races.
- Purge expired families only after their final expiry and a 30-day evidence period; retain replay evidence during that period.

The refresh token is sent only in a cookie with `HttpOnly`, `Secure` in
production, `SameSite=Strict`, and path `/api/v1/auth`. Cookie-authenticated
endpoints require a CSRF header and exact Origin validation.

## Browser and API protections

| Risk | Required control |
|---|---|
| SQL injection | Parameterized queries, approved sort fields, and no user-supplied SQL fragments |
| XSS | React escaping, plain-text descriptions, no untrusted HTML rendering, restrictive CSP |
| CSRF | CSRF token header plus exact Origin validation on cookie-authenticated endpoints |
| CORS abuse | Explicit development origins; same-origin production; no wildcard credentialed origins |
| Privilege escalation | Service-layer endpoint and field authorization on every mutation |
| Mass assignment | Explicit DTO fields; reject unknown and unauthorized fields |
| Credential guessing | Per-account and per-IP login limits with generic failure responses |
| Secret leakage | Redacted logs, private secret storage, HTTPS, and no frontend secret variables |
| Sensitive caching | `Cache-Control: no-store` on login, refresh, logout, and password responses |
| Clickjacking | Frame-ancestors policy in CSP and equivalent response headers |
| Unsafe transport | HSTS in production after HTTPS is confirmed |

Authentication failures must not reveal whether an email is registered. Error
responses must not include tokens, cookies, password hashes, or secret values.

## Rate limiting

The MVP has one backend instance and uses bounded in-process token buckets.
Revisit shared limiting before scaling to multiple backend instances.

| Operation | Limit |
|---|---|
| Login | 5 attempts/minute per normalized account key; 30/minute per IP |
| Refresh | 30/minute per session family and IP |
| General API | 300 requests/minute per authenticated user |

Return `429` with `Retry-After`. Account and IP keys expire. Trust forwarded IP
headers only from a configured reverse-proxy edge; otherwise use the direct
peer address. Rate-limit counters must not be keyed by raw credentials.

## Audit and security logging

Operational audit events are append-only and capture actor, action, entity
type, entity ID, safe before/after fields, timestamp, and trace ID. Business
mutation and audit insertion share one database transaction, so a failed audit
insert rolls back the mutation.

- Exclude passwords, password hashes, access tokens, refresh tokens, cookies, authorization headers, and complete sensitive request bodies.
- Application database privileges permit audit inserts but not audit updates/deletes. A separate migration/retention identity handles administration.
- MVP has no audit browsing API or UI. Restricted operations tooling may read security/audit records for Admins.
- Login failures, denied requests, replay detection, account changes, and rate-limit events go to structured security logs.
- Log only the normalized account identifier or user ID needed for investigation; avoid unnecessary personal data.

Structured logs include timestamp, severity, service, environment, trace ID,
request ID, authenticated user ID when available, event, and outcome. Never
log passwords, JWTs, cookies, authorization headers, or complete payloads.

## Secrets and deployment controls

- Store JWT signing keys, database credentials, bootstrap secrets, and edge credentials in a deployment secret store or restricted mounted files.
- Never commit secrets, generated credentials, or `.env` files. `.env.example` contains safe placeholders only.
- Production startup fails when required secrets are absent or insecure defaults remain.
- Database and management ports are private; expose only the HTTPS edge.
- Actuator health and metrics endpoints are restricted to the internal management network. Public health responses disclose no database or configuration details.
- Run frontend static serving and backend runtime containers as non-root users.
- Use pinned dependency versions and block unresolved critical/high image or dependency findings unless a documented exception is approved.
- Backups are encrypted, access-controlled, monitored, and tested through restoration drills.

## Security validation requirements

Before release, verify:

- JWT algorithm, issuer, audience, expiry, subject, and key rotation behavior.
- Refresh rotation, replay-family revocation, logout, expiry, and concurrent-refresh handling.
- CSRF token enforcement, Origin validation, cookie flags, CORS, CSP, and HSTS.
- Password hashing, temporary-password enforcement, generic login errors, and rate limits.
- Backend authorization for every role and endpoint, including field-level checks.
- Optimistic conflicts return `409` without partial updates.
- Audit failure rolls back the corresponding business mutation.
- Logs and error responses contain no credentials or secret values.
- Archive/deactivation preserves historical references while preventing new access.
- Production secrets, TLS, private database ports, restricted management endpoints,
  backup freshness, and restoration objectives are verified.
