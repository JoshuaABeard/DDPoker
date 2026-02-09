# Email Configuration Guide

## Overview

DD Poker requires email functionality to send activation passwords when users create online profiles. The system supports modern SMTP servers including Gmail, Outlook, and custom mail servers.

## Requirements

Email is **required** for:
- Creating online profiles (activation password sent via email)
- Password recovery
- Profile email address changes

Without email configured, users cannot create or activate online profiles.

## Supported SMTP Servers

- ✅ Gmail / Google Workspace
- ✅ Outlook / Office 365
- ✅ Custom SMTP servers (Postfix, Sendmail, etc.)
- ✅ STARTTLS (port 587) - **Recommended**
- ✅ SSL/TLS (port 465)
- ✅ Plain SMTP (port 25) - Not recommended

---

## Configuration Methods

### Method 1: Docker Environment Variables (Recommended)

Edit `docker/docker-compose.yml` and uncomment the email section:

```yaml
environment:
  # Gmail Example
  - SMTP_HOST=smtp.gmail.com
  - SMTP_PORT=587
  - SMTP_USER=your-email@gmail.com
  - SMTP_PASSWORD=your-16-char-app-password
  - SMTP_AUTH=true
  - SMTP_STARTTLS_ENABLE=true
  - SMTP_FROM=your-email@gmail.com
```

Then restart the container:
```bash
docker compose down
docker compose up -d
```

### Method 2: Server Properties File

Edit `code/pokerserver/src/main/resources/config/poker/server.properties`:

```properties
settings.smtp.host=smtp.gmail.com
settings.smtp.port=587
settings.smtp.auth=true
settings.smtp.user=your-email@gmail.com
settings.smtp.pass=your-app-password
settings.smtp.starttls.enable=true
```

Then rebuild and redeploy:
```bash
mvn clean install -DskipTests
docker compose up -d --build
```

---

## Gmail Configuration (Recommended)

Gmail is the easiest option for most users.

### Step 1: Enable 2-Factor Authentication

1. Go to https://myaccount.google.com/security
2. Enable **2-Step Verification** if not already enabled

### Step 2: Generate App Password

1. Go to https://myaccount.google.com/apppasswords
2. Select **App**: Mail
3. Select **Device**: Other (Custom name) → "DD Poker Server"
4. Click **Generate**
5. Copy the 16-character password (e.g., `abcd efgh ijkl mnop`)

### Step 3: Configure Docker

Edit `docker/docker-compose.yml`:

```yaml
environment:
  - SMTP_HOST=smtp.gmail.com
  - SMTP_PORT=587
  - SMTP_USER=your-email@gmail.com
  - SMTP_PASSWORD=abcdefghijklmnop  # 16-char app password (no spaces)
  - SMTP_AUTH=true
  - SMTP_STARTTLS_ENABLE=true
  - SMTP_FROM=your-email@gmail.com
```

### Step 4: Test

1. Restart container: `docker compose restart`
2. Launch DD Poker client
3. Go to **Online** → **Create Profile**
4. Enter a test email address
5. Check for activation email

### Gmail Troubleshooting

**Error: "Username and Password not accepted"**
- Make sure you're using the **App Password**, not your regular Gmail password
- Remove all spaces from the app password
- Check that 2FA is enabled

**Error: "Unable to send email"**
- Verify SMTP settings are correct
- Check Docker logs: `docker logs ddpoker-ddpoker-1`
- Ensure firewall allows outbound connections on port 587

---

## Outlook / Office 365 Configuration

### Configuration

```yaml
environment:
  - SMTP_HOST=smtp.office365.com
  - SMTP_PORT=587
  - SMTP_USER=your-email@outlook.com
  - SMTP_PASSWORD=your-password
  - SMTP_AUTH=true
  - SMTP_STARTTLS_ENABLE=true
  - SMTP_FROM=your-email@outlook.com
```

**Notes:**
- Use your regular Outlook password (unless you have 2FA enabled)
- If you have 2FA, generate an app password similar to Gmail
- Office 365 business accounts work the same way

---

## Custom SMTP Server Configuration

### For STARTTLS (Port 587) - Recommended

```yaml
environment:
  - SMTP_HOST=mail.example.com
  - SMTP_PORT=587
  - SMTP_USER=username
  - SMTP_PASSWORD=password
  - SMTP_AUTH=true
  - SMTP_STARTTLS_ENABLE=true
  - SMTP_FROM=noreply@example.com
```

### For SSL/TLS (Port 465)

```yaml
environment:
  - SMTP_HOST=mail.example.com
  - SMTP_PORT=465
  - SMTP_USER=username
  - SMTP_PASSWORD=password
  - SMTP_AUTH=true
  - SMTP_STARTTLS_ENABLE=false
  - SMTP_SSL_ENABLE=true
  - SMTP_FROM=noreply@example.com
```

### For Plain SMTP (Port 25) - Not Recommended

```yaml
environment:
  - SMTP_HOST=localhost
  - SMTP_PORT=25
  - SMTP_AUTH=false
  - SMTP_STARTTLS_ENABLE=false
  - SMTP_FROM=noreply@localhost
```

---

## Configuration Properties Reference

| Environment Variable | Property File | Default | Description |
|---------------------|---------------|---------|-------------|
| `SMTP_HOST` | `settings.smtp.host` | `127.0.0.1` | SMTP server hostname |
| `SMTP_PORT` | `settings.smtp.port` | `587` | SMTP server port |
| `SMTP_USER` | `settings.smtp.user` | _(empty)_ | SMTP username (usually email) |
| `SMTP_PASSWORD` | `settings.smtp.pass` | _(empty)_ | SMTP password or app password |
| `SMTP_AUTH` | `settings.smtp.auth` | `false` | Enable SMTP authentication |
| `SMTP_STARTTLS_ENABLE` | `settings.smtp.starttls.enable` | `true` | Enable STARTTLS encryption |
| `SMTP_FROM` | `settings.server.profilefrom` | `noreply@ddpoker.local` | From address for emails |

### Advanced Properties

These can be added to `server.properties` but don't have environment variables:

| Property | Default | Description |
|----------|---------|-------------|
| `settings.smtp.starttls.required` | `false` | Require STARTTLS (fail if not available) |
| `settings.smtp.ssl.enable` | `false` | Use SSL/TLS (for port 465) |
| `settings.smtp.ssl.protocols` | `TLSv1.2 TLSv1.3` | Allowed TLS protocol versions |
| `settings.smtp.timeout.millis` | `30000` | Socket timeout (30 seconds) |
| `settings.smtp.connectiontimeout.millis` | `30000` | Connection timeout (30 seconds) |
| `settings.mailqueue.init` | `true` | Enable email system |
| `settings.mailqueue.wait` | `10` | Queue processing interval (seconds) |

---

## Testing Email Configuration

### Method 1: Check Docker Logs

```bash
docker logs ddpoker-ddpoker-1 --tail 100 -f
```

Look for:
- `Initializing DD Postal Service` - Email system starting
- `DD Postal Service props:` - Shows SMTP configuration
- `Mail queue: N messages to process` - Email queue activity

### Method 2: Create Test Profile

1. Launch DD Poker client
2. Configure server address (Options → Online)
3. Go to **Online** → **Create Profile**
4. Enter:
   - Profile name: `TestUser`
   - Email: Your test email address
5. Check email for activation password

### Method 3: View Queue Status

If emails aren't sending, check the logs for errors:

```bash
docker logs ddpoker-ddpoker-1 2>&1 | grep -i "mail\|smtp\|email"
```

Common errors:
- `Failed sending message: Authentication failed` → Check username/password
- `Connection refused` → Check host/port
- `Connection timed out` → Check firewall/network

---

## Email Templates

DD Poker sends these emails:

### 1. Profile Activation Email

**Subject:** `DD Poker Online Profile Password for {username}`

**Content:**
```
The password for your online profile "{username}" is:

===> {password}

Please keep it in a safe place!

- Donohoe Digital
```

### 2. Password Recovery Email

Same format as activation email.

---

## Security Considerations

### App Passwords (Recommended)

- ✅ Use **App Passwords** instead of regular passwords
- ✅ Revoke app passwords if compromised
- ✅ One app password per application

### Regular Passwords

- ⚠️ Only use if app passwords not available
- ⚠️ Ensure strong password
- ⚠️ Consider security implications

### Environment Variables

- ✅ Keep `.env` files out of version control
- ✅ Use secrets management in production
- ✅ Rotate passwords regularly

### STARTTLS vs SSL

- ✅ **STARTTLS (port 587)** - Recommended for most servers
- ✅ **SSL/TLS (port 465)** - Alternative if STARTTLS unavailable
- ❌ **Plain SMTP (port 25)** - Only for localhost/trusted networks

---

## Troubleshooting

### Emails Not Sending

**Check 1: Is email system enabled?**
```bash
docker logs ddpoker-ddpoker-1 | grep "Initializing DD Postal Service"
```
If you see "NOT initializing DDPostalServiceImpl", email is disabled.

**Check 2: Are SMTP settings correct?**
```bash
docker logs ddpoker-ddpoker-1 | grep "DD Postal Service props"
```
Verify host, port, authentication settings.

**Check 3: Are there errors in the queue?**
```bash
docker logs ddpoker-ddpoker-1 | grep -i "failed sending"
```

### Common Error Messages

**"Authentication failed"**
- Wrong username or password
- For Gmail: Using regular password instead of app password
- For Gmail: 2FA not enabled

**"Connection refused"**
- Wrong SMTP host or port
- SMTP server not accessible
- Firewall blocking outbound connections

**"Connection timed out"**
- Network connectivity issues
- Firewall blocking port 587/465
- SMTP server not responding

**"STARTTLS is required"**
- Server requires STARTTLS but it's disabled
- Set `SMTP_STARTTLS_ENABLE=true`

### Enable Debug Logging

Edit `code/mail/src/main/java/com/donohoedigital/mail/DDPostalServiceImpl.java`:

```java
private static final boolean DEBUG = true;  // Change from false
```

Rebuild and view detailed JavaMail logs.

---

## Production Deployment

### Gmail for Small Deployments

For personal servers (20-50 users):
- ✅ Use Gmail with app password
- ✅ Free and reliable
- ⚠️ Daily sending limit: 500 emails/day

### SendGrid/Mailgun for Larger Deployments

For production servers (50+ users):
- Use dedicated email service
- Better deliverability
- Higher sending limits
- SMTP credentials provided by service

**SendGrid Example:**
```yaml
- SMTP_HOST=smtp.sendgrid.net
- SMTP_PORT=587
- SMTP_USER=apikey
- SMTP_PASSWORD=your-sendgrid-api-key
- SMTP_AUTH=true
- SMTP_STARTTLS_ENABLE=true
- SMTP_FROM=noreply@yourdomain.com
```

---

## Disabling Email (Not Recommended)

To disable email functionality entirely:

Edit `server.properties`:
```properties
settings.mailqueue.init=false
```

**Consequences:**
- Users cannot create online profiles
- Password recovery unavailable
- Email address changes disabled

Only disable for testing/development.

---

## Technical Details

### Implementation

- **Library:** Jakarta Mail API 2.1.5 (formerly JavaMail)
- **Implementation:** Eclipse Angus Mail 2.0.5
- **Architecture:** Asynchronous queue-based delivery
- **Queue Processing:** Every 10 seconds (configurable)
- **Retry Logic:** Failed emails remain in queue and retry

### Queue Behavior

1. Emails added to in-memory queue
2. Background thread processes queue every 10 seconds
3. SMTP connection opened per batch
4. Failed emails remain in queue for retry
5. On shutdown, attempts to send all queued emails

### Thread Safety

- Queue operations are synchronized
- Single background thread handles all email sending
- Safe for concurrent use from multiple servlet threads

---

## Additional Resources

- **Gmail App Passwords:** https://myaccount.google.com/apppasswords
- **Jakarta Mail Docs:** https://eclipse-ee4j.github.io/mail/
- **DD Poker Setup:** [DDPOKER-DOCKER.md](DDPOKER-DOCKER.md)

---

## Quick Start (Gmail)

```bash
# 1. Generate Gmail App Password
# Visit: https://myaccount.google.com/apppasswords

# 2. Edit docker/docker-compose.yml
vim docker/docker-compose.yml

# 3. Add these lines under 'environment:'
  - SMTP_HOST=smtp.gmail.com
  - SMTP_PORT=587
  - SMTP_USER=your-email@gmail.com
  - SMTP_PASSWORD=your-16-char-app-password
  - SMTP_AUTH=true
  - SMTP_STARTTLS_ENABLE=true
  - SMTP_FROM=your-email@gmail.com

# 4. Restart container
docker compose restart

# 5. Test by creating a profile in DD Poker client
# Check your email for the activation password!
```
