/// <reference types="vite/client" />

declare const __APP_VERSION__: string;

interface ImportMetaEnv {
  readonly VITE_ENABLE_TEST_TOOLS?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
