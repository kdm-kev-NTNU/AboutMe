import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useCounterStore } from '../counter'

describe('useCounterStore', () => {
	beforeEach(() => {
		setActivePinia(createPinia())
	})

	it('increments count and updates doubleCount', () => {
		const store = useCounterStore()
		expect(store.count).toBe(0)
		expect(store.doubleCount).toBe(0)
		store.increment()
		expect(store.count).toBe(1)
		expect(store.doubleCount).toBe(2)
	})
})
