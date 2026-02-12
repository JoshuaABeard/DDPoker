import type { Metadata } from 'next';
import Link from 'next/link';

export const metadata: Metadata = {
  title: 'Online Play - DD Poker Community Edition',
  description: 'Play Texas Hold\'em online with friends through DD Poker\'s free multiplayer platform featuring private tournaments, chat, and more.',
};

export default function OnlinePage() {
  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold text-green-700 mb-6">Online Multiplayer Poker</h1>

      <div className="mb-8">
        <p className="text-lg text-gray-700 mb-4">
          Host and join private online poker tournaments with friends and fellow poker enthusiasts. DD Poker's online play is completely free with no registration required for players.
        </p>
      </div>

      <div className="space-y-8">
        <section>
          <h2 className="text-2xl font-semibold text-green-700 mb-4">How Online Play Works</h2>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-3">For Players (Join Tournaments):</h3>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>No registration or account creation needed</li>
              <li>Simply download DD Poker and choose "Online Play"</li>
              <li>Enter the game code provided by your tournament host</li>
              <li>Join the lobby, chat with other players, and play</li>
              <li>All features completely free</li>
            </ul>
          </div>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-3">For Hosts (Create Tournaments):</h3>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Create a free account on ddpoker.com</li>
              <li>Configure your tournament settings</li>
              <li>Generate a unique game code</li>
              <li>Share the code with your players</li>
              <li>Monitor and manage your tournament</li>
              <li>Host unlimited tournaments</li>
            </ul>
          </div>
        </section>

        <section>
          <h2 className="text-2xl font-semibold text-green-700 mb-4">Tournament Features</h2>

          <p className="text-gray-700 mb-4">
            Online tournaments support all the same features as practice mode, with additional multiplayer-specific functionality.
          </p>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-3">Game Features:</h3>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Multi-table tournaments with automatic balancing</li>
              <li>Support for 2-100+ players</li>
              <li>Customizable blind structures and payouts</li>
              <li>Configurable rebuys and add-ons</li>
              <li>Tournament clock with synchronized timers</li>
              <li>Automatic hand dealing and pot calculation</li>
              <li>Side pot handling</li>
              <li>All-in showdown management</li>
            </ul>
          </div>
        </section>

        <section>
          <h2 className="text-2xl font-semibold text-green-700 mb-4">Communication</h2>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-3">Chat System:</h3>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Pre-game lobby chat for coordinating start time</li>
              <li>Table chat during play</li>
              <li>Private messaging between players</li>
              <li>Tournament-wide announcements from hosts</li>
              <li>Automated system messages for game events</li>
              <li>Chat history and moderation tools</li>
            </ul>
          </div>
        </section>

        <section>
          <h2 className="text-2xl font-semibold text-green-700 mb-4">Host Controls</h2>

          <p className="text-gray-700 mb-4">
            Tournament hosts have comprehensive tools for managing their games and ensuring smooth operation.
          </p>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-3">Host Capabilities:</h3>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Pause and resume tournaments</li>
              <li>Adjust tournament settings on the fly</li>
              <li>Remove disruptive players</li>
              <li>Manual table balancing if needed</li>
              <li>Extend or shorten levels</li>
              <li>Add unscheduled breaks</li>
              <li>View all player statistics</li>
              <li>Export tournament results</li>
            </ul>
          </div>
        </section>

        <section>
          <h2 className="text-2xl font-semibold text-green-700 mb-4">Privacy and Security</h2>

          <p className="text-gray-700 mb-4">
            DD Poker is designed for private games among friends and acquaintances. We prioritize your privacy and security.
          </p>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-3">Privacy Features:</h3>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Private tournaments accessible only via game code</li>
              <li>No public game listings or random matchmaking</li>
              <li>Minimal personal information required</li>
              <li>Secure connections for all game data</li>
              <li>Host can control who joins their games</li>
              <li>No real money gambling or transactions</li>
            </ul>
          </div>
        </section>

        <section>
          <h2 className="text-2xl font-semibold text-green-700 mb-4">Technical Requirements</h2>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-3">System Requirements:</h3>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>DD Poker installed (Windows, macOS, or Linux)</li>
              <li>Stable internet connection (broadband recommended)</li>
              <li>Java 8 or higher</li>
              <li>Minimum 512MB RAM available</li>
              <li>Screen resolution 1024x768 or higher</li>
            </ul>
          </div>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-3">Network Requirements:</h3>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Outbound connections on port 443 (HTTPS)</li>
              <li>No port forwarding or firewall configuration needed</li>
              <li>Works with most home and office networks</li>
              <li>Compatible with VPN connections</li>
            </ul>
          </div>
        </section>

        <section>
          <h2 className="text-2xl font-semibold text-green-700 mb-4">Community Guidelines</h2>

          <p className="text-gray-700 mb-4">
            DD Poker is a community-driven project. We expect all players to treat each other with respect and maintain a friendly atmosphere.
          </p>

          <div className="bg-gray-50 p-6 rounded-lg mb-4">
            <h3 className="text-xl font-semibold text-gray-800 mb-3">Expected Behavior:</h3>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Be respectful and courteous to all players</li>
              <li>No harassment, hate speech, or abusive behavior</li>
              <li>Play in good faith and avoid collusion</li>
              <li>Report technical issues to help improve the platform</li>
              <li>Help new players learn the game</li>
              <li>Have fun and enjoy the competition</li>
            </ul>
          </div>
        </section>

        <section className="bg-green-50 p-6 rounded-lg">
          <h2 className="text-2xl font-semibold text-green-700 mb-4">Ready to Play Online?</h2>
          <p className="text-gray-700 mb-4">
            Download DD Poker to join tournaments or create an account to host your own games.
          </p>
          <div className="flex gap-4">
            <Link
              href="/download"
              className="inline-block bg-green-600 hover:bg-green-700 text-white font-semibold px-6 py-3 rounded-lg transition-colors"
            >
              Download DD Poker
            </Link>
            <Link
              href="/login"
              className="inline-block bg-white hover:bg-gray-50 text-green-600 font-semibold px-6 py-3 rounded-lg border-2 border-green-600 transition-colors"
            >
              Create Host Account
            </Link>
          </div>
        </section>
      </div>
    </div>
  );
}
