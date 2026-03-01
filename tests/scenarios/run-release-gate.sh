#!/usr/bin/env bash
# run-release-gate.sh — Execute release-gate scenario scripts sequentially.
#
# Builds and launches the game JVM once, then passes --skip-build --skip-launch
# to each individual script so they reuse the running instance.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Source lib.sh for build/launch/cleanup infrastructure.
# The cleanup trap will kill the JVM when this script exits.
source "$SCRIPT_DIR/lib.sh"
lib_parse_args "$@"

# Build once and launch the game JVM once.
lib_launch

SCRIPTS=(
    "test-app-launch.sh"
    "test-game-start-params.sh"
    "test-all-actions.sh"
    "test-chip-conservation.sh"
    "test-blind-posting.sh"
    "test-allin-side-pot.sh"
    "test-rebuy-dialog.sh"
    "test-save-load.sh"
    "test-save-load-extended.sh"
    "test-heads-up.sh"
    "test-fold-every-hand.sh"
    "test-gameover-ranks.sh"
    "test-hand-rankings.sh"
    "test-pot-distribution.sh"
    "test-hand-flow.sh"
    "test-level-advance.sh"
    "test-large-field.sh"
)

total=${#SCRIPTS[@]}
passed=0
failed=0

echo "=== DDPoker Release Gate ==="
echo "Scripts: $total"

for script in "${SCRIPTS[@]}"; do
    path="$SCRIPT_DIR/$script"
    echo
    echo "--- Running: $script ---"
    if bash "$path" --skip-build --skip-launch "$@"; then
        echo "PASS: $script"
        passed=$((passed + 1))
    else
        echo "FAIL: $script"
        failed=$((failed + 1))
    fi
done

echo
echo "=== Release Gate Summary ==="
echo "Passed: $passed"
echo "Failed: $failed"
echo "Total:  $total"

if [[ $failed -gt 0 ]]; then
    exit 1
fi

exit 0
