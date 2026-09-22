# Machines

- **Purpose/access:** searchable machine registry; all roles can view the tab. Create/edit controls are shown to admin, production manager, and engineer; archive is admin-only. The status-override row button is rendered for every viewer of this tab.
- **Composition:** heading/action row, dismissible error banner, horizontal status-filter buttons plus serial/name search, wide registry table, pagination. Columns are serial, machine name, location, status, version, and actions. Badges and status colors encode operating condition.
- **Interaction/states:** register, edit specification, and operational-state override use separate modals; archive is a row action. Table has loading and empty rows, with pagination controls.
- **Responsive/design review:** header/filter controls wrap and the table scrolls horizontally. The dense action column uses compact icon buttons with `title`; verify keyboard names/focus and tap sizes on a tablet. Review whether status override should be role-gated in the UI as well as the API; this inventory does not verify backend authorization.

Source: [MachinesView.tsx](../../../frontend/src/components/views/MachinesView.tsx).
