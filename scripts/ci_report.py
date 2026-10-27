#!/usr/bin/env python3
"""Post the tail of a CI build log as a GitHub commit comment.

Runs inside GitHub Actions. Uses the preinstalled `gh` CLI and the
job's GITHUB_TOKEN, so failure details stay readable through the REST
JSON API even when raw log/artifact downloads are not reachable.
"""
import os
import subprocess
import sys

LOG_PATH = sys.argv[1] if len(sys.argv) > 1 else "build-log.txt"
MAX_CHARS = 55000

with open(LOG_PATH, "r", encoding="utf-8", errors="replace") as f:
    content = f.read()

tail = content[-MAX_CHARS:]
# Keep the markdown code fence intact regardless of log contents.
tail = tail.replace("```", "'''")
body = "### CI build log (tail)\n\n```\n" + tail + "\n```"

repo = os.environ["GITHUB_REPOSITORY"]
sha = os.environ["GITHUB_SHA"]
subprocess.run(
    ["gh", "api", f"repos/{repo}/commits/{sha}/comments", "-f", f"body={body}"],
    check=True,
)
print("Posted build log as commit comment")
