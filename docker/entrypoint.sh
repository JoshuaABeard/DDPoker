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
