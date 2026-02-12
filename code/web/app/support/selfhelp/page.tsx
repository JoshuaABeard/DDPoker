/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Self Help Page
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import Link from 'next/link'
import { Metadata } from 'next'

export const metadata: Metadata = {
  title: 'Self Help - DD Poker Community Edition',
  description: 'Self-help guide for common DD Poker issues',
}

export default function SelfHelp() {
  return (
    <div className="container mx-auto px-4 py-8 max-w-4xl">
      <h1 className="text-3xl font-bold mb-4">Self Help</h1>

      <p className="mb-6 leading-relaxed">
        If you are having an issue with DD Poker, please look over the options listed below.
      </p>

      <div className="space-y-8">
        {/* Help Menu */}
        <div className="grid md:grid-cols-2 gap-6">
          <div className="p-4 bg-[var(--bg-light-gray)] rounded-lg">
            <h2 className="text-xl font-bold mb-4">Known Issues</h2>
            <div className="text-center space-x-4">
              <a href="#win" className="text-[var(--color-poker-green)] hover:underline">
                Windows
              </a>
              <a href="#mac" className="text-[var(--color-poker-green)] hover:underline">
                Mac OS X
              </a>
              <a href="#linux" className="text-[var(--color-poker-green)] hover:underline">
                Linux
              </a>
            </div>
          </div>

          <div className="p-4 bg-[var(--bg-light-gray)] rounded-lg">
            <h2 className="text-xl font-bold mb-4">Security Program Issues</h2>
            <p className="mb-2">
              The Loading... screen appears, then goes away{' '}
              <a href="#win1" className="text-[var(--color-poker-green)] hover:underline">
                See here for details.
              </a>
            </p>
          </div>

          <div className="p-4 bg-[var(--bg-light-gray)] rounded-lg">
            <h2 className="text-xl font-bold mb-4">Password Help</h2>
            <ul className="list-disc list-inside space-y-1">
              <li>
                <Link href="/support/passwords#forgot" className="text-[var(--color-poker-green)] hover:underline">
                  I forgot my Password
                </Link>
              </li>
              <li>
                <Link href="/support/passwords#create" className="text-[var(--color-poker-green)] hover:underline">
                  Create an Online Profile
                </Link>
              </li>
              <li>
                <Link href="/support/passwords#change" className="text-[var(--color-poker-green)] hover:underline">
                  Change your Password or email
                </Link>
              </li>
              <li>
                <Link href="/support/passwords#webpage" className="text-[var(--color-poker-green)] hover:underline">
                  Online Games Portal Login
                </Link>
              </li>
            </ul>
          </div>

          <div className="p-4 bg-[var(--bg-light-gray)] rounded-lg">
            <h2 className="text-xl font-bold mb-4">Help Using DD Poker</h2>
            <p>
              We have tons of information in the game help. Click the Help button in your game to access it.
            </p>
          </div>
        </div>

        {/* Updates */}
        <div>
          <h2 className="text-2xl font-bold mb-4 pb-2 border-b-2 border-[var(--color-poker-green)]">Updates</h2>
          <p className="leading-relaxed">
            DD Poker 3 checks with our server regularly and will notify you when a new release is available for
            download. However, if you missed or ignored an update message, you can make sure you are at the most
            recent version by checking the <strong>Current Version</strong> at the top of the{' '}
            <Link href="/download" className="text-[var(--color-poker-green)] hover:underline">
              download page
            </Link>{' '}
            and comparing it to your version (seen at the bottom right of any menu screen).
          </p>
        </div>

        {/* Windows Issues */}
        <div id="win">
          <h2 className="text-2xl font-bold mb-4 pb-2 border-b-2 border-[var(--color-poker-green)]">
            Windows Issues
          </h2>

          <div className="space-y-4">
            <div className="p-4 bg-[var(--bg-light-gray)] rounded-lg">
              <div id="win1" className="mb-4">
                <h3 className="text-xl font-bold mb-2">Issue 1</h3>
                <p className="mb-2">
                  Nothing happens when you run the game or stops during the <span className="font-bold">Loading...</span>{' '}
                  screen.
                </p>
              </div>

              <div className="space-y-2">
                <div>
                  <strong>Platform:</strong> Any Windows
                </div>

                <div>
                  <strong>Cause 1:</strong> The most likely cause is your Internet security software is preventing DD
                  Poker from running or accessing your LAN or the internet. Examples include but are not limited to
                  Windows Firewall, Norton Internet Security, Norton Personal Firewall, McAfee Anti Virus, CA products,
                  Zone Alarm among others.
                </div>

                <div>
                  <strong>Solution 1:</strong> Review your security program's instructions. Each has specific ways to
                  enter the information below:
                  <ul className="list-disc list-inside ml-4 mt-2">
                    <li>Allow the execution of ddpoker.exe.</li>
                    <li>Allow TCP over ports 11885 to 11889.</li>
                    <li>Allow UDP over ports 11885 to 11889.</li>
                  </ul>
                  <p className="mt-2">See the firewall and security software documentation for configuration details.</p>
                </div>

                <div>
                  <strong>Cause 2:</strong> It is possible that the poker database is corrupt.
                </div>

                <div>
                  <strong>Solution 2:</strong> Rename your database folder and try running the game again.
                  <p className="mt-2">
                    Using My Computer, navigate to C:\Documents and Settings\Your Name\.dd-poker2\save\
                    <br />
                    Note: &quot;Your Name&quot; is the unique name of your computer which may be your name.
                    <br />
                    Inside the save folder is the database folder named db.
                    <br />
                    Rename the db folder to db2
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Mac OS X Issues */}
        <div id="mac">
          <h2 className="text-2xl font-bold mb-4 pb-2 border-b-2 border-[var(--color-poker-green)]">
            Mac OS X Issues
          </h2>

          <div className="space-y-4">
            <div className="p-4 bg-[var(--bg-light-gray)] rounded-lg">
              <div className="mb-4">
                <h3 className="text-xl font-bold mb-2">Issue 1: Installing Version 3</h3>
              </div>

              <div className="space-y-2">
                <div>
                  <strong>Platform:</strong> Mac OS X
                </div>

                <div>
                  <strong>Details:</strong> In order to install DD Poker 3 (or higher), your minimum system
                  requirements must have macOS 10.4.5 (or higher) and the Java 5.0 update.
                </div>

                <div>
                  <strong>Mac OS:</strong>
                  <p className="mt-2">
                    What version Mac OS am I at?
                    <br />
                    Click the Apple logo in the menu bar on the top left of your screen, a menu will appear, choose
                    About This Mac. Your version will be shown here.
                  </p>
                  <p className="mt-2">
                    If you are at macOS 10.3 Panther, you will need to upgrade to a newer version of Mac OS X to run DD
                    Poker 3.
                  </p>
                  <p className="mt-2">
                    If you have Mac OS 10.4 Tiger, but are not up to 10.4.5, click the Apple logo in the menu on the
                    top left of your screen, a menu will appear, choose Software Update for a free update.
                  </p>
                </div>

                <div>
                  <strong>Java:</strong>
                  <p className="mt-2">What version of Java am I at?</p>
                  <p className="mt-2">
                    Using Finder, open a Terminal window in Applications, Utilities. Type <code>java -version</code>{' '}
                    then press the <strong>Enter</strong> key. The line printed will display the version of java. It
                    should show &quot;<strong>build 1.5</strong>&quot;. If the version is below 1.5 you need to update
                    to Java 5.0.
                  </p>
                  <p className="mt-2">
                    If you are not at Java 5.0, you will need to update your Java version through Apple Software Update
                    or download a newer Java version.
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Linux Issues */}
        <div id="linux">
          <h2 className="text-2xl font-bold mb-4 pb-2 border-b-2 border-[var(--color-poker-green)]">Linux Issues</h2>

          <div className="space-y-6">
            <div className="p-4 bg-[var(--bg-light-gray)] rounded-lg">
              <div className="mb-4">
                <h3 className="text-xl font-bold mb-2">Issue 1</h3>
                <p className="mb-2">After Installation, when you run DD Poker, you get an error saying:</p>
                <p className="mb-2">
                  This application has unexpectedly quit
                  <br />
                  Invocation of this Java application has caused an InvocationTargetException. This application will
                  now exit.
                </p>
                <p>
                  If you look at the details or log in your ddpoker2/log/err.txt you will see a data which includes
                  Java.lang.unsatisifiedLinkError with the statement below:
                  <br />
                  <code>
                    libDDClassLoader.so: libstdc++-libc6.2-2.so.3: cannot open shared object file: No such file or
                    directory
                  </code>
                </p>
              </div>

              <div className="space-y-2">
                <div>
                  <strong>Details:</strong> This library is apparently not on all linux installations and was not
                  statically linked into the DD Poker library. This has been seen on Mandrake and Gentoo.
                </div>

                <div>
                  <strong>Solution:</strong> Download the <code>libstdc++-libc6.2-2.so.3</code> library, place it in
                  your <code>/usr/lib</code> from the root directory and make sure it has 555 permissions with the
                  command <code>chmod 555 libstdc++-libc6.2-2.so.3</code> You may need to be root.
                </div>

                <div>
                  <strong>Solution for Debian Linux:</strong> Debian Linux may still not find the shared object file
                  above. The closest file in the Debian repository is libstdc++2.10-glibc2.2
                  <p className="mt-2">
                    Install this file using the Debian's 'apt' tool with the following command:{' '}
                    <code>$ apt-get install libstdc++2.10-glibc2.2</code>
                  </p>
                </div>
              </div>
            </div>

            <div className="p-4 bg-[var(--bg-light-gray)] rounded-lg">
              <div className="mb-4">
                <h3 className="text-xl font-bold mb-2">Issue 2: Sound does not work on Linux</h3>
              </div>

              <div>
                <strong>Solution:</strong>
                <p className="mt-2">Here is how one customer fixed this problem:</p>
                <p className="mt-2">
                  &quot;The volume control was useless for almost all applications or had really low volume. When I
                  went to the terminal window and typed <code>alsamixer</code>, I found that the system was running
                  Pulse Audio! It should be running <code>alsa</code>. I typed <code>sudo alsamixer -c 0</code> (zero)
                  which set the alsa mixer to a default configuration. I then went to two places,{' '}
                  <code>system =&gt; preferences =&gt; sound</code> and set all to alsa mixer and used the test
                  buttons. Also <code>system =&gt; preferences =&gt; sessions</code> and made sure Pulse Audio was
                  unchecked.&quot;
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
