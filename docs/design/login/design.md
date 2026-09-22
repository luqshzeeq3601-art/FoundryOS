# Login and password change

- **Purpose/access:** unauthenticated entry; authenticated users flagged `mustChangePassword` see a mandatory password-change variant before the shell appears.
- **Composition:** full-height centered card (`max-w-md`) on dark substrate. Login uses a red hazard-stripe top edge, compact F mark, terminal eyebrow, two stacked inputs, full-width red submit action, and small deployment caption. The password-change card switches to amber border/stripe, security message, two password fields, and amber action. Decorative node/build markers sit in opposite background corners on login.
- **Interaction/states:** controlled email/password and new/confirm-password forms, disabled processing button, in-card error banner. The login error distinguishes unavailable API from rejected credentials; password change checks matching passwords and minimum length.
- **Design review:** the login form is prefilled with an administrative email and password in source. Remove this default before any real deployment; it is also a misleading first-use affordance. Labels are visible text but are not programmatically associated with inputs (`htmlFor`/`id` absent). The background latency/build claim is static copy, not measured telemetry. Verify focus, contrast, and small-screen corner-marker collisions in a browser.

Source: [LoginView.tsx](../../../frontend/src/components/views/LoginView.tsx). Shared button: [IndustrialButton.tsx](../../../frontend/src/components/common/IndustrialButton.tsx).
