import type { Metadata } from 'next';
import Link from 'next/link';

export const metadata: Metadata = {
  title: 'Hand Analysis - DD Poker Community Edition',
  description: 'Comprehensive hand analysis tools for Texas Hold\'em including detailed statistics, hand replay, and performance tracking.',
};

export default function AnalysisPage() {
  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold text-green-700 mb-6">Hand Analysis and Statistics</h1>

      <div className="mb-8">
        <p className="text-lg text-gray-700 mb-4">
          Improve your poker game with comprehensive hand analysis and statistical tracking. Review every decision, identify patterns in your play, and track your progress over time.
        </p>
      </div>

      <div className="space-y-8">
        <section>
          <h2 className="text-2xl font-semibold text-green-700 mb-4">Hand History and Replay</h2>

          <p className="text-gray-700 mb-4">
            DD Poker automatically records every hand you play, whether in practice mode or online tournaments. Review any hand with complete action replay showing every bet, raise, and fold.
          </p>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-3">Hand History Features:</h3>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Automatic recording of all hands played</li>
              <li>Complete action sequence for each hand</li>
              <li>Hole cards for all players at showdown</li>
              <li>Pot size at each betting stage</li>
              <li>Final board cards (flop, turn, river)</li>
              <li>Winner and pot distribution</li>
              <li>Time stamps for tournament context</li>
              <li>Searchable and filterable hand database</li>
            </ul>
          </div>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-3">Replay Capabilities:</h3>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Step through action one decision at a time</li>
              <li>View board as it developed (pre-flop, flop, turn, river)</li>
              <li>See pot size and stack sizes at each stage</li>
              <li>Review your thought process and decisions</li>
              <li>Compare your actions to AI recommendations</li>
              <li>Share interesting hands with others</li>
              <li>Export hands to text format</li>
            </ul>
          </div>
        </section>

        <section>
          <h2 className="text-2xl font-semibold text-green-700 mb-4">Statistical Analysis</h2>

          <p className="text-gray-700 mb-4">
            Gain deep insights into your playing patterns with comprehensive statistical breakdowns. Identify strengths and weaknesses to focus your improvement efforts.
          </p>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-3">Overall Statistics:</h3>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Total hands played</li>
              <li>Hands won at showdown</li>
              <li>Hands won without showdown (bluffs)</li>
              <li>Voluntary pot participation rate (VPIP)</li>
              <li>Pre-flop raise percentage (PFR)</li>
              <li>Aggression factor</li>
              <li>Went-to-showdown percentage</li>
              <li>Won-at-showdown percentage</li>
            </ul>
          </div>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-3">Starting Hand Analysis:</h3>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Win rate by starting hand</li>
              <li>Frequency of playing each starting hand</li>
              <li>Performance with pocket pairs</li>
              <li>Performance with suited vs. unsuited hands</li>
              <li>Performance with premium hands (AA, KK, QQ, AK)</li>
              <li>Profitability by hand strength category</li>
            </ul>
          </div>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-3">Positional Statistics:</h3>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Performance by table position</li>
              <li>VPIP by position (early, middle, late, blinds)</li>
              <li>Aggression by position</li>
              <li>Steal attempt frequency (late position)</li>
              <li>Blind defense frequency</li>
              <li>Three-bet percentage by position</li>
            </ul>
          </div>
        </section>

        <section>
          <h2 className="text-2xl font-semibold text-green-700 mb-4">Tournament Performance Tracking</h2>

          <p className="text-gray-700 mb-4">
            Track your tournament results over time and identify trends in your performance. See which tournament formats suit your playing style best.
          </p>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-3">Tournament Metrics:</h3>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Total tournaments played</li>
              <li>First, second, and third place finishes</li>
              <li>In-the-money percentage (ITM)</li>
              <li>Average finish position</li>
              <li>Return on investment (ROI)</li>
              <li>Total prize money won</li>
              <li>Performance by tournament type</li>
              <li>Performance by player count</li>
            </ul>
          </div>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-3">Tournament Stage Analysis:</h3>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Early stage performance (levels 1-3)</li>
              <li>Middle stage performance</li>
              <li>Bubble performance</li>
              <li>Final table performance</li>
              <li>Heads-up performance</li>
              <li>Common elimination scenarios</li>
            </ul>
          </div>
        </section>

        <section>
          <h2 className="text-2xl font-semibold text-green-700 mb-4">Betting Pattern Analysis</h2>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-3">Betting Metrics:</h3>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Average bet size by street (pre-flop, flop, turn, river)</li>
              <li>Continuation bet frequency</li>
              <li>Three-bet and four-bet frequency</li>
              <li>Check-raise frequency</li>
              <li>Fold to continuation bet percentage</li>
              <li>Call vs. raise frequency</li>
              <li>Bluff frequency by position and stack size</li>
              <li>Value bet sizing patterns</li>
            </ul>
          </div>
        </section>

        <section>
          <h2 className="text-2xl font-semibold text-green-700 mb-4">Advanced Filters and Reports</h2>

          <p className="text-gray-700 mb-4">
            Focus your analysis with powerful filtering options. Examine specific scenarios or time periods to track improvement and identify leaks in your game.
          </p>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-3">Filter Options:</h3>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Date range filters</li>
              <li>Tournament type filters</li>
              <li>Position filters</li>
              <li>Starting hand filters</li>
              <li>Stack size filters (short, medium, deep)</li>
              <li>Tournament stage filters</li>
              <li>Opponent count filters</li>
              <li>Result filters (won, lost, showdown, no showdown)</li>
            </ul>
          </div>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-808 mb-3">Export and Sharing:</h3>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Export statistics to CSV format</li>
              <li>Export hand histories to text files</li>
              <li>Generate printable reports</li>
              <li>Share specific hands with other players</li>
              <li>Compare statistics across multiple profiles</li>
            </ul>
          </div>
        </section>

        <section>
          <h2 className="text-2xl font-semibold text-green-700 mb-4">Using Analysis to Improve</h2>

          <p className="text-gray-700 mb-4">
            The key to improving at poker is honest self-assessment. Use DD Poker's analysis tools to:
          </p>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Identify your most costly mistakes</li>
              <li>Find hands you should have played differently</li>
              <li>Discover patterns in your losing hands</li>
              <li>Recognize situations where you're too tight or too loose</li>
              <li>Track improvement over time as you apply new strategies</li>
              <li>Compare your statistics to recommended ranges</li>
              <li>Focus practice sessions on your weakest areas</li>
            </ul>
          </div>

          <p className="text-gray-700">
            Combine hand analysis with <Link href="/about/practice" className="text-green-600 hover:text-green-700 underline">practice mode</Link> to work on specific aspects of your game, or review your <Link href="/about/online" className="text-green-600 hover:text-green-700 underline">online tournament</Link> hands to learn from real competition.
          </p>
        </section>

        <section className="bg-green-50 p-6 rounded-lg">
          <h2 className="text-2xl font-semibold text-green-700 mb-4">Start Analyzing Your Game</h2>
          <p className="text-gray-700 mb-4">
            Download DD Poker and begin tracking your poker performance today. All analysis tools are included free.
          </p>
          <Link
            href="/download"
            className="inline-block bg-green-600 hover:bg-green-700 text-white font-semibold px-6 py-3 rounded-lg transition-colors"
          >
            Download DD Poker
          </Link>
        </section>
      </div>
    </div>
  );
}
