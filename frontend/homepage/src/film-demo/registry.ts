import type { Component } from 'vue'

export const DEMO_REGISTRY: Record<
  string,
  () => Promise<{ default: Component }>
> = {
  aboutme: () => import('./demos/aboutme/FilmDemoShell.vue'),
  krisefikser: () => import('./demos/krisefikser/FilmDemoShell.vue'),
  'explaining-markets': () => import('./demos/explaining-markets/FilmDemoShell.vue'),
}
