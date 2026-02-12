/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Download Page
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { Metadata } from 'next'

export const metadata: Metadata = {
  title: 'Download - DD Poker Community Edition',
  description: 'Download DD Poker Community Edition for Windows, macOS, or Linux',
}

export default function Download() {
  const version = '3.3.0'

  return (
    <div className="container mx-auto px-4 py-8 max-w-4xl">
      <h1 className="text-3xl font-bold text-center mb-4">Download DD Poker Community Edition</h1>

      <p className="text-center mb-8">
        Current Version: <strong>{version}</strong>
      </p>

      <div className="space-y-6">
        {/* Windows Installer */}
        <div className="p-6 bg-green-50 border-2 border-green-500 rounded-lg">
          <h2 className="text-2xl font-bold text-green-800 mt-0 mb-4">🪟 Windows Installer (Recommended)</h2>
          <p className="mb-4">
            <strong>
              <a href={`/api/downloads/DDPokerCE-${version}.msi`} className="text-lg text-blue-600 hover:underline">
                ⬇️ Download Windows Installer
              </a>
            </strong>{' '}
            (~98 MB)
            <br />
            <span className="text-sm text-gray-700">
              <strong>Best for Windows users</strong> - No Java installation required!
            </span>
          </p>
          <div className="bg-white p-4 rounded">
            <strong>Installation:</strong>
            <ol className="list-decimal list-inside mt-2 space-y-1 ml-2">
              <li>Download and run the .msi installer</li>
              <li>Windows SmartScreen may warn (unsigned) - click "More info" → "Run anyway"</li>
              <li>Follow the installation wizard</li>
              <li>
                Launch from Start Menu: <strong>DD Poker Community Edition</strong>
              </li>
            </ol>
          </div>
          <p className="text-xs bg-yellow-100 p-2 rounded mt-4">
            ℹ️ <strong>Note:</strong> The installer includes a bundled Java runtime, so no separate Java
            installation is needed.
          </p>
        </div>

        {/* macOS Installer */}
        <div className="p-6 bg-blue-50 border-2 border-blue-500 rounded-lg">
          <h2 className="text-2xl font-bold text-blue-800 mt-0 mb-4">
            🍎 macOS Installer (Recommended for Mac)
          </h2>
          <p className="mb-4">
            <strong>
              <a href={`/api/downloads/DDPokerCE-${version}.dmg`} className="text-lg text-blue-600 hover:underline">
                ⬇️ Download macOS Installer
              </a>
            </strong>{' '}
            (~98 MB)
            <br />
            <span className="text-sm text-gray-700">
              <strong>Best for Mac users</strong> - No Java installation required!
            </span>
          </p>
          <div className="bg-white p-4 rounded">
            <strong>Installation:</strong>
            <ol className="list-decimal list-inside mt-2 space-y-1 ml-2">
              <li>Download and open the .dmg file</li>
              <li>
                Drag <strong>DD Poker CE</strong> to the Applications folder
              </li>
              <li>
                <strong>Important:</strong> Right-click the app and select "Open" (bypasses Gatekeeper for unsigned apps)
              </li>
              <li>Confirm when macOS asks "Are you sure you want to open it?"</li>
            </ol>
          </div>
          <p className="text-xs bg-yellow-100 p-2 rounded mt-4">
            ℹ️ <strong>Note:</strong> This app is not signed with an Apple Developer certificate. Use
            "Right-click → Open" to bypass Gatekeeper.
          </p>
        </div>

        {/* Source Code */}
        <div className="p-6 bg-gray-50 border-2 border-gray-400 rounded-lg">
          <h2 className="text-2xl font-bold text-gray-800 mt-0 mb-4">📦 Source Code & JAR</h2>
          <p className="mb-4">
            <strong>GitHub Repository:</strong>{' '}
            <a
              href="https://github.com/Jsabeard/DDPoker"
              target="_blank"
              rel="noopener noreferrer"
              className="text-blue-600 hover:underline"
            >
              https://github.com/Jsabeard/DDPoker
            </a>
          </p>
          <p className="leading-relaxed">
            Build from source or run the JAR file directly if you have Java 25+ installed. See the GitHub
            repository README for build instructions and Docker deployment guide.
          </p>
        </div>
      </div>

      <div className="mt-8 p-6 bg-blue-50 rounded-lg">
        <h3 className="text-xl font-bold mb-2">System Requirements</h3>
        <ul className="list-disc list-inside space-y-1 ml-4">
          <li>
            <strong>Windows:</strong> Windows 10 or later (bundled Java runtime included)
          </li>
          <li>
            <strong>macOS:</strong> macOS 10.14 (Mojave) or later (bundled Java runtime included)
          </li>
          <li>
            <strong>Linux:</strong> Java 25+ required (use JAR or build from source)
          </li>
          <li>
            <strong>RAM:</strong> Minimum 512 MB, 1 GB recommended
          </li>
          <li>
            <strong>Disk Space:</strong> 200 MB free space
          </li>
        </ul>
      </div>
    </div>
  )
}
