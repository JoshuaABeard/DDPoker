# Quick Gmail Setup for DD Poker

## 5-Minute Gmail Configuration

### Step 1: Generate Gmail App Password (2 minutes)

1. **Enable 2-Factor Authentication:**
   - Visit: https://myaccount.google.com/security
   - Click **2-Step Verification**
   - Follow the prompts to enable it

2. **Create App Password:**
   - Visit: https://myaccount.google.com/apppasswords
   - Select **App:** Mail
   - Select **Device:** Other (Custom name)
   - Enter: `DD Poker Server`
   - Click **Generate**
   - **Copy the 16-character password** (it looks like: `abcd efgh ijkl mnop`)

### Step 2: Configure Docker (2 minutes)

1. **Edit docker/docker-compose.yml:**
   ```bash
   nano docker/docker-compose.yml
   ```

2. **Find the email section and uncomment these lines:**
   ```yaml
   environment:
     - SMTP_HOST=smtp.gmail.com
     - SMTP_PORT=587
     - SMTP_USER=your-email@gmail.com
     - SMTP_PASSWORD=abcdefghijklmnop
     - SMTP_AUTH=true
     - SMTP_STARTTLS_ENABLE=true
     - SMTP_FROM=your-email@gmail.com
   ```

3. **Replace:**
   - `your-email@gmail.com` → Your Gmail address
   - `abcdefghijklmnop` → The 16-character app password (remove spaces)

### Step 3: Restart Container (1 minute)

```bash
docker compose restart
```

Wait for the server to restart (~10 seconds).

### Step 4: Test (1 minute)

1. **Launch DD Poker client**
2. **Configure server** (if not already done):
   - Options → Online → Public Online Servers
   - Enable checkbox
   - Online Server: `your-server.com:8877`
   - Chat Server: `your-server.com:11886`
   - Click **Test**

3. **Create a test profile:**
   - Online → Create Profile
   - Profile Name: `TestUser`
   - Email: Your email address
   - Click **Create**

4. **Check your email!** 📧
   - Look for: "DD Poker Online Profile Password for TestUser"
   - Copy the password

5. **Activate profile:**
   - Online → Activate Profile
   - Enter the password from the email
   - Success! ✅

---

## Troubleshooting

### Not receiving emails?

**Check Docker logs:**
```bash
docker logs ddpoker-ddpoker-1 --tail 50
```

Look for errors like:
- `Authentication failed` → Wrong app password
- `Connection refused` → Wrong host/port

### "Authentication failed" error?

- Make sure you're using the **App Password**, not your Gmail password
- Remove all spaces from the app password
- Verify 2FA is enabled

### Still not working?

See the complete guide: [EMAIL-CONFIGURATION.md](guidelines/EMAIL-CONFIGURATION.md)

---

## Complete docker/docker-compose.yml Example

```yaml
services:
  ddpoker:
    build:
      context: .
      dockerfile: docker/Dockerfile
    ports:
      - "8877:8877"
      - "8080:8080"
      - "11886:11886/udp"
      - "11889:11889/udp"
    volumes:
      - ddpoker_data:/data
      - installer_cache:/app/downloads
    environment:
      - SERVER_HOST=poker.example.com
      - SERVER_PORT=8877
      - CHAT_PORT=11886
      - WEB_PORT=8080

      # Gmail Configuration
      - SMTP_HOST=smtp.gmail.com
      - SMTP_PORT=587
      - SMTP_USER=yourname@gmail.com
      - SMTP_PASSWORD=abcdefghijklmnop
      - SMTP_AUTH=true
      - SMTP_STARTTLS_ENABLE=true
      - SMTP_FROM=yourname@gmail.com
    restart: unless-stopped

volumes:
  ddpoker_data:
  installer_cache:
```

**That's it!** Your DD Poker server can now send emails. 🎉
