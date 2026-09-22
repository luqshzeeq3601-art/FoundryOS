# Downtime

- **Purpose/access:** track stops, classify root cause, and resolve events; tab visible to all roles.
- **Composition:** heading with record/evaluate actions, a pending-root-cause attention banner, dismissible result banners, four analytics blocks (micro-stops, major stops, ratio, sensor detection), status/machine filters, then paginated events table. The table foregrounds machine, trigger, reason, start, duration/status, notes, and action.
- **Interaction/states:** events poll every 4 seconds, pending classifications every 3, summary every 10. Separate modals handle operator root-cause acknowledgement, manual downtime logging, and resolution. Sensor evaluation and summary refresh provide feedback.
- **Responsive/design review:** analytics grid grows 1/2/4 columns; the event table scrolls horizontally. The high-density table and several alert tiers may compete for attention during an incident. Keep the distinction between sensor-detected and manually recorded events clear; verify banner wrapping and modal focus in a rendered session.

Source: [DowntimeView.tsx](../../../frontend/src/components/views/DowntimeView.tsx).
