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
  --skip-kb              Do not create or attach knowledge base
  --skip-prompt          Do not update system prompt / first message

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


def main() -> int:
    parser = argparse.ArgumentParser(description="Sync ElevenLabs Kevin portfolio agent from classpath text files.")
    parser.add_argument("--agent-id", default=os.environ.get("ELEVENLABS_AGENT_ID", DEFAULT_AGENT_ID))
    parser.add_argument("--kb-name", default=DEFAULT_KB_NAME)
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--print-agent", action="store_true")
    parser.add_argument("--skip-kb", action="store_true")
    parser.add_argument("--skip-prompt", action="store_true")
    args = parser.parse_args()

    if not KB_FILE.is_file():
        print(f"Missing knowledge base file: {KB_FILE}", file=sys.stderr)
        return 1
    if not PROMPT_FILE.is_file() or not FIRST_MSG_FILE.is_file():
        print(f"Missing prompt files under {RESOURCE_REALTIME}", file=sys.stderr)
        return 1

    api_key = load_api_key()
    if args.dry_run:
        print("[dry-run] Would load API key from env or backend/.env")
        print(f"[dry-run] KB file: {KB_FILE} ({KB_FILE.stat().st_size} bytes)")
        print(f"[dry-run] Agent: {args.agent_id}")
        return 0

    if not api_key:
        print("Set ELEVENLABS_API_KEY or add it to backend/.env", file=sys.stderr)
        return 1

    try:
        from elevenlabs import ElevenLabs
        from elevenlabs.types.conversational_config import ConversationalConfig
        from elevenlabs.types.knowledge_base_locator import KnowledgeBaseLocator
    except ImportError:
        print("Install dependencies: pip install elevenlabs", file=sys.stderr)
        return 1

    client = ElevenLabs(api_key=api_key)

    if args.print_agent:
        print_agent_summary(client, args.agent_id)
        if args.skip_kb and args.skip_prompt:
            return 0

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

    new_cc = ConversationalConfig.model_validate(cc_dump)
    client.conversational_ai.agents.update(agent_id=args.agent_id, conversation_config=new_cc)
    print(f"Agent update submitted: {args.agent_id}")
    print_agent_summary(client, args.agent_id)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
