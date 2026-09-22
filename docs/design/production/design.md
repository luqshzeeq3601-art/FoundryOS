# Production

- **Purpose/access:** manage production orders, progress, and traceability; all roles see the tab, while new-order and certain transition/cancel controls are production-manager/admin gated.
- **Composition:** heading with schedule action, error banner, horizontal state filters, machine selector and search, paginated order table. Columns prioritize order/item, assigned asset, good-versus-target progress, scrap rate, status, and controls. Progress bars/badges make yield and lifecycle visible.
- **Interaction/states:** orders poll every 5 seconds. Modals schedule an order, update good/scrap counters, and confirm lifecycle transitions with notes. Active orders expose material-lot barcode scanning through a separate scanner modal. Loading and empty table states are present.
- **Responsive/design review:** controls and table allow horizontal scrolling; the live counter uses large increment/decrement affordances. Check whether abbreviated row actions and compact status labels remain understandable at tablet width, and verify scanner fallback/failure messaging with real hardware.

Source: [ProductionView.tsx](../../../frontend/src/components/views/ProductionView.tsx). Scanner: [BarcodeScannerModal.tsx](../../../frontend/src/components/common/BarcodeScannerModal.tsx).
