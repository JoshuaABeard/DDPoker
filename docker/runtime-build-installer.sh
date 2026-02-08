#!/bin/bash
# =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
# DD Poker - Source Code
# Copyright (c) 2003-2026 Doug Donohoe
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# For the full License text, please see the LICENSE.txt file
# in the root directory of this project.
#
# The "DD Poker" and "Donohoe Digital" names and logos, as well as any images,
# graphics, text, and documentation found in this repository (including but not
# limited to written documentation, website content, and marketing materials)
# are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives
# 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets
# without explicit written permission for any uses not covered by this License.
# For the full LICENSE text, please see the LICENSE-CREATIVE-COMMONS.txt file
# in the root directory of this project.
#
# For inquiries regarding commercial licensing of this source code or
# the use of names, logos, images, text, or other assets, please contact
# doug [at] donohoe [dot] info.
# =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
#
# Runtime installer builder - uses PRE-BUILT classes
# Only modifies config files and packages into fat JAR
#
# FAST: 1-2 seconds (vs 5-10 minutes for full Maven build)
# SIMPLE: Uses JDK's jpackage for native installers (no external tools!)
#

set -e

# Parse arguments
SERVER_HOST=${1:-localhost}
SERVER_PORT=${2:-8877}
CHAT_PORT=${3:-11886}
WEB_PORT=${4:-8080}
OUTPUT_DIR=${5:-/app/downloads}

echo "=========================================="
echo "Building DDPoker Client Installer"
echo "=========================================="
echo "Server: ${SERVER_HOST}:${SERVER_PORT}"
echo "Chat:   ${SERVER_HOST}:${CHAT_PORT}"
echo "Web:    http://${SERVER_HOST}:${WEB_PORT}"
echo "Output: ${OUTPUT_DIR}"
echo "=========================================="

TEMP_DIR="/tmp/ddpoker-build-$$"
mkdir -p "${TEMP_DIR}"
mkdir -p "${OUTPUT_DIR}"

# Step 1: Copy pre-built classes to temp directory
echo ""
echo "[1/4] Copying pre-built classes..."
cp -r /app/client-build/* "${TEMP_DIR}/"

# Step 2: Modify configuration files in temp directory
echo ""
echo "[2/4] Configuring server URLs..."

CLIENT_PROPS="${TEMP_DIR}/config/poker/client.properties"
GAMEDEF_XML="${TEMP_DIR}/config/poker/gamedef.xml"

# Update client.properties
sed -i "s|option.onlineserver.default=.*|option.onlineserver.default= ${SERVER_HOST}:${SERVER_PORT}|g" "$CLIENT_PROPS"
sed -i "s|option.onlinechat.default=.*|option.onlinechat.default=   ${SERVER_HOST}:${CHAT_PORT}|g" "$CLIENT_PROPS"

# Update gamedef.xml
sed -i "s|<param name=\"url\" strvalue=\"http://www.ddpoker.com/online\"/>|<param name=\"url\" strvalue=\"http://${SERVER_HOST}:${WEB_PORT}/online\"/>|g" "$GAMEDEF_XML"
sed -i "s|<param name=\"url\" strvalue=\"http://www.ddpoker.com/forums\"/>|<param name=\"url\" strvalue=\"http://${SERVER_HOST}:${WEB_PORT}/forums\"/>|g" "$GAMEDEF_XML"

echo "✓ Server URLs configured"

# Step 3: Build native installers using jpackage (built into JDK)
echo ""
echo "[3/5] Building native installers with jpackage..."

BUILT_NATIVE=false

# First create the fat JAR (needed for jpackage)
echo "  → Creating temporary JAR for jpackage..."
cd "${TEMP_DIR}"

cat > MANIFEST.MF << 'EOF'
Manifest-Version: 1.0
Main-Class: com.donohoedigital.games.poker.PokerMain
EOF

"${JAVA_HOME}/bin/jar" cfm "${TEMP_DIR}/DDPoker-temp.jar" MANIFEST.MF -C . .

if [ $? -eq 0 ]; then
    echo "  ✓ Temporary JAR created"

    # Use jpackage to create native installer
    # Note: This creates a Linux .deb by default on Linux
    # For Windows .exe or Mac .dmg, build on those platforms
    echo "  → Running jpackage..."

    "${JAVA_HOME}/bin/jpackage" \
        --input "${TEMP_DIR}" \
        --name "DDPoker" \
        --main-jar DDPoker-temp.jar \
        --main-class com.donohoedigital.games.poker.PokerMain \
        --type app-image \
        --dest "${OUTPUT_DIR}" \
        --app-version 3.0 \
        --vendor "DD Poker" \
        --description "DD Poker - Texas Hold'em Poker Game" \
        --java-options "-Xms256m" \
        --java-options "-Xmx512m" 2>/dev/null

    if [ $? -eq 0 ]; then
        echo "  ✓ Native app image built successfully"
        BUILT_NATIVE=true
    else
        echo "  ℹ jpackage not available or failed (this is OK)"
        echo "  → Will provide cross-platform JAR instead"
    fi
else
    echo "  ⚠ Failed to create temporary JAR for jpackage"
fi

# Step 4: Create fat JAR (primary distribution method)
echo ""
echo "[4/5] Creating cross-platform JAR..."

cd "${TEMP_DIR}"

# If we didn't already create it for jpackage, create it now
if [ ! -f "${TEMP_DIR}/DDPoker-temp.jar" ]; then
    cat > MANIFEST.MF << 'EOF'
Manifest-Version: 1.0
Main-Class: com.donohoedigital.games.poker.PokerMain
EOF

    "${JAVA_HOME}/bin/jar" cfm "${OUTPUT_DIR}/DDPoker.jar" MANIFEST.MF -C . .
else
    # Move the temp JAR to final location
    mv "${TEMP_DIR}/DDPoker-temp.jar" "${OUTPUT_DIR}/DDPoker.jar"
fi

if [ -f "${OUTPUT_DIR}/DDPoker.jar" ]; then
    echo "✓ Fat JAR created successfully"
    JAR_SIZE=$(du -h "${OUTPUT_DIR}/DDPoker.jar" | cut -f1)
    echo "  Size: ${JAR_SIZE}"
else
    echo "✗ Failed to create JAR"
    exit 1
fi

# Step 5: Cleanup
echo ""
echo "[5/5] Cleaning up..."
rm -rf "${TEMP_DIR}"

echo ""
echo "=========================================="
echo "✅ Build Complete!"
echo "=========================================="
echo "Installer: ${OUTPUT_DIR}/DDPoker.jar"
echo "Configured for: ${SERVER_HOST}"
echo ""
echo "Players can run with:"
echo "  java -jar DDPoker.jar"
echo "=========================================="
