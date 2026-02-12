# DD Poker REST API

Spring Boot REST API for DD Poker online features.

## Required Environment Variables

### JWT_SECRET (Auto-generated)
- **Purpose:** Secret key for JWT token signing (HMAC-SHA256)
- **Default Behavior:** Auto-generated on first startup and saved to `$WORK/jwt.secret` (or `./data/jwt.secret`)
- **Persistence:** Stored in Docker volume, survives container restarts
- **Manual Override:** Set `JWT_SECRET` environment variable (min 32 characters)
- **Generation:** `JWT_SECRET=$(openssl rand -base64 32)`
- **Security:** Uses `SecureRandom` for cryptographically secure generation

### SERVER_HOST (Auto-configured in Docker)
- **Purpose:** The hostname/domain where the application is accessible
- **Default:** `localhost` (from docker-compose.yml)
- **Automatically used for:** CORS allowed origins
- **Examples:** `poker.example.com`, `192.168.1.100`, `mydomain.com`
- **Note:** This is the same variable used by the existing pokerserver

### CORS_ALLOWED_ORIGINS (Optional Override)
- **Purpose:** Comma-separated list of allowed origins for CORS
- **Default:** Auto-generated from `SERVER_HOST`:
  - `http://${SERVER_HOST}:3000` (Next.js dev)
  - `http://${SERVER_HOST}:8080` (production web)
  - `https://${SERVER_HOST}` (reverse proxy)
- **Override Example:** `CORS_ALLOWED_ORIGINS=https://poker.example.com,https://www.poker.example.com`
- **When to override:** Multiple domains, custom ports, or complex setups

## Configuration Files

- `application.properties` - Main configuration (port, JWT expiration, logging)
- Environment variables override property defaults

## Running

### In Docker (Recommended)
Zero configuration required! Both CORS and JWT are auto-configured:

```bash
# Optional: Set custom domain (defaults to localhost)
SERVER_HOST=poker.example.com

# Optional: Set custom JWT secret (auto-generated if not set)
# JWT_SECRET=$(openssl rand -base64 32)
```

On first startup, the API will:
1. Auto-generate a secure JWT secret
2. Save it to `/data/jwt.secret` (persisted in Docker volume)
3. Configure CORS based on `SERVER_HOST`

### Standalone (Development)
```bash
# Optional environment variables
export SERVER_HOST="localhost"
# JWT_SECRET auto-generated to ./data/jwt.secret if not set

# Run the API
cd code/api
mvn spring-boot:run
```

## Security Notes

1. **JWT Secret Auto-Generation** - Secure 256-bit secret auto-generated on first run and persisted to volume
2. **Never commit jwt.secret file to version control** - Added to `.gitignore` automatically
3. **CORS Auto-Configuration** - Uses `SERVER_HOST` from docker-compose, supports custom domains/IPs
4. **Cookies use `SameSite=Strict`** - CSRF protection for cookie-based JWT
5. **Password hashes never exposed** - `@JsonIgnore` prevents API exposure
6. **HTTPS recommended for production** - Set `secure=true` on cookies in AuthController for HTTPS deployments

## Endpoints

See `.claude/reviews/feature-website-modernization.md` for full API documentation.
