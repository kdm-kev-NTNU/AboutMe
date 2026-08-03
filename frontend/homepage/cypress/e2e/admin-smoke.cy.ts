/**
 * Smoke navigation for admin hub + pipeline when an ADMIN session is restored from sessionStorage.
 * API responses are stubbed so the suite does not require a running backend.
 */

describe('Admin smoke', () => {
  function seedAdminSession(win: Window) {
    win.sessionStorage.setItem(
      'auth',
      JSON.stringify({
        username: 'admin',
        role: 'ADMIN',
        basicToken: win.btoa('admin:pass'),
      }),
    )
  }

  beforeEach(() => {
    cy.intercept('GET', '**/api/auth/me', {
      statusCode: 200,
      body: { username: 'admin', role: 'ADMIN' },
    }).as('authMe')

    cy.intercept('GET', '**/api/health/chroma', {
      statusCode: 200,
      body: {
        healthy: true,
        collectionName: 'portfolio-documents',
        embeddingCount: 0,
      },
    }).as('chromaHealth')

    cy.intercept('GET', '**/api/admin/tools/documents/files', {
      statusCode: 200,
      body: [],
    }).as('adminFiles')

    cy.intercept('GET', '**/api/admin/tools/documents/collections', {
      statusCode: 200,
      body: {
        activeCollectionName: 'portfolio-documents',
        activeCollectionEmbeddingCount: 0,
        collections: [{ id: 'col-1', name: 'portfolio-documents' }],
      },
    }).as('adminCollections')

    cy.intercept('GET', '**/api/admin/tools/documents', {
      statusCode: 200,
      body: [],
    }).as('adminDocumentsList')
  })

  it('shows internal tools hub and document pipeline for an ADMIN session', () => {
    cy.visit('/admin/tools', {
      onBeforeLoad(win) {
        seedAdminSession(win)
      },
    })
    cy.wait('@authMe')
    cy.wait('@chromaHealth')
    cy.contains('Internal tools').should('be.visible')
    cy.contains('Document pipeline').should('be.visible')

    cy.visit('/admin/pipeline', {
      onBeforeLoad(win) {
        seedAdminSession(win)
      },
    })
    cy.wait('@authMe')
    cy.wait('@adminDocumentsList')
    cy.wait('@adminCollections')
    cy.contains('Document pipeline').should('be.visible')
    cy.contains('PostgreSQL / pgvector').should('be.visible')
  })
})
