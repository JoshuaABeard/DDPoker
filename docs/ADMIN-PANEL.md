# DD Poker Admin Panel Guide

The DD Poker admin panel provides server administrators with tools to manage users, ban problematic accounts, and monitor server activity. This guide covers how to configure admin access and use the admin features.

## Table of Contents

- [Configuration](#configuration)
  - [Docker / Docker Compose](#docker--docker-compose)
  - [Unraid](#unraid)
  - [Manual Configuration](#manual-configuration)
- [Accessing the Admin Panel](#accessing-the-admin-panel)
- [Admin Features](#admin-features)
  - [User Search and Management](#user-search-and-management)
  - [Ban Management](#ban-management)
- [Security Considerations](#security-considerations)
- [Troubleshooting](#troubleshooting)

---

## Configuration

Admin access is configured via environment variables. If no admin credentials are provided, the admin panel will be disabled.

### Docker / Docker Compose

**Method 1: Docker Compose (Recommended)**

Edit your `docker-compose.yml` file to add admin credentials:

```yaml
services:
  ddpoker:
    image: joshuaabeard/ddpoker:3.3.0-community
    ports:
      - "8080:8080"
      - "8877:8877"
      - "11886:11886/udp"
      - "11889:11889/udp"
    volumes:
      - ddpoker_data:/data
    environment:
      # Admin configuration
      - ADMIN_USERNAME=admin
      - ADMIN_PASSWORD=your-secure-password-here

      # Optional: SMTP for email notifications
      # - SMTP_HOST=smtp.gmail.com
      # - SMTP_PORT=587
      # - SMTP_USER=your-email@gmail.com
      # - SMTP_PASSWORD=your-app-password
    restart: unless-stopped

volumes:
  ddpoker_data:
```

Then restart the container:

```bash
docker compose down
docker compose up -d
```

**Method 2: Docker Run Command**

```bash
docker run -d \
  --name ddpoker \
  -p 8080:8080 \
  -p 8877:8877 \
  -p 11886:11886/udp \
  -p 11889:11889/udp \
  -v ddpoker_data:/data \
  -e ADMIN_USERNAME=admin \
  -e ADMIN_PASSWORD=your-secure-password-here \
  joshuaabeard/ddpoker:3.3.0-community
```

**Auto-Generated Password:**

If you only provide `ADMIN_USERNAME` without `ADMIN_PASSWORD`, the server will generate a random 8-character password and log it to the console:

```bash
docker logs ddpoker
```

Look for a line like:
```
Admin password not set, generated random password: Xy7Bk9Mn
```

### Unraid

1. Install DD Poker from the Community Applications store, or add the template manually:
   - Go to **Docker** tab
   - Click **Add Container**
   - Set **Template URL**: `https://raw.githubusercontent.com/JoshuaABeard/DDPoker/main/docker/unraid-template.xml`

2. Configure admin credentials in the container settings:
   - **Admin Username**: `admin` (or your preferred username)
   - **Admin Password**: Your secure password (or leave empty for auto-generated)

3. Click **Apply** to create/update the container

4. Check logs if using auto-generated password:
   - Click the container icon → **Logs**
   - Look for: `Admin password not set, generated random password: ...`

### Manual Configuration

If running the server outside Docker (development/testing), set Java system properties:

```bash
java -Dsettings.admin.user=admin \
     -Dsettings.admin.password=your-password \
     -jar pokerserver.jar
```

Or set environment variables before starting:

```bash
export ADMIN_USERNAME=admin
export ADMIN_PASSWORD=your-password
./start-server.sh
```

---

## Accessing the Admin Panel

Once configured, the admin panel is accessible at:

```
http://your-server-ip:8080/admin
```

**Login Process:**

1. Navigate to `http://your-server-ip:8080/admin`
2. You'll be redirected to the login page
3. Enter your **Admin Username** (e.g., `admin`)
4. Enter your **Admin Password**
5. Click **Login**

**Important Notes:**
- Admin credentials are **case-sensitive**
- The admin user is automatically created/updated when the server starts
- The admin profile is activated and not retired (even if previously disabled)
- You can change the admin password by updating the environment variable and restarting the container

---

## Admin Features

### User Search and Management

**Access:** `http://your-server-ip:8080/admin/search`

The user search page allows you to:

- **Search Users**: Find players by username, email, or license key
- **View User Details**: See registration date, email, license status
- **Check Account Status**: View if accounts are active, retired, or banned
- **View Play History**: See tournament participation and results

**Search Filters:**

| Filter | Description | Example |
|--------|-------------|---------|
| **Name** | Search by username (partial match) | `john` finds `JohnSmith`, `johnny123` |
| **Email** | Search by email address (partial match) | `@gmail.com` finds all Gmail users |
| **License Key** | Search by exact license key | `1234-5678-90AB-CDEF` |
| **Include Retired** | Show inactive/retired accounts | Check to see all users |

**Actions:**

- **View Profile**: Click username to see full profile details
- **Ban User**: Click "Ban" to add user to ban list (see next section)
- **Unretire**: Reactivate a retired account

### Ban Management

**Access:** `http://your-server-ip:8080/admin/banned`

The ban management page allows you to:

- **View Banned Users**: See all currently banned accounts
- **View Banned License Keys**: See all banned license keys
- **Add New Bans**: Ban users by username or license key
- **Remove Bans**: Unban users to allow them access again
- **Ban Reasons**: Document why a user was banned (optional notes)

**Banning a User:**

1. Navigate to the ban management page
2. Click **Add Ban** or search for the user first
3. Enter the **Username** or **License Key**
4. (Optional) Add a **Reason** for the ban
5. Click **Ban**

**Effects of Banning:**

- User cannot log in to the game client
- User cannot create new online profiles
- User cannot join online tournaments
- Existing games are terminated
- Ban applies to username, license key, or both

**Unbanning a User:**

1. Find the user in the banned list
2. Click **Unban** next to their entry
3. Confirm the action

---

## Security Considerations

### Password Security

**Strong Passwords:**
- Use passwords at least 12 characters long
- Include uppercase, lowercase, numbers, and symbols
- Avoid common words or patterns
- Don't reuse passwords from other services

**Example Strong Passwords:**
```
Good:    K9#mP2$xL5@nQ8
Bad:     admin123
Bad:     password
Bad:     ddpoker2024
```

**Changing Admin Password:**

1. Update the environment variable in your `docker-compose.yml` or Unraid settings
2. Restart the container
3. The admin profile password is automatically updated on server startup

### Access Control

**Network Security:**
- If running on a public server, use a reverse proxy (nginx, Caddy) with HTTPS
- Consider using firewall rules to restrict admin panel access to specific IPs
- Use VPN access for remote administration

**Example nginx reverse proxy with HTTPS:**

```nginx
server {
    listen 443 ssl http2;
    server_name ddpoker.example.com;

    ssl_certificate /etc/letsencrypt/live/ddpoker.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/ddpoker.example.com/privkey.pem;

    # Only allow admin panel access from specific IPs
    location /admin {
        allow 192.168.1.0/24;  # Local network
        allow 10.0.0.100;      # VPN IP
        deny all;

        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # Public access to everything else
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### Monitoring

**Check Server Logs:**

```bash
# Docker
docker logs ddpoker

# Docker Compose
docker compose logs -f ddpoker

# Filter for admin activities
docker logs ddpoker | grep -i admin
```

**Log Entries to Monitor:**
- Admin login attempts (successful and failed)
- Admin profile creation/updates
- User ban/unban actions
- Suspicious activity patterns

---

## Troubleshooting

### Cannot Access Admin Panel

**Issue:** Navigating to `/admin` shows "Access Denied" or redirects to home page.

**Solutions:**

1. **Verify admin configuration:**
   ```bash
   docker logs ddpoker | grep -i admin
   ```

   Should show:
   ```
   Admin User: admin
   Admin profile created: admin
   ```

2. **Check environment variables:**
   ```bash
   docker inspect ddpoker | grep -A5 ADMIN
   ```

   Should show `ADMIN_USERNAME` and optionally `ADMIN_PASSWORD`

3. **Restart the container:**
   ```bash
   docker restart ddpoker
   ```

### Admin Login Fails

**Issue:** Login page shows "Invalid credentials" or "Authentication failed".

**Solutions:**

1. **Verify username is correct** (case-sensitive)
   - Default: `admin`
   - Check your `docker-compose.yml` or Unraid settings

2. **Check for auto-generated password:**
   ```bash
   docker logs ddpoker | grep "generated random password"
   ```

3. **Reset admin password:**
   - Update `ADMIN_PASSWORD` in `docker-compose.yml`
   - Run: `docker compose down && docker compose up -d`
   - New password is set on next startup

4. **Check for typos:**
   - Passwords are case-sensitive
   - No extra spaces before/after password

### Admin Profile Not Created

**Issue:** Server starts but no admin profile is created.

**Solutions:**

1. **Check server logs for warnings:**
   ```bash
   docker logs ddpoker | grep -i "admin"
   ```

2. **Verify database is writable:**
   ```bash
   docker exec ddpoker ls -la /data
   ```

   Should show `poker.mv.db` file with read/write permissions

3. **Check for database errors:**
   ```bash
   docker logs ddpoker | grep -i error
   ```

### Password Not Working After Change

**Issue:** Changed admin password but old password still works (or neither work).

**Solutions:**

1. **Container must be restarted** for changes to take effect:
   ```bash
   docker compose restart
   ```

2. **Verify new environment variable:**
   ```bash
   docker compose config | grep ADMIN
   ```

3. **Check update logs:**
   ```bash
   docker logs ddpoker | grep "Admin profile updated"
   ```

### Admin Panel Shows "No Users Found"

**Issue:** User search returns no results even though users exist.

**Solutions:**

1. **Check database connection:**
   ```bash
   docker logs ddpoker | grep -i database
   ```

2. **Verify data volume is mounted:**
   ```bash
   docker volume inspect ddpoker_data
   ```

3. **Check for database file:**
   ```bash
   docker exec ddpoker ls -la /data/*.db
   ```

4. **Test with wildcard search:**
   - Leave all search fields empty
   - Check "Include Retired"
   - Click Search to see all users

---

## Additional Resources

- **Main Documentation**: [README.md](../README.md)
- **Configuration Guide**: [FILE-BASED-CONFIGURATION.md](FILE-BASED-CONFIGURATION.md)
- **Docker Setup**: [docker/docker-compose.yml](../docker/docker-compose.yml)
- **Issue Tracker**: [GitHub Issues](https://github.com/JoshuaABeard/DDPoker/issues)

---

## Support

For issues, questions, or feature requests:

1. Check this documentation and troubleshooting section
2. Search existing [GitHub Issues](https://github.com/JoshuaABeard/DDPoker/issues)
3. Create a new issue with:
   - DD Poker version
   - Deployment method (Docker/Unraid/Manual)
   - Relevant log excerpts
   - Steps to reproduce the problem

---

**Last Updated:** 2026-02-10 (Version 3.3.0-community)
