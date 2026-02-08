# Security Guidelines - CRITICAL

**Never commit secrets, credentials, or connection strings to the repository.**

## What Must Stay Local
- Connection strings (database, storage, etc.)
- API keys and tokens
- Certificates (.pfx, .pem, .key, .p12)
- Service principal credentials
- Any personally identifiable information (PII)

## Before Every Commit
- Review `git diff` to ensure no secrets are included
- Check that `.gitignore` covers all sensitive files
- Never hardcode credentials in source code
- Verify error messages don't leak secrets (sanitize exception messages)
