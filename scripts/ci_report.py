#!/usr/bin/env python3
"""Post a CI-generated text file as a GitHub commit comment.

Runs inside GitHub Actions. Uses the preinstalled `gh` CLI and the
job's GITHUB_TOKEN, so build details stay readable through the REST
JSON API even when raw log/artifact downloads are not reachable.

Usage: ci_report.py <file> [title]
"""
import os
import subprocess
import sys

LOG_PATH = sys.argv[1] if len(sys.argv) > 1 else "build-log.txt"
TITLE = sys.argv[2] if len(sys.argv) > 2 else "CI build log (tail)"
MAX_CHARS = 55000

with open(LOG_PATH, "r", encoding="utf-8", errors="replace") as f:
    content = f.read()

tail = content[-MAX_CHARS:]
# Keep the markdown code fence intact regardless of file contents.
tail = tail.replace("```", "'''")
body = f"### {TITLE}\n\n```\n" + tail + "\n```"

repo = os.environ["GITHUB_REPOSITORY"]
sha = os.environ["GITHUB_SHA"]
subprocess.run(
    ["gh", "api", f"repos/{repo}/commits/{sha}/comments", "-f", f"body={body}"],
    check=True,
)
print(f"Posted {LOG_PATH} as commit comment")
