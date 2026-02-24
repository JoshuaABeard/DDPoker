#!/usr/bin/env bash
# run-release-gate.sh — Execute release-gate scenario scripts sequentially.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

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
    if bash "$path" "$@"; then
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
