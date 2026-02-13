# Password Reset Feature

**Status:** Planning
**Created:** 2026-02-12

## Summary

Implement forgot password functionality that sends the user's password to their registered email address when they provide their username.

## Background

The `/forgot` page currently shows a "Coming Soon in Phase 3" placeholder. Users need a way to recover their password if they forget it. The backend has email infrastructure (`DDPostalService`) but no password reset endpoint.

## Scope

**In Scope:**
- Backend API endpoint: `POST /api/profile/forgot-password`
- Email service integration using existing `DDPostalService`
- Frontend form to collect username
- Success/error message display
- Email template with password

**Out of Scope:**
- Temporary password generation (send existing password)
- Password reset tokens/links (send actual password for simplicity)
- Email server configuration (assume existing SMTP settings work)

## Implementation Steps

### 1. Backend - Add Forgot Password Endpoint

**File:** `code/api/src/main/java/com/donohoedigital/poker/api/controller/ProfileController.java`

Add endpoint:
```java
@PostMapping("/forgot-password")
public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody Map<String, String> request)
```

Logic:
1. Extract username from request body
2. Look up OnlineProfile by username
3. Validate profile exists and has email
4. Send email with password
5. Return success/error response

### 2. Backend - Create Email Service

**File:** `code/api/src/main/java/com/donohoedigital/poker/api/service/EmailService.java` (new)

Create Spring service to wrap `DDPostalService`:
- `sendPasswordResetEmail(String email, String username, String password)`
- Configure email settings from properties
- Handle email sending errors

### 3. Backend - Add DTO

**File:** `code/api/src/main/java/com/donohoedigital/poker/api/dto/ForgotPasswordRequest.java` (new)

```java
public class ForgotPasswordRequest {
    @NotBlank
    private String username;
}
```

### 4. Frontend - Add API Client Method

**File:** `code/web/lib/api.ts`

Add to authApi:
```typescript
forgotPassword: async (username: string): Promise<{ success: boolean, message: string }> => {
  const response = await apiFetch<{ success: boolean, message: string }>(
    '/api/profile/forgot-password',
    {
      method: 'POST',
      body: JSON.stringify({ username }),
    }
  )
  return response.data
}
```

### 5. Frontend - Implement Forgot Password Form

**File:** `code/web/app/forgot/page.tsx`

Replace placeholder with:
- Form with username input field
- Submit button with loading state
- Success message (email sent)
- Error message display
- Link to password help page
- Link back to login

### 6. Frontend - Create Reusable Form Component (Optional)

**File:** `code/web/components/auth/ForgotPasswordForm.tsx` (new, optional)

Extract form logic to reusable component if needed.

## Files Changed

**Backend:**
- `code/api/src/main/java/com/donohoedigital/poker/api/controller/ProfileController.java` - Add endpoint
- `code/api/src/main/java/com/donohoedigital/poker/api/service/EmailService.java` - New email service
- `code/api/src/main/java/com/donohoedigital/poker/api/dto/ForgotPasswordRequest.java` - New DTO
- `code/api/pom.xml` - Add mail module dependency (if not present)

**Frontend:**
- `code/web/lib/api.ts` - Add forgotPassword method
- `code/web/app/forgot/page.tsx` - Implement form

**Total:** ~6 files, estimated ~300 lines

## Testing

**Backend:**
- Unit test for forgot password endpoint
- Test with valid username
- Test with invalid username
- Test with username that has no email
- Test email sending (mock DDPostalService)

**Frontend:**
- Manual testing of form submission
- Test success/error states
- Test validation

## Email Template

Plain text email:
```
Subject: DD Poker - Password Reset

Hello {username},

You requested a password reset for your DD Poker account.

Your password is: {password}

If you did not request this, please contact support immediately.

Thanks,
The DD Poker Team
```

## Security Considerations

- Rate limiting should be added to prevent abuse (future enhancement)
- Email should be sent to registered email only (no email input from user)
- Password is sent in plain text (acceptable for this use case, users should change it after)
- No password reset tokens needed (simpler approach)

## Configuration Required

Assumes email settings exist in property files:
- `settings.email.smtp.host`
- `settings.email.smtp.port`
- `settings.email.from`
- `settings.email.username`
- `settings.email.password`

If these don't exist, need to add them or use defaults.

## Open Questions

1. Should we log password reset requests for security audit?
2. Should we send a confirmation email after password is sent?
3. Rate limiting - how many requests per hour?

## Implementation Order

1. Backend email service (test independently)
2. Backend endpoint (test with Postman)
3. Frontend API client
4. Frontend form
5. Integration testing
6. Update placeholder page message
