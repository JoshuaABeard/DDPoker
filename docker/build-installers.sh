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
# For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
# in the root directory of this project.
#
# For inquiries regarding commercial licensing of this source code or
# the use of names, logos, images, text, or other assets, please contact
# doug [at] donohoe [dot] info.
# =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
#
# Script to build DDPoker native installers with Install4j
#
# Usage: build-installers.sh <server_host> [server_port] [chat_port] [web_port] [output_dir]
#

set -e

# Parse arguments
SERVER_HOST=${1:-localhost}
SERVER_PORT=${2:-8877}
CHAT_PORT=${3:-11886}
WEB_PORT=${4:-8080}
OUTPUT_DIR=${5:-/app/downloads}

echo "=========================================="
echo "Building DDPoker Installers"
echo "=========================================="
echo "Server Configuration:"
echo "  Server: ${SERVER_HOST}:${SERVER_PORT}"
echo "  Chat:   ${SERVER_HOST}:${CHAT_PORT}"
echo "  Web:    http://${SERVER_HOST}:${WEB_PORT}"
echo ""
echo "Output:   ${OUTPUT_DIR}"
echo "=========================================="

# Get repository root
REPO_ROOT=$(pwd)

# Create output directory
mkdir -p "${OUTPUT_DIR}"

# Step 1: Configure server URLs
echo ""
echo "[1/5] Configuring server URLs..."
./docker/configure-server-url.sh "${SERVER_HOST}" "${SERVER_PORT}" "${CHAT_PORT}" "${WEB_PORT}"

# Step 2: Build client with Maven
echo ""
echo "[2/5] Building client JARs with Maven..."
mvn clean package -DskipTests=true -q
if [ $? -ne 0 ]; then
    echo "ERROR: Maven build failed"
    exit 1
fi

# Step 3: Prepare release directory for Install4j
echo ""
echo "[3/5] Preparing release directory..."
RELEASE_DIR="${REPO_ROOT}/installer/release"
rm -rf "${RELEASE_DIR}"
mkdir -p "${RELEASE_DIR}"

# Run buildrelease.pl to copy files to release directory
perl ./tools/bin/buildrelease.pl \
    -releasedir "${RELEASE_DIR}" \
    -devdir "${REPO_ROOT}" \
    -stagingdir "${REPO_ROOT}/code/poker/target/classes" \
    -product poker \
    -clean

if [ $? -ne 0 ]; then
    echo "ERROR: Release directory preparation failed"
    exit 1
fi

echo "Release directory prepared at: ${RELEASE_DIR}"
echo "Contents:"
du -sh "${RELEASE_DIR}"

# Step 4: Build installers with Install4j
echo ""
echo "[4/5] Building native installers with Install4j..."

# Check if Install4j is available
if [ ! -f "/opt/install4j/bin/install4jc" ]; then
    echo "WARNING: Install4j not found at /opt/install4j"
    echo "Installers will NOT be built."
    echo ""
    echo "To enable installer builds:"
    echo "  1. Install Install4j in the Docker image"
    echo "  2. Ensure it's available at /opt/install4j"
    echo ""
    echo "Falling back to fat JAR distribution..."

    # Create fat JAR instead
    cd "${REPO_ROOT}/code/poker"
    mvn package assembly:single -DskipTests=true -q

    # Copy fat JAR to output directory
    cp target/poker-3.0-jar-with-dependencies.jar "${OUTPUT_DIR}/DDPoker.jar"

    echo "✅ Fat JAR created: ${OUTPUT_DIR}/DDPoker.jar"
    echo "   Size: $(du -h ${OUTPUT_DIR}/DDPoker.jar | cut -f1)"

    exit 0
fi

# Run Install4j compiler
INSTALL4J_CONFIG="${REPO_ROOT}/installer/install4j/poker.install4j"
INSTALLER_BUILD_DIR="${REPO_ROOT}/installer/builds"

mkdir -p "${INSTALLER_BUILD_DIR}"

/opt/install4j/bin/install4jc \
    --destination="${INSTALLER_BUILD_DIR}" \
    "${INSTALL4J_CONFIG}"

if [ $? -ne 0 ]; then
    echo "ERROR: Install4j build failed"
    exit 1
fi

# Step 5: Copy installers to output directory
echo ""
echo "[5/5] Copying installers to output directory..."

# Copy all built installers
cp "${INSTALLER_BUILD_DIR}"/*.exe "${OUTPUT_DIR}/" 2>/dev/null || echo "  No Windows installer found"
cp "${INSTALLER_BUILD_DIR}"/*.dmg "${OUTPUT_DIR}/" 2>/dev/null || echo "  No Mac installer found"
cp "${INSTALLER_BUILD_DIR}"/*.sh "${OUTPUT_DIR}/" 2>/dev/null || echo "  No Linux installer found"

# Also create fat JAR as backup
echo ""
echo "Creating portable JAR as backup option..."
cd "${REPO_ROOT}/code/poker"
mvn package assembly:single -DskipTests=true -q
cp target/poker-3.0-jar-with-dependencies.jar "${OUTPUT_DIR}/DDPoker-portable.jar"

echo ""
echo "=========================================="
echo "✅ Build Complete!"
echo "=========================================="
echo "Installers available at: ${OUTPUT_DIR}"
echo ""
ls -lh "${OUTPUT_DIR}"
echo ""
echo "Configuration:"
echo "  Clients will connect to: ${SERVER_HOST}:${SERVER_PORT}"
echo "  Web portal at: http://${SERVER_HOST}:${WEB_PORT}/online"
echo "=========================================="
