# Dashboard

- **Purpose/access:** default authenticated plant overview, visible to all roles in the tab bar.
- **Composition:** conditional full-width red critical alert when machines are down; OEE panel with heading, live badge, refresh and emergency action; four OEE breakdown cells; four status-colored KPI tiles; then a responsive machine-fleet card grid. Each machine card shows serial, status beacon, name, location, version, and action status.
- **Interaction/states:** summary and first 50 machines poll every 5 seconds. Emergency breakdown modal selects asset/reason, collects a description, and can create a work order. Action feedback appears above the overview. Missing summary renders `--`; an empty fleet gives a register-assets instruction.
- **Responsive/design review:** OEE is 2 columns then 4; KPIs are 1/2/4; machine cards grow 1/2/3/4. The critical alert and down-state text pulse continuously; review reduced-motion support and attention cost. The fleet card's `TOTAL ASSETS` counts the fetched first page, not necessarily the whole plant. Refresh is icon-only with a `title`, so verify its accessible name.

Source: [DashboardView.tsx](../../../frontend/src/components/views/DashboardView.tsx).
