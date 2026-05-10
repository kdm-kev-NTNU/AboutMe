import fs from 'node:fs'
import path from 'node:path'
import type { Plugin } from 'vite'

/** JSON bundles imported by CareerView / ProjectsView; real copies are gitignored locally. */
export const PORTFOLIO_JSON_FILES = new Set([
  'projects.en.json',
  'projects.no.json',
  'education.en.json',
  'education.no.json',
  'workExperience.en.json',
  'workExperience.no.json',
  'courses.en.json',
  'courses.no.json',
])

/**
 * Resolves each portfolio `*.json` to the real file when present, otherwise to `*.stub.json`
 * so CI and fresh clones build without committing proprietary content.
 */
export function portfolioJsonFallbackPlugin(): Plugin {
  let root = process.cwd()

  return {
    name: 'portfolio-json-fallback',
    enforce: 'pre',
    configResolved(config) {
      root = config.root
    },
    resolveId(source, importer) {
      const cleanSource = source.replace(/\0/g, '').split('?', 1)[0]
      const base = path.basename(cleanSource)
      if (!PORTFOLIO_JSON_FILES.has(base)) {
        return null
      }

      let resolved: string
      if (cleanSource.startsWith('@/')) {
        resolved = path.normalize(path.join(root, 'src', cleanSource.slice(2)))
      } else if (path.isAbsolute(cleanSource)) {
        resolved = cleanSource
      } else if (importer) {
        resolved = path.normalize(path.resolve(path.dirname(importer), cleanSource))
      } else {
        return null
      }

      const dirTypes = `${path.sep}types${path.sep}`
      if (!resolved.includes(dirTypes)) {
        return null
      }

      if (fs.existsSync(resolved)) {
        return resolved
      }
      const stub = resolved.replace(/\.json$/i, '.stub.json')
      if (fs.existsSync(stub)) {
        return stub
      }
      return null
    },
  }
}
