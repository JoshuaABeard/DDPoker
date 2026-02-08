# Security Guidelines - CRITICAL

**Never commit secrets, credentials, or connection strings to the repository.**

## What Must Stay Local
- Azure OpenAI endpoints and keys
- Brave Search API keys
- Azure DevOps organization names and PATs
- Connection strings (database, storage, etc.)
- API keys and tokens
- Certificates (.pfx, .pem, .key, .p12)
- Service principal credentials
- Any personally identifiable information (PII)

## How to Handle Configuration

### Development (Local Machine)
Use **User Secrets** for sensitive configuration:
```powershell
# Azure OpenAI (required)
dotnet user-secrets set "AzureOpenAI:Endpoint" "https://your-resource.cognitiveservices.azure.com"
dotnet user-secrets set "AzureOpenAI:ApiKey" "your-api-key-here"

# Brave Search (optional - for web search)
dotnet user-secrets set "BRAVE_API_KEY" "your-brave-api-key"

# Azure DevOps (optional - for AzureDevOps agent)
dotnet user-secrets set "AZURE_DEVOPS_ORG" "your-org"
dotnet user-secrets set "AZURE_DEVOPS_PAT" "your-personal-access-token"
```

User secrets are stored in `~/.microsoft/usersecrets/` and never committed to git.

### Deployment (Production/CI)
Use **Environment Variables**:
```powershell
# PowerShell
$env:AZUREOPENAI__ENDPOINT = "https://your-resource.cognitiveservices.azure.com"
$env:AZUREOPENAI__APIKEY = "your-api-key"
$env:BRAVE_API_KEY = "your-brave-api-key"
```

For production environments, use Azure Key Vault or similar secret management.

### Configuration Files
- ❌ **Never** put secrets in `appsettings.json` or `appsettings.Development.json`
- ✅ Use placeholder values in config files, document in README
- ✅ `.gitignore` already covers `appsettings.*.user.json` patterns

## Before Every Commit
- Review `git diff` to ensure no secrets are included
- Check that `.gitignore` covers all sensitive files
- Never hardcode credentials in source code
- Verify error messages don't leak secrets (sanitize exception messages)
