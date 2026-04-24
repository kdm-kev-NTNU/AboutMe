#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT/backend"
./mvnw -B verify
cd "$ROOT/frontend/homepage"
CYPRESS_INSTALL_BINARY=0 npm ci
npm run type-check
npm run lint:ci
npm run test:unit:coverage
