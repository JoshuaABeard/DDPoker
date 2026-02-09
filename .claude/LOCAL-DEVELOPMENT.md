# Local Development Setup

This guide explains how to run the DD Poker server and client locally on Windows for development and testing.

## Why Run Locally?

Running the server and client natively on Windows (instead of in Docker) provides:
- ✅ **UDP chat works perfectly** - no Docker networking issues
- ✅ **Faster development cycle** - no container rebuilds needed
- ✅ **Easy debugging** - attach debugger to Java processes
- ✅ **Live code changes** - rebuild with Maven and restart

## Prerequisites

- Java 25+ installed
- Maven 3.9+ installed
- Project built: `cd code && mvn clean install -DskipTests`

## Quick Start

### 1. Start the Server

```powershell
powershell -ExecutionPolicy Bypass -File tools\scripts\run-server-local.ps1
```

**What it does:**
- Starts pokerserver with embedded H2 database
- Listens on all network interfaces (not just localhost)
- Configures Gmail SMTP for user activation emails
- Database stored in: `runtime/poker.mv.db`

**Server ports:**
- **8877** (TCP) - Game server API
- **11886** (UDP) - Chat server
- **11889** (UDP) - Connection test

### 2. Start the Client

```powershell
powershell -ExecutionPolicy Bypass -File tools\scripts\run-client-local.ps1
```

Or just launch the client normally from your IDE/installer.

### 3. Configure Client Settings

**IMPORTANT:** Do NOT use `localhost` for UDP chat - it won't work!

In the client, go to **Options → Online**:
- **Online Server**: `192.168.1.x:8877` (use your actual LAN IP)
- **Online Chat**: `192.168.1.x:11886` (use your actual LAN IP)

**Why not localhost?**
- The UDP stack binds to all network interfaces
- It prefers non-loopback addresses for sending
- Using your actual IP ensures client and server use the same interface

**How to find your IP:**
```powershell
ipconfig | findstr "IPv4"
```
Look for an address like `192.168.x.x` or `10.x.x.x`

## Troubleshooting

### "Database may be already in use" Error

The H2 database is locked by another process.

**Solution:**
```powershell
# Stop all Java processes
powershell -Command "Get-Process java | Stop-Process -Force"
```

Then restart the server.

### Chat Timeout

If chat connection times out:

1. **Check client configuration** - Make sure you're using your actual IP (not localhost)
2. **Verify server is running** - The server PowerShell window should be open
3. **Check IP address** - Make sure the IP matches what the server is binding to

Look at server startup logs for binding addresses:
```
INFO  Binding: 192.168.1.x:11886
INFO  Binding: 192.168.1.x:11889
```

Use that IP in your client settings.

### Email Not Sending

The server is configured with Gmail SMTP. If activation emails aren't sending:

1. **Check Gmail credentials** in `tools\scripts\run-server-local.ps1`
2. **Verify Gmail App Password** is still valid
3. **Check server logs** for email errors in server console

## How It Works

### Server Script (`tools\scripts\run-server-local.ps1`)

- Builds classpath from all compiled modules
- Excludes duplicate JARs (uses classes, not JARs for our modules)
- Configures H2 database with MySQL compatibility
- Sets up Gmail SMTP for activation emails
- Runs `com.donohoedigital.games.poker.server.PokerServerMain`

### Client Script (`tools\scripts\run-client-local.ps1`)

- Builds classpath including poker (client) module
- Runs `com.donohoedigital.games.poker.PokerMain`
- Uses same runtime directory as server

### Shared Database

Both server and client use: `C:\Repos\DDPoker\runtime\poker.mv.db`

To reset the database:
```powershell
# Stop server first!
Remove-Item runtime\poker.mv.db
```

## Development Workflow

1. **Make code changes** in your IDE
2. **Rebuild** the affected module:
   ```bash
   cd code
   mvn install -DskipTests -pl <module-name> -am
   ```
3. **Restart server** - Press Ctrl+C in server window, run script again
4. **Restart client** - Close client, run script again

## Email Configuration

The server can be configured with SMTP credentials in `tools\scripts\run-server-local.ps1`:

- **SMTP Host**: smtp.gmail.com (or your email provider)
- **Account**: your-email@gmail.com
- **App Password**: Configure in script
- **From Address**: your-email@gmail.com

See [EMAIL-CONFIGURATION.md](EMAIL-CONFIGURATION.md) for detailed SMTP setup instructions.

## Notes

- **Configure your own SMTP credentials** in tools\scripts\run-server-local.ps1 for email functionality
- **Use actual IP addresses** for client configuration, not localhost for UDP chat
- **Stop server cleanly** with Ctrl+C to avoid database locks
- **One server at a time** - only run one instance to avoid port conflicts
