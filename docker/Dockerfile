# ============================================================
# DD Poker - Production Container (Modernized Frontend)
#
# Strategy:
#   BUILD TIME: Build Next.js static export + Java modules
#   RUNTIME: Serve static files + REST API via Spring Boot
#
# Build: (from repo root, after Maven build)
#   docker compose -f docker/docker-compose.yml build
#
# Run:
#   docker compose -f docker/docker-compose.yml up
# ============================================================

# ============================================================
# STAGE 1: Build Next.js Frontend
# ============================================================
FROM node:22-alpine AS web-builder

WORKDIR /build

# Copy Next.js project
COPY code/web/package*.json ./
COPY code/web/ ./

# Install dependencies and build static export
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
# Platform-specific installers must be built on their native OS BEFORE
# building the Docker image. The Dockerfile packages pre-built installers.
#
# Why pre-build? jpackage requires:
# - Windows: WiX Toolset (Windows-only, can't run in Linux container)
# - macOS: Code signing tools (macOS-only)
# - Linux: Native tools for DEB/RPM creation
# - Native platform: jpackage must run on the target OS
#
# Build commands (run on each platform):
#
#   Windows (MSI):
#     cd code/poker
#     mvn clean package assembly:single jpackage:jpackage -DskipTests
#     cp target/dist/DDPokerCE-3.3.0.msi ../../docker/downloads/
#
#   macOS (DMG):
#     cd code/poker
#     mvn clean package assembly:single jpackage:jpackage -DskipTests
#     cp target/dist/DDPokerCE-3.3.0.dmg ../../docker/downloads/
#
#   Linux (DEB + RPM):
#     cd code/poker
#     mvn clean package assembly:single jpackage:jpackage -DskipTests
#     mvn jpackage:jpackage -Pinstaller-linux-rpm -DskipTests
#     cp target/dist/*.deb target/dist/*.rpm ../../docker/downloads/
#
# Alternatively, download pre-built installers from GitHub Releases:
#   gh release download v3.3.0 -D docker/downloads/
#
# Then build Docker image (from repository root):
#   docker compose -f docker/docker-compose.yml build

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

# Email configuration (optional - set to enable email notifications)
# For Gmail, use smtp.gmail.com:587 with app password
# Generate app password: https://myaccount.google.com/apppasswords
ENV SMTP_HOST=127.0.0.1
ENV SMTP_PORT=587
ENV SMTP_USER=
ENV SMTP_PASSWORD=
ENV SMTP_AUTH=false
ENV SMTP_STARTTLS_ENABLE=true
ENV SMTP_FROM=noreply@ddpoker.local

# Admin configuration (optional - set to enable admin panel access at /admin)
ENV ADMIN_USERNAME=
ENV ADMIN_PASSWORD=

# JWT configuration (optional - auto-generated and persisted if not set)
ENV JWT_SECRET=

# Expose ports
EXPOSE 8877 8080 11886/udp 11889/udp

# Persistent volumes
VOLUME /data

ENTRYPOINT ["/app/entrypoint.sh"]
