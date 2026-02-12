# DD Poker REST API

Spring Boot REST API for DD Poker online features.

## Required Environment Variables

### JWT_SECRET (Required)
- **Purpose:** Secret key for JWT token signing (HMAC-SHA256)
- **Requirements:** Minimum 32 characters (256 bits)
- **Example:** `JWT_SECRET=your-secure-random-secret-key-here-at-least-32-characters`
- **Generation:** Use a cryptographically secure random string generator

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
CORS is automatically configured using `SERVER_HOST` from docker-compose.yml:

```bash
# Set in docker-compose.yml or .env file
SERVER_HOST=poker.example.com
JWT_SECRET=your-secure-random-secret-key-here-at-least-32-characters
```

### Standalone (Development)
```bash
# Set required environment variables
export JWT_SECRET="your-secure-random-secret-key-here-at-least-32-characters"
export SERVER_HOST="localhost"

# Run the API
cd code/api
mvn spring-boot:run
```

## Security Notes

1. **Never commit JWT_SECRET to version control**
2. **Always set CORS_ALLOWED_ORIGINS for production** - default is localhost only
3. Cookies use `SameSite=Strict` for CSRF protection
4. Password hashes are never exposed in API responses
5. HTTPS recommended for production (set `secure=true` on cookies in AuthController)

## Endpoints

See `.claude/reviews/feature-website-modernization.md` for full API documentation.
