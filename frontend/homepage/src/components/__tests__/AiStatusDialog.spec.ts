import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import AiStatusDialog from '../AiStatusDialog.vue'

/** Minimal dialog stack so AiStatusDialog can render without Reka UI internals. */
function dialogStubs() {
  return {
    Dialog: {
      props: ['open'],
      emits: ['update:open'],
      template:
        '<div v-if="open" data-testid="dialog-root" role="dialog"><slot /></div>',
    },
    DialogContent: { template: '<div data-testid="dialog-content"><slot /></div>' },
    DialogHeader: { template: '<div><slot /></div>' },
    DialogTitle: { template: '<h2><slot /></h2>' },
    DialogDescription: { template: '<p><slot /></p>' },
    DialogFooter: { template: '<div><slot /></div>' },
    AlertTriangle: { template: '<svg data-testid="alert-icon" />' },
  }
}

describe('AiStatusDialog.vue', () => {
  it('renders title, description, and message when open', () => {
    const wrapper = mount(AiStatusDialog, {
      attachTo: document.body,
      props: {
        open: true,
        title: 'Something failed',
        description: 'Please try later.',
        message: 'Detailed error text',
      },
      global: {
        stubs: {
          ...dialogStubs(),
          Button: {
            props: ['variant'],
            template: '<button type="button"><slot /></button>',
          },
        },
      },
    })

    expect(wrapper.text()).toContain('Something failed')
    expect(wrapper.text()).toContain('Please try later.')
    expect(wrapper.text()).toContain('Detailed error text')
    wrapper.unmount()
  })

  it('emits update:open false when dismiss is clicked', async () => {
    const wrapper = mount(AiStatusDialog, {
      attachTo: document.body,
      props: {
        open: true,
        title: 'T',
        message: 'M',
      },
      global: {
        stubs: {
          ...dialogStubs(),
          Button: {
            props: ['variant'],
            template: '<button type="button"><slot /></button>',
          },
        },
      },
    })

    const buttons = wrapper.findAll('button')
    const dismiss = buttons.find((b) => b.text() === 'OK')
    expect(dismiss).toBeDefined()
    await dismiss!.trigger('click')

    expect(wrapper.emitted('update:open')?.[0]).toEqual([false])
    wrapper.unmount()
  })

  it('uses custom dismissLabel', async () => {
    const wrapper = mount(AiStatusDialog, {
      attachTo: document.body,
      props: {
        open: true,
        title: 'T',
        message: 'M',
        dismissLabel: 'Close',
      },
      global: {
        stubs: {
          ...dialogStubs(),
          Button: {
            props: ['variant'],
            template: '<button type="button"><slot /></button>',
          },
        },
      },
    })

    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('update:open')?.[0]).toEqual([false])
    wrapper.unmount()
  })

  it('shows retry button when showRetry is true and emits retry on click', async () => {
    const wrapper = mount(AiStatusDialog, {
      attachTo: document.body,
      props: {
        open: true,
        title: 'T',
        message: 'M',
        showRetry: true,
        retryLabel: 'Again',
      },
      global: {
        stubs: {
          ...dialogStubs(),
          Button: {
            props: ['variant'],
            template: '<button type="button"><slot /></button>',
          },
        },
      },
    })

    const retryBtn = wrapper.findAll('button').find((b) => b.text() === 'Again')
    expect(retryBtn).toBeDefined()
    await retryBtn!.trigger('click')

    expect(wrapper.emitted('retry')).toHaveLength(1)
    wrapper.unmount()
  })

  it('does not render retry button when showRetry is false', () => {
    const wrapper = mount(AiStatusDialog, {
      attachTo: document.body,
      props: {
        open: true,
        title: 'T',
        message: 'M',
        showRetry: false,
        retryLabel: 'Again',
      },
      global: {
        stubs: {
          ...dialogStubs(),
          Button: {
            props: ['variant'],
            template: '<button type="button"><slot /></button>',
          },
        },
      },
    })

    expect(wrapper.findAll('button').some((b) => b.text() === 'Again')).toBe(false)
    wrapper.unmount()
  })

  it('keeps dialog hidden when open is false', () => {
    const wrapper = mount(AiStatusDialog, {
      props: {
        open: false,
        title: 'T',
        message: 'M',
      },
      global: {
        stubs: {
          ...dialogStubs(),
          Button: {
            template: '<button type="button"><slot /></button>',
          },
        },
      },
    })

    expect(wrapper.find('[data-testid="dialog-root"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('updates visibility when open prop changes', async () => {
    const wrapper = mount(AiStatusDialog, {
      props: {
        open: false,
        title: 'T',
        message: 'M',
      },
      global: {
        stubs: {
          ...dialogStubs(),
          Button: {
            template: '<button type="button"><slot /></button>',
          },
        },
      },
    })

    expect(wrapper.find('[data-testid="dialog-root"]').exists()).toBe(false)
    await wrapper.setProps({ open: true })
    expect(wrapper.find('[data-testid="dialog-root"]').exists()).toBe(true)
    wrapper.unmount()
  })
})
