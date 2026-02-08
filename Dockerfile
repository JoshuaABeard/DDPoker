# ============================================================
# DD Poker - Combined Server Container
# Runs pokerserver + pokerweb + H2 embedded database
#
# Build: (from repo root, after Maven build)
#   docker compose build
#
# Run:
#   docker compose up
# ============================================================
FROM eclipse-temurin:25-jre

LABEL maintainer="DD Poker Docker"
LABEL description="DD Poker server with embedded H2 database"

# Create app and data directories
RUN mkdir -p /app/lib /app/classes /data

WORKDIR /app

# Copy compiled classes from all modules
# Order: base → mid → top to leverage Docker layer caching
COPY code/common/target/classes/ /app/classes/
COPY code/mail/target/classes/ /app/classes/
COPY code/gui/target/classes/ /app/classes/
COPY code/installer/target/classes/ /app/classes/
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

# Copy pokerweb classes (both main and test — PokerJetty is in test source)
COPY code/pokerwicket/target/classes/ /app/classes/
COPY code/pokerwicket/target/test-classes/ /app/classes/

# Copy the webapp directory (needed by embedded Jetty)
COPY code/pokerwicket/src/main/webapp/ /app/webapp/

# Copy runtime messages files
COPY runtime/messages/ /app/runtime/messages/

# Copy all dependency JARs from both modules (deduplicated by filename)
COPY code/pokerserver/target/dependency/ /app/lib/
COPY code/pokerwicket/target/dependency/ /app/lib/

# Remove project JARs to avoid classpath conflicts (we use classes/ instead)
# Keep Apache Wicket libraries (wicket-core, etc.) but remove our wicket-3.0.jar
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

# Copy entrypoint
COPY docker/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

# Environment defaults (H2 embedded database)
ENV DB_DRIVER=org.h2.Driver
ENV DB_URL="jdbc:h2:file:/data/poker;MODE=MySQL;AUTO_SERVER=TRUE"
ENV DB_USER=sa
ENV DB_PASSWORD=

# Ports:
#   8877  - pokerserver HTTP API (game client connects here)
#   8080  - pokerweb Jetty (website)
#   11886 - pokerserver UDP (chat)
#   11889 - pokerserver UDP (connection test)
EXPOSE 8877
EXPOSE 8080
EXPOSE 11886/udp
EXPOSE 11889/udp

# Persistent data volume for H2 database
VOLUME /data

ENTRYPOINT ["/app/entrypoint.sh"]
