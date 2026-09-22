# Fleet Benchmark

- **Purpose/access:** compare OEE across plants/lines; tab appears for admin, production manager, engineer, and viewer.
- **Composition:** federation header with 24H/7D/30D/ALL period selector and refresh, six compact fleet KPI cells, then a wide ranked matrix. The matrix compares OEE against fleet average with availability/performance/quality, output, scrap, and running/idle/down machine counts. Expanded plant rows show line-level breakdowns. Bottom controls export CSV/PDF or dispatch an executive report.
- **Interaction/states:** matrix polls every 30 seconds; search and status filter narrow the table; expand/collapse exposes lines; export/dispatch show progress and feedback. Loading and empty table states are present.
- **Responsive/design review:** KPI grid grows 2/3/6 columns, but the matrix remains horizontally scrollable with many columns. Keep row identity visible during horizontal inspection in future iterations. The source uses `hazard-green` utility names in parts of this page, whereas the Tailwind extension defines `terminal-green`; verify those green cues in a rendered build.

Source: [EnterpriseFleetAnalyticsView.tsx](../../../frontend/src/components/views/EnterpriseFleetAnalyticsView.tsx).
