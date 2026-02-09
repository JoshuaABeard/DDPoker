# Email Configuration Guide

DD Poker requires email functionality to send activation passwords when users create online profiles.

## Quick Start: Gmail (Recommended)

### 1. Generate App Password

1. Enable 2FA: https://myaccount.google.com/security
2. Create App Password: https://myaccount.google.com/apppasswords
   - App: Mail
   - Device: Other → "DD Poker Server"
   - Copy the 16-character password (remove spaces)

### 2. Configure Docker

Edit `docker/docker-compose.yml` and uncomment:

```yaml
environment:
  - SMTP_HOST=smtp.gmail.com
  - SMTP_PORT=587
  - SMTP_USER=your-email@gmail.com
  - SMTP_PASSWORD=abcdefghijklmnop  # 16-char app password
  - SMTP_AUTH=true
  - SMTP_STARTTLS_ENABLE=true
  - SMTP_FROM=your-email@gmail.com
```

### 3. Restart and Test

```bash
docker compose restart

# Test by creating a profile in the client
# Check email for activation password
```

---

## Other Email Providers

### Outlook / Office 365

```yaml
- SMTP_HOST=smtp.office365.com
- SMTP_PORT=587
- SMTP_USER=your-email@outlook.com
- SMTP_PASSWORD=your-password  # Use app password if 2FA enabled
- SMTP_AUTH=true
- SMTP_STARTTLS_ENABLE=true
- SMTP_FROM=your-email@outlook.com
```

### Custom SMTP Server (STARTTLS)

```yaml
- SMTP_HOST=mail.example.com
- SMTP_PORT=587
- SMTP_USER=username
- SMTP_PASSWORD=password
- SMTP_AUTH=true
- SMTP_STARTTLS_ENABLE=true
- SMTP_FROM=noreply@example.com
```

### Custom SMTP Server (SSL/TLS)

```yaml
- SMTP_HOST=mail.example.com
- SMTP_PORT=465
- SMTP_USER=username
- SMTP_PASSWORD=password
- SMTP_AUTH=true
- SMTP_STARTTLS_ENABLE=false
- SMTP_SSL_ENABLE=true
- SMTP_FROM=noreply@example.com
```

---

## Configuration Reference

| Environment Variable | Default | Description |
|---------------------|---------|-------------|
| `SMTP_HOST` | `127.0.0.1` | SMTP server hostname |
| `SMTP_PORT` | `587` | SMTP server port |
| `SMTP_USER` | _(empty)_ | SMTP username |
| `SMTP_PASSWORD` | _(empty)_ | SMTP password or app password |
| `SMTP_AUTH` | `false` | Enable authentication |
| `SMTP_STARTTLS_ENABLE` | `true` | Enable STARTTLS encryption |
| `SMTP_FROM` | `noreply@ddpoker.local` | From address |

---

## Troubleshooting

### Check Email System Status

```bash
docker logs ddpoker-ddpoker-1 | grep -i "mail\|smtp"
```

Look for:
- `Initializing DD Postal Service` - Email system started
- `DD Postal Service props:` - Shows SMTP configuration

### Common Errors

**"Authentication failed"**
- Gmail: Using regular password instead of app password
- Gmail: 2FA not enabled
- Wrong username or password

**"Connection refused"**
- Wrong SMTP host or port
- Firewall blocking outbound connections

**"Connection timed out"**
- Network connectivity issues
- Firewall blocking port 587/465

### Test Email Delivery

1. Launch DD Poker client
2. Online → Create Profile
3. Enter test email address
4. Check email for activation password

---

## Production Deployment

**Gmail** (20-50 users):
- ✅ Free and reliable
- ⚠️ 500 emails/day limit

**SendGrid/Mailgun** (50+ users):
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

## Technical Details

- **Library:** Jakarta Mail API 2.1.5
- **Implementation:** Eclipse Angus Mail 2.0.5
- **Architecture:** Asynchronous queue-based delivery
- **Queue Processing:** Every 10 seconds
- **Retry:** Failed emails remain in queue for retry

---

## Alternative Configuration Method

Instead of environment variables, you can edit `code/pokerserver/src/main/resources/config/poker/server.properties`:

```properties
settings.smtp.host=smtp.gmail.com
settings.smtp.port=587
settings.smtp.auth=true
settings.smtp.user=your-email@gmail.com
settings.smtp.pass=your-app-password
settings.smtp.starttls.enable=true
settings.server.profilefrom=your-email@gmail.com
```

Then rebuild: `mvn clean install -DskipTests && docker compose up -d --build`

---

## Resources

- **Gmail App Passwords:** https://myaccount.google.com/apppasswords
- **Jakarta Mail Docs:** https://eclipse-ee4j.github.io/mail/
- **DD Poker Docker Guide:** [DDPOKER-DOCKER.md](DDPOKER-DOCKER.md)
