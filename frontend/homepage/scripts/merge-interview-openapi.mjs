import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const dir = path.dirname(fileURLToPath(import.meta.url))
const basePath = path.join(dir, '..', 'openapi', 'openapi.json')
const exportPath = path.join(dir, '..', 'openapi', 'openapi-export-snapshot.json')

const base = JSON.parse(fs.readFileSync(basePath, 'utf8'))
const exported = JSON.parse(fs.readFileSync(exportPath, 'utf8'))

const interviewPathPrefix = '/admin/tools/interview'

const interviewPaths = Object.fromEntries(
  Object.entries(exported.paths ?? {}).filter(([p]) => p.startsWith(interviewPathPrefix)),
)

function collectSchemaRefs(node, out) {
  if (!node || typeof node !== 'object') return
  if (typeof node.$ref === 'string' && node.$ref.startsWith('#/components/schemas/')) {
    out.add(node.$ref.replace('#/components/schemas/', ''))
  }
  if (Array.isArray(node)) {
    node.forEach((n) => collectSchemaRefs(n, out))
    return
  }
  for (const v of Object.values(node)) collectSchemaRefs(v, out)
}

const exportedSchemas = exported.components?.schemas ?? {}
const schemaNames = new Set()
collectSchemaRefs(interviewPaths, schemaNames)

const visited = new Set()
const queue = [...schemaNames]
while (queue.length > 0) {
  const name = queue.shift()
  if (visited.has(name)) continue
  visited.add(name)
  const extra = new Set()
  collectSchemaRefs(exportedSchemas[name], extra)
  for (const n of extra) {
    schemaNames.add(n)
    if (!visited.has(n)) queue.push(n)
  }
}

base.components ??= { schemas: {} }
base.components.schemas ??= {}

for (const name of schemaNames) {
  if (exportedSchemas[name]) {
    base.components.schemas[name] = exportedSchemas[name]
  }
}

base.paths = { ...base.paths, ...interviewPaths }

base.tags ??= []
if (!base.tags.some((t) => t.name === 'Admin interview')) {
  base.tags.push({
    name: 'Admin interview',
    description: 'Voice interview practice with document context (ADMIN)',
  })
}

const renameOps = {
  uploadDocument: 'interviewDocumentsUpload',
  createTextDocument: 'interviewDocumentsCreateText',
  getDocument: 'interviewDocumentsGet',
  createSession: 'interviewSessionsCreate',
  listSessions: 'interviewSessionsList',
  getSession: 'interviewSessionsGet',
  appendTurns: 'interviewSessionsAppendTurns',
  finalizeSession: 'interviewSessionsFinalize',
  createRealtimeSession: 'interviewSessionsRealtimeSession',
  getTranscript: 'interviewTranscriptsGet',
  cleanTranscript: 'interviewTranscriptsClean',
  ingestTranscript: 'interviewTranscriptsIngest',
  deleteSession: 'interviewSessionsDelete',
}

for (const pathItem of Object.values(interviewPaths)) {
  for (const op of Object.values(pathItem)) {
    if (op && typeof op === 'object' && typeof op.operationId === 'string') {
      const next = renameOps[op.operationId]
      if (next) op.operationId = next
      if (op.security == null) {
        op.security = [{ basicAuth: [] }]
      }
    }
  }
}

fs.writeFileSync(basePath, JSON.stringify(base, null, 2) + '\n')
console.log(`Merged ${Object.keys(interviewPaths).length} interview paths and ${schemaNames.size} schemas`)
