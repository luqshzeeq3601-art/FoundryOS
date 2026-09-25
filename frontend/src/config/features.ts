/**
 * Test and simulation tools (synthetic telemetry, drill simulators, barcode test codes)
 * are hidden in production builds unless VITE_ENABLE_TEST_TOOLS=true.
 */
export const TEST_TOOLS_ENABLED =
  import.meta.env.DEV || import.meta.env.VITE_ENABLE_TEST_TOOLS === 'true';
