import type { Metadata } from 'next';
import Link from 'next/link';

export const metadata: Metadata = {
  title: 'Practice Mode - DD Poker Community Edition',
  description: 'Master Texas Hold\'em with DD Poker\'s comprehensive practice mode featuring AI opponents, tournament simulation, and detailed hand analysis.',
};

export default function PracticePage() {
  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold text-green-700 mb-6">Practice Mode - Learn and Master Texas Hold'em</h1>

      <div className="mb-8">
        <p className="text-lg text-gray-700 mb-4">
          Perfect your poker skills with DD Poker's comprehensive practice mode. Play against intelligent AI opponents in realistic tournament settings, get instant feedback on your decisions, and analyze every hand to improve your game.
        </p>
      </div>

      <div className="space-y-8">
        <section>
          <h2 className="text-2xl font-semibold text-green-700 mb-4">Tournament Simulation</h2>

          <p className="text-gray-700 mb-4">
            Experience authentic tournament poker with realistic blind structures, multiple tables, and dynamic table balancing. Choose from 15 pre-configured tournaments or create your own custom formats.
          </p>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-3">Tournament Features:</h3>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Multi-table tournaments with automatic table balancing</li>
              <li>Realistic blind progression and antes</li>
              <li>Configurable starting chips and blind structures</li>
              <li>Support for rebuys and add-ons</li>
              <li>Customizable payout structures</li>
              <li>Tournament clock with level timer</li>
              <li>15 pre-defined tournament formats</li>
              <li>Full tournament editor for custom events</li>
            </ul>
          </div>
        </section>

        <section>
          <h2 className="text-2xl font-semibold text-green-700 mb-4">AI Opponents</h2>

          <p className="text-gray-700 mb-4">
            Face off against sophisticated computer opponents that adapt their strategy based on position, stack size, and tournament stage. Each AI player has a distinct personality and playing style.
          </p>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-3">AI Capabilities:</h3>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Multiple difficulty levels from beginner to expert</li>
              <li>Varied playing styles (tight-aggressive, loose-passive, etc.)</li>
              <li>Position-aware decision making</li>
              <li>Stack-size appropriate strategies</li>
              <li>Tournament stage adaptability (early, middle, bubble, final table)</li>
              <li>Realistic bluffing and semi-bluffing</li>
              <li>Hand reading and opponent modeling</li>
              <li>Pot odds and implied odds calculations</li>
            </ul>
          </div>
        </section>

        <section>
          <h2 className="text-2xl font-semibold text-green-700 mb-4">Learning Tools</h2>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-3">DD Dashboard:</h3>
            <p className="text-gray-700 mb-3">
              Real-time information display providing crucial game data at a glance:
            </p>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Current tournament rank and prize standings</li>
              <li>Tables remaining and total players</li>
              <li>Current blinds, antes, and level timer</li>
              <li>Pot odds calculator</li>
              <li>Hand strength indicator</li>
              <li>Recommended action based on situation</li>
              <li>Historical hand statistics</li>
            </ul>
          </div>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-3">AI Advisor:</h3>
            <p className="text-gray-700 mb-3">
              Get expert guidance on every decision with context-aware advice:
            </p>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Situation-specific recommendations (fold, call, raise)</li>
              <li>Detailed explanations of suggested actions</li>
              <li>Position-based strategy tips</li>
              <li>Stack size considerations</li>
              <li>Tournament stage awareness</li>
              <li>Pot odds and equity analysis</li>
              <li>Toggle on/off as you improve</li>
            </ul>
          </div>
        </section>

        <section>
          <h2 className="text-2xl font-semibold text-green-700 mb-4">Hand Analysis</h2>

          <p className="text-gray-700 mb-4">
            Review every hand you play with comprehensive statistics and detailed analysis. Track your progress over time and identify areas for improvement.
          </p>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-3">Analysis Features:</h3>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Complete hand history with action replay</li>
              <li>Statistical breakdown of playing patterns</li>
              <li>Winning percentage by starting hand</li>
              <li>Position-based performance metrics</li>
              <li>Betting pattern analysis</li>
              <li>Showdown analysis</li>
              <li>Tournament results tracking</li>
              <li>Export hands for further study</li>
            </ul>
          </div>

          <p className="text-gray-700">
            Learn more about hand analysis features on the <Link href="/about/analysis" className="text-green-600 hover:text-green-700 underline">Hand Analysis page</Link>.
          </p>
        </section>

        <section>
          <h2 className="text-2xl font-semibold text-green-700 mb-4">Player Profiles</h2>

          <p className="text-gray-700 mb-4">
            Track your progress with automatic recording of tournament results, wins, and prize money. Support for multiple player profiles makes it easy for family members or roommates to maintain separate statistics.
          </p>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-3">Profile Features:</h3>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Unlimited player profiles</li>
              <li>Comprehensive tournament history</li>
              <li>Win/loss records</li>
              <li>Prize money tracking</li>
              <li>Best finishes and achievements</li>
              <li>Playing statistics over time</li>
              <li>Import/export profile data</li>
            </ul>
          </div>
        </section>

        <section className="bg-green-50 p-6 rounded-lg">
          <h2 className="text-2xl font-semibold text-green-700 mb-4">Ready to Start Practicing?</h2>
          <p className="text-gray-700 mb-4">
            Download DD Poker and start improving your game today. Practice mode is completely free and works offline.
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
