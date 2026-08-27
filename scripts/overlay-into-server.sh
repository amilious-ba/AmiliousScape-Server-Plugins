#!/usr/bin/env bash
# Copy Amilious plugin sources into a 2009Scape Server tree.
# Usage:
#   ./scripts/overlay-into-server.sh /path/to/2009scape
# or set SERVER_ROOT and run with no args.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SERVER_ROOT="${1:-${SERVER_ROOT:-}}"

if [ -z "$SERVER_ROOT" ]; then
  echo "Usage: $0 /path/to/2009scape"
  echo "  (repo root that contains Server/src/main)"
  exit 1
fi

DEST="$SERVER_ROOT/Server/src/main/content/amilious"
SRC="$ROOT/src/main/content/amilious"

if [ ! -d "$SERVER_ROOT/Server/src/main" ]; then
  echo "ERROR: $SERVER_ROOT does not look like a 2009Scape checkout (missing Server/src/main)"
  exit 1
fi

mkdir -p "$DEST"
rsync -a --delete "$SRC/" "$DEST/"
echo "Overlaid plugins -> $DEST"
find "$DEST" -type f | sort
