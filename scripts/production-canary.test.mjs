import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import { classifyDeployedRevision } from './production-canary.mjs'

const DEPLOYED = 'eab576ceebd3e87e6370bb9328471397d001f97c'
const MAIN = '56e6e0ffdac67b1f7a3fd185a03fea9d029107d5'

describe('classifyDeployedRevision', () => {
  it('is inconclusive when no expected revision is configured', () => {
    const verdict = classifyDeployedRevision({ deployed: DEPLOYED })
    assert.equal(verdict.status, 'INCONCLUSIVE')
  })

  it('fails when the actuator payload has no revision', () => {
    const verdict = classifyDeployedRevision({ expected: MAIN })
    assert.equal(verdict.status, 'FAIL')
  })

  it('passes when the deployed SHA matches main', () => {
    const verdict = classifyDeployedRevision({ deployed: DEPLOYED, expected: DEPLOYED })
    assert.equal(verdict.status, 'PASS')
  })

  it('passes when Railway reports a unique prefix of main', () => {
    const verdict = classifyDeployedRevision({
      deployed: MAIN.slice(0, 12),
      expected: MAIN,
    })
    assert.equal(verdict.status, 'PASS')
  })

  it('passes when main is ahead only outside backend/**', () => {
    const verdict = classifyDeployedRevision({
      deployed: DEPLOYED,
      expected: MAIN,
      compare: {
        status: 'ahead',
        files: [
          'frontend/homepage/src/types/projects.en.json',
          'frontend/homepage/src/views/ReasonView.vue',
        ],
      },
    })
    assert.equal(verdict.status, 'PASS')
    assert.match(verdict.detail, /no backend\/\*\* changes/)
  })

  it('fails when main changed a file under backend/', () => {
    const verdict = classifyDeployedRevision({
      deployed: DEPLOYED,
      expected: MAIN,
      compare: {
        status: 'ahead',
        files: [
          'frontend/homepage/src/types/projects.en.json',
          'backend/src/main/resources/realtime/kevin-profile.json',
        ],
      },
    })
    assert.equal(verdict.status, 'FAIL')
    assert.match(verdict.detail, /kevin-profile\.json/)
  })

  it('fails closed when the compare API is unavailable and the SHAs differ', () => {
    const verdict = classifyDeployedRevision({
      deployed: DEPLOYED,
      expected: MAIN,
      compare: { unavailable: true, detail: 'GitHub compare HTTP 503' },
    })
    assert.equal(verdict.status, 'FAIL')
    assert.match(verdict.detail, /HTTP 503/)
  })

  it('fails when the compare file list is truncated', () => {
    const verdict = classifyDeployedRevision({
      deployed: DEPLOYED,
      expected: MAIN,
      compare: { status: 'ahead', files: ['README.md'], truncated: true },
    })
    assert.equal(verdict.status, 'FAIL')
  })

  it('fails when the deployed revision is not an ancestor of main', () => {
    const verdict = classifyDeployedRevision({
      deployed: DEPLOYED,
      expected: MAIN,
      compare: { status: 'diverged', files: [] },
    })
    assert.equal(verdict.status, 'FAIL')
    assert.match(verdict.summary, /not an ancestor/)
  })
})
