# Digital SOP / Quality

- **Purpose/access:** bind a production order to an SOP, execute checklist steps, and show quality-gate status. All listed roles can see the tab; sign-off action is shown to admin, production manager, engineer, and technician.
- **Composition:** header with order selector and session action, notification strip, conditional compliant/blocked quality-gate banner. Main workspace is a 5/7 split at `lg`: SOP category/template and safety details above a CAD-style SVG viewport on the left; step navigation, progress dots, instructions, measurements/photo/barcode/notes inputs and actions on the right. Sign-off opens a stamp-preview modal.
- **Interaction/states:** order selection can match a product SOP; start/switch session, move between steps, record evidence and submit quality sign-off. Steps may expose numeric tolerance bounds, photo, or barcode input. Order/SOP/gate queries poll every 5–10 seconds.
- **Responsive/design review:** workspace stacks on narrow screens; long steps and progress dots scroll. The photo-capture handler creates a mock URL rather than capturing/uploading a photo, while its message claims capture and hashing. Treat this as a prototype affordance and relabel or implement before operational use. The blueprint is code-drawn SVG, not an uploaded CAD drawing.

Source: [DigitalSopView.tsx](../../../frontend/src/components/views/DigitalSopView.tsx).
