# ============================================================
# DD Poker - Production Container
#
# Strategy:
#   BUILD TIME: Build universal JAR
#   RUNTIME: Serve JAR via downloads directory
#
# Clients configure their own server settings on first run!
#
# Build: (from repo root, after Maven build)
#   docker build -t ddpoker:latest .
#
# Run:
#   docker run -p 8080:8080 -p 8877:8877 ddpoker:latest
# ============================================================
FROM eclipse-temurin:25-jdk

LABEL maintainer="DD Poker Docker"
LABEL description="DD Poker server with client installers"

# Create directories
RUN mkdir -p /app/lib /app/classes /app/downloads /data

WORKDIR /app

# ============================================================
# SERVER RUNTIME
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

# Copy pokerweb classes (both main and test)
COPY code/pokerwicket/target/classes/ /app/classes/
COPY code/pokerwicket/target/test-classes/ /app/classes/

# Copy the webapp directory
COPY code/pokerwicket/src/main/webapp/ /app/webapp/

# Copy runtime messages files
COPY runtime/messages/ /app/runtime/messages/

# Copy all dependency JARs
COPY code/pokerserver/target/dependency/ /app/lib/
COPY code/pokerwicket/target/dependency/ /app/lib/

# Remove project JARs to avoid classpath conflicts
RUN rm -f /app/lib/pokerserver-*.jar \
    /app/lib/pokerwicket-*.jar \
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
    "${JAVA_HOME}/bin/jar" cfm /app/downloads/DDPoker.jar MANIFEST.MF -C . . && \
    echo "✓ Universal JAR built: $(du -h /app/downloads/DDPoker.jar | cut -f1)"

# Cleanup temp files
RUN rm -rf /tmp/client-build /tmp/client-libs

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

# Server ports (no SERVER_HOST needed - clients configure themselves!)
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

# Expose ports
EXPOSE 8877 8080 11886/udp 11889/udp

# Persistent volumes
VOLUME /data

ENTRYPOINT ["/app/entrypoint.sh"]
