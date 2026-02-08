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
# Script to configure client with server URL for custom deployments
#
# Usage: configure-server-url.sh <server_host> [server_port] [chat_port] [web_port]
#

set -e

# Parse arguments
SERVER_HOST=${1:-localhost}
SERVER_PORT=${2:-8877}
CHAT_PORT=${3:-11886}
WEB_PORT=${4:-8080}

echo "=========================================="
echo "Configuring DDPoker client for:"
echo "  Server: ${SERVER_HOST}:${SERVER_PORT}"
echo "  Chat:   ${SERVER_HOST}:${CHAT_PORT}"
echo "  Web:    http://${SERVER_HOST}:${WEB_PORT}"
echo "=========================================="

# Paths
CLIENT_PROPERTIES="code/poker/src/main/resources/config/poker/client.properties"
GAMEDEF_XML="code/poker/src/main/resources/config/poker/gamedef.xml"

# Check if files exist
if [ ! -f "$CLIENT_PROPERTIES" ]; then
    echo "ERROR: client.properties not found at $CLIENT_PROPERTIES"
    exit 1
fi

if [ ! -f "$GAMEDEF_XML" ]; then
    echo "ERROR: gamedef.xml not found at $GAMEDEF_XML"
    exit 1
fi

# Backup original files if not already backed up
if [ ! -f "${CLIENT_PROPERTIES}.original" ]; then
    echo "Backing up original client.properties..."
    cp "$CLIENT_PROPERTIES" "${CLIENT_PROPERTIES}.original"
fi

if [ ! -f "${GAMEDEF_XML}.original" ]; then
    echo "Backing up original gamedef.xml..."
    cp "$GAMEDEF_XML" "${GAMEDEF_XML}.original"
fi

# Update client.properties - online server
echo "Updating online server URL in client.properties..."
sed -i "s|option.onlineserver.default=.*|option.onlineserver.default= ${SERVER_HOST}:${SERVER_PORT}|g" "$CLIENT_PROPERTIES"

# Update client.properties - chat server
echo "Updating chat server URL in client.properties..."
sed -i "s|option.onlinechat.default=.*|option.onlinechat.default=   ${SERVER_HOST}:${CHAT_PORT}|g" "$CLIENT_PROPERTIES"

# Update gamedef.xml - online website URL
echo "Updating web portal URL in gamedef.xml..."
sed -i "s|<param name=\"url\" strvalue=\"http://www.ddpoker.com/online\"/>|<param name=\"url\" strvalue=\"http://${SERVER_HOST}:${WEB_PORT}/online\"/>|g" "$GAMEDEF_XML"

# Also update forums URL (less important, but for completeness)
sed -i "s|<param name=\"url\" strvalue=\"http://www.ddpoker.com/forums\"/>|<param name=\"url\" strvalue=\"http://${SERVER_HOST}:${WEB_PORT}/forums\"/>|g" "$GAMEDEF_XML"

echo "✅ Configuration complete!"
echo ""
echo "Verification:"
echo "-------------"
echo "Server URL:"
grep "option.onlineserver.default=" "$CLIENT_PROPERTIES"
echo ""
echo "Chat URL:"
grep "option.onlinechat.default=" "$CLIENT_PROPERTIES"
echo ""
echo "Web Portal URL:"
grep "LaunchOnlineWebsite" -A1 "$GAMEDEF_XML" | grep "param name=\"url\""
echo ""
