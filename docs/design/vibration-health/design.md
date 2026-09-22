# Vibration / FFT

- **Purpose/access:** inspect machine vibration health and spectral peaks; tab appears for admin, production manager, engineer, technician, and viewer.
- **Composition:** header with machine selector, action notification, six fleet-health metric cells, then a 4/8 split at `lg`. Left column contains circular health score, physical sensor values, ISO severity scale, diagnostic recommendation and active maintenance ticket. Right column contains an SVG FFT spectrum with grid/peak callouts and a harmonic-classification table. A bottom panel offers synthetic burst rehearsal presets.
- **Interaction/states:** machine selection drives health/spectrum/ticket queries (5–10-second polling). Burst buttons send `NORMAL`, 1X unbalance, 2X misalignment, or bearing-fault scenarios and display response feedback. The rehearsal panel should not be interpreted as a real sensor reading.
- **Responsive/design review:** top metrics grow 2/3/6 columns and the diagnostic/chart panes stack on narrow screens; peak table scrolls horizontally. Small FFT labels and color-coded severity need visual and contrast QA. The scale is labelled ISO 10816-3 in UI; this source review does not validate the engineering thresholds.

Source: [VibrationHealthView.tsx](../../../frontend/src/components/views/VibrationHealthView.tsx).
