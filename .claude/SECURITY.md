# Security & Privacy — CRITICAL

**Never commit secrets, credentials, private information, or connection strings to the repository.**

## What to Check For

Before committing any file, scan for:
- **Credentials** — Passwords, API keys, tokens, SSH keys, certificates (.pfx, .pem, .key, .p12)
- **Private IP addresses** — 192.168.x.x, 10.x.x.x, 172.16-31.x.x
- **Domain names & hostnames** — Personal domains, company domains, actual server names
- **Email addresses** — Personal emails
- **File paths with usernames** — C:\Users\John\...
- **Database connection strings** — With real hosts/passwords
- **Network details** — MAC addresses, specific network configs
- **PII** — Any personally identifiable information

Replace any found with placeholders (`YOUR_IP_HERE`, `example.com`, `${API_KEY}`, etc.) or environment variables.

## What Must Stay Local

- Connection strings (database, storage, etc.)
- API keys and tokens
- Certificates
- Service principal credentials

## Commit Workflow

1. **List files** being committed
2. **Review each file** for private information
3. **Present findings** to user clearly:
   - "File X is SAFE — no private info"
   - "File Y contains: IP 192.168.1.50 on line 23"
4. **Wait for approval** if any issues found
5. **Proceed with commit** only after user confirms

## Code Security

- Never hardcode credentials in source code
- Check that `.gitignore` covers all sensitive files
- Verify error messages don't leak secrets (sanitize exception messages)
