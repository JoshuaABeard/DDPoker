#!/usr/bin/env python3
"""
Replace HoldemHand.ROUND_ constants with BettingRound enum across all files.
"""
import re
import sys
from pathlib import Path

# Files to process
FILES = [
    "poker/src/main/java/com/donohoedigital/games/poker/ai/AIOutcome.java",
    "poker/src/main/java/com/donohoedigital/games/poker/ai/gui/AdvisorInfoDialog.java",
    "poker/src/main/java/com/donohoedigital/games/poker/ai/OpponentModel.java",
    "poker/src/main/java/com/donohoedigital/games/poker/ai/PocketWeights.java",
    "poker/src/main/java/com/donohoedigital/games/poker/ai/PokerAI.java",
    "poker/src/main/java/com/donohoedigital/games/poker/ai/RuleEngine.java",
    "poker/src/main/java/com/donohoedigital/games/poker/ai/V1Player.java",
    "poker/src/main/java/com/donohoedigital/games/poker/ai/V2Player.java",
    "poker/src/main/java/com/donohoedigital/games/poker/Bet.java",
    "poker/src/main/java/com/donohoedigital/games/poker/dashboard/AdvanceAction.java",
    "poker/src/main/java/com/donohoedigital/games/poker/dashboard/DashboardAdvisor.java",
    "poker/src/main/java/com/donohoedigital/games/poker/dashboard/HandStrengthDash.java",
    "poker/src/main/java/com/donohoedigital/games/poker/dashboard/ImproveOdds.java",
    "poker/src/main/java/com/donohoedigital/games/poker/dashboard/Odds.java",
    "poker/src/main/java/com/donohoedigital/games/poker/dashboard/PotOdds.java",
    "poker/src/main/java/com/donohoedigital/games/poker/DealCommunity.java",
    "poker/src/main/java/com/donohoedigital/games/poker/HandAction.java",
    "poker/src/main/java/com/donohoedigital/games/poker/HandHistoryPanel.java",
    "poker/src/main/java/com/donohoedigital/games/poker/HandPotential.java",
    "poker/src/main/java/com/donohoedigital/games/poker/HoldemHand.java",
    "poker/src/main/java/com/donohoedigital/games/poker/impexp/ImpExpParadise.java",
    "poker/src/main/java/com/donohoedigital/games/poker/impexp/ImpExpUB.java",
    "poker/src/main/java/com/donohoedigital/games/poker/logic/DealingRules.java",
    "poker/src/main/java/com/donohoedigital/games/poker/online/TournamentDirector.java",
    "poker/src/main/java/com/donohoedigital/games/poker/PlayerProfile.java",
    "poker/src/main/java/com/donohoedigital/games/poker/PokerDatabase.java",
    "poker/src/main/java/com/donohoedigital/games/poker/PokerGame.java",
    "poker/src/main/java/com/donohoedigital/games/poker/PokerGameboard.java",
    "poker/src/main/java/com/donohoedigital/games/poker/PokerStatsPanel.java",
    "poker/src/main/java/com/donohoedigital/games/poker/PokerTable.java",
    "poker/src/main/java/com/donohoedigital/games/poker/Showdown.java",
    "poker/src/main/java/com/donohoedigital/games/poker/SimulatorDialog.java",
    "poker/src/main/java/com/donohoedigital/games/poker/StatisticsViewer.java",
    "poker/src/main/java/com/donohoedigital/games/poker/TournamentDirectorPauser.java",
    "poker/src/test/java/com/donohoedigital/games/poker/ai/OpponentModelTest.java",
    "poker/src/test/java/com/donohoedigital/games/poker/HandActionTest.java",
    "poker/src/test/java/com/donohoedigital/games/poker/HoldemHandPotCalculationTest.java",
    "poker/src/test/java/com/donohoedigital/games/poker/HoldemHandTest.java",
]

# Constant replacements
CONSTANT_REPLACEMENTS = [
    (r'\bHoldemHand\.ROUND_NONE\b', 'BettingRound.NONE'),
    (r'\bHoldemHand\.ROUND_PRE_FLOP\b', 'BettingRound.PRE_FLOP'),
    (r'\bHoldemHand\.ROUND_FLOP\b', 'BettingRound.FLOP'),
    (r'\bHoldemHand\.ROUND_TURN\b', 'BettingRound.TURN'),
    (r'\bHoldemHand\.ROUND_RIVER\b', 'BettingRound.RIVER'),
    (r'\bHoldemHand\.ROUND_SHOWDOWN\b', 'BettingRound.SHOWDOWN'),
]


def add_import_if_needed(content):
    """Add BettingRound import if not already present."""
    if 'import com.donohoedigital.games.poker.core.state.BettingRound;' in content:
        return content

    # Find the last import statement
    import_pattern = r'^import\s+[\w.]+;'
    lines = content.split('\n')
    last_import_idx = -1

    for i, line in enumerate(lines):
        if re.match(import_pattern, line):
            last_import_idx = i

    if last_import_idx >= 0:
        # Insert after the last import
        lines.insert(last_import_idx + 1, 'import com.donohoedigital.games.poker.core.state.BettingRound;')
        return '\n'.join(lines)

    return content


def process_file(filepath):
    """Process a single file."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()

        original_content = content

        # Check if file needs BettingRound import
        needs_import = any(pattern in content for pattern, _ in CONSTANT_REPLACEMENTS)

        # Replace all constants
        for pattern, replacement in CONSTANT_REPLACEMENTS:
            content = re.sub(pattern, replacement, content)

        # Add import if needed
        if needs_import:
            content = add_import_if_needed(content)

        # Write back if changed
        if content != original_content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"✓ Updated: {filepath}")
            return True
        else:
            print(f"  No changes: {filepath}")
            return False

    except Exception as e:
        print(f"✗ Error processing {filepath}: {e}", file=sys.stderr)
        return False


def main():
    """Main entry point."""
    base_dir = Path(__file__).parent

    updated_count = 0
    for filepath in FILES:
        full_path = base_dir / filepath
        if full_path.exists():
            if process_file(full_path):
                updated_count += 1
        else:
            print(f"✗ File not found: {full_path}", file=sys.stderr)

    print(f"\nProcessed {len(FILES)} files, updated {updated_count}")


if __name__ == '__main__':
    main()
