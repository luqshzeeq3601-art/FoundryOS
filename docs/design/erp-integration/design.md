# ERP / SCM Sync

- **Purpose/access:** material backflushing/BOM costing and ERP connector sync; tab appears for admin, production manager, engineer, technician, and viewer.
- **Composition:** page-local zinc/cyan header and refresh, conditional rose variance alert, action message, and two cyan-underlined sub-tabs. Material tab uses a 2/1 BOM-explosion/recording split, then inventory and consumption-ledger tables. Connector tab uses a 2/1 layout for connector cards plus sync history, and outbound confirmation dispatcher/feed. Payload opens in a JSON modal.
- **Interaction/states:** material product/quantity input requests a BOM explosion; output recording/backflush includes a variance-simulation toggle. Connector actions trigger sync, inspect payloads, and dispatch confirmations. Data reloads every 15 seconds; actions and errors show inline feedback. Variance alert links back to the material ledger.
- **Responsive/design review:** panels stack below `lg` and tables scroll horizontally, but the two long sub-tab labels have no explicit overflow wrapper in source; verify narrow-phone behavior. The palette and extra page padding differ from the shared shell, creating a visibly separate sub-system. Clearly label injected variance as a test and distinguish connector status from a successfully completed end-to-end ERP exchange.

Source: [ErpIntegrationHubView.tsx](../../../frontend/src/components/views/ErpIntegrationHubView.tsx).
