#!/bin/bash
set -e

# ============================================================
# DD Poker Optimized Entrypoint with Fast Installer Builds
# Uses pre-built classes, only modifies config at runtime
# Build time: ~30 seconds (vs 5-10 minutes for full Maven build)
# ============================================================

APP_DIR=/app
DATA_DIR=/data
WORK_DIR=/data/work
DOWNLOADS_DIR=/app/downloads
INSTALLER_CACHE_FLAG="${DOWNLOADS_DIR}/.installer-cache-info"

# Environment variables for server configuration
SERVER_HOST=${SERVER_HOST:-localhost}
SERVER_PORT=${SERVER_PORT:-8877}
CHAT_PORT=${CHAT_PORT:-11886}
WEB_PORT=${WEB_PORT:-8080}

echo "============================================"
echo "  DD Poker Server with Fast Installer Builder"
echo "============================================"
echo "  Server: ${SERVER_HOST}:${SERVER_PORT}"
echo "  Chat:   ${SERVER_HOST}:${CHAT_PORT}"
echo "  Web:    http://${SERVER_HOST}:${WEB_PORT}"
echo "============================================"

# ============================================================
# INSTALLER BUILD LOGIC (optimized - uses pre-built classes)
# ============================================================

BUILD_INSTALLERS=false

if [ ! -d "${DOWNLOADS_DIR}" ] || [ ! -f "${INSTALLER_CACHE_FLAG}" ]; then
    echo "[installer] No cached installers found. Will build."
    BUILD_INSTALLERS=true
else
    # Check if SERVER_HOST has changed
    CACHED_HOST=$(cat "${INSTALLER_CACHE_FLAG}" 2>/dev/null || echo "")
    if [ "${CACHED_HOST}" != "${SERVER_HOST}" ]; then
        echo "[installer] Server host changed from '${CACHED_HOST}' to '${SERVER_HOST}'"
        echo "[installer] Rebuilding installers..."
        BUILD_INSTALLERS=true
    else
        echo "[installer] Using cached installers for: ${SERVER_HOST}"
        echo "[installer] Cached installers:"
        ls -lh "${DOWNLOADS_DIR}" 2>/dev/null | grep -v "^total" | grep -v "^\." || echo "  (none found)"
    fi
fi

# Build installers if needed (FAST - only config + repackage)
if [ "${BUILD_INSTALLERS}" = true ]; then
    echo ""
    echo "[installer] =========================================="
    echo "[installer] Building installer (~30 seconds)..."
    echo "[installer] =========================================="

    START_TIME=$(date +%s)

    # Run optimized runtime build (uses pre-built classes)
    if /app/runtime-build-installer.sh "${SERVER_HOST}" "${SERVER_PORT}" "${CHAT_PORT}" "${WEB_PORT}" "${DOWNLOADS_DIR}"; then
        # Save cache info
        echo "${SERVER_HOST}" > "${INSTALLER_CACHE_FLAG}"

        END_TIME=$(date +%s)
        DURATION=$((END_TIME - START_TIME))

        echo "[installer] ✅ Installer built in ${DURATION} seconds!"
        echo "[installer] Available downloads:"
        ls -lh "${DOWNLOADS_DIR}" | grep -v "^total" | grep -v "^\."
    else
        echo "[installer] ⚠️  WARNING: Installer build failed"
        echo "[installer] The server will still start, but installers may not be available."
    fi

    echo ""
fi

# ============================================================
# START SERVER PROCESSES
# ============================================================

cd "${APP_DIR}"

# Ensure data directory and runtime directories exist
mkdir -p "$DATA_DIR"
mkdir -p "$WORK_DIR/ddpoker/runtime/log"
mkdir -p "$WORK_DIR/ddpoker/runtime/messages"

# Copy runtime messages files if they don't exist
if [ ! -f "$WORK_DIR/ddpoker/runtime/messages/poker.html" ]; then
    cp -r /app/runtime/messages/* "$WORK_DIR/ddpoker/runtime/messages/"
fi

# Export WORK environment variable
export WORK="$WORK_DIR"

# Build classpath
CLASSPATH="$APP_DIR/classes"
for jar in $APP_DIR/lib/*.jar; do
  CLASSPATH="$CLASSPATH:$jar"
done
export CLASSPATH

# JVM options
JAVA_OPTS="-server -Dfile.encoding=UTF-8"

echo ""
echo "============================================"
echo "  DD Poker Server Starting"
echo "============================================"
echo "  Data directory: $DATA_DIR"
echo "  DB Driver: ${DB_DRIVER:-org.h2.Driver}"
echo "  Installers: ${DOWNLOADS_DIR}"
echo "============================================"

# Start pokerserver (background)
echo "[entrypoint] Starting pokerserver..."
java $JAVA_OPTS -Xms24m -Xmx96m \
  -cp "$CLASSPATH" \
  com.donohoedigital.games.poker.server.PokerServerMain &
SERVER_PID=$!
echo "[entrypoint] pokerserver PID: $SERVER_PID"

# Brief pause
sleep 3

# Start pokerweb (background)
echo "[entrypoint] Starting pokerweb (Jetty)..."
java $JAVA_OPTS -Xms24m -Xmx96m \
  -Dpokerweb.war.path=/app/webapp \
  -Dpokerweb.daemon=true \
  -Dwicket.configuration=deployment \
  -cp "$CLASSPATH" \
  com.donohoedigital.games.poker.wicket.PokerJetty &
WEB_PID=$!
echo "[entrypoint] pokerweb PID: $WEB_PID"

echo "[entrypoint] Both processes started."
echo "[entrypoint] Web portal: http://${SERVER_HOST}:${WEB_PORT}"
echo "[entrypoint] Downloads:  http://${SERVER_HOST}:${WEB_PORT}/downloads"
echo ""

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
wait -n $SERVER_PID $WEB_PID
EXIT_CODE=$?
echo "[entrypoint] A process exited with code $EXIT_CODE. Stopping remaining..."
kill $SERVER_PID $WEB_PID 2>/dev/null
wait $SERVER_PID $WEB_PID 2>/dev/null
exit $EXIT_CODE
