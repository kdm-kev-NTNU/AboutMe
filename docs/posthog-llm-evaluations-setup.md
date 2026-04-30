# PostHog LLM evaluations (AboutMe)

After deploying backend changes that emit `$ai_generation`, `$ai_trace`, and `$ai_span` with `$ai_output_choices`, `$ai_input`, `$ai_session_id`, and `$ai_latency`, add evaluations in PostHog (**LLM Analytics → Evaluations**) or via PostHog MCP (`evaluation-create`).

## Hog (deterministic) evaluations

Prefer **Hog** evaluators for latency, structure, and cost gates (no extra LLM cost, no org AI approval).

### 1. Latency under 15 seconds

- **Name:** `RAG latency under 15s`
- **Type:** Hog
- **Pass when:** `$ai_latency` is missing or ≤ 15 (seconds)

Suggested Hog source (verify against [PostHog Hog docs](https://posthog.com/docs) if your project uses a different Hog dialect):

```text
let lat := event.properties.$ai_latency
return lat == null or lat <= 15
```

### 2. Assistant output present

- **Name:** `Assistant output non-empty`
- **Type:** Hog
- **Pass when:** `$ai_output_choices` exists and first choice has non-empty `content`

```text
let choices := event.properties.$ai_output_choices
return choices != null and size(choices) > 0 and length(choices[1].content) > 0
```

(Indexing may be `choices[0]` in your Hog runtime—use **evaluation-test-hog** in MCP to validate.)

### 3. Output token budget

- **Name:** `Output tokens under 2000`
- **Type:** Hog

```text
let t := event.properties.$ai_output_tokens
return t == null or t <= 2000
```

### 4. No LLM error flag

- **Name:** `Generation not marked error`
- **Type:** Hog

```text
return event.properties.$ai_is_error != true
```

## LLM judge evaluations

Use **LLM judge** only for fuzzy quality checks; requires org **AI data processing** approval. Start with `enabled: false`, run `evaluation-run` on sample generations, then enable.

### 5. Stays on-topic (relevance)

- **Name:** `Answer addresses user question`
- **Type:** LLM judge
- **Prompt (sketch):** Ask the judge whether the assistant’s last message answers the user’s question, using `$ai_input` and `$ai_output_choices` from the generation event; return true/false/N/A when there is no user question.

### 6. Faithfulness to RAG context

- **Name:** `RAG answer grounded in context`
- **Type:** LLM judge
- **Prompt (sketch):** For generations with `rag_doc_count` > 0 or with `$ai_context`, judge whether claims in the answer are supported by the provided context; return N/A for non-RAG generations.

## MCP workflow (when PostHog MCP is connected)

1. `evaluations-get` — list existing evals to avoid duplicates.
2. `evaluation-test-hog` — dry-run Hog source on recent `$ai_generation` events.
3. `evaluation-create` — promote tested Hog or create LLM judge with `model_configuration`.
4. `llm-analytics-evaluation-summary-create` — review pass/fail patterns over time.

## REST / script (same API as MCP)

If the PostHog MCP server is unavailable, use the [PostHog LLM analytics API](https://posthog.com/docs/api/llm-analytics): `GET/POST /api/environments/:project_id/evaluations/`, `POST .../evaluations/test_hog/`. Authenticate with a **Personal API key** (`evaluation:read`, `evaluation:write`), not the capture `POSTHOG_API_KEY`.

From the repo root (set env vars first — see [.env.example](../.env.example)):

```powershell
./scripts/posthog-llm-evaluations-setup.ps1
```

The script lists evaluations, runs `test_hog` for each Hog evaluator, creates missing Hog evals (enabled), then creates the two LLM-judge evals **disabled** with `gpt-4o-mini`.

## Related code

- Capture: [`PostHogLlmService.java`](../backend/src/main/java/com/kevinmazali/portfolio/service/PostHogLlmService.java)
- RAG trace + retrieval span: [`OpenAIServiceImpl.java`](../backend/src/main/java/com/kevinmazali/portfolio/service/OpenAIServiceImpl.java)
