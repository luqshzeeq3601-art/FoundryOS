# Hierarchy

- **Purpose/access:** explore the plant structure and add organizational nodes. Admin, production manager, and engineer see the tab; create controls appear for admin/production manager.
- **Composition:** header with plant context and refresh, four compact metric cards, then a 1-to-3-column explorer. The wider left pane is a nested enterprise → area → line → work-cell → machine tree; the right pane holds selected-node details and a tenant-isolation information card.
- **Interaction/states:** expand/collapse and select nodes; add-area, add-line, and add-cell dialogs collect codes/names (plus area description). Loading/empty tree treatment and refresh are present.
- **Responsive/design review:** the explorer stacks at narrow widths. Deep indentation plus inline add buttons may compress labels in small viewports; verify keyboard tree semantics and focus. The security card presents tenant-isolation claims as UI copy; the code review here does not verify those backend guarantees.

Source: [HierarchyManagementView.tsx](../../../frontend/src/components/views/HierarchyManagementView.tsx).
