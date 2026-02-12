import type { Metadata } from 'next';
import Link from 'next/link';

export const metadata: Metadata = {
  title: 'Poker FAQ - DD Poker Community Edition',
  description: 'Frequently asked questions about Texas Hold\'em poker rules, terminology, and strategy. Learn the basics of poker and common terms.',
};

export default function FaqPage() {
  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold text-green-700 mb-6">Poker FAQ</h1>

      <div className="mb-8">
        <p className="text-lg text-gray-700 mb-4">
          New to Texas Hold'em? This FAQ covers the basics of the game, common terminology, and answers to frequently asked questions about poker rules and strategy.
        </p>
      </div>

      <div className="space-y-6">
        <section className="bg-gray-50 p-6 rounded-lg">
          <h2 className="text-xl font-semibold text-green-700 mb-3">Q1. How do you play Texas Hold'em?</h2>
          <div className="text-gray-700 space-y-3">
            <p>
              Texas Hold'em is a game of poker, some call it THE game; the Cadillac of poker. No-limit Texas Hold'em is the game played at the World Series of Poker to determine the best poker player in the land. The actual play is where each player receives two cards face-down and then combines them with five community (face-up) cards to make the best possible five-card hand. A very simple game that takes a lifetime to master.
            </p>
            <p>
              Texas Hold'em gets the betting started by forcing bets before the hole cards are dealt. These are called 'blinds'. There are two blinds; the Small Blind and the Big Blind. The two players immediately to the left of the dealer make the blind bets before they receive their hole cards. Thus a 'blind' bet. Two cards are then dealt face down to each player; these are called the 'hole cards'. Play proceeds clockwise from the blinds. Each player then chooses to call the Big Blind, raise, or fold. The player placing the Big Blind may raise, check the bet, or fold.
            </p>
            <p>
              Three cards are then dealt face up on the table; this is called the 'Flop'. A round of betting takes place, with the action starting to the dealer's immediate left; players again choose to check, bet, raise, or fold. Another card is dealt face up; this is called the 'Turn' or '4th street'. Another round of betting takes place. The fifth and final card, called the 'River' or '5th Street', is dealt face up and followed by the final round of betting.
            </p>
            <p>
              The remaining players make the best five-card hand possible using any of their two cards and the five community cards. The winner is either the player with the best hand or the player that is left after having every other player fold.
            </p>
          </div>
        </section>

        <section className="bg-gray-50 p-6 rounded-lg">
          <h2 className="text-xl font-semibold text-green-700 mb-3">Q2. What is No Limit Hold'em?</h2>
          <p className="text-gray-700">
            'No limit' means that there is <strong>no limit to the amount a player may bet</strong> in any given round; up to the amount the player has in his stack. A player can choose to go 'All In' and bet everything they have forcing the other players to either match a very large bet or to fold.
          </p>
        </section>

        <section className="bg-gray-50 p-6 rounded-lg">
          <h2 className="text-xl font-semibold text-green-700 mb-3">Q3. What are the Flop, Turn and River?</h2>
          <p className="text-gray-700">
            The Flop refers to the first three community cards dealt in a game of hold'em. The fourth community card is called The Turn or <strong>4th Street</strong>. The fifth community card is called The River or 5th Street. <em>I'm ahead with a straight when he sucks out a flush on the Turn; I got drowned on the River.</em>
          </p>
        </section>

        <section className="bg-gray-50 p-6 rounded-lg">
          <h2 className="text-xl font-semibold text-green-700 mb-3">Q4. What are the Small Blind and Big Blind?</h2>
          <div className="text-gray-700 space-y-3">
            <p><strong>A blind bet is a mandatory forced bet</strong> before any cards are dealt.</p>
            <p>
              The Small Blind is the first bet placed by the player immediately to the left of the dealer. It is generally half the Big Blind. Example: Small Blind = $2.00 then the Big Blind = $4.00.
            </p>
            <p>
              The Big Blind is the next bet, placed by the player immediately to the left of the Small Blind. Example: Small Blind = $2.00 then the Big Blind = $4.00.
            </p>
            <p>
              During tournaments the blinds are increased on a regular basis. Example: Blinds are raised every 30min. Small Blind increases to $3.00 and the Big Blind increases to $6.00; another 30min. the blinds increase to $4.00 and $8.00, and so on. This increases the pressure on the players and increases the chance of elimination.
            </p>
          </div>
        </section>

        <section className="bg-gray-50 p-6 rounded-lg">
          <h2 className="text-xl font-semibold text-green-700 mb-3">Q5. What does it mean to go 'All In'?</h2>
          <p className="text-gray-700">
            A player goes 'All In' when they choose to bet all of their chips in a betting round.
          </p>
        </section>

        <section className="bg-gray-50 p-6 rounded-lg">
          <h2 className="text-xl font-semibold text-green-700 mb-3">Q6. What does it mean to check, call, raise, and fold?</h2>
          <div className="text-gray-700 space-y-2">
            <p><strong>Check</strong>: to pass on betting keeping yourself in the hand to call, raise or fold after other players act.</p>
            <p><strong>Call</strong>: to match a player's bet with even money.</p>
            <p><strong>Raise</strong>: to place a bet larger than a bet already placed on the table. A raise must be greater than or equal to the bet being called.</p>
            <p><strong>Fold</strong>: to throw your hand away.</p>
          </div>
        </section>

        <section className="bg-gray-50 p-6 rounded-lg">
          <h2 className="text-xl font-semibold text-green-700 mb-3">Q7. What is the Button?</h2>
          <div className="text-gray-700 space-y-3">
            <p>
              The button is a white disc that is passed around the table in clockwise order. The button is used in games where there is a single or casino dealer. The button identifies the player as the 'dealer'. It is still important that play continues as if each player is dealing cards.
            </p>
            <p>
              Cards are dealt beginning with the player to the left of the button, and ending with the player who has the button in front of him. When the hand is over, the button is moved one spot clockwise for the start of the next hand.
            </p>
          </div>
        </section>

        <section className="bg-gray-50 p-6 rounded-lg">
          <h2 className="text-xl font-semibold text-green-700 mb-3">Q8. What does it mean to 'Limp In'?</h2>
          <p className="text-gray-700">
            The player calls a bet before the flop trying to see the flop cheaply, or hoping to check-raise a player that bets farther around the table. <em>He limped in with a call on the big blind and then flopped a full house.</em>
          </p>
        </section>

        <section className="bg-gray-50 p-6 rounded-lg">
          <h2 className="text-xl font-semibold text-green-700 mb-3">Q9. What is 'tight play', 'loose play', 'aggressive play' and 'passive play'?</h2>
          <div className="text-gray-700 space-y-3">
            <p>
              These are all terms that describe playing styles. Loose and tight generally refer to a player's attitude toward his stack of chips. The loose player bets more often prior to the flop and will gamble more to try and make a hand. They tend to bluff a lot.
            </p>
            <p>
              A player that is 'tight' plays few hands that are carefully selected; they'll fold easily. They tend to only play strong hands.
            </p>
            <p>
              Passive and aggressive refer to a player's attitude toward other players. A 'passive' player tends to follow other player's leads and not try to command the table while an 'aggressive' player tries to command the table and force others to chase him or fold.
            </p>
            <p>These attitudes combine to create four generalized playing styles:</p>
            <p>
              <strong>Loose-passive:</strong> These players don't guard every penny, but they do tend to follow other players leads. These players will play most hands, but rarely initiate bets or raises. Loose-passive play is typified by casual 'poker night' games amongst friends.
            </p>
            <p>
              <strong>Loose-aggressive:</strong> These players are also free with their money, but they are in the game for the action and want to command the game. These players will raise often, even with weak cards. Loose-aggressive play is typified by the 'play money' internet poker rooms where no real money is at stake.
            </p>
            <p>
              <strong>Tight-passive:</strong> These players are careful with their money and follow other player's leads. These players let others initiate betting, call when they have good cards and seldom raise. Tight-passive play is typified by regulars at public card rooms.
            </p>
            <p>
              <strong>Tight-aggressive:</strong> These players guard their money, but also love the competition and want to command the game. They may let other's initiate betting, but when they raise it is very probable that they hold good cards. Tight-aggressive play is typified by professional poker players and those in tournaments.
            </p>
            <p>
              It is important to note that none of these play styles is absolute – anyone may 'push his luck' or bluff if they feel the time is right.
            </p>
          </div>
        </section>

        <section className="bg-gray-50 p-6 rounded-lg">
          <h2 className="text-xl font-semibold text-green-700 mb-3">Q10. What is 'playing the board'?</h2>
          <p className="text-gray-700">
            A player can choose to 'play the board' and use only community cards. If the best hand is in the community cards the pot is split between the players still in the hand. <em>We played the board with a full house and split the pot.</em>
          </p>
        </section>

        <section className="bg-gray-50 p-6 rounded-lg">
          <h2 className="text-xl font-semibold text-green-700 mb-3">Q11. What are Overcards?</h2>
          <p className="text-gray-700">
            If you hold a pocket pair (a pair in your face-down cards) then overcards are any higher card. For example, you are dealt a pair of 5's in your face down cards and the Flop is 2, 9, Jack. The overcards are 9 and J.
          </p>
        </section>

        <section className="bg-gray-50 p-6 rounded-lg">
          <h2 className="text-xl font-semibold text-green-700 mb-3">Q12. What is 'Tilt Mode'?</h2>
          <p className="text-gray-700">
            A player in tilt mode is playing reckless because of a recent loss. <em>He lost that last hand on the River so now he's on tilt mode trying to win back his money.</em>
          </p>
        </section>

        <section className="bg-gray-50 p-6 rounded-lg">
          <h2 className="text-xl font-semibold text-green-700 mb-3">Q13. What is 'burning a card'?</h2>
          <p className="text-gray-700">
            When dealing the Flop, The Turn, and the River the dealer should take the top card off the deck and remove it from play without any player seeing it. Burning a card protects against cheating or accidentally seeing the cards.
          </p>
        </section>

        <section className="bg-gray-50 p-6 rounded-lg">
          <h2 className="text-xl font-semibold text-green-700 mb-3">Q14. What is to 'Come Over The Top'?</h2>
          <p className="text-gray-700">
            Coming over the top is when you raise or re-raise an opponent's bet. <em>I raised $50.00 but then he came over the top with $100.00.</em>
          </p>
        </section>

        <section className="bg-gray-50 p-6 rounded-lg">
          <h2 className="text-xl font-semibold text-green-700 mb-3">Q15. When am I 'Playing Dead'?</h2>
          <p className="text-gray-700">
            Playing Dead is a hand where you have no chance of winning while there are still cards to be dealt. <em>He flopped into four of a kind so I was playing dead with my two pair.</em>
          </p>
        </section>

        <section className="bg-gray-50 p-6 rounded-lg">
          <h2 className="text-xl font-semibold text-green-700 mb-3">Q16. What's a fish?</h2>
          <p className="text-gray-700">
            A fish is a very loose, often inexperienced player. Fish tend to lose often. Sometimes called a live one. <em>If you haven't figured out who the fish is at the table it's probably you.</em>
          </p>
        </section>

        <section className="bg-gray-50 p-6 rounded-lg">
          <h2 className="text-xl font-semibold text-green-700 mb-3">Q17. What's a rebuy?</h2>
          <p className="text-gray-700">
            Some tournaments allow players to buy in again if they go broke or if their chip stack falls below a certain level.
          </p>
        </section>

        <section className="bg-gray-50 p-6 rounded-lg">
          <h2 className="text-xl font-semibold text-green-700 mb-3">Q18. What is 'rabbit-hunting'?</h2>
          <p className="text-gray-700">
            A player searches through the undealt cards to see what would have happened had the hand played out. This is not allowed in tournament games and many casinos. <em>He folded on the flop and then tried to go rabbit-hunting to see the Turn and the River.</em>
          </p>
        </section>

        <section className="bg-gray-50 p-6 rounded-lg">
          <h2 className="text-xl font-semibold text-green-700 mb-3">Q19. What is Big Slick?</h2>
          <p className="text-gray-700">Ace-King hole cards.</p>
        </section>

        <section className="bg-gray-50 p-6 rounded-lg">
          <h2 className="text-xl font-semibold text-green-700 mb-3">Q20. What is American Airlines?</h2>
          <p className="text-gray-700">A pair of Aces.</p>
        </section>

        <section className="bg-gray-50 p-6 rounded-lg">
          <h2 className="text-xl font-semibold text-green-700 mb-3">Q21. What is Presto?</h2>
          <p className="text-gray-700">Pocket 5s.</p>
        </section>

        <section className="bg-gray-50 p-6 rounded-lg">
          <h2 className="text-xl font-semibold text-green-700 mb-3">Q22. What are The Nuts?</h2>
          <p className="text-gray-700">
            In a hold-em hand, the nuts refer to the two hole cards that make the best possible hand. For example, if the board shows A Q J 5 4 (different suits), then the nuts are K 10, to make a straight; the best possible hand.
          </p>
        </section>

        <section className="bg-gray-50 p-6 rounded-lg">
          <h2 className="text-xl font-semibold text-green-700 mb-3">Q23. What are Gut Shot and Belly Busters?</h2>
          <p className="text-gray-700">
            This refers to a inside straight draw where only one card will complete the straight. For example, if your best hand is 9 8 6 5, then you are on a Gut Shot (or Belly Buster) draw for a 7.
          </p>
        </section>

        <section className="bg-gray-50 p-6 rounded-lg">
          <h2 className="text-xl font-semibold text-green-700 mb-3">Q24. What are Trips?</h2>
          <p className="text-gray-700">Three of a kind.</p>
        </section>

        <section className="bg-gray-50 p-6 rounded-lg">
          <h2 className="text-xl font-semibold text-green-700 mb-3">Q25. What is the Overpair, 2nd Pair, and Bottom Pair?</h2>
          <p className="text-gray-700">
            If the Flop is Q, 9, 4 then an Overpair would be KK, the second pair would be J9, and the bottom pair would be 42.
          </p>
        </section>

        <section className="bg-gray-50 p-6 rounded-lg">
          <h2 className="text-xl font-semibold text-green-700 mb-3">Q26. What is The Deadman's Hand?</h2>
          <p className="text-gray-700">
            The Deadman's Hand was the hand held by Wild Bill Hickock when he was shot in the back during a poker game in Deadwood, SD. It consisted of two pair: black Aces and Eights. The fifth card is in dispute.
          </p>
        </section>

        <section className="bg-gray-50 p-6 rounded-lg">
          <h2 className="text-xl font-semibold text-green-700 mb-3">Q27. What's Union Oil?</h2>
          <p className="text-gray-700">
            Hole cards are a 7 and a 6. 76 was part of the logo for Union Oil.
          </p>
        </section>

        <section className="bg-gray-50 p-6 rounded-lg">
          <h2 className="text-xl font-semibold text-green-700 mb-3">Q28. Where can I find the official poker tournament rules?</h2>
          <p className="text-gray-700">
            The Tournament Directors Association (TDA) is the group which publishes official tournament rules. The latest rules can be found at the <a href="https://www.pokertda.com/poker-tda-rules/" target="_blank" rel="noopener noreferrer" className="text-green-600 hover:text-green-700 underline">Official TDA Website</a>.
          </p>
        </section>
      </div>

      <section className="bg-green-50 p-6 rounded-lg mt-12">
        <h2 className="text-2xl font-semibold text-green-700 mb-4">Ready to Learn More?</h2>
        <p className="text-gray-700 mb-4">
          The best way to learn poker is by playing. Download DD Poker and start practicing today.
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
