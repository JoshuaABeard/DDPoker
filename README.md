# DD Poker - Community Edition

> **Community-maintained fork** of DD Poker with enhanced Docker deployment and modern build tooling.
>
> **Version:** 3.3.0-CommunityEdition
> **Original Work:** [DD Poker by Doug Donohoe](https://github.com/dougdonohoe/ddpoker) (2003-2017)

![dd-poker-3.jpg](images/dd-poker-3.jpg)

## About This Fork

This is a **community-maintained fork** of DD Poker, created to provide:
- 🐳 **Modern Docker deployment** with Docker Compose support
- 🏗️ **Streamlined build process** optimized for Windows development
- 📦 **Universal client JAR** for easy distribution (21MB)
- 🔧 **Updated dependencies** compatible with Java 25
- 📚 **Comprehensive documentation** for deployment and development

**This fork is not affiliated with or endorsed by Doug Donohoe or Donohoe Digital LLC.** All original code, trademarks, and creative assets remain the property of their respective owners. See [Copyright and Licenses](#copyright-and-licenses) for details.

### What's New in 3.3.0-CommunityEdition

- 📁 **File-based JSON configuration** - Replaced Java Preferences with portable JSON config files
- 🔄 **Easy backup/restore** - Configuration now in human-readable format at platform-specific locations
- 🐳 **Container-friendly settings** - No Windows Registry dependencies, perfect for Docker deployments
- 💾 **Automatic backup & recovery** - Config files automatically backed up before changes with corruption recovery
- 🔐 **Configurable admin panel** - Environment variable-based admin access for user management and moderation
- 🧪 **Rock-solid reliability** - 116 comprehensive tests ensuring configuration system stability
- 📖 **Complete documentation** - Detailed guides for configuration management and troubleshooting

**Configuration file locations:**
- Windows: `%APPDATA%\ddpoker\config.json`
- macOS: `~/Library/Application Support/ddpoker/config.json`
- Linux: `~/.ddpoker/config.json`

See [docs/FILE-BASED-CONFIGURATION.md](docs/FILE-BASED-CONFIGURATION.md) for complete documentation.

### What's New in 3.3.0 Community Edition

- Docker deployment with pre-built images on Docker Hub
- Maven dependency plugin for reliable builds
- Sanitized PII and removed legacy components
- Enhanced documentation and quick-start guides
- Windows-focused development environment
- Unraid Community Application template

## About DD Poker

DD Poker is a full-featured **Texas Hold'em** (no-limit, pot-limit, limit) simulator originally developed by [Doug Donohoe](https://www.donohoedigital.com/) and Donohoe Digital LLC from 2003-2017.

**Key Features:**
- 🎮 Play against AI and human opponents
- 🏆 Customizable tournament modes
- ⏱️ Poker clock for home tournaments
- 🧮 Sophisticated hand strength calculator
- 💬 In-game chat and lobby system
- 🌐 Online multiplayer with game discovery

The game is a Java Swing-based desktop application that runs on Mac, Linux, and Windows. The backend server uses an embedded H2 database, and the web portal (for game discovery and client downloads) is built on Apache Wicket.

![screenshots.png](images/screenshots.png)

_For more screenshots, visit [ddpoker.com](https://www.ddpoker.com/about/screenshots/)_

## Getting Started

### For Players

**Docker Deployment (Recommended):**

The easiest way to run DD Poker server is using Docker. Pre-built images are available on [Docker Hub](https://hub.docker.com/r/joshuaabeard/ddpoker).

**Quick Start (Single Command):**
```bash
docker run -d \
  --name ddpoker \
  -p 8080:8080 \
  -p 8877:8877 \
  -p 11886:11886/udp \
  -p 11889:11889/udp \
  -v ddpoker_data:/data \
  joshuaabeard/ddpoker:3.3.0-CommunityEdition
```

**Or with Docker Compose:**
```bash
# Clone the repository
git clone https://github.com/JoshuaABeard/DDPoker.git
cd DDPoker/docker

# Start the server
docker compose up -d
```

**What you get:**
- 🌐 **Web Interface**: http://localhost:8080 - Download clients and manage games
- 🎮 **Game Server**: Port 8877 - Connect your desktop client here
- 💬 **Chat Server**: Ports 11886/11889 (UDP) - In-game chat

**Download Desktop Client:**

**Option 1: Windows Installer (Recommended for Windows Users)**
1. Visit http://localhost:8080/downloads/
2. Download `DDPokerCE-3.3.0.msi` (~98MB)
3. Double-click to install (Windows SmartScreen may warn - click "More info" → "Run anyway")
4. Launch from Start Menu → DD Poker Community Edition → DDPokerCE
5. **No Java installation required!** JRE is bundled with the installer.

**Option 2: Universal JAR (Cross-Platform, Advanced Users)**
1. Visit http://localhost:8080/downloads/
2. Download `DDPokerCE-3.3.0.jar` (21MB universal JAR)
3. Requires Java 25 to be installed separately
4. Run with: `java -jar DDPokerCE-3.3.0.jar`
5. Connect to your server at `localhost:8877`

**Admin Panel Access (Optional):**

To enable server administration features (user management, ban controls), configure admin credentials:

```yaml
environment:
  - ADMIN_USERNAME=admin
  - ADMIN_PASSWORD=your-secure-password
```

Then access the admin panel at http://localhost:8080/admin

For complete admin setup and usage guide, see [docs/ADMIN-PANEL.md](docs/ADMIN-PANEL.md).

For complete Docker documentation, configuration options, and troubleshooting, see [docker/DEPLOYMENT.md](docker/DEPLOYMENT.md).

**Unraid Users:**
DD Poker is available as an Unraid Community Application with built-in admin configuration. See [unraid/README.md](unraid/README.md) for installation instructions and the template at [unraid/DDPoker.xml](unraid/DDPoker.xml).

### For Developers

See [README-DEV.md](README-DEV.md) for comprehensive development documentation, including:
- Building from source with Maven
- Running locally for development
- Testing online multiplayer
- Architecture overview
- Email configuration

**Quick Start for Developers:**

```bash
# Prerequisites: Java 25, Maven 3.9+
git clone https://github.com/JoshuaABeard/DDPoker.git
cd DDPoker/code

# Build (Windows PowerShell)
mvn clean package -DskipTests

# Run server
..\tools\scripts\run-server-local.ps1

# Run client (separate terminal)
..\tools\scripts\run-client-local.ps1
```

## History

DD Poker was originally developed by **Donohoe Digital LLC**, a small computer games studio founded by [Doug Donohoe](https://www.donohoedigital.com/) in 2003.

**Timeline:**
- **2003**: Development begins after inspiration at a Las Vegas poker tournament
- **June 2004**: DD Poker 1.0 released in boxes as *DD Tournament Poker No Limit Texas Hold'em*
- **August 2005**: DD Poker 2.0 released featuring online multiplayer and Phil Gordon on the box
- **January 2009**: DD Poker 3.0 released as donation-ware
- **July 2017**: Backend servers shut down, but game continued to be played via manual URL sharing
- **2024**: Original source code [open-sourced by Doug Donohoe](https://github.com/dougdonohoe/ddpoker) under GPL-3.0
- **February 2026**: Community fork created with Docker support and modern tooling

Donohoe Digital's first game, [War! Age of Imperialism](https://www.donohoedigital.com/war/), was a finalist in the 2005 Independent Games Festival.

For detailed release history, see [whatsnew.html](code/poker/src/main/resources/config/poker/help/whatsnew.html).

## Why This Fork Exists

The original DD Poker backend servers were shut down in July 2017, but the community continued to play by manually sharing game URLs. This fork was created to:

1. **Simplify deployment** - Docker makes it easy to run your own server
2. **Modernize the build** - Updated dependencies and streamlined development
3. **Enable self-hosting** - Run servers for local poker communities
4. **Preserve the game** - Ensure DD Poker remains playable for years to come

This fork maintains the spirit of Doug Donohoe's open-sourcing decision: making DD Poker available to anyone who wants to host their own poker server.

## Copyright and Licenses

### Source Code

Unless otherwise noted, the **source code** in this repository is:

**Copyright (c) 2003-2026 Doug Donohoe. All rights reserved.**

This program is free software: you can redistribute it and/or modify
it under the terms of the **GNU General Public License** as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

For the full License text, please see the [LICENSE.txt](LICENSE.txt) file.

### Names, Logos, and Creative Assets

The **"DD Poker" and "Donohoe Digital"** names and logos, as well as any images,
graphics, text, and documentation found in this repository (including but not
limited to written documentation, website content, and marketing materials)
are licensed under the **Creative Commons Attribution-NonCommercial-NoDerivatives
4.0 International License (CC BY-NC-ND 4.0)**.

You may not use these assets without explicit written permission for any uses not covered by this License.
For the full License text, please see the [LICENSE-CREATIVE-COMMONS.txt](LICENSE-CREATIVE-COMMONS.txt) file.

For inquiries regarding commercial licensing of this source code or
the use of names, logos, images, text, or other assets, please contact
doug [at] donohoe [dot] info.

### Community Fork Modifications

Modifications made in this community fork (version 3.3.0-CommunityEdition and later) are:

**Copyright (c) 2026 Joshua Beard and contributors**

These modifications include:
- Docker deployment configuration and scripts
- Build system improvements
- Documentation updates and guides
- Version management and release tooling

These modifications are also licensed under **GPL-3.0** to maintain compatibility with the original license.

## Trademarks

**Donohoe Digital owns the following registered trademarks:**

 * `DD Poker` (registration #7856123) — The mark consists of standard characters without claim to
   any particular font style, size or color.
 * The `DD` spade logo (registration #7856124) — The mark consists of the letters "DD", colored red with a black
   border superimposed over a black playing card spade symbol.

**This community fork is not affiliated with, endorsed by, or sponsored by Donohoe Digital LLC or Doug Donohoe.** Use of the DD Poker name and logos is permitted under fair use for the purpose of identifying the software and its origins.

## Third-Party Code

Third party source code directly copied into this repository includes the following:

* Zookitec Explicit Layout in `code/poker/src/main/java/com/zookitec/layout/*.java`
* `MersenneTwisterFast` random number generator in `code/common/src/main/java/com/donohoedigital/base/MersenneTwisterFast.java`
* `RandomGUID` generator in `code/common/src/main/java/com/donohoedigital/base/RandomGUID.java`
* `Base64` encode/decoder in `code/common/src/main/java/com/donohoedigital/base/Base64.java`

## Contributors

**Original DD Poker Development (2003-2017):**

The following folks made excellent contributions to the DD Poker
code base as employees of Donohoe Digital:

+ **Doug Donohoe** - Creator, lead developer, and founder of Donohoe Digital LLC
+ Greg King
+ Sam Neth
+ Brian Zak

**Community Fork Contributors (2026-):**

+ **Joshua Beard** - Repository maintenance, Docker deployment, build improvements
+ **Claude (Anthropic)** - Documentation, code refactoring, and development assistance

## Links

- **Original Repository**: https://github.com/dougdonohoe/ddpoker
- **Community Fork**: https://github.com/JoshuaABeard/DDPoker
- **Docker Hub**: https://hub.docker.com/r/joshuaabeard/ddpoker
- **Official Website**: https://www.ddpoker.com/
- **Donohoe Digital**: https://www.donohoedigital.com/

## Support

For issues related to this community fork:
- Open an issue: https://github.com/JoshuaABeard/DDPoker/issues
- Discussions: https://github.com/JoshuaABeard/DDPoker/discussions

For questions about the original DD Poker or licensing:
- Contact Doug Donohoe: doug [at] donohoe [dot] info

---

**Note:** This is a community-maintained fork. For the original DD Poker source code, visit https://github.com/dougdonohoe/ddpoker
