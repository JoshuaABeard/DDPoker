# DD Poker Developer Notes

## Introduction

Welcome to the DD Poker source code. This page tells you (hopefully)
everything you need to know to run the three main programs that
make up DD Poker:

* **DD Poker Game** — the poker game itself, a Java Swing desktop application
* **Poker Server** — the backend API server that the game talks to for online games
* **Poker Web** — the old Apache Wicket-based DD Poker website, including the "Online Game Portal", which
  shows various information about online games like current games, history, games by player, etc.

## Mac vs. Linux vs. Windows (a note from Doug)

These instructions are admittedly Mac-centric, largely because that is what I have used
for the last 15 years.  I've done cursory testing on Linux (see Appendix D for testing tips
on Ubuntu from Docker/Mac).

Even though DD Poker was originally developed
mainly on Windows (with Cygwin), I haven't used Windows for development in a
very long time, so apologies to Windows developers for the lack of instructions.
Cygwin worked back in the day, and I imagine that the Windows Subsystem for Linux (WSL)
should be helpful here, too.

Feel free to submit a PR with any changes to these docs that would help Linux or Windows users.

## Prerequisites

Required software:

* Java 25 - [See Adoptium](https://adoptium.net/temurin/releases/?os=any&package=jdk&version=25)
* Maven 3 - [See Apache Maven](https://maven.apache.org/install.html)
* Docker (optional, but useful to run some things) - [See Docker](https://docs.docker.com/engine/install/)

Both `java` and `mvn` must be on your `PATH`.

We provide the `ddpoker.rc` file, which sets some environment variables required by the scripts in
`tools/bin` and `tools/db`, adds these script directories to the `PATH`, creates some useful
`mvn` aliases (used below) and performs some sanity checks.

**NOTE**: all commands below assume you have sourced `ddpoker.rc`, have `mvn` and `java` installed and are
in the root of the `ddpoker` repository.

```shell
source ddpoker.rc
```

## Mac Installs

[Brew](https://brew.sh/) is useful to install Java and Maven:

```shell
brew install temurin@25 maven
```

## Compile Code

To compile the code and create the `.jar` and `.war` files,
use maven.  This version skips the tests, which you can
run separately (see below).

```shell
mvn-package-notests
```

After you have run this, any of the scripts discussed below should just work.

## Poker Game

To run the desktop poker game, either run `PokerMain` in IntelliJ or use the script:

```shell
poker
```

If you want to run the game using your personal servers, you'll need to set go
to _Options → Online → Public Online Servers_ and check the **Enabled** checkbox and
enter the server information in the two fields.  See below for details on running the
servers.

If you start `poker` with `enabled=true`, but your servers are not running, you may see a
several-second delay on startup as a connection attempt is made and freezes the UI until it times out.

## Docker Deployment (Recommended for Hosting)

For hosting a DD Poker server for friends or a small community, Docker deployment is the easiest approach.
A single Docker container runs both the poker server and web interface with an embedded H2 database.

**Quick Start:**

```shell
# Build the project
mvn clean install -DskipTests

# Copy dependencies for Docker
cd code
mvn dependency:copy-dependencies -pl poker,pokerserver,pokerwicket -DoutputDirectory=target/dependency -DincludeScope=runtime
cd ..

# Build and start the container
docker compose up -d --build

# Check logs
docker logs ddpoker-ddpoker-1 --tail 50 -f
```

The server will be available at:
- **Game Server**: `localhost:8877` (configure in client: Options → Online)
- **Website**: `http://localhost:8080/online`
- **Downloads**: `http://localhost:8080/downloads/DDPoker.jar`

**Key Features:**
- Single container with poker server + web interface
- Embedded H2 database (zero configuration)
- Gmail/SMTP email support for profile activation
- Persistent data in Docker volumes

**See comprehensive documentation:**
- [`.claude/DDPOKER-DOCKER.md`](.claude/DDPOKER-DOCKER.md) - Complete Docker deployment guide
- [`.claude/EMAIL-CONFIGURATION.md`](.claude/EMAIL-CONFIGURATION.md) - Email/SMTP setup for profile creation
- [`.claude/CLIENT-CONFIGURATION.md`](.claude/CLIENT-CONFIGURATION.md) - Connecting clients to your server

## Development

IntelliJ can be used to run the programs described below.  If you open up the
root of this project in IntelliJ, it should auto-detect
the `code/pom.xml` file and prompt you to load it:

<img src="images/intellij-maven.png" alt="IntelliJ Maven" width="400px">

**NOTE**: You will probably need to edit the Project Structure to tell IntelliJ to use Java 25.
Go to _File → Project Structure... → Project Settings → Project → SDK_ and
set to Java 25 (you may need to add it (_+ Add SDK_) as a new SDK if not already there).

## Server Dependencies

To run server code locally (not in Docker):

**Database:**
- **H2 Database** (Automatic) - Embedded database, no setup required
- Data stored in `./data/poker.mv.db`
- Creates schema automatically on first run

**Email (Optional):**
For email functionality (profile creation/activation), you'll need SMTP access:
- Use Gmail with App Password (recommended, see `.claude/EMAIL-CONFIGURATION.md`)
- Use local SMTP server
- Disable email (profiles won't work, but local games will)

**Historical Setup Guides:**
* `Appendix B` Setup Email SMTP server on Mac (if not using Gmail)

## Run Tests

To build code and run unit tests, use `mvn-test`.  Some
tests assume the `pokertest` database exists.

```shell
mvn-test
```

## Poker Server

To run the DD Poker server and chat server, which is what the game talks to,
you can run `PokerServerMain` in IntelliJ or use the `pokerserver` script:

```shell
pokerserver
```

## Poker Website

To run the DD Poker website, a wicket-based app, you have two options:

The first option is to run the `PokerJetty` testing app, which
runs the webapp using Jetty.  This setup allows one to
auto-detect changes to Wicket `.html` files, so you don't
have to restart after each edit.  You can run `PokerJetty`
directly in IntelliJ, or via the `pokerweb` script:

```shell
pokerweb
```

Once started, you can visit [http://localhost:8080/online](http://localhost:8080/online).

For a production deployment, see the Docker Deployment section above.

## Code Notes

This section is meant to help developers understand the code base, and it contains random
bits of knowledge and advice.

### Warning — This Code is Old!

This code base was originally written over 20 years ago, beginning in 2002.  The majority
of DD Poker was written from 2004 to 2007, with sporadic updates after that.  The original
JDK was 1.5 (aka Java 5).

Amazingly, nearly all of our dependencies (Swing, Hibernate, Wicket, Jetty, Tomcat, log4j, etc.) have been updated 
to the latest versions that work with Java 25.  The only exception is `HSQLDB`, which we currently
have at 1.8.0.10. The latest is 2.7.4, but this requires updating existing databases, which
we don't want to deal with at this time.

### Modules

Here is a brief overview of the modules in this repo, in the order maven builds them, which
means the later modules are dependent on one or more of the earlier modules.

* `common` - core functionality including configuration, logging, xml, properties, various utils
* `mail` - email sending tools
* `gui` - GUI infrastructure extending Java Swing
* `installer` - custom installer logic (e.g., cleanup)
* `db` - database infrastructure extending Hibernate
* `wicket` - web infrastructure extending Apache Wicket (currently on [Wicket 10](https://nightlies.apache.org/wicket/guide/10.x/single.html))
* `jsp` - tools using `.jsp` pages to generate emails and files
* `server` - core server functionality
* `udp` - core UDP networking functionality
* `gamecommon` - core game utilities shared across client and server
* `gameengine` - core game engine
* `ddpoker` - a few classes put into `com.ddpoker` package instead of `com.donohoedigital` (for reasons lost to history)
* `pokerengine` - core poker utilities shared across client and server
* `pokernetwork` - core poker networking infrastructure shared across client and server
* `poker` - DD Poker UI (aka client)
* `tools` - misc tools used for running a games business
* `gameserver` - core game server
* `pokerserver`- DD Poker backend server
* `gametools` - tools to help build games (e.g., Border and Territory mangers)
* `pokerwicket` - DD Poker website and Online Portal
* `proto` - prototype code used for experiments and proof of concept code

### Unit Tests (a note from Doug)

There is some test coverage, but it is sorely lacking in the core poker logic.  This actually
bit me once when I had to solve a multiple-split pots bug.  I didn't get religion
on good test coverage until I worked at a high-frequency trading company writing code
to trade on the US stock markets with my boss's money.

I apologize for the lack of tests.

### Properties Files

Properties files are used for two primary purposes

* `log4j2.*.properties` - `LoggingConfig` - configure logging
* `*.properties` - `PropertyConfig` - configure application behavior, various settings, localizable text

One key tenet we adhered to at Donohoe Digital was to avoid making "temporary" changes
to `.properties` files for personal use (e.g., development, debugging or testing).
Instead, settings could be overridden using user-specific files.  These could be
checked into the tree and not impact production code.  This is why you see properties
files with `donohoe` in the name.

Here's roughly how the two versions work:

#### LoggingConfig (log4j)

Based on "application type", our config looks for:

* Client - `log4j2.client.properties`
* Webapp - `log4j2.webapp.properties`
* Server - `log4j2.server.properties`
* Command Line + Unit Tests - `log4j2.cmdline.properties`

It looks for and loads these files on the classpath in this order:

* `config/common/log4j2.[apptype].properties` - default settings for `apptype`
* `config/[appname]/log4j2.[apptype].properties` - override default settings for application named `appname`
* `config/override/[username].log4j2.properties` - overrides all types for `username`
* `config/override/[username].log4j2.[apptype].properties` - overrides for just `apptype` for `username`

The latter files override any settings in the earlier files.  In log4j, this is commonly used
to turn on logging to the console or to change the logging level for a particular library.

#### PropertyConfig

Similar to logging config, each `apptype` has its own properties file, which are loaded in this order:

* `config/[appname]/common.properties` - properties for application named `appname`, shared across all types
* `config/[appname]/[apptype].properties.[locale]` - properties for `apptype` for `appname` for given locale
* `config/[appname]/[apptype].properties` - properties for `apptype` for `appname` (if no locale provided)
* `config/[appname]/override/[username].properties` - overrides for `appname` for `username`

The user-specific overrides were commonly used to enable debug/testing settings and to change the IP of the
backend server to something running locally.

There aren't any locale-specific settings, but it was successfully used in the past to localize a game into
another language.

### Debug Settings

There are lots of `settings.debug.*` entries in the code which are used to make
development easier.  Typically, you put these in your `[username].properties` file,
so they only are used by you.

Here are a few interesting ones

```properties
# Enable debug flags
settings.debug.enabled=true

# In game, draw border around areas that Swing is repainting
settings.debug.repaint=true

# Human player makes decisions for AI players in game (useful for
# creating various scenarios, like all players go all-in)
settings.debug.dougcontrolsai=true

# Print info about each pot
settings.debug.pots=true

# On server, when sending online profile email, always send to this address,
# Which is useful for testing registrations with other emails
settings.debug.profile.email.override=true
settings.debug.profile.email.override.to=my-email@my-domain.com
```

There are many other examples, just take a look in the code for `settings.debug` to
find the constants and then find usages of those constants.

### Installers

An alternative to using the installers found in [Releases](https://github.com/dougdonohoe/ddpoker/releases)
is to distribute an all-in-one `.jar` file by doing this:

```shell
mvn-install-notests
cd code/poker
mvn package assembly:single -DskipTests=true
```

This creates a `poker-3.0-jar-with-dependencies.jar` in the `target` directory.  You can then
distribute this `.jar` file and run it like so:

```shell
java -jar poker-3.0-jar-with-dependencies.jar
```

For Mac users, you can set a custom dock icon if available:

```shell
java -Xdock:icon=ddpokericon.icns -jar poker-3.0-jar-with-dependencies.jar
```

### Questionable Features

When a player registers a profile for online play, the server sends an email with a password
as a way to confirm the email is correct.  While the player can change this password after the
fact, it isn't forced.  Worse, we store the password in the database (encrypted), but can
decrypt it programmatically, which we use for the "I forgot my password" functionality (we
email the user their current password!).  I have no idea why we went down this path, but
it was just a game after all, and this probably reduced our support costs.

Yes, this is embarrassing in retrospect.

### Computer AI

While not "AI" by today's standards, there is a white paper in `docs/AI_Whitepaper.rtf` that
explains the design of DD Poker's computer opponents.

### Database Host

Back when this code was originally written and deployed, the code ran on the same machine
as the database, so using the MySQL host of `localhost` or `127.0.0.1` was sufficient.  To allow
use from within Docker, we needed more flexibility here, so I added use of the `DB_HOST` environment
variable.

### Game Engine Tools

This codebase includes an underlying game engine, which was originally used to
build a computer version of the board game, War! Age of Imperialism.  One of the needs there was
to draw all the "territories" on a world map as well as identify where to place things like
playing pieces and labels.  DD Poker uses this same game engine, where territories are the
playing seats and pieces are things like the cards and chips.  There are two tools used
to trace the borders and mark the territory locations:

```shell
territorymgr -module poker
bordermgr -module poker
```

These edited the corresponding `gameboard.xml` and `border*.xml`  files, but remembering
the keyboard shortcuts and how to save requires looking at the
code (`GameboardTerritoryManager`, `GameboardBorderManager` and base `GameManager`).

### Preferences

Preferences set in the game are saved using Java Preferences API, which on a Mac can be found
in `com.donohoedigital.poker3.plist`.  To view the contents of this file:

```shell
cd ~/Library/Preferences
plutil -convert xml1 com.donohoedigital.poker3.plist -o -
```

Default values for items in Options dialog are set in
`code/poker/src/main/resources/config/poker/client.properties`, and actual values
set by the user are stored in the `.plist` file.

If you want to clear all preferences, on a Mac, you need to delete the `.plist` file
**AND** restart the `cfprefsd` service, which can keep preferences values in
memory.

```shell
cd ~/Library/Preferences
rm -f com.donohoedigital.poker3.plist
killall -u $USER cfprefsd
```

### Classpath and Dependency Tree

We override the `mvn dependency:tree` to create `target/classpath.txt` in each module, which
is used by the `runjava` script to determine the jar files needed to run a program.

To get the default tree output, to diagnose dependency issues, run this in `code` or in a particular
module, like `code/wicket`.

```shell
# Need to "install" to get proper trees when doing it in sub-tree (for reasons I'm not clear on)
mvn-install-no-tests

# cd to a module
cd code/pokerwicket

# output to console, with other maven INFO
mvn dependency:tree -Ddependency.classpath.outputFile=

# just the tree
mvn dependency:tree -q -Dscope=runtime -Ddependency.classpath.outputFile=/tmp/t && cat /tmp/t && rm -f /tmp/t

# ddpoker.rc has alias for this previous one
mvn-tree
```

## Appendix A - Email

DD Poker's backend server and website are configured to send emails during
the Online Profile setup process (a password is emailed to the user).  It is
also used in response to registering the game and "I forgot my password"
functionality.

The "from" email addresses are set in `poker/server.properties`.  If you run the server,
you ought to use a different email than these.

```shell
settings.server.profilefrom= no-reply@ddpoker.com
settings.server.regfrom=     no-reply@ddpoker.com
```

To enable the `postfix` SMTP mail server on a Mac:

```shell
# turn on
sudo postfix start

# turn off
sudo postfix stop

# status
sudo postfix status

# test (may go to spam), will generate a response report
echo "Test email body" | sendmail -v your_email@your_domain.com
 
# to view response reports, use cmd line 'mail' tool
mail
1 # to view msg
d # to delete
q # to quit
```

**NOTE**: Emails sent this way typically go to spam because they are coming from a random machine,
so check your spam folder and mark as "not spam".

## Appendix B: Running GitHub Actions Locally

You can run GitHub actions locally using the [`act`](https://nektosact.com/) tool (which requires Docker).

To install `act`:

```shell
brew install act
```

The `act-ddpoker` alias uses a custom Docker image you need to build once:

```shell
docker build -t ddpoker-act-runner -f Dockerfile.act .
```

To run the GitHub testing action locally, just use the alias:

```shell
act-ddpoker
```

## Appendix C: Testing Notes

When testing major changes, here's a checklist of things to manually
verify:

* `mvn-package`
* Start server via `PokerServerMain` and `pokerserver`
* Start website via `PokerJetty` and `pokerweb`
* Build website Docker image and run via Docker
* Start game via `PokerMain` and `poker`
* With the server running
  * verify game can start an online game (adjust online settings using server's IP)
  * verify global *Online Lobby*
* Start game from Ubuntu Docker
* Build `act` docker image and running `act-ddpoker`

### Testing Online Multiplayer Locally

For local development testing of online multiplayer, you can run multiple client instances on the same machine using different scripts:

**Start the first client:**
```powershell
.\tools\scripts\run-client-local.ps1
```

**Start the second client (in a new terminal):**
```powershell
.\tools\scripts\run-client-local-2.ps1
```

The second client script uses a separate user profile directory (`.dd-poker3-client2`) to avoid database conflicts. Each client maintains its own:
- Local HSQLDB database
- Player profiles and settings
- Save games and hand histories
- Preferences and window layouts

**Important Notes:**
- Both clients can connect to the same local server (`localhost:11886`)
- Each client must create/use a different online profile (e.g., "User1" and "User2")
- See the Known Limitations section below for chat behavior on the same machine

### Known Limitation: Multiple Clients on Same Machine

When testing online multiplayer with multiple clients on the same machine, you may encounter
**asymmetric chat behavior** where one client can send messages but not receive them.

**Symptoms:**
- Client A (host) can see their own messages but not Client B's messages
- Client B (joining) can see both their own and Client A's messages
- Game functionality works correctly (players can join and play)

**Root Cause:**
The in-game chat system uses UDP for direct peer-to-peer communication. When both clients
run on the same machine, they compete for the same UDP port (11885):
- The host client successfully binds to the port
- The joining client fails to bind (gets "Address already in use" error)
- This creates one-way communication (joining client → host works, host → joining client fails)

**Workarounds:**
- For development testing, accept this limitation - game functionality works fine
- For real testing, use clients on different machines
- Chat through the lobby (before joining a game) uses the server and works correctly

**Future Fix:**
This can be resolved by implementing dynamic port selection or routing in-game chat
through the TCP server instead of direct UDP communication. See issue tracking for details.

## Appendix D: DD Poker Website

Back in the day, the Wicket-based webapp (aka the Online Portal) was also the 
source of `ddpoker.com`.  This site was replaced with a simple static memorial page in
July 2017. In November 2025, we modernized the website, made it responsive (aka mobile-friendly),
and republished as a means to document DD Poker features and functionality.

The `generate-website` script is used to extract this static version
of the website without interactive features like the Online Portal and Admin.
This is now used as the source of [ddpoker.com](https://www.ddpoker.com/).

The debug setting `settings.debug.docmode=true` must be on before starting `PokerJetty`
or running `pokerweb`.  The `generate-website` script assumes `node` is installed and 
available on the `PATH`.  The script saves all Javascript, CSS, HTML and images to the
current working directory.  To preview the site run:

```bash
python3 -m http.server 8000
```

