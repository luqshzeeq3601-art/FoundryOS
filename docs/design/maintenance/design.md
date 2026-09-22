# Maintenance

- **Purpose/access:** work-order queue for preventive/corrective tasks. All roles see the tab; create action is shown to admin, production manager, and engineer.
- **Composition:** heading/action row, error banner, horizontal status and prescriptive-auto filters plus priority selector/search, paginated work-order table. Columns show WO number/title, target machine, priority, status, assigned technician, and actions. Priority/status badges signal urgency.
- **Interaction/states:** work orders poll every 5 seconds. Modal flows create, assign technician, complete with notes, and cancel with reason. A diagnostic-snapshot modal adds anomaly/health summary, sensor readings, harmonic peaks, recommended spare parts, and prescriptive guidance. Loading and empty rows are represented.
- **Responsive/design review:** filters and table scroll horizontally; diagnostic readings use a 2-to-4-column grid. The diagnostic view packs several technical panels into a scrollable modal, so check readability and focus/scroll containment on smaller tablets. Verify row action names and permissions for each role during QA.

Source: [MaintenanceView.tsx](../../../frontend/src/components/views/MaintenanceView.tsx).
