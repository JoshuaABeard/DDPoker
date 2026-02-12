# DD Poker REST API

Spring Boot REST API for DD Poker online features.

## Required Environment Variables

### JWT_SECRET (Required)
- **Purpose:** Secret key for JWT token signing (HMAC-SHA256)
- **Requirements:** Minimum 32 characters (256 bits)
- **Example:** `JWT_SECRET=your-secure-random-secret-key-here-at-least-32-characters`
- **Generation:** Use a cryptographically secure random string generator

### CORS_ALLOWED_ORIGINS (Required for Production)
- **Purpose:** Comma-separated list of allowed origins for CORS
- **Default:** `http://localhost:3000` (development only)
- **Production Examples:**
  - Single domain: `CORS_ALLOWED_ORIGINS=https://poker.example.com`
  - Multiple origins: `CORS_ALLOWED_ORIGINS=https://poker.example.com,http://poker.example.com`
  - IP-based: `CORS_ALLOWED_ORIGINS=http://192.168.1.100:3000`
- **Important:** Set this to match where your Next.js frontend is hosted

## Configuration Files

- `application.properties` - Main configuration (port, JWT expiration, logging)
- Environment variables override property defaults

## Running

```bash
# Set required environment variables
export JWT_SECRET="your-secure-random-secret-key-here-at-least-32-characters"
export CORS_ALLOWED_ORIGINS="https://your-domain.com"

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
