import { createPortfolioRouter } from './createPortfolioRouter'

export { createPortfolioRouter, type PortfolioRouterOptions } from './createPortfolioRouter'

const router = createPortfolioRouter(
	import.meta.env.VITEST ? { useMemoryHistory: true } : undefined,
)

export default router
