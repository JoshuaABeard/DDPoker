/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Password Help Page
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import Link from 'next/link'
import { Metadata } from 'next'

export const metadata: Metadata = {
  title: 'Password Help - DD Poker Community Edition',
  description: 'Help with passwords and online profiles for DD Poker',
}

export default function PasswordHelp() {
  return (
    <div className="container mx-auto px-4 py-8 max-w-4xl">
      <h1 className="text-3xl font-bold mb-4">Password Help</h1>

      <p className="mb-4 leading-relaxed">This page is designed to help you with common password issues.</p>

      <ul className="list-disc list-inside space-y-1 mb-8 ml-4">
        <li>
          <a href="#forgot" className="text-[var(--color-poker-green)] hover:underline">
            I forgot my password
          </a>
        </li>
        <li>
          <a href="#create" className="text-[var(--color-poker-green)] hover:underline">
            Create an Online Profile
          </a>
        </li>
        <li>
          <a href="#change" className="text-[var(--color-poker-green)] hover:underline">
            Change your password or email
          </a>
        </li>
        <li>
          <a href="#webpage" className="text-[var(--color-poker-green)] hover:underline">
            Online Games Portal login
          </a>
        </li>
      </ul>

      <div className="space-y-8">
        {/* I Forgot My Password */}
        <div id="forgot">
          <h2 className="text-2xl font-bold mb-4 pb-2 border-b-2 border-[var(--color-poker-green)]">
            I Forgot My Password
          </h2>

          <div className="p-4 bg-[var(--bg-light-gray)] rounded-lg">
            <p className="mb-4">Instructions to change your password or email associated with your online profile.</p>

            <div className="flex gap-4 items-start">
              <div className="text-3xl font-bold text-[var(--color-poker-green)] min-w-[40px]">1</div>
              <div>
                <p>
                  Visit the{' '}
                  <Link href="/forgot" className="text-[var(--color-poker-green)] hover:underline">
                    I Forgot My Password
                  </Link>{' '}
                  page. There you can enter your Online Profile name and we'll send your password to the associated
                  email.
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Create An Online Profile */}
        <div id="create">
          <h2 className="text-2xl font-bold mb-4 pb-2 border-b-2 border-[var(--color-poker-green)]">
            Create An Online Profile
          </h2>

          <div className="p-4 bg-[var(--bg-light-gray)] rounded-lg space-y-6">
            <p>To play public online games DD Poker needs to validate your email address. Follow these steps to do so.</p>

            <div className="flex gap-4 items-start">
              <div className="text-3xl font-bold text-[var(--color-poker-green)] min-w-[40px]">1</div>
              <div>
                <p>Click the Profile button from the main menu of the game.</p>
              </div>
            </div>

            <div className="flex gap-4 items-start">
              <div className="text-3xl font-bold text-[var(--color-poker-green)] min-w-[40px]">2</div>
              <div>
                <p className="mb-2">Click <strong>New</strong> to create a new profile.</p>
                <p>Or select an existing player profile and click the <strong>Edit</strong> button.</p>
              </div>
            </div>

            <div className="flex gap-4 items-start">
              <div className="text-3xl font-bold text-[var(--color-poker-green)] min-w-[40px]">3</div>
              <div className="space-y-4">
                <p>
                  Choose <strong>Activate Profile to Join/List Public Games</strong>, enter your email address and
                  click OK.
                </p>
                <p>
                  An email will be sent to you with a password. This email will be sent immediately, but allow a few
                  minutes for the email to arrive due to potentially high traffic on your ISP.
                </p>
                <p>
                  If you still have not received an email first click the <strong>Change Email</strong> button shown in{' '}
                  <span className="text-[var(--color-poker-green)]">Step 4</span> to confirm you typed in your email
                  address correctly. Then check to make sure your email provider or email program does not think it's
                  spam.
                </p>
                <p>Check your spam folder and configure your email to allow messages from the server.</p>
                <p>
                  <strong>Note:</strong> The player profile name is what other players see when you join public games
                  and is what is listed on the public DD Poker website. You cannot change this name after it is
                  created, so choose wisely. If your name is already taken, you will be notified and will have to
                  choose another name.
                </p>
              </div>
            </div>

            <div className="flex gap-4 items-start">
              <div className="text-3xl font-bold text-[var(--color-poker-green)] min-w-[40px]">4</div>
              <div className="space-y-4">
                <p>
                  Once you get the email with the password, enter it into your player profile{' '}
                  <strong>in all lowercase</strong> and click OK to complete the validation.
                </p>
                <p>
                  Once your email has been verified, you may wish to change your password to a familiar one you will
                  not forget and allow easier access to our{' '}
                  <Link href="/online" className="text-[var(--color-poker-green)] hover:underline">
                    Online Games Web Page
                  </Link>
                  . See below for instructions on{' '}
                  <a href="#change" className="text-[var(--color-poker-green)] hover:underline">
                    changing your password
                  </a>
                  .
                </p>
                <p>
                  You can change your email address or verify you typed it in correctly by clicking the{' '}
                  <strong>Change Email</strong> button.
                </p>
                <p>
                  Click <strong>Resend Password</strong> button for DD Poker to send another email to your email
                  address with the password.
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Change Your Password Via Website */}
        <div id="change">
          <h2 className="text-2xl font-bold mb-4 pb-2 border-b-2 border-[var(--color-poker-green)]">
            Change Your Password Via Website
          </h2>

          <div className="p-4 bg-[var(--bg-light-gray)] rounded-lg space-y-6">
            <p>Instructions to change your password for your online profile.</p>

            <div className="flex gap-4 items-start">
              <div className="text-3xl font-bold text-[var(--color-poker-green)] min-w-[40px]">1</div>
              <div>
                <p>
                  You can change your password on the{' '}
                  <Link href="/myprofile" className="text-[var(--color-poker-green)] hover:underline">
                    My Profile
                  </Link>{' '}
                  page.
                </p>
              </div>
            </div>

            <div className="flex gap-4 items-start">
              <div className="text-3xl font-bold text-[var(--color-poker-green)] min-w-[40px]">2</div>
              <div>
                <p>
                  Don't forget to change your password in the DD Poker game as well (see the explanation in the next
                  section regarding the <strong>Sync Password</strong> button).
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Change Your Password Or Email in the Game */}
        <div>
          <h2 className="text-2xl font-bold mb-4 pb-2 border-b-2 border-[var(--color-poker-green)]">
            Change Your Password Or Email in the Game
          </h2>

          <div className="p-4 bg-[var(--bg-light-gray)] rounded-lg space-y-6">
            <p>Instructions to change your password or email associated with your online profile.</p>

            <div className="flex gap-4 items-start">
              <div className="text-3xl font-bold text-[var(--color-poker-green)] min-w-[40px]">1</div>
              <div>
                <p>Click the Profile button from the main menu of the game.</p>
              </div>
            </div>

            <div className="flex gap-4 items-start">
              <div className="text-3xl font-bold text-[var(--color-poker-green)] min-w-[40px]">2</div>
              <div>
                <p>
                  Select your existing player profile you wish to change and click the <strong>Edit</strong> button.
                </p>
              </div>
            </div>

            <div className="flex gap-4 items-start">
              <div className="text-3xl font-bold text-[var(--color-poker-green)] min-w-[40px]">3</div>
              <div className="space-y-4">
                <p>
                  Click the <strong>Change Email</strong> button will allow you to enter in a new email address.
                  Changing your email address will deactivate your profile. You will be sent an email to verify your
                  new address including a new password to reactivate the profile.
                </p>
                <p>
                  Click the <strong>Change Password</strong> button to change your online password to one you will
                  remember.
                </p>
                <p>
                  Click the <strong>Sync Password</strong> button if you already changed your password on the{' '}
                  <Link href="/myprofile" className="text-[var(--color-poker-green)] hover:underline">
                    My Profile
                  </Link>{' '}
                  web page and you need to update the password in the DD Poker game to match.
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Online Games Portal Login */}
        <div id="webpage">
          <h2 className="text-2xl font-bold mb-4 pb-2 border-b-2 border-[var(--color-poker-green)]">
            Online Games Portal Login
          </h2>

          <p className="mb-4 leading-relaxed">
            Use your Online Profile name and password you{' '}
            <a href="#create" className="text-[var(--color-poker-green)] hover:underline">
              created
            </a>{' '}
            to log on to our{' '}
            <Link href="/online" className="text-[var(--color-poker-green)] hover:underline">
              Online Games Portal
            </Link>
            . Do so by clicking the login box at the top-right corner of any DD Poker page.
          </p>

          <p className="leading-relaxed">
            Once you have{' '}
            <a href="#create" className="text-[var(--color-poker-green)] hover:underline">
              online-activated
            </a>{' '}
            your profile to List/Join public games, you can use the online games portal to view the leaderboard,
            available games, running games, recent games, and your tournament history.
          </p>
        </div>
      </div>
    </div>
  )
}
