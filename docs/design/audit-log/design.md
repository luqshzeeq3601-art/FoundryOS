# Audit Log

- **Purpose/access:** inspect immutable-looking event history; frontend tab is visible to admin, production manager, and engineer. This document describes presentation only, not audit integrity.
- **Composition:** compact heading, entity-type selector and action search, paginated event-stream table. Columns show UTC timestamp, action, entity type/ID, trace ID, and state-diff action. Expanded rows reveal before/after JSON in a two-column comparison.
- **Interaction/states:** filter/search reset pagination, row expansion reveals JSON, table shows loading/empty states, and pagination controls move through results. No write modal is present.
- **Responsive/design review:** table and JSON panels scroll horizontally; before/after stacks to one column below `md`. Long trace IDs and JSON can dominate the viewport, so verify truncation, copyability, keyboard expansion, and the readability of an empty-versus-null state.

Source: [AuditLogView.tsx](../../../frontend/src/components/views/AuditLogView.tsx).
