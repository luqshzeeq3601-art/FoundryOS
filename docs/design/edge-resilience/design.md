# Edge Resilience

- **Purpose/access:** inspect edge gateways and exercise offline buffering/reconciliation; tab appears for admin, production manager, engineer, and technician.
- **Composition:** header/refresh, four KPI tiles, two-column gateway hardware matrix with status and buffer gauges, then a prominent WAN-disconnect rehearsal card. Beneath it, a one-third batch list sits alongside a two-thirds synchronized transaction ledger. A manifest modal lists cached orders, machines, and BOM items.
- **Interaction/states:** gateways poll every 5 seconds; batches/transactions every 10. Gateway cards expose manifest and heartbeat actions. The rehearsal form takes duration, output, scans, downtime and reason, then displays processed/duplicate counts, reconciliation status, and a replay test. This is a simulation workflow, not evidence of a real outage.
- **Responsive/design review:** metrics go 2-to-4, gateways 1-to-2, bottom panels stack before `lg`; ledger has fixed maximum height and horizontal scrolling. The input grid reaches seven columns on large screens, so verify labels and numeric controls at intermediate widths. The default gateway identifier is hard-coded; check the selected state when live gateway data differs.

Source: [EdgeResilienceView.tsx](../../../frontend/src/components/views/EdgeResilienceView.tsx).
