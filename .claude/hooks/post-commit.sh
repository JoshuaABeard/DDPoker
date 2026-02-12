#!/bin/bash
# Post-commit hook - consume stdin to prevent broken pipe
cat > /dev/null 2>&1
exit 0
