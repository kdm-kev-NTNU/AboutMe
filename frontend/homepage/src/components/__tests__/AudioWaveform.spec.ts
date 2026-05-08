import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import AudioWaveform from '../AudioWaveform.vue'

let resizeObserverInstances: Array<{ disconnect: ReturnType<typeof vi.fn> }> = []

vi.stubGlobal(
  'MediaStream',
  class {
    getTracks() {
      return []
    }
  } as unknown as typeof MediaStream,
)

class FakeAnalyser {
  fftSize = 2048
  smoothingTimeConstant = 0
  connect() {
    return this
  }
  disconnect() {}
  getFloatTimeDomainData(arr: Float32Array) {
    for (let i = 0; i < arr.length; i++) {
      arr[i] = Math.sin((i / Math.max(1, arr.length - 1)) * Math.PI) * 0.4
    }
  }
}

class FakeMediaStreamAudioSourceNode {
  connect() {
    return new FakeAnalyser()
  }
  disconnect() {}
}

class FakeAudioContext {
  state: AudioContextState = 'running'
  resume = vi.fn().mockResolvedValue(undefined)
  close = vi.fn().mockResolvedValue(undefined)
  createMediaStreamSource = vi.fn(() => new FakeMediaStreamAudioSourceNode())
  createAnalyser = vi.fn(() => new FakeAnalyser())
}

function createFakeCanvasContext() {
  return {
    clearRect: vi.fn(),
    beginPath: vi.fn(),
    moveTo: vi.fn(),
    lineTo: vi.fn(),
    stroke: vi.fn(),
    lineJoin: 'round' as CanvasLineJoin,
    lineCap: 'round' as CanvasLineCap,
    lineWidth: 1,
    strokeStyle: '',
    globalAlpha: 1,
  }
}

describe('AudioWaveform.vue', () => {
  let canvasGetContextSpy: ReturnType<typeof vi.spyOn> | null = null
  let canvasGetBoundingClientRectSpy: ReturnType<typeof vi.spyOn> | null = null
  let fakeCtx: ReturnType<typeof createFakeCanvasContext>

  /** Unmount stops the waveform composable RAF loop before prototype spies are restored. */
  const wrappers: ReturnType<typeof mount>[] = []

  function mountWaveform(opts: Parameters<typeof mount<typeof AudioWaveform>>[1]) {
    const w = mount(AudioWaveform, opts)
    wrappers.push(w)
    return w
  }

  beforeEach(() => {
    resizeObserverInstances.length = 0
    wrappers.length = 0
    fakeCtx = createFakeCanvasContext()
    canvasGetContextSpy = vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockImplementation((type: string) =>
      type === '2d' ? (fakeCtx as unknown as CanvasRenderingContext2D) : null,
    )
    canvasGetBoundingClientRectSpy = vi.spyOn(HTMLCanvasElement.prototype, 'getBoundingClientRect').mockImplementation(
      function (this: HTMLCanvasElement) {
        const logicalH = Number(this.style.height.replace('px', '')) || 40
        return {
          width: 200,
          height: logicalH,
          top: 0,
          left: 0,
          right: 200,
          bottom: logicalH,
          x: 0,
          y: 0,
          toJSON() {
            return {}
          },
        } as DOMRect
      },
    )
    vi.stubGlobal('AudioContext', FakeAudioContext as unknown as typeof AudioContext)
    vi.stubGlobal(
      'ResizeObserver',
      class {
        disconnect = vi.fn()
        observe = vi.fn()
        unobserve = vi.fn()
        constructor() {
          resizeObserverInstances.push(this)
        }
      },
    )
  })

  afterEach(async () => {
    for (const wrapper of wrappers) {
      try {
        wrapper.unmount()
      } catch {
        /* already unmounted */
      }
    }
    wrappers.length = 0
    await flushPromises()
    canvasGetContextSpy?.mockRestore()
    canvasGetContextSpy = null
    canvasGetBoundingClientRectSpy?.mockRestore()
    canvasGetBoundingClientRectSpy = null
    vi.unstubAllGlobals()
    vi.stubGlobal(
      'MediaStream',
      class {
        getTracks() {
          return []
        }
      } as unknown as typeof MediaStream,
    )
  })

  it('renders role=img with default aria-label when ariaLabel prop is omitted', () => {
    const wrapper = mountWaveform({
      props: { stream: null },
    })
    expect(wrapper.find('[role="img"]').attributes('aria-label')).toBe('Recording level')
  })

  it('renders a canvas with role=img and forwards a custom aria-label', () => {
    const wrapper = mountWaveform({
      props: {
        stream: null,
        ariaLabel: 'Custom recording level',
      },
    })
    expect(wrapper.find('[role="img"]').exists()).toBe(true)
    expect(wrapper.find('[role="img"]').attributes('aria-label')).toBe('Custom recording level')
    expect(wrapper.find('canvas').exists()).toBe(true)
  })

  it('registers ResizeObserver on mount and disconnects on unmount', async () => {
    const wrapper = mountWaveform({
      props: { stream: null },
    })
    await flushPromises()
    expect(resizeObserverInstances.length).toBeGreaterThan(0)
    const ro = resizeObserverInstances[resizeObserverInstances.length - 1]!
    wrapper.unmount()
    expect(ro.disconnect).toHaveBeenCalled()
  })

  it('requests a 2D canvas context during draw when a stream is connected', async () => {
    mountWaveform({
      props: { stream: new MediaStream() },
    })

    await flushPromises()

    await vi.waitFor(() => {
      expect(canvasGetContextSpy!.mock.calls.some((call: unknown[]) => call[0] === '2d')).toBe(true)
    })
  })

  it('strokes a path from analyser samples when stream is active', async () => {
    mountWaveform({
      props: { stream: new MediaStream() },
    })
    await flushPromises()

    await vi.waitFor(() => {
      expect(fakeCtx.lineTo).toHaveBeenCalled()
      expect(fakeCtx.stroke).toHaveBeenCalled()
    })
  })

  it('requests a redraw via requestAnimationFrame when stream prop appears after mount', async () => {
    const wrapper = mountWaveform({
      props: { stream: null },
    })
    await flushPromises()
    const strokesBefore = fakeCtx.stroke.mock.calls.length

    await wrapper.setProps({ stream: new MediaStream() })
    await flushPromises()

    await vi.waitFor(() => {
      expect(fakeCtx.stroke.mock.calls.length).toBeGreaterThan(strokesBefore)
    })
  })

  it('does not throw when 2D context is unavailable', async () => {
    canvasGetContextSpy!.mockImplementation(() => null)
    expect(() =>
      mountWaveform({
        props: { stream: new MediaStream() },
      }),
    ).not.toThrow()
    await flushPromises()
  })

  it('invokes ResizeObserver callback to schedule a redraw without throwing', async () => {
    let scheduled: (() => void) | null = null
    vi.stubGlobal(
      'ResizeObserver',
      class {
        disconnect = vi.fn()
        observe = vi.fn()
        unobserve = vi.fn()
        constructor(cb: ResizeObserverCallback) {
          resizeObserverInstances.push(this)
          scheduled = () => cb([], this as unknown as ResizeObserver)
        }
      },
    )

    mountWaveform({
      props: { stream: new MediaStream() },
    })
    await flushPromises()
    await vi.waitFor(() => expect(scheduled).not.toBeNull())

    expect(() => scheduled!()).not.toThrow()
  })
})
