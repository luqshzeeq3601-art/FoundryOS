# FoundryOS frontend design inventory

Code-based snapshot, 2026-09-22. Each page has its own `design.md`. This records the implemented layout and behavior, not a proposed redesign. No authenticated browser walkthrough, rendered mobile check, keyboard test, or API-data validation was performed.

## Shared design language

- **Shell:** `App.tsx` shows an initialization screen, then either Login or the authenticated layout. The latter stacks an offline banner (when `navigator.onLine` is false), sticky header, horizontally scrollable role-filtered tab bar, centered `max-w-7xl` content area, and footer. Tabs are local React state, not URL routes; switching tabs remounts views. A plant selector appears in the header from `sm` upward; telemetry clock/status appear from `md` upward.
- **Visual system:** dark substrate (`#0C0D0E`) and cards (`#121417`), thin borders (`#262930`), predominantly JetBrains Mono, uppercase operational labels, squared-off controls, red for hazard, amber for caution, green for healthy/success, and cyan for information. `index.css` adds hazard stripes, scanlines, custom scrollbars, and a 48px touch-target utility. ERP Hub uses a more local zinc/cyan/rose palette. Tokens are in `frontend/tailwind.config.js`.
- **Reusable pieces:** `IndustrialCard`, `IndustrialButton`, `IndustrialBadge`, `MetricTile`, `StatusBeacon`, and `Modal`. Small and medium buttons are 36px and 44px tall, respectively; large buttons are 48px. The shared modal has an overlay, 90vh maximum height, internal scrolling, and Escape/backdrop close.
- **Responsive pattern:** headings and controls stack at narrow widths; KPI grids grow from one/two columns to four/six; most dense tables remain horizontally scrollable. This is source-observed behavior, not proof of visual quality at a specific device width.
- **Cross-page review points:** many data tables and 10–11px labels may be hard to scan on factory tablets; the horizontal tab bar is long; several icon-only buttons rely on `title`; and the shared modal has no explicit dialog role, focus trap, initial focus, or focus restoration in its source. Audit these in a rendered, keyboard-only session. The header's `TELEMETRY: ACTIVE` label is unconditional, while the offline banner reacts only to browser network status. Do not interpret either as verified backend health.

## Pages

| Tab / state | Design file |
| --- | --- |
| Login / required password change | [login/design.md](login/design.md) |
| Dashboard | [dashboard/design.md](dashboard/design.md) |
| Fleet Benchmark | [fleet-analytics/design.md](fleet-analytics/design.md) |
| Edge Resilience | [edge-resilience/design.md](edge-resilience/design.md) |
| Digital SOP / Quality | [digital-sop/design.md](digital-sop/design.md) |
| Vibration / FFT | [vibration-health/design.md](vibration-health/design.md) |
| ERP / SCM Sync | [erp-integration/design.md](erp-integration/design.md) |
| Machines | [machines/design.md](machines/design.md) |
| Telemetry / IIoT | [live-telemetry/design.md](live-telemetry/design.md) |
| Downtime | [downtime/design.md](downtime/design.md) |
| Production | [production/design.md](production/design.md) |
| Maintenance | [maintenance/design.md](maintenance/design.md) |
| Hierarchy | [hierarchy/design.md](hierarchy/design.md) |
| Audit Log | [audit-log/design.md](audit-log/design.md) |
| Users / IAM | [admin-users/design.md](admin-users/design.md) |

Access descriptions refer to frontend visibility only; server authorization was not audited. Source: `frontend/src/App.tsx`, `frontend/src/components/common/`, `frontend/src/styles/index.css`, and `frontend/tailwind.config.js`.
