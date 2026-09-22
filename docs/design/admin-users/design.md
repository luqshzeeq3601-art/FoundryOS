# Users / IAM

- **Purpose/access:** admin-only identity and role management tab.
- **Composition:** heading with provision action, dismissible error/success banners, role and active-status filter buttons with name/email search, paginated user table. Columns show display name, email, role/privilege, status, policy flags, and row actions.
- **Interaction/states:** separate modals provision a user with temporary password, edit role/status, and force a password reset. Archive is available from each row. Query-driven loading, empty state, mutation processing, and response banners are present.
- **Responsive/design review:** filters and table scroll horizontally. Small action icons for reset/edit/archive depend on `title` labels; verify accessible names and destructive-action distinction with keyboard and touch. Temporary credentials are sensitive, so avoid exposing them in screenshots or design examples.

Source: [AdminUsersView.tsx](../../../frontend/src/components/views/AdminUsersView.tsx).
