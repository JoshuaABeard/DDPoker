#!/bin/bash
# ============================================================
# DD Poker - Local Docker Build with Installer Management
#
# This script handles building the Docker image locally by:
# 1. Checking if installers exist in docker/downloads/
# 2. If missing, downloading from latest GitHub Release
# 3. Building the Docker image with docker compose
#
# Usage:
#   ./docker/build-with-installers.sh [version]
#
# Examples:
#   ./docker/build-with-installers.sh          # Use latest release
#   ./docker/build-with-installers.sh v3.3.0   # Use specific version
# ============================================================

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Get script directory (works on Linux, macOS, Git Bash on Windows)
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
REPO_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"
DOWNLOADS_DIR="$SCRIPT_DIR/downloads"

# Version to download (default: latest)
VERSION="${1:-latest}"

echo -e "${BLUE}======================================${NC}"
echo -e "${BLUE}DD Poker Docker Build${NC}"
echo -e "${BLUE}======================================${NC}"
echo ""

# ============================================================
# Step 1: Check for GitHub CLI
# ============================================================
if ! command -v gh &> /dev/null; then
    echo -e "${RED}Error: GitHub CLI (gh) is not installed.${NC}"
    echo ""
    echo "To install:"
    echo "  - macOS:   brew install gh"
    echo "  - Windows: winget install GitHub.cli"
    echo "  - Linux:   https://github.com/cli/cli/blob/trunk/docs/install_linux.md"
    echo ""
    echo "After installing, authenticate with: gh auth login"
    exit 1
fi

# Check if authenticated
if ! gh auth status &> /dev/null; then
    echo -e "${RED}Error: Not authenticated with GitHub CLI.${NC}"
    echo "Run: gh auth login"
    exit 1
fi

# ============================================================
# Step 2: Create downloads directory
# ============================================================
mkdir -p "$DOWNLOADS_DIR"

# ============================================================
# Step 3: Check which installers are missing
# ============================================================
REQUIRED_FILES=(
    "DDPokerCE-3.3.0.msi"
    "DDPokerCE-3.3.0.dmg"
    "ddpoker-ce_3.3.0-1_amd64.deb"
    "ddpoker-ce-3.3.0-1.x86_64.rpm"
)

MISSING_FILES=()
for file in "${REQUIRED_FILES[@]}"; do
    if [ ! -f "$DOWNLOADS_DIR/$file" ]; then
        MISSING_FILES+=("$file")
    fi
done

if [ ${#MISSING_FILES[@]} -eq 0 ]; then
    echo -e "${GREEN}✓ All required installers found in docker/downloads/${NC}"
    echo ""
else
    echo -e "${YELLOW}Missing installers:${NC}"
    for file in "${MISSING_FILES[@]}"; do
        echo "  - $file"
    done
    echo ""

    # ============================================================
    # Step 4: Download missing installers from GitHub Release
    # ============================================================
    echo -e "${BLUE}Downloading installers from GitHub Release...${NC}"

    if [ "$VERSION" = "latest" ]; then
        echo "Using latest release"
        RELEASE_VERSION=$(gh release view --repo JoshuaABeard/DDPoker --json tagName -q .tagName)
    else
        RELEASE_VERSION="$VERSION"
        echo "Using release: $RELEASE_VERSION"
    fi

    echo ""
    echo "Downloading files..."

    # Download to downloads directory
    cd "$DOWNLOADS_DIR"

    # Download only the required files
    for file in "${MISSING_FILES[@]}"; do
        echo "  Downloading $file..."
        if ! gh release download "$RELEASE_VERSION" \
            --repo JoshuaABeard/DDPoker \
            --pattern "$file" \
            --skip-existing; then
            echo -e "${RED}Error: Failed to download $file from release $RELEASE_VERSION${NC}"
            echo ""
            echo "Available releases:"
            gh release list --repo JoshuaABeard/DDPoker --limit 5
            exit 1
        fi
    done

    cd "$REPO_ROOT"
    echo ""
    echo -e "${GREEN}✓ Installers downloaded successfully${NC}"
    echo ""
fi

# ============================================================
# Step 5: Build Docker image
# ============================================================
echo -e "${BLUE}Building Docker image...${NC}"
echo ""

cd "$REPO_ROOT"

# First, ensure Maven build is complete
echo "Building Java project..."
cd code
if ! mvn clean package -DskipTests -q; then
    echo -e "${RED}Error: Maven build failed${NC}"
    exit 1
fi
cd "$REPO_ROOT"

echo ""
echo "Building Docker image with docker compose..."
if ! docker compose -f docker/docker-compose.yml build; then
    echo -e "${RED}Error: Docker build failed${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}======================================${NC}"
echo -e "${GREEN}✓ Docker image built successfully!${NC}"
echo -e "${GREEN}======================================${NC}"
echo ""
echo "To run the container:"
echo "  docker compose -f docker/docker-compose.yml up -d"
echo ""
echo "To view client downloads:"
echo "  http://localhost:8080/downloads/"
echo ""
