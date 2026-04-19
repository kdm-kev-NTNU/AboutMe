/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_CONSENTIFY_TOKEN: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
