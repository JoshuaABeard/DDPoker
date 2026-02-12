import type { Metadata } from 'next';
import Link from 'next/link';
import Image from 'next/image';

export const metadata: Metadata = {
  title: 'Screenshots - DD Poker Community Edition',
  description: 'Browse screenshots of DD Poker showcasing the interface, features, and gameplay of this comprehensive Texas Hold\'em poker simulator.',
};

export default function ScreenshotsPage() {
  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold text-green-700 mb-6">Screenshots</h1>

      <div className="mb-8">
        <p className="text-lg text-gray-700 mb-4">
          Browse screenshots showing DD Poker's interface and features. For the best look, <Link href="/download" className="text-green-600 hover:text-green-700 underline">download the game</Link> and try it yourself.
        </p>
      </div>

      <div className="space-y-12">
        <section className="flex flex-col md:flex-row gap-6 items-start">
          <div className="md:w-1/2">
            <a href="/images/ss_v2/mainmenu.jpg" target="_blank" rel="noopener noreferrer">
              <Image
                src="/images/ss_v2/mainmenu.jpg"
                alt="DD Poker Main Menu"
                width={800}
                height={600}
                className="rounded-lg shadow-lg hover:shadow-xl transition-shadow"
              />
            </a>
          </div>
          <div className="md:w-1/2">
            <h2 className="text-2xl font-semibold text-green-700 mb-3">Main Menu</h2>
            <p className="text-gray-700">
              Quick access to Practice, Online, Analysis, Poker Clock, Calculator, and Help.
            </p>
          </div>
        </section>

        <section className="flex flex-col md:flex-row gap-6 items-start">
          <div className="md:w-1/2">
            <a href="/images/ss_v2/playerprofile.jpg" target="_blank" rel="noopener noreferrer">
              <Image
                src="/images/ss_v2/playerprofile.jpg"
                alt="Player Profile"
                width={800}
                height={600}
                className="rounded-lg shadow-lg hover:shadow-xl transition-shadow"
              />
            </a>
          </div>
          <div className="md:w-1/2">
            <h2 className="text-2xl font-semibold text-green-700 mb-3">Player Profiles</h2>
            <p className="text-gray-700">
              Automatic tracking of tournament results, wins, and prize money. Support for multiple player profiles.
            </p>
          </div>
        </section>

        <section className="flex flex-col md:flex-row gap-6 items-start">
          <div className="md:w-1/2">
            <a href="/images/ss_v2/levels.jpg" target="_blank" rel="noopener noreferrer">
              <Image
                src="/images/ss_v2/levels.jpg"
                alt="Tournament Levels"
                width={800}
                height={600}
                className="rounded-lg shadow-lg hover:shadow-xl transition-shadow"
              />
            </a>
          </div>
          <div className="md:w-1/2">
            <h2 className="text-2xl font-semibold text-green-700 mb-3">Tournament Formats</h2>
            <p className="text-gray-700">
              15 pre-defined tournaments including various blind structures and formats.
            </p>
          </div>
        </section>

        <section className="flex flex-col md:flex-row gap-6 items-start">
          <div className="md:w-1/2">
            <a href="/images/ss_v2/edittourney.jpg" target="_blank" rel="noopener noreferrer">
              <Image
                src="/images/ss_v2/edittourney.jpg"
                alt="Tournament Editor"
                width={800}
                height={600}
                className="rounded-lg shadow-lg hover:shadow-xl transition-shadow"
              />
            </a>
          </div>
          <div className="md:w-1/2">
            <h2 className="text-2xl font-semibold text-green-700 mb-3">Tournament Editor</h2>
            <p className="text-gray-700">
              Full customization of levels, blinds, antes, buy-ins, rebuys, add-ons, and payouts.
            </p>
          </div>
        </section>

        <section className="flex flex-col md:flex-row gap-6 items-start">
          <div className="md:w-1/2">
            <a href="/images/ss_v2/samplehand.jpg" target="_blank" rel="noopener noreferrer">
              <Image
                src="/images/ss_v2/samplehand.jpg"
                alt="Game Interface"
                width={800}
                height={600}
                className="rounded-lg shadow-lg hover:shadow-xl transition-shadow"
              />
            </a>
          </div>
          <div className="md:w-1/2">
            <h2 className="text-2xl font-semibold text-green-700 mb-3">Game Interface</h2>
            <p className="text-gray-700">
              DD Dashboard shows rank, tables remaining, blinds/antes, level timer, pot odds, and hand strength.
            </p>
          </div>
        </section>

        <section className="flex flex-col md:flex-row gap-6 items-start">
          <div className="md:w-1/2">
            <a href="/images/ss_v2/advisor.jpg" target="_blank" rel="noopener noreferrer">
              <Image
                src="/images/ss_v2/advisor.jpg"
                alt="AI Advisor"
                width={800}
                height={600}
                className="rounded-lg shadow-lg hover:shadow-xl transition-shadow"
              />
            </a>
          </div>
          <div className="md:w-1/2">
            <h2 className="text-2xl font-semibold text-green-700 mb-3">AI Advisor</h2>
            <p className="text-gray-700">
              Context-aware advice with detailed explanations of recommendations.
            </p>
          </div>
        </section>

        <section className="flex flex-col md:flex-row gap-6 items-start">
          <div className="md:w-1/2">
            <a href="/images/ss_v2/stats.jpg" target="_blank" rel="noopener noreferrer">
              <Image
                src="/images/ss_v2/stats.jpg"
                alt="Hand Analysis Statistics"
                width={800}
                height={600}
                className="rounded-lg shadow-lg hover:shadow-xl transition-shadow"
              />
            </a>
          </div>
          <div className="md:w-1/2">
            <h2 className="text-2xl font-semibold text-green-700 mb-3">Hand Analysis</h2>
            <p className="text-gray-700">
              Complete hand history tracking and statistical analysis of your play.
            </p>
          </div>
        </section>

        <section className="flex flex-col md:flex-row gap-6 items-start">
          <div className="md:w-1/2">
            <a href="/images/ss_v2/clock.jpg" target="_blank" rel="noopener noreferrer">
              <Image
                src="/images/ss_v2/clock.jpg"
                alt="Poker Clock"
                width={800}
                height={600}
                className="rounded-lg shadow-lg hover:shadow-xl transition-shadow"
              />
            </a>
          </div>
          <div className="md:w-1/2">
            <h2 className="text-2xl font-semibold text-green-700 mb-3">Poker Clock</h2>
            <p className="text-gray-700">
              Tournament timer with automated blind tracking and level management for home games.
            </p>
          </div>
        </section>

        <section className="flex flex-col md:flex-row gap-6 items-start">
          <div className="md:w-1/2">
            <a href="/images/ss_v2/chatlobby.jpg" target="_blank" rel="noopener noreferrer">
              <Image
                src="/images/ss_v2/chatlobby.jpg"
                alt="Online Lobby"
                width={800}
                height={600}
                className="rounded-lg shadow-lg hover:shadow-xl transition-shadow"
              />
            </a>
          </div>
          <div className="md:w-1/2">
            <h2 className="text-2xl font-semibold text-green-700 mb-3">Online Lobby</h2>
            <p className="text-gray-700">
              Chat and coordinate with other players before, during, and after tournaments.
            </p>
          </div>
        </section>

        <section className="flex flex-col md:flex-row gap-6 items-start">
          <div className="md:w-1/2">
            <a href="/images/ss_v2/calctool-showdown.jpg" target="_blank" rel="noopener noreferrer">
              <Image
                src="/images/ss_v2/calctool-showdown.jpg"
                alt="Showdown Calculator"
                width={800}
                height={600}
                className="rounded-lg shadow-lg hover:shadow-xl transition-shadow"
              />
            </a>
          </div>
          <div className="md:w-1/2">
            <h2 className="text-2xl font-semibold text-green-700 mb-3">Showdown Calculator</h2>
            <p className="text-gray-700">
              Calculate exact all-in odds with Monte Carlo simulation.
            </p>
          </div>
        </section>

        <section className="flex flex-col md:flex-row gap-6 items-start">
          <div className="md:w-1/2">
            <a href="/images/ss_v2/calctool-flop.jpg" target="_blank" rel="noopener noreferrer">
              <Image
                src="/images/ss_v2/calctool-flop.jpg"
                alt="Flop Calculator"
                width={800}
                height={600}
                className="rounded-lg shadow-lg hover:shadow-xl transition-shadow"
              />
            </a>
          </div>
          <div className="md:w-1/2">
            <h2 className="text-2xl font-semibold text-green-700 mb-3">Flop/Turn/River Analysis</h2>
            <p className="text-gray-700">
              Project hand improvement probabilities.
            </p>
          </div>
        </section>

        <section className="flex flex-col md:flex-row gap-6 items-start">
          <div className="md:w-1/2">
            <a href="/images/ss_v2/calctool-ladder.jpg" target="_blank" rel="noopener noreferrer">
              <Image
                src="/images/ss_v2/calctool-ladder.jpg"
                alt="Hand Ladder"
                width={800}
                height={600}
                className="rounded-lg shadow-lg hover:shadow-xl transition-shadow"
              />
            </a>
          </div>
          <div className="md:w-1/2">
            <h2 className="text-2xl font-semibold text-green-700 mb-3">Hand Ladder</h2>
            <p className="text-gray-700">
              Compare your hand against all possible opponent holdings.
            </p>
          </div>
        </section>

        <section className="flex flex-col md:flex-row gap-6 items-start">
          <div className="md:w-1/2">
            <a href="/images/ss_v2/calctool-sims.jpg" target="_blank" rel="noopener noreferrer">
              <Image
                src="/images/ss_v2/calctool-sims.jpg"
                alt="Hand Simulations"
                width={800}
                height={600}
                className="rounded-lg shadow-lg hover:shadow-xl transition-shadow"
              />
            </a>
          </div>
          <div className="md:w-1/2">
            <h2 className="text-2xl font-semibold text-green-700 mb-3">Hand Group Simulations</h2>
            <p className="text-gray-700">
              Test your hand against customizable opponent ranges.
            </p>
          </div>
        </section>
      </div>

      <section className="bg-green-50 p-6 rounded-lg mt-12">
        <h2 className="text-2xl font-semibold text-green-700 mb-4">Experience DD Poker Yourself</h2>
        <p className="text-gray-700 mb-4">
          These screenshots only show a glimpse of what DD Poker has to offer. Download the full application to experience all features firsthand.
        </p>
        <Link
          href="/download"
          className="inline-block bg-green-600 hover:bg-green-700 text-white font-semibold px-6 py-3 rounded-lg transition-colors"
        >
          Download DD Poker
        </Link>
      </section>
    </div>
  );
}
