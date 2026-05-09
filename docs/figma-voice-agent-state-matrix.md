# Voice agent state matrix (Figma MCP)

This mirrors [`VoiceView.vue`](../frontend/homepage/src/views/VoiceView.vue) (live GPT-Realtime WebRTC): **Loading**, **Unavailable**, **Idle**, **Connecting**, **Connected**, **Error** (modal overlay).

## File

- Design file: [AboutMe – Voice agent (3 utkast)](https://www.figma.com/design/dHEdCWNWsMkQI6yqX1uhLO)
- `fileKey`: `dHEdCWNWsMkQI6yqX1uhLO`

## Apply via Cursor Figma MCP

1. Wait until the Figma MCP quota resets (Starter plan shows a paywall when the limit is hit).
2. In Chat, ask the agent to run **`use_figma`** with `fileKey` above and paste the **full contents** of [`figma-voice-agent-state-matrix.use_figma.js`](figma-voice-agent-state-matrix.use_figma.js) as the `code` argument (same pattern as other `use_figma` calls: top-level `await` + final `return`).
3. **`skillNames`**: `figma-use`

## Layout

- Grid **6 columns × 3 rows** below the original row of concepts.
- Column headers: Loading, Unavailable, Idle, Connecting, Connected, Error.
- Row labels for concepts 01–03.
- **Connected** cells reuse the existing frames; the script expects node IDs `1:2`, `1:21`, `1:38`. If your file differs (e.g. duplicated file), update `connectedIds` in the script after inspecting the canvas.

## Idempotency

Running the script **twice** duplicates frames and labels. Delete generated nodes first, or run on a duplicate page/file.

## Troubleshooting

- First run failed on invalid enum: use **`MIN`** / **`CENTER`** / **`MAX`** for `primaryAxisAlignItems`, not `END`.
- Overlays use **`clipsContent: true`** on parent shells and **`layoutPositioning: 'ABSOLUTE'`** on the overlay frame.
