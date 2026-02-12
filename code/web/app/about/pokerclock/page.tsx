import type { Metadata } from 'next';
import Link from 'next/link';

export const metadata: Metadata = {
  title: 'Poker Clock - DD Poker Community Edition',
  description: 'Host professional-quality home tournaments with DD Poker\'s built-in tournament clock featuring blind management, level timers, and payout calculation.',
};

export default function PokerClockPage() {
  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold text-green-700 mb-6">Tournament Clock for Home Games</h1>

      <div className="mb-8">
        <p className="text-lg text-gray-700 mb-4">
          Host professional-quality home tournaments with the built-in poker clock. Manage blinds, levels, breaks, and payouts just like the major tournaments.
        </p>
      </div>

      <div className="space-y-8">
        <section>
          <h2 className="text-2xl font-semibold text-green-700 mb-4">Run Your Home Tournament</h2>

          <p className="text-gray-700 mb-4">
            Perfect for weekly poker nights or special events. Define your tournament structure once and reuse it week after week.
          </p>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-3">Tournament Configuration:</h3>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Set buy-in amounts and prize structure</li>
              <li>Configure rebuys and add-ons</li>
              <li>Define blind and ante schedules</li>
              <li>Specify level duration and break times</li>
              <li>Save tournament profiles for reuse</li>
              <li>Adjust settings mid-tournament if needed</li>
              <li>Automatic payout calculation based on prize pool</li>
            </ul>
          </div>
        </section>

        <section>
          <h2 className="text-2xl font-semibold text-green-700 mb-4">Clock Features</h2>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Large, easy-to-read display visible across the room</li>
              <li>Countdown timer with audio and visual alerts for level changes</li>
              <li>Displays current and upcoming blinds/antes</li>
              <li>Automated reminders for rebuy periods and add-ons</li>
              <li>Pause for breaks or chip color-ups</li>
              <li>Variable level and break lengths</li>
              <li>Optional pause between levels</li>
              <li>Configurable color-up notifications</li>
            </ul>
          </div>

          <p className="text-gray-700">
            The clock helps resolve rule disputes and keeps your game running smoothly, so everyone can focus on playing poker.
          </p>
        </section>

        <section>
          <h2 className="text-2xl font-semibold text-green-700 mb-4">Blind Structure Management</h2>

          <p className="text-gray-700 mb-4">
            Create custom blind structures tailored to your game's needs, or use one of the pre-configured structures based on popular tournament formats.
          </p>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-3">Blind Structure Options:</h3>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Define unlimited blind levels</li>
              <li>Set custom small blind and big blind amounts</li>
              <li>Add antes at any level</li>
              <li>Specify different durations for each level</li>
              <li>Insert breaks between levels</li>
              <li>Preview entire tournament structure before starting</li>
              <li>Modify structure on-the-fly if needed</li>
            </ul>
          </div>
        </section>

        <section>
          <h2 className="text-2xl font-semibold text-green-700 mb-4">Payout Calculator</h2>

          <p className="text-gray-700 mb-4">
            Automatically calculate prize distributions based on the total prize pool and number of players. Support for various payout structures from winner-take-all to deep payouts.
          </p>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-3">Payout Features:</h3>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Multiple payout structure templates</li>
              <li>Custom payout percentages</li>
              <li>Handle rebuys and add-ons in prize pool</li>
              <li>Display current payouts during tournament</li>
              <li>Adjust payout structure based on final player count</li>
              <li>Print or export payout information</li>
            </ul>
          </div>
        </section>

        <section>
          <h2 className="text-2xl font-semibold text-green-700 mb-4">Additional Tools</h2>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-3">Tournament Management:</h3>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Track number of rebuys and add-ons</li>
              <li>Monitor total prize pool in real-time</li>
              <li>Player elimination tracking</li>
              <li>Chip color-up reminders</li>
              <li>Break countdown timers</li>
              <li>Tournament duration estimates</li>
            </ul>
          </div>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-3">Display Options:</h3>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Full-screen mode for TV or projector</li>
              <li>Adjustable font sizes and colors</li>
              <li>Sound effects for level changes and breaks</li>
              <li>Customizable display layout</li>
              <li>Show/hide various information panels</li>
            </ul>
          </div>
        </section>

        <section>
          <h2 className="text-2xl font-semibold text-green-700 mb-4">Pre-Configured Tournament Types</h2>

          <p className="text-gray-700 mb-4">
            DD Poker includes several ready-to-use tournament structures based on popular formats. These can be used as-is or customized to fit your needs.
          </p>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-3">Included Structures:</h3>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Quick tournaments (15-30 minute levels)</li>
              <li>Standard tournaments (30-60 minute levels)</li>
              <li>Deep stack tournaments</li>
              <li>Turbo formats with fast blind increases</li>
              <li>Sit-and-go formats for small player counts</li>
            </ul>
          </div>
        </section>

        <section>
          <h2 className="text-2xl font-semibold text-green-700 mb-4">Perfect for Home Games</h2>

          <p className="text-gray-700 mb-4">
            Whether you're running a casual weekly game with friends or organizing a larger home tournament, DD Poker's clock provides all the tools you need to run a professional event.
          </p>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-3">Use Cases:</h3>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Weekly poker nights with friends</li>
              <li>Special event tournaments</li>
              <li>Office or workplace tournaments</li>
              <li>Charity poker events</li>
              <li>Practice for casino tournaments</li>
              <li>Teaching new players tournament poker</li>
            </ul>
          </div>
        </section>

        <section className="bg-green-50 p-6 rounded-lg">
          <h2 className="text-2xl font-semibold text-green-700 mb-4">Ready to Host Your Tournament?</h2>
          <p className="text-gray-700 mb-4">
            Download DD Poker and get access to the tournament clock along with all other features. No additional purchase required.
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
