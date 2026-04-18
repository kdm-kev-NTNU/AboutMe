import { mkdirSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const url = process.env.OPENAPI_URL ?? 'http://localhost:8080/v3/api-docs'
const outPath = join(__dirname, '..', 'openapi', 'openapi.json')

const res = await fetch(url)
if (!res.ok) {
  console.error(`Failed to fetch OpenAPI: ${res.status} ${res.statusText}`)
  process.exit(1)
}
const doc = await res.json()
mkdirSync(dirname(outPath), { recursive: true })
writeFileSync(outPath, JSON.stringify(doc, null, 2), 'utf8')
console.log(`Wrote ${outPath}`)
