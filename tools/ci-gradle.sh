#!/usr/bin/env bash
#
# Runs a command in CI and, when it fails, re-emits the interesting part of the output as a
# GitHub annotation.
#
# Why: Actions job logs live on results-receiver.actions.githubusercontent.com, which is not
# reachable from every environment (restricted sandboxes, artifact-only access). Check-run
# annotations are served from api.github.com and are always readable, so a failing build stays
# diagnosable without the log archive.
#
# Usage: tools/ci-gradle.sh "<annotation title>" <command> [args...]

set -uo pipefail

if [ "$#" -lt 2 ]; then
  echo "usage: $0 <annotation title> <command> [args...]" >&2
  exit 2
fi

title=$1
shift

log=$(mktemp)
"$@" >"$log" 2>&1
code=$?

if [ "$code" -ne 0 ]; then
  # Human-readable version for anyone who *can* read the step log.
  tail -80 "$log"

  detail=$(
    {
      grep -E "FAILED$" "$log"
      grep -A4 "What went wrong" "$log"
    } | tr -d '\r' | tr '\n' '|' | sed 's/%/%25/g' | cut -c1-1200
  )
  echo "::error title=${title}::${detail}"
fi

rm -f "$log"
exit "$code"
