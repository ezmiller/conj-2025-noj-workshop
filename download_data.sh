#!/bin/bash
set -e

RELEASE_TAG="v1-data"
DATA_DIR="notebooks/data"
DOWNLOAD_RAW=false

# Parse arguments
if [[ "$1" == "--raw" ]]; then
    DOWNLOAD_RAW=true
fi

# Get repository path from git remote
REPO_URL=$(git config --get remote.origin.url 2>/dev/null || echo "")
if [ -z "$REPO_URL" ]; then
    echo "Error: No git remote 'origin' found."
    echo "Run: git remote add origin https://github.com/username/repo.git"
    exit 1
fi

# Convert git URL to repo path
REPO_PATH=$(echo "$REPO_URL" | sed 's|.*github.com[:/]||; s/.git$//')

mkdir -p "$DATA_DIR"

# Files to download
if [ "$DOWNLOAD_RAW" = true ]; then
    FILES=(
        "Service_Requests_311.csv"
        "clt-311-workshop.csv"
    )
else
    FILES=(
        "clt-311-workshop.csv"
    )
fi

for FILE in "${FILES[@]}"; do
    OUTPUT_PATH="$DATA_DIR/$FILE"

    if [ -f "$OUTPUT_PATH" ]; then
        echo "File already exists: $OUTPUT_PATH"
        echo "Delete it first if you want to re-download."
        exit 1
    fi

    echo "Downloading $FILE..."
    URL="https://github.com/$REPO_PATH/releases/download/$RELEASE_TAG/$FILE"
    curl -L -o "$OUTPUT_PATH" "$URL"
done

echo "Download complete!"
