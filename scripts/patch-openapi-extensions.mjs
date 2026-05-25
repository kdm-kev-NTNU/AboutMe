/**
 * Merges missing API paths into frontend/homepage/openapi/openapi.json for Orval.
 * Run: node scripts/patch-openapi-extensions.mjs
 */
import { readFileSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const openapiPath = join(root, 'frontend', 'homepage', 'openapi', 'openapi.json')
const doc = JSON.parse(readFileSync(openapiPath, 'utf8'))

const tagNames = [
  'Experiments',
  'AI cost controls',
]
for (const name of tagNames) {
  if (!doc.tags.some((t) => t.name === name)) {
    doc.tags.push({ name, description: name })
  }
}

Object.assign(doc.components.schemas, {
  SynthesizeRequest: {
    type: 'object',
    properties: { text: { type: 'string' } },
  },
  RealtimeStatusResponse: {
    type: 'object',
    properties: {
      enabled: { type: 'boolean' },
      standardEnabled: { type: 'boolean' },
      liveEnabled: { type: 'boolean' },
      voices: { type: 'array', items: { type: 'string' } },
      reasoningEfforts: { type: 'array', items: { type: 'string' } },
      defaultVoice: { type: 'string' },
      defaultReasoningEffort: { type: 'string' },
    },
  },
  RealtimeModelOption: {
    type: 'object',
    properties: {
      id: { type: 'string' },
      label: { type: 'string' },
      provider: { type: 'string' },
    },
  },
  EvalDatasetSummary: {
    type: 'object',
    properties: {
      id: { type: 'string' },
      name: { type: 'string' },
      exampleCount: { type: 'integer' },
    },
  },
  GenerateDatasetRequest: {
    type: 'object',
    properties: {
      name: { type: 'string' },
      description: { type: 'string' },
      documentId: { type: 'string', nullable: true },
      model: { type: 'string' },
      questionsPerChunk: { type: 'integer' },
      maxQuestions: { type: 'integer' },
      seed: { type: 'integer' },
    },
  },
  DatasetGenerationStartResponse: {
    type: 'object',
    properties: {
      generationId: { type: 'integer', format: 'int64' },
      status: { type: 'string' },
    },
  },
  DatasetGenerationStatusResponse: {
    type: 'object',
    properties: {
      id: { type: 'integer', format: 'int64' },
      status: { type: 'string' },
      questionsGenerated: { type: 'integer', nullable: true },
      resultDatasetId: { type: 'string', nullable: true },
      errorMessage: { type: 'string', nullable: true },
      createdAt: { type: 'string', nullable: true },
      completedAt: { type: 'string', nullable: true },
    },
  },
  RunExperimentRequest: {
    type: 'object',
    required: ['datasetId', 'generatorModel', 'evaluatorModel'],
    properties: {
      datasetId: { type: 'string' },
      datasetName: { type: 'string' },
      name: { type: 'string' },
      generatorModel: { type: 'string' },
      evaluatorModel: { type: 'string' },
      maxExamples: { type: 'integer', nullable: true },
    },
  },
  ExperimentRunSummaryResponse: {
    type: 'object',
    properties: {
      id: { type: 'integer', format: 'int64' },
      name: { type: 'string' },
      datasetName: { type: 'string' },
      generatorModel: { type: 'string' },
      evaluatorModel: { type: 'string' },
      status: { type: 'string' },
      totalExamples: { type: 'integer' },
      meanFaithfulness: { type: 'number', nullable: true },
      meanRelevance: { type: 'number', nullable: true },
      meanCorrectness: { type: 'number', nullable: true },
      meanConciseness: { type: 'number', nullable: true },
      meanLanguageConsistency: { type: 'number', nullable: true },
      errorMessage: { type: 'string', nullable: true },
      createdAt: { type: 'string' },
      completedAt: { type: 'string', nullable: true },
    },
  },
  ExperimentRunDetailResponse: {
    type: 'object',
    additionalProperties: true,
  },
  ExperimentsConfigResponse: {
    type: 'object',
    properties: {
      posthogConfigured: { type: 'boolean' },
      posthogHost: { type: 'string' },
    },
  },
  TranscribeResponse: {
    type: 'object',
    properties: { text: { type: 'string' } },
  },
})

const basicSecurity = [{ basicAuth: [] }]

Object.assign(doc.paths, {
  '/synthesize': {
    post: {
      tags: ['Chat'],
      summary: 'Synthesize speech',
      operationId: 'synthesizeSpeech',
      requestBody: {
        required: true,
        content: {
          'application/json': { schema: { $ref: '#/components/schemas/SynthesizeRequest' } },
        },
      },
      responses: {
        '200': { description: 'MP3 audio', content: { 'audio/mpeg': { schema: { type: 'string', format: 'binary' } } } },
        '400': { description: 'Bad Request', content: { 'application/json': { schema: { $ref: '#/components/schemas/ApiError' } } } },
      },
    },
  },
  '/transcribe': {
    post: {
      tags: ['Chat'],
      summary: 'Transcribe audio',
      operationId: 'transcribeAudio',
      requestBody: {
        required: true,
        content: {
          'multipart/form-data': {
            schema: {
              type: 'object',
              required: ['file'],
              properties: { file: { type: 'string', format: 'binary' } },
            },
          },
        },
      },
      responses: {
        '200': {
          description: 'OK',
          content: { 'application/json': { schema: { $ref: '#/components/schemas/TranscribeResponse' } } },
        },
      },
    },
  },
  '/realtime/status': {
    get: {
      tags: ['Chat'],
      summary: 'Realtime voice status',
      operationId: 'realtimeStatus',
      responses: {
        '200': {
          description: 'OK',
          content: { 'application/json': { schema: { $ref: '#/components/schemas/RealtimeStatusResponse' } } },
        },
      },
    },
  },
  '/realtime/models': {
    get: {
      tags: ['Chat'],
      summary: 'List realtime voice models',
      operationId: 'realtimeModels',
      responses: {
        '200': {
          description: 'OK',
          content: {
            'application/json': {
              schema: { type: 'array', items: { $ref: '#/components/schemas/RealtimeModelOption' } },
            },
          },
        },
      },
    },
  },
  '/admin/tools/experiments/config': {
    get: {
      tags: ['Experiments'],
      summary: 'Experiments PostHog config',
      operationId: 'experimentsConfig',
      security: basicSecurity,
      responses: {
        '200': {
          description: 'OK',
          content: { 'application/json': { schema: { $ref: '#/components/schemas/ExperimentsConfigResponse' } } },
        },
      },
    },
  },
  '/admin/tools/experiments/datasets': {
    get: {
      tags: ['Experiments'],
      summary: 'List eval datasets',
      operationId: 'experimentsListDatasets',
      security: basicSecurity,
      responses: {
        '200': {
          description: 'OK',
          content: {
            'application/json': {
              schema: { type: 'array', items: { $ref: '#/components/schemas/EvalDatasetSummary' } },
            },
          },
        },
      },
    },
  },
  '/admin/tools/experiments/datasets/generate': {
    post: {
      tags: ['Experiments'],
      summary: 'Start dataset generation',
      operationId: 'experimentsGenerateDataset',
      security: basicSecurity,
      requestBody: {
        required: true,
        content: {
          'application/json': { schema: { $ref: '#/components/schemas/GenerateDatasetRequest' } },
        },
      },
      responses: {
        '202': {
          description: 'Accepted',
          content: {
            'application/json': { schema: { $ref: '#/components/schemas/DatasetGenerationStartResponse' } },
          },
        },
      },
    },
  },
  '/admin/tools/experiments/datasets/generate/{id}/status': {
    get: {
      tags: ['Experiments'],
      summary: 'Dataset generation status',
      operationId: 'experimentsDatasetGenerationStatus',
      security: basicSecurity,
      parameters: [{ name: 'id', in: 'path', required: true, schema: { type: 'integer', format: 'int64' } }],
      responses: {
        '200': {
          description: 'OK',
          content: {
            'application/json': { schema: { $ref: '#/components/schemas/DatasetGenerationStatusResponse' } },
          },
        },
      },
    },
  },
  '/admin/tools/experiments/models': {
    get: {
      tags: ['Experiments'],
      summary: 'Models for experiments',
      operationId: 'experimentsListModels',
      security: basicSecurity,
      responses: {
        '200': {
          description: 'OK',
          content: {
            'application/json': {
              schema: { type: 'array', items: { $ref: '#/components/schemas/ChatModelOption' } },
            },
          },
        },
      },
    },
  },
  '/admin/tools/experiments/run': {
    post: {
      tags: ['Experiments'],
      summary: 'Start experiment run',
      operationId: 'experimentsStartRun',
      security: basicSecurity,
      requestBody: {
        required: true,
        content: {
          'application/json': { schema: { $ref: '#/components/schemas/RunExperimentRequest' } },
        },
      },
      responses: {
        '202': {
          description: 'Accepted',
          content: {
            'application/json': {
              schema: {
                type: 'object',
                properties: { runId: { type: 'integer', format: 'int64' } },
              },
            },
          },
        },
      },
    },
  },
  '/admin/tools/experiments/runs': {
    get: {
      tags: ['Experiments'],
      summary: 'List experiment runs',
      operationId: 'experimentsListRuns',
      security: basicSecurity,
      responses: {
        '200': {
          description: 'OK',
          content: {
            'application/json': {
              schema: { type: 'array', items: { $ref: '#/components/schemas/ExperimentRunSummaryResponse' } },
            },
          },
        },
      },
    },
  },
  '/admin/tools/experiments/runs/{id}': {
    get: {
      tags: ['Experiments'],
      summary: 'Experiment run detail',
      operationId: 'experimentsGetRun',
      security: basicSecurity,
      parameters: [{ name: 'id', in: 'path', required: true, schema: { type: 'integer', format: 'int64' } }],
      responses: {
        '200': {
          description: 'OK',
          content: {
            'application/json': { schema: { $ref: '#/components/schemas/ExperimentRunDetailResponse' } },
          },
        },
      },
    },
  },
  '/admin/tools/experiments/runs/{id}/status': {
    get: {
      tags: ['Experiments'],
      summary: 'Experiment run status',
      operationId: 'experimentsRunStatus',
      security: basicSecurity,
      parameters: [{ name: 'id', in: 'path', required: true, schema: { type: 'integer', format: 'int64' } }],
      responses: {
        '200': {
          description: 'OK',
          content: {
            'application/json': { schema: { $ref: '#/components/schemas/ExperimentRunSummaryResponse' } },
          },
        },
      },
    },
  },
  '/admin/tools/experiments/datasets/{id}': {
    delete: {
      tags: ['Experiments'],
      summary: 'Delete eval dataset',
      operationId: 'experimentsDeleteDataset',
      security: basicSecurity,
      parameters: [{ name: 'id', in: 'path', required: true, schema: { type: 'string' } }],
      responses: { '204': { description: 'No Content' } },
    },
  },
  '/admin/tools/ai/status': {
    get: {
      tags: ['AI cost controls'],
      summary: 'AI circuit status',
      operationId: 'aiAdminStatus',
      security: basicSecurity,
      responses: {
        '200': {
          description: 'OK',
          content: { 'application/json': { schema: { type: 'object', additionalProperties: true } } },
        },
      },
    },
  },
})

writeFileSync(openapiPath, `${JSON.stringify(doc, null, 2)}\n`, 'utf8')
console.log(`Patched ${openapiPath}`)
