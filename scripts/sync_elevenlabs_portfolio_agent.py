"""
Sync Kevin's ElevenLabs Conversational AI agent with portfolio text assets.

  pip install elevenlabs

  ELEVENLABS_API_KEY=... python sync_elevenlabs_portfolio_agent.py
  # or rely on backend/.env in the repo (same key as Spring)

Actions (default: all):
  1) create_from_text knowledge document from kevin-profile-knowledge-base.txt
  2) attach document to agent knowledge_base (replaces same document name)
  3) PATCH agent prompt + first_message from elevenlabs-agent-*.txt

Options:
  --dry-run              Print planned reads only (no API calls)
  --print-agent          GET agent and print a short summary (prompt length, KB count, voice)
  --dump-config          GET agent and print full conversation_config JSON (tools, workflow, edges)
  --fix-validation       Remove stale transfer_to_agent tool and fix duplicate unconditional workflow edges
  --skip-kb              Do not create or attach knowledge base
  --skip-prompt          Do not update system prompt / first message

Fix-only (no local prompt/KB files needed): --fix-validation --skip-kb --skip-prompt

Docs: https://github.com/elevenlabs/elevenlabs-mcp (MCP); this script uses the official Python SDK directly.
"""

from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
# Maven layout: src/main/resources
RESOURCE_REALTIME = REPO_ROOT / "backend" / "src" / "main" / "resources" / "realtime"

KB_FILE = RESOURCE_REALTIME / "kevin-profile-knowledge-base.txt"
PROMPT_FILE = RESOURCE_REALTIME / "elevenlabs-agent-system-prompt.txt"
FIRST_MSG_FILE = RESOURCE_REALTIME / "elevenlabs-agent-first-message.txt"
BACKEND_ENV = REPO_ROOT / "backend" / ".env"

DEFAULT_AGENT_ID = "agent_3201kr8t7mnzfpta4svtbqzmafa1"
DEFAULT_KB_NAME = "kevin-portfolio-profile-knowledge"


def load_api_key() -> str:
    env = os.environ.get("ELEVENLABS_API_KEY", "").strip()
    if env:
        return env
    if not BACKEND_ENV.is_file():
        return ""
    for raw in BACKEND_ENV.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if line.startswith("ELEVENLABS_API_KEY=") and not line.startswith("#"):
            val = line.split("=", 1)[1].strip().strip('"').strip("'")
            return val
    return ""


def print_agent_summary(client, agent_id: str) -> None:
    from elevenlabs import ElevenLabs

    assert isinstance(client, ElevenLabs)
    agent = client.conversational_ai.agents.get(agent_id=agent_id)
    cc = agent.conversation_config
    voice = getattr(cc.tts, "voice_id", None) if cc and cc.tts else None
    lang = None
    prompt_len = 0
    kb_count = 0
    llm = None
    first_preview = ""
    if cc and cc.agent:
        lang = getattr(cc.agent, "language", None)
        fm = getattr(cc.agent, "first_message", None)
        if fm:
            first_preview = (fm[:120] + "…") if len(fm) > 120 else fm
        pr = getattr(cc.agent, "prompt", None)
        if pr:
            llm = getattr(pr, "llm", None)
            pt = getattr(pr, "prompt", None)
            if isinstance(pt, str):
                prompt_len = len(pt)
            kb = getattr(pr, "knowledge_base", None) or []
            kb_count = len(kb)
    print(
        json.dumps(
            {
                "agent_id": agent_id,
                "agent_name": getattr(agent, "name", None),
                "language": lang,
                "llm": llm,
                "voice_id": voice,
                "system_prompt_chars": prompt_len,
                "knowledge_base_documents": kb_count,
                "first_message_preview": first_preview,
            },
            indent=2,
            ensure_ascii=False,
        )
    )


def dump_conversation_config(client, agent_id: str) -> None:
    """Print full conversation_config as JSON for debugging validation / workflow issues."""
    agent = client.conversational_ai.agents.get(agent_id=agent_id)
    cc = agent.conversation_config
    payload = cc.model_dump(mode="json", exclude_none=False)
    print(json.dumps(payload, indent=2, ensure_ascii=False))


def strip_transfer_from_prompt_dict(prompt: dict, context: str) -> list[str]:
    """
    Remove transfer_to_agent from prompt.tools (legacy/system tool entries) and from
    prompt.built_in_tools.transfer_to_agent (dashboard validation path:
    agent.prompt.built_in_tools.transfer_to_agent.params.transfers).
    """
    messages: list[str] = []

    tools = prompt.get("tools")
    if isinstance(tools, list):
        kept: list = []
        for t in tools:
            if isinstance(t, dict) and t.get("name") == "transfer_to_agent":
                messages.append(f"{context}: removed transfer_to_agent from tools list.")
                continue
            kept.append(t)
        if len(kept) != len(tools):
            prompt["tools"] = kept

    bit = prompt.get("built_in_tools")
    if isinstance(bit, dict) and "transfer_to_agent" in bit:
        # ElevenLabs PATCH keeps omitted nested fields. Send explicit null so the
        # invalid transfer tool is actually cleared server-side.
        if bit.get("transfer_to_agent") is not None:
            messages.append(f"{context}: nulled built_in_tools.transfer_to_agent.")
        else:
            messages.append(f"{context}: kept built_in_tools.transfer_to_agent=null.")
        bit["transfer_to_agent"] = None

    return messages


def strip_transfer_to_agent_tool(cc_dump: dict) -> list[str]:
    """Remove transfer_to_agent everywhere the API/dashboard store it (base agent + workflow overrides)."""
    messages: list[str] = []
    agent = cc_dump.get("agent")
    if isinstance(agent, dict):
        prompt = agent.get("prompt")
        if isinstance(prompt, dict):
            messages.extend(strip_transfer_from_prompt_dict(prompt, "agent.prompt"))

    workflow = cc_dump.get("workflow")
    if isinstance(workflow, dict):
        nodes = workflow.get("nodes")
        if isinstance(nodes, dict):
            for node_id, node in nodes.items():
                if not isinstance(node, dict):
                    continue
                ov = node.get("conversation_config")
                if not isinstance(ov, dict):
                    continue
                sub_agent = ov.get("agent")
                if not isinstance(sub_agent, dict):
                    continue
                sub_prompt = sub_agent.get("prompt")
                if isinstance(sub_prompt, dict):
                    messages.extend(
                        strip_transfer_from_prompt_dict(
                            sub_prompt, f"workflow.nodes[{node_id}].conversation_config.agent.prompt"
                        )
                    )

    return messages


def _forward_condition_type(edge: dict) -> str | None:
    fc = edge.get("forward_condition")
    if not isinstance(fc, dict):
        return None
    t = fc.get("type")
    return str(t) if t is not None else None


def fix_duplicate_unconditional_workflow_edges(cc_dump: dict) -> list[str]:
    """
    ElevenLabs allows at most one unconditional outgoing edge per workflow node.
    Keep the first unconditional edge per source; convert extras to LLM-evaluated conditions.
    """
    messages: list[str] = []
    workflow = cc_dump.get("workflow")
    if not isinstance(workflow, dict):
        return messages
    edges = workflow.get("edges")
    if not isinstance(edges, dict):
        return messages

    by_source: dict[str, list[tuple[str, dict]]] = {}
    for edge_id, edge in edges.items():
        if not isinstance(edge, dict):
            continue
        src = edge.get("source")
        if not src:
            continue
        by_source.setdefault(str(src), []).append((str(edge_id), edge))

    for src, items in by_source.items():
        unconditional = [
            (eid, e)
            for eid, e in items
            if _forward_condition_type(e) == "unconditional"
        ]
        if len(unconditional) <= 1:
            continue
        for eid, e in unconditional[1:]:
            fc = e.setdefault("forward_condition", {})
            fc["type"] = "llm"
            fc["condition"] = (
                "Take this transition only when it clearly matches the user's intent "
                f"(secondary path from node {src})."
            )
            messages.append(
                f"Edge {eid}: forward_condition changed from unconditional to llm "
                f"(duplicate unconditional outgoing edge from {src})."
            )
    return messages


def apply_validation_fixes(cc_dump: dict) -> list[str]:
    """Apply all ElevenLabs validation fixes; mutates cc_dump."""
    out: list[str] = []
    out.extend(strip_transfer_to_agent_tool(cc_dump))
    out.extend(fix_duplicate_unconditional_workflow_edges(cc_dump))
    return out


def main() -> int:
    parser = argparse.ArgumentParser(description="Sync ElevenLabs Kevin portfolio agent from classpath text files.")
    parser.add_argument("--agent-id", default=os.environ.get("ELEVENLABS_AGENT_ID", DEFAULT_AGENT_ID))
    parser.add_argument("--kb-name", default=DEFAULT_KB_NAME)
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--print-agent", action="store_true")
    parser.add_argument("--dump-config", action="store_true")
    parser.add_argument("--fix-validation", action="store_true")
    parser.add_argument("--skip-kb", action="store_true")
    parser.add_argument("--skip-prompt", action="store_true")
    args = parser.parse_args()

    fix_only = args.fix_validation and args.skip_kb and args.skip_prompt

    if not args.dump_config and not fix_only:
        if not KB_FILE.is_file():
            print(f"Missing knowledge base file: {KB_FILE}", file=sys.stderr)
            return 1
        if not PROMPT_FILE.is_file() or not FIRST_MSG_FILE.is_file():
            print(f"Missing prompt files under {RESOURCE_REALTIME}", file=sys.stderr)
            return 1

    api_key = load_api_key()
    if args.dry_run:
        print("[dry-run] Would load API key from env or backend/.env")
        if not fix_only and KB_FILE.is_file():
            print(f"[dry-run] KB file: {KB_FILE} ({KB_FILE.stat().st_size} bytes)")
        print(f"[dry-run] Agent: {args.agent_id}")
        return 0

    if not api_key:
        print("Set ELEVENLABS_API_KEY or add it to backend/.env", file=sys.stderr)
        return 1

    try:
        from elevenlabs import ElevenLabs
        from elevenlabs.types.conversational_config import ConversationalConfig
    except ImportError:
        print("Install dependencies: pip install elevenlabs", file=sys.stderr)
        return 1

    client = ElevenLabs(api_key=api_key)

    if args.dump_config:
        dump_conversation_config(client, args.agent_id)
        return 0

    if args.print_agent:
        print_agent_summary(client, args.agent_id)
        if args.skip_kb and args.skip_prompt and not args.fix_validation:
            return 0

    kb_text = ""
    system_prompt = ""
    first_message = ""
    if not fix_only:
        kb_text = KB_FILE.read_text(encoding="utf-8")
        system_prompt = PROMPT_FILE.read_text(encoding="utf-8").strip()
        first_message = FIRST_MSG_FILE.read_text(encoding="utf-8").strip()

    current = client.conversational_ai.agents.get(agent_id=args.agent_id)
    cc_dump = current.conversation_config.model_dump(mode="json", exclude_none=False)

    if not args.skip_kb:
        doc = client.conversational_ai.knowledge_base.documents.create_from_text(
            text=kb_text,
            name=args.kb_name,
        )
        agent_part = cc_dump.setdefault("agent", {})
        prompt_part = agent_part.setdefault("prompt", {})
        kb_list = list(prompt_part.get("knowledge_base") or [])
        kb_list = [x for x in kb_list if isinstance(x, dict) and x.get("name") != args.kb_name]
        kb_list.append({"type": "file", "name": args.kb_name, "id": doc.id})
        prompt_part["knowledge_base"] = kb_list
        print(f"Knowledge document created/updated: id={doc.id} name={args.kb_name}")

    if not args.skip_prompt:
        agent_part = cc_dump.setdefault("agent", {})
        prompt_part = agent_part.setdefault("prompt", {})
        prompt_part["prompt"] = system_prompt
        agent_part["first_message"] = first_message
        print("Patched conversation_config.agent.prompt.prompt and first_message from resource files.")

    if args.fix_validation:
        applied = apply_validation_fixes(cc_dump)
        if applied:
            for line in applied:
                print(line)
        else:
            print("No validation fixes applied (transfer_to_agent absent; no duplicate unconditional edges).")

    new_cc = ConversationalConfig.model_validate(cc_dump)
    client.conversational_ai.agents.update(agent_id=args.agent_id, conversation_config=new_cc)
    print(f"Agent update submitted: {args.agent_id}")
    print_agent_summary(client, args.agent_id)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
