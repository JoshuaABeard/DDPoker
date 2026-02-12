# Phase 5: Docker Integration & Deployment

## Context

Phases 1-4 are complete: Spring Boot REST API, Next.js frontend setup, authentication, online portal pages, and admin section. Phase 5 integrates the modernized stack into Docker, replacing the Wicket webapp with the Next.js frontend served by the Spring Boot API.

**Current Docker Architecture:**
- **Runtime:** `eclipse-temurin:25-jdk` (Java-only container)
- **Two processes:**
  1. `PokerServerMain` (game server, port 8877)
  2. `PokerJetty` (Wicket webapp, port 8080)
- **Webapp:** `/app/webapp/` contains Wicket files
- **Build:** Single-stage, copies compiled Java classes

**Target Architecture:**
- **Runtime:** Same `eclipse-temurin:25-jdk` (Java-only at runtime)
- **Two processes:**
  1. `PokerServerMain` (game server, port 8877) - UNCHANGED
  2. `ApiApplication` (Spring Boot API + static files, port 8080) - NEW
- **Webapp:** `/app/webapp/` contains Next.js static export
- **Build:** Multi-stage (Node.js build stage + Java runtime)

## Scope

**In Scope:**
- Multi-stage Dockerfile (Node.js build + Java runtime)
- Next.js static export configuration
- Spring Boot static file serving
- Update entrypoint.sh to start ApiApplication instead of PokerJetty
- Environment variable configuration
- Full Docker build and deployment testing

**Out of Scope:**
- Removing Wicket modules (Phase 6 - Cleanup)
- Backend API changes (Phase 1 complete)
- Frontend feature additions (Phases 2-4 complete)

## Decision: Static Export vs SSR

**Selected Approach:** Static Export

**Rationale:**
- No server-side rendering needed (content is data tables, forms)
- Keeps container Java-only at runtime (no Node.js runtime dependency)
- Simpler deployment (Spring Boot serves static files)
- Lower resource usage
- API calls happen client-side via `fetch`

**Implementation:**
- `next build` produces optimized static HTML/CSS/JS
- Spring Boot serves static files from `/app/webapp/`
- API endpoints at `/api/*` handled by controllers
- Static files at `/*` served from filesystem

**Alternative (if needed later):** Next.js SSR with Node.js runtime
- Would require running Node.js alongside Java processes
- More complex entrypoint script
- Higher memory usage
- Only needed for true SSR features (which we don't use)

## Implementation Approach

### Strategy: Incremental Build Integration

**Step 1:** Configure Next.js for static export
**Step 2:** Add Spring Boot static file serving
**Step 3:** Update Dockerfile with multi-stage build
**Step 4:** Update entrypoint.sh to start API instead of PokerJetty
**Step 5:** Update environment variables and config
**Step 6:** Test full Docker build and deployment
**Step 7:** Verify all functionality works

## Critical Files

### Files to Modify

**1. `code/web/next.config.ts`** - Configure static export
```typescript
const nextConfig: NextConfig = {
  output: 'export',
  trailingSlash: true,
  images: {
    unoptimized: true, // Static export doesn't support image optimization
  },
}
```

**2. `code/api/src/main/resources/application.properties`** - Add static file serving
```properties
# Static Resources (Next.js frontend)
spring.web.resources.static-locations=file:/app/webapp/
spring.web.resources.add-mappings=true

# Don't serve static files for /api/* paths
spring.mvc.static-path-pattern=/**

# Fallback to index.html for client-side routing
spring.web.resources.chain.enabled=true
```

**3. `docker/Dockerfile`** - Multi-stage build
- Add Node.js build stage (build Next.js)
- Copy Next.js output to `/app/webapp/`
- Copy API module classes
- Remove Wicket webapp copy

**4. `docker/entrypoint.sh`** - Replace PokerJetty with ApiApplication
- Change second process from `PokerJetty` to `ApiApplication`
- Keep environment variable handling
- Same graceful shutdown logic

**5. `code/api/src/main/java/.../config/WebConfig.java`** (NEW) - SPA routing support
- Add configuration for serving index.html on 404 (client-side routing)

### Files to Verify

**6. `docker/docker-compose.yml`** - Should work as-is
- Port 8080 already mapped
- Environment variables already configured
- May need to update comments

**7. `code/web/lib/config.ts`** - API URL configuration
- Verify `getApiUrl()` works in production (should use relative paths)

## Implementation Steps

### Step 1: Configure Next.js for Static Export

**Update `code/web/next.config.ts`:**

```typescript
import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Static export configuration
  output: 'export',

  // Add trailing slashes to URLs for static hosting
  trailingSlash: true,

  // Disable image optimization (not available in static export)
  images: {
    unoptimized: true,
  },

  // Base path (empty for root)
  basePath: '',

  // Asset prefix (empty for same-origin)
  assetPrefix: '',
}

export default nextConfig;
```

**Verify:**
- Run `npm run build` in `code/web/`
- Check that `out/` directory is created with static HTML files
- Verify `out/index.html` exists

**Known Limitation:**
- Next.js Image component `<Image>` won't optimize images
- Solution: Images are already optimized, no issue
- API routes won't work (we use Spring Boot API instead)

### Step 2: Add Spring Boot Static File Serving

**Create `code/api/src/main/java/com/donohoedigital/poker/api/config/WebConfig.java`:**

```java
package com.donohoedigital.poker.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Web MVC configuration for serving Next.js static export
 * and handling client-side routing fallback to index.html
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve Next.js static files from /app/webapp/
        // Fallback to index.html for client-side routing (SPA)
        registry.addResourceHandler("/**")
                .addResourceLocations("file:/app/webapp/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);

                        // If resource exists, serve it
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }

                        // Otherwise, fallback to index.html (for client-side routing)
                        // BUT: Don't fallback for /api/* requests (let 404 happen)
                        if (resourcePath.startsWith("api/")) {
                            return null;
                        }

                        // Fallback to index.html
                        Resource indexHtml = location.createRelative("index.html");
                        return (indexHtml.exists() && indexHtml.isReadable()) ? indexHtml : null;
                    }
                });
    }
}
```

**Update `code/api/src/main/resources/application.properties`:**

```properties
# Static Resources (Next.js frontend)
# Note: WebConfig.java handles the resource locations and fallback logic
spring.web.resources.add-mappings=true
```

**Verify:**
- API still compiles (`mvn clean install` in code/api/)
- No Spring Boot errors on startup

### Step 3: Update Dockerfile with Multi-Stage Build

**Replace `docker/Dockerfile` with:**

```dockerfile
# ============================================================
# DD Poker - Production Container (Modernized Frontend)
#
# Strategy:
#   BUILD TIME: Build Next.js static export + JAR
#   RUNTIME: Serve static files + REST API via Spring Boot
#
# Build: (from repo root, after Maven build)
#   docker build -t ddpoker:latest .
#
# Run:
#   docker run -p 8080:8080 -p 8877:8877 ddpoker:latest
# ============================================================

# ============================================================
# STAGE 1: Build Next.js Frontend
# ============================================================
FROM node:22-alpine AS web-builder

WORKDIR /build

# Copy Next.js project
COPY code/web/package*.json ./
COPY code/web/ ./

# Install dependencies and build
RUN npm ci && npm run build

# Verify build output exists
RUN test -d out && echo "✓ Next.js static export built: $(du -sh out | cut -f1)"

# ============================================================
# STAGE 2: Java Runtime with API + Game Server
# ============================================================
FROM eclipse-temurin:25-jdk

LABEL maintainer="DD Poker Docker"
LABEL description="DD Poker server with modernized Next.js frontend"

# Create directories
RUN mkdir -p /app/lib /app/classes /app/downloads /app/webapp /data

WORKDIR /app

# ============================================================
# COPY COMPILED CLASSES (SERVER + API)
# ============================================================

# Copy compiled classes from all modules
COPY code/common/target/classes/ /app/classes/
COPY code/mail/target/classes/ /app/classes/
COPY code/gui/target/classes/ /app/classes/
COPY code/db/target/classes/ /app/classes/
COPY code/wicket/target/classes/ /app/classes/
COPY code/jsp/target/classes/ /app/classes/
COPY code/server/target/classes/ /app/classes/
COPY code/udp/target/classes/ /app/classes/
COPY code/gamecommon/target/classes/ /app/classes/
COPY code/gameengine/target/classes/ /app/classes/
COPY code/ddpoker/target/classes/ /app/classes/
COPY code/pokerengine/target/classes/ /app/classes/
COPY code/pokernetwork/target/classes/ /app/classes/
COPY code/tools/target/classes/ /app/classes/
COPY code/gameserver/target/classes/ /app/classes/
COPY code/pokerserver/target/classes/ /app/classes/

# Copy API module classes (Spring Boot REST API)
COPY code/api/target/classes/ /app/classes/

# Copy runtime messages files
COPY runtime/messages/ /app/runtime/messages/

# Copy all dependency JARs
COPY code/pokerserver/target/dependency/ /app/lib/
COPY code/api/target/dependency/ /app/lib/

# Remove project JARs to avoid classpath conflicts
RUN rm -f /app/lib/pokerserver-*.jar \
    /app/lib/api-*.jar \
    /app/lib/gameserver-*.jar \
    /app/lib/ddpoker-*.jar \
    /app/lib/pokerengine-*.jar \
    /app/lib/pokernetwork-*.jar \
    /app/lib/pokertools-*.jar \
    /app/lib/tools-*.jar \
    /app/lib/gamecommon-*.jar \
    /app/lib/gameengine-*.jar \
    /app/lib/common-*.jar \
    /app/lib/mail-*.jar \
    /app/lib/gui-*.jar \
    /app/lib/installer-*.jar \
    /app/lib/db-*.jar \
    /app/lib/wicket-3.0.jar \
    /app/lib/jsp-*.jar \
    /app/lib/server-*.jar \
    /app/lib/udp-*.jar

# ============================================================
# COPY NEXT.JS STATIC EXPORT
# ============================================================

# Copy Next.js static files from build stage
COPY --from=web-builder /build/out/ /app/webapp/

# Verify webapp files exist
RUN test -f /app/webapp/index.html && echo "✓ Next.js webapp deployed"

# ============================================================
# BUILD UNIVERSAL CLIENT JAR (at build time)
# ============================================================

# Copy ALL client materials
COPY code/poker/target/classes/ /tmp/client-build/
COPY code/poker/target/dependency/ /tmp/client-libs/

# Extract all dependency JARs into client build (for fat JAR)
RUN cd /tmp/client-build && \
    for jar in /tmp/client-libs/*.jar; do \
        jar xf "$jar" 2>/dev/null || true; \
    done && \
    rm -rf META-INF/*.SF META-INF/*.RSA META-INF/*.DSA

# Build universal FAT JAR (no server configuration)
RUN cd /tmp/client-build && \
    printf "Manifest-Version: 1.0\nMain-Class: com.donohoedigital.games.poker.PokerMain\n" > MANIFEST.MF && \
    "${JAVA_HOME}/bin/jar" cfm /app/downloads/DDPokerCE-3.3.0.jar MANIFEST.MF -C . . && \
    echo "✓ Universal JAR built: $(du -h /app/downloads/DDPokerCE-3.3.0.jar | cut -f1)"

# Cleanup temp files
RUN rm -rf /tmp/client-build /tmp/client-libs

# ============================================================
# COPY PRE-BUILT INSTALLERS (REQUIRED)
# ============================================================

COPY docker/downloads/ /app/downloads/

# ============================================================
# ENTRYPOINT
# ============================================================

COPY docker/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

# ============================================================
# ENVIRONMENT & CONFIGURATION
# ============================================================

# Database configuration
ENV DB_DRIVER=org.h2.Driver
ENV DB_URL="jdbc:h2:file:/data/poker;MODE=MySQL;AUTO_SERVER=TRUE"
ENV DB_USER=sa
ENV DB_PASSWORD=

# Server ports
ENV SERVER_PORT=8877
ENV CHAT_PORT=11886
ENV WEB_PORT=8080

# Email configuration
ENV SMTP_HOST=127.0.0.1
ENV SMTP_PORT=587
ENV SMTP_USER=
ENV SMTP_PASSWORD=
ENV SMTP_AUTH=false
ENV SMTP_STARTTLS_ENABLE=true
ENV SMTP_FROM=noreply@ddpoker.local

# Admin configuration
ENV ADMIN_USERNAME=
ENV ADMIN_PASSWORD=

# JWT configuration (optional - auto-generated if not set)
ENV JWT_SECRET=

# Expose ports
EXPOSE 8877 8080 11886/udp 11889/udp

# Persistent volumes
VOLUME /data

ENTRYPOINT ["/app/entrypoint.sh"]
```

**Key Changes:**
1. **Stage 1 (web-builder):** Build Next.js static export
2. **Stage 2:** Copy API classes + Next.js output
3. **Remove:** Wicket webapp copy (line 52 in old Dockerfile)
4. **Add:** API dependency JARs

### Step 4: Update entrypoint.sh

**Replace second Java process in `docker/entrypoint.sh`:**

```bash
# Start pokerweb via Spring Boot API (background)
echo "[entrypoint] Starting API (Spring Boot)..."
java $JAVA_OPTS -Xms24m -Xmx96m \
  -cp "$CLASSPATH" \
  com.donohoedigital.poker.api.ApiApplication &
WEB_PID=$!
echo "[entrypoint] API PID: $WEB_PID"
```

**Full updated entrypoint.sh:**

```bash
#!/bin/bash
set -e

# ============================================================
# DD Poker Combined Server Entrypoint
# Starts pokerserver and API as separate Java processes
# ============================================================

APP_DIR=/app
DATA_DIR=/data
WORK_DIR=/data/work

# Ensure data directory and runtime directories exist
mkdir -p "$DATA_DIR"
mkdir -p "$WORK_DIR/ddpoker/runtime/log"
mkdir -p "$WORK_DIR/ddpoker/runtime/messages"

# Copy runtime messages files if they don't exist
if [ ! -f "$WORK_DIR/ddpoker/runtime/messages/poker.html" ]; then
    cp -r /app/runtime/messages/* "$WORK_DIR/ddpoker/runtime/messages/"
fi

# Export WORK environment variable so app finds runtime directories
export WORK="$WORK_DIR"

# Build classpath from all JARs and classes
CLASSPATH="$APP_DIR/classes"
for jar in $APP_DIR/lib/*.jar; do
  CLASSPATH="$CLASSPATH:$jar"
done

export CLASSPATH

# Common JVM options
JAVA_OPTS="-server -Dfile.encoding=UTF-8"

# Email configuration (if provided via environment variables)
if [ -n "$SMTP_HOST" ]; then
  JAVA_OPTS="$JAVA_OPTS -Dsettings.smtp.host=$SMTP_HOST"
fi
if [ -n "$SMTP_PORT" ]; then
  JAVA_OPTS="$JAVA_OPTS -Dsettings.smtp.port=$SMTP_PORT"
fi
if [ -n "$SMTP_USER" ]; then
  JAVA_OPTS="$JAVA_OPTS -Dsettings.smtp.user=$SMTP_USER"
fi
if [ -n "$SMTP_PASSWORD" ]; then
  JAVA_OPTS="$JAVA_OPTS -Dsettings.smtp.pass=$SMTP_PASSWORD"
fi
if [ -n "$SMTP_AUTH" ]; then
  JAVA_OPTS="$JAVA_OPTS -Dsettings.smtp.auth=$SMTP_AUTH"
fi
if [ -n "$SMTP_STARTTLS_ENABLE" ]; then
  JAVA_OPTS="$JAVA_OPTS -Dsettings.smtp.starttls.enable=$SMTP_STARTTLS_ENABLE"
fi
if [ -n "$SMTP_FROM" ]; then
  JAVA_OPTS="$JAVA_OPTS -Dsettings.server.profilefrom=$SMTP_FROM"
fi

# Admin user configuration (if provided via environment variables)
if [ -n "$ADMIN_USERNAME" ]; then
  JAVA_OPTS="$JAVA_OPTS -Dsettings.admin.user=$ADMIN_USERNAME"
fi
if [ -n "$ADMIN_PASSWORD" ]; then
  JAVA_OPTS="$JAVA_OPTS -Dsettings.admin.password=$ADMIN_PASSWORD"
fi

echo "============================================"
echo "  DD Poker Server Starting (Modernized)"
echo "============================================"
echo "  Data directory: $DATA_DIR"
echo "  DB Driver: ${DB_DRIVER:-org.h2.Driver}"
echo "  SMTP Host: ${SMTP_HOST:-127.0.0.1}:${SMTP_PORT:-587}"
echo "  Admin User: ${ADMIN_USERNAME:-not configured}"
echo "============================================"

# Start pokerserver (background)
echo "[entrypoint] Starting pokerserver..."
java $JAVA_OPTS -Xms24m -Xmx96m \
  -cp "$CLASSPATH" \
  com.donohoedigital.games.poker.server.PokerServerMain &
SERVER_PID=$!
echo "[entrypoint] pokerserver PID: $SERVER_PID"

# Brief pause to let server initialize Spring context first
sleep 3

# Start API via Spring Boot (background)
echo "[entrypoint] Starting API (Spring Boot + Next.js)..."
java $JAVA_OPTS -Xms24m -Xmx96m \
  -cp "$CLASSPATH" \
  com.donohoedigital.poker.api.ApiApplication &
WEB_PID=$!
echo "[entrypoint] API PID: $WEB_PID"

echo "[entrypoint] Both processes started. Waiting..."

# Trap signals for graceful shutdown
shutdown() {
  echo "[entrypoint] Shutting down..."
  kill $SERVER_PID $WEB_PID 2>/dev/null
  wait $SERVER_PID $WEB_PID 2>/dev/null
  echo "[entrypoint] Shutdown complete."
  exit 0
}
trap shutdown SIGTERM SIGINT

# Wait for either process to exit
# If one dies, stop the other and exit
wait -n $SERVER_PID $WEB_PID
EXIT_CODE=$?
echo "[entrypoint] A process exited with code $EXIT_CODE. Stopping remaining..."
kill $SERVER_PID $WEB_PID 2>/dev/null
wait $SERVER_PID $WEB_PID 2>/dev/null
exit $EXIT_CODE
```

**Key Changes:**
- Line 88-93: Replace `PokerJetty` with `ApiApplication`
- Line 69: Update startup message
- Remove: `-Dpokerweb.war.path` and `-Dwicket.configuration` flags

### Step 5: Environment Variables (Already Configured)

**Verify `docker/docker-compose.yml` has JWT_SECRET comment:**

The docker-compose.yml already has JWT_SECRET documented (lines 21-26). No changes needed.

### Step 6: Build Script Updates

**Create `docker/build.sh` helper script (optional):**

```bash
#!/bin/bash
# Build DD Poker Docker image with modernized frontend

set -e

echo "============================================"
echo "  Building DD Poker Docker Image"
echo "============================================"

# Build Java modules (from repo root)
echo "[1/3] Building Java modules..."
cd ..
mvn clean package -DskipTests -P fast

# Build Next.js frontend
echo "[2/3] Building Next.js frontend..."
cd code/web
npm ci
npm run build
cd ../..

# Build Docker image
echo "[3/3] Building Docker image..."
docker compose -f docker/docker-compose.yml build

echo "============================================"
echo "  Build Complete!"
echo "============================================"
echo "Run: docker compose -f docker/docker-compose.yml up"
```

### Step 7: Verification & Testing

**Local Testing (Before Docker):**

1. **Test API serves static files:**
   ```bash
   # Build Next.js
   cd code/web && npm run build

   # Copy output to API resources (temporary test)
   mkdir -p ../api/src/main/resources/static
   cp -r out/* ../api/src/main/resources/static/

   # Run API locally
   cd ../api
   mvn spring-boot:run

   # Test in browser: http://localhost:8080
   # Should see Next.js homepage
   # Test API: http://localhost:8080/api/health
   ```

2. **Test full Docker build:**
   ```bash
   # From repo root
   mvn clean package -DskipTests
   docker compose -f docker/docker-compose.yml build
   docker compose -f docker/docker-compose.yml up
   ```

3. **Verify all functionality:**
   - [ ] Homepage loads (http://localhost:8080)
   - [ ] Static pages load (About, Download, Support)
   - [ ] Login works
   - [ ] Game lists display data
   - [ ] Leaderboard works
   - [ ] Admin pages accessible (with admin user)
   - [ ] API endpoints respond (http://localhost:8080/api/health)
   - [ ] Game server works (port 8877)
   - [ ] Client JAR downloads (http://localhost:8080/downloads/DDPokerCE-3.3.0.jar)

## Known Issues & Solutions

**Issue 1: Next.js routing doesn't work (404 on refresh)**
- **Cause:** Direct URL access doesn't fallback to index.html
- **Solution:** `WebConfig.java` PathResourceResolver handles fallback

**Issue 2: API calls return 404**
- **Cause:** Spring Boot serving index.html for /api/* paths
- **Solution:** WebConfig excludes /api/* from fallback

**Issue 3: Images don't load**
- **Cause:** Next.js Image optimization doesn't work in static export
- **Solution:** Set `images.unoptimized: true` in next.config.ts

**Issue 4: Build fails - npm not found**
- **Cause:** Multi-stage Dockerfile stage 1 needs Node.js
- **Solution:** Use `node:22-alpine` base image for build stage

**Issue 5: Spring Boot can't find static files**
- **Cause:** Static files copied to wrong location
- **Solution:** Ensure Dockerfile copies to `/app/webapp/`, WebConfig reads from `file:/app/webapp/`

## Testing Checklist

### Pre-Docker Testing
- [ ] Next.js builds successfully (`npm run build` produces `out/` directory)
- [ ] API compiles with new WebConfig.java
- [ ] API serves static files locally (test with copied files)

### Docker Build
- [ ] Multi-stage build completes without errors
- [ ] Stage 1 (web-builder) produces Next.js output
- [ ] Stage 2 copies files to correct locations
- [ ] Final image size is reasonable (< 1GB)

### Runtime Testing
- [ ] Container starts both processes (pokerserver + API)
- [ ] No startup errors in logs
- [ ] Homepage loads at http://localhost:8080
- [ ] API health endpoint responds: http://localhost:8080/api/health
- [ ] Static assets load (CSS, JS, images)
- [ ] Client-side routing works (navigate, refresh)

### Functional Testing
- [ ] Login/logout works
- [ ] Game lists paginate and filter correctly
- [ ] Leaderboard displays data
- [ ] Profile management works
- [ ] Admin pages require auth
- [ ] Ban management CRUD works
- [ ] Client JAR downloads
- [ ] Game server accepts connections (port 8877)

### Deployment Testing
- [ ] Docker Compose up/down works
- [ ] Volume persistence works (database survives restart)
- [ ] Environment variables applied correctly
- [ ] Logs are accessible (docker logs)
- [ ] Graceful shutdown works (SIGTERM)

## Success Criteria

- ✅ Docker image builds successfully with multi-stage process
- ✅ Container runs Spring Boot API + PokerServerMain
- ✅ Next.js frontend accessible at http://localhost:8080
- ✅ All API endpoints functional
- ✅ Client-side routing works (SPA navigation)
- ✅ Static assets served correctly
- ✅ Game server still functions
- ✅ Database persistence works
- ✅ Admin authentication works
- ✅ Zero downtime from modernization (feature parity)

## Rollback Plan

If Phase 5 deployment fails:
1. Revert Dockerfile to previous version (git checkout)
2. Revert entrypoint.sh to PokerJetty startup
3. Rebuild: `docker compose build && docker compose up`
4. Previous Wicket webapp will work as before

The modernized frontend code (code/web/) is not used by the old setup, so it's safe to keep.
