/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This file is part of DD Poker, originally created by Doug Donohoe.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file
 * in the root directory of this project.
 *
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images,
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials)
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 *
 * For inquiries regarding commercial licensing of this source code or
 * the use of names, logos, images, text, or other assets, please contact
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { spawn, execSync } from 'child_process'
import { existsSync } from 'fs'
import path from 'path'

const GAME_SERVER_PORT = 8877
const NEXT_PORT = 3000
const GAME_SERVER_URL = `http://localhost:${GAME_SERVER_PORT}`
const NEXT_URL = `http://localhost:${NEXT_PORT}`

const WEB_DIR = __dirname
const CODE_DIR = path.resolve(WEB_DIR, '..')

const JAR_PATH = path.join(
  CODE_DIR,
  'pokergameserver',
  'target',
  'pokergameserver-3.3.0-CommunityEdition-exec.jar',
)

async function waitForServer(url: string, label: string, timeoutMs: number): Promise<void> {
  const start = Date.now()
  while (Date.now() - start < timeoutMs) {
    try {
      const res = await fetch(url)
      if (res.ok || res.status < 500) {
        console.log(`  ${label} is ready`)
        return
      }
    } catch {
      // server not up yet
    }
    await new Promise((r) => setTimeout(r, 1000))
  }
  throw new Error(`${label} did not become ready within ${timeoutMs}ms`)
}

async function isPortInUse(port: number): Promise<boolean> {
  try {
    const res = await fetch(`http://localhost:${port}`)
    return res.ok || res.status > 0
  } catch {
    return false
  }
}

export default async function globalSetup(): Promise<void> {
  console.log('\n--- Playwright Global Setup ---')

  // Build the game server JAR if missing
  if (!existsSync(JAR_PATH)) {
    console.log('  Building pokergameserver JAR...')
    execSync('mvn package -pl pokergameserver -am -DskipTests', {
      cwd: CODE_DIR,
      stdio: 'inherit',
      shell: true,
    })
  }

  // Start game server if not already running
  if (await isPortInUse(GAME_SERVER_PORT)) {
    console.log(`  Game server already running on port ${GAME_SERVER_PORT}`)
  } else {
    console.log('  Starting game server...')
    const gameServer = spawn(
      'java',
      [
        '-jar',
        JAR_PATH,
        '--spring.profiles.active=embedded',
        `--server.port=${GAME_SERVER_PORT}`,
        '--spring.datasource.url=jdbc:h2:mem:e2etest;DB_CLOSE_DELAY=-1',
      ],
      {
        stdio: ['ignore', 'pipe', 'pipe'],
        detached: false,
      },
    )

    gameServer.stdout?.on('data', (data: Buffer) => {
      if (process.env.DEBUG) process.stdout.write(`[game-server] ${data}`)
    })
    gameServer.stderr?.on('data', (data: Buffer) => {
      if (process.env.DEBUG) process.stderr.write(`[game-server] ${data}`)
    })

    process.env.GAME_SERVER_PID = String(gameServer.pid)

    await waitForServer(
      `${GAME_SERVER_URL}/api/v1/auth/check-username?username=healthcheck`,
      'Game server',
      60_000,
    )
  }

  // Build Next.js if .next dir missing
  if (!existsSync(path.join(WEB_DIR, '.next'))) {
    console.log('  Building Next.js...')
    execSync('npm run build', {
      cwd: WEB_DIR,
      stdio: 'inherit',
      shell: true,
      env: {
        ...process.env,
        NEXT_PUBLIC_GAME_SERVER_URL: GAME_SERVER_URL,
        NEXT_PUBLIC_API_BASE_URL: GAME_SERVER_URL,
      },
    })
  }

  // Start Next.js if not already running
  if (await isPortInUse(NEXT_PORT)) {
    console.log(`  Next.js already running on port ${NEXT_PORT}`)
  } else {
    console.log('  Starting Next.js production server...')
    const nextServer = spawn('npx', ['next', 'start', '-p', String(NEXT_PORT)], {
      cwd: WEB_DIR,
      stdio: ['ignore', 'pipe', 'pipe'],
      shell: true,
      env: {
        ...process.env,
        NEXT_PUBLIC_GAME_SERVER_URL: GAME_SERVER_URL,
        NEXT_PUBLIC_API_BASE_URL: GAME_SERVER_URL,
      },
    })

    nextServer.stdout?.on('data', (data: Buffer) => {
      if (process.env.DEBUG) process.stdout.write(`[next] ${data}`)
    })
    nextServer.stderr?.on('data', (data: Buffer) => {
      if (process.env.DEBUG) process.stderr.write(`[next] ${data}`)
    })

    process.env.NEXT_SERVER_PID = String(nextServer.pid)

    await waitForServer(NEXT_URL, 'Next.js', 30_000)
  }

  console.log('--- Global Setup Complete ---\n')
}
