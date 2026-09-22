# Telemetry / IIoT

- **Purpose/access:** inspect machine sensor values, historical rollups, PLC tag mapping, and ingestion activity. Tab is visible to all roles; tag and ingest actions are admin/engineer gated, retention settings admin-only.
- **Composition:** machine selector with auto-poll, retention, simulation and add-tag controls; asset/gateway status bar; four sensor readouts (spindle speed, vibration, motor current, bearing temperature); historical time-series panel with tag/range controls, aggregate strip, custom SVG min/max envelope and mean trendline; then tag-mapping table beside a scrolling ingestion ticker.
- **Interaction/states:** initial loading view, selected-machine changes, periodic refresh, chart hover crosshair/tooltip, manual series refresh, tag registration/removal, and retention-policy modal. Ingest simulation sends generated speed/vibration/current/temperature points; it is explicitly synthetic input even though real API endpoints and displays are also used.
- **Responsive/design review:** status/readouts collapse to one/two columns; the lower table/feed becomes a 1/2 split at `lg`, with horizontal table scroll. Chart detail and hover-only information need keyboard/touch alternatives. Confirm auto-poll state and empty/error wording under a disconnected gateway in a real session.

Source: [LiveTelemetryView.tsx](../../../frontend/src/components/views/LiveTelemetryView.tsx).
