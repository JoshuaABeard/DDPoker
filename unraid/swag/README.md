# SWAG Reverse Proxy Configuration for DD Poker

These configurations allow you to expose DD Poker through SWAG (Secure Web Application Gateway) with HTTPS.

## Prerequisites

1. SWAG container installed and configured
2. DD Poker container running on bridge network
3. DNS record pointing to your server

## Installation

Choose **ONE** of the following options:

### Option 1: Subdomain (Recommended)

Access DD Poker at: `https://ddpoker.yourdomain.com`

**Copy the config:**
```bash
cp ddpoker.subdomain.conf /mnt/user/appdata/swag/nginx/proxy-confs/
```

**Setup DNS:**
- Add A record: `ddpoker.yourdomain.com` → Your public IP
- Or CNAME: `ddpoker` → `yourdomain.com`

### Option 2: Subfolder

Access DD Poker at: `https://yourdomain.com/ddpoker`

**Copy the config:**
```bash
cp ddpoker.subfolder.conf /mnt/user/appdata/swag/nginx/proxy-confs/
```

No additional DNS needed.

## After Installing Config

1. **Restart SWAG:**
   ```bash
   docker restart swag
   ```

2. **Test access:**
   - Subdomain: `https://ddpoker.yourdomain.com`
   - Subfolder: `https://yourdomain.com/ddpoker`

## Network Configuration

These configs assume DD Poker is at **192.168.4.18:8080** on a bridge network accessible to SWAG.

**To change the IP/port**, edit the config file:
```nginx
set $upstream_app 192.168.4.18;  # Change to your IP
set $upstream_port 8080;          # Change to your port
```

## Game Server Ports

**Important:** SWAG only proxies the web interface (HTTP/HTTPS). The game server and UDP ports need direct access:

**Required port forwards on your router:**
- `8877` TCP → 192.168.4.18:8877 (Game Server)
- `11886` UDP → 192.168.4.18:11886 (Chat)
- `11889` UDP → 192.168.4.18:11889 (Connection Test)

**Client connection:**
- Web: `https://ddpoker.yourdomain.com`
- Game Server: `yourdomain.com:8877` (or your public IP:8877)

## Troubleshooting

**502 Bad Gateway:**
- Check DD Poker is running: `docker ps | grep ddpoker`
- Verify IP address is correct: `docker inspect DDPoker | grep IPAddress`
- Check SWAG can reach DD Poker network

**WebSocket not working:**
- Ensure the `/wicket/websocket` location block is present
- Check browser console for connection errors

**Can't access from outside:**
- Verify DNS is resolving correctly
- Check SWAG logs: `docker logs swag`
- Ensure SSL certificate is valid

## Files

- `ddpoker.subdomain.conf` - Subdomain configuration (ddpoker.yourdomain.com)
- `ddpoker.subfolder.conf` - Subfolder configuration (yourdomain.com/ddpoker)
- `README.md` - This file
