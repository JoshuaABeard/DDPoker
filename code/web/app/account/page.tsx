/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client';
import { useState } from 'react';
import { useAuth } from '@/lib/auth/useAuth';
import { authApi } from '@/lib/api';

export default function AccountPage() {
  const { user } = useAuth();

  // Change password section
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passwordStatus, setPasswordStatus] = useState<'idle' | 'success' | 'error'>('idle');
  const [passwordError, setPasswordError] = useState('');

  // Change email section
  const [newEmail, setNewEmail] = useState('');
  const [emailStatus, setEmailStatus] = useState<'idle' | 'sent' | 'error'>('idle');

  // Resend verification section
  const [resendStatus, setResendStatus] = useState<'idle' | 'sent' | 'error'>('idle');

  const handlePasswordChange = async (e: React.FormEvent) => {
    e.preventDefault();
    if (newPassword !== confirmPassword) {
      setPasswordError('Passwords do not match.');
      return;
    }
    if (newPassword.length < 8) {
      setPasswordError('Password must be at least 8 characters.');
      return;
    }
    setPasswordError('');
    try {
      const res = await authApi.changePassword(currentPassword, newPassword);
      setPasswordStatus(res.ok ? 'success' : 'error');
    } catch {
      setPasswordStatus('error');
    }
  };

  const handleEmailChange = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await authApi.changeEmail(newEmail);
      setEmailStatus(res.ok ? 'sent' : 'error');
    } catch {
      setEmailStatus('error');
    }
  };

  const handleResend = async () => {
    try {
      const res = await authApi.resendVerification();
      setResendStatus(res.ok ? 'sent' : 'error');
    } catch {
      setResendStatus('error');
    }
  };

  return (
    <main>
      <h1>Account Settings</h1>

      <section>
        <h2>Change Password</h2>
        <form onSubmit={handlePasswordChange}>
          <input type="password" placeholder="Current password" value={currentPassword} onChange={e => setCurrentPassword(e.target.value)} required />
          <input type="password" placeholder="New password (min 8 chars)" value={newPassword} onChange={e => setNewPassword(e.target.value)} required />
          <input type="password" placeholder="Confirm new password" value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)} required />
          {passwordError && <p>{passwordError}</p>}
          {passwordStatus === 'success' && <p>Password changed successfully.</p>}
          {passwordStatus === 'error' && <p>Failed to change password. Check your current password.</p>}
          <button type="submit">Change password</button>
        </form>
      </section>

      <section>
        <h2>Change Email</h2>
        <p>Current email: {user?.email}</p>
        <form onSubmit={handleEmailChange}>
          <input type="email" placeholder="New email address" value={newEmail} onChange={e => setNewEmail(e.target.value)} required />
          {emailStatus === 'sent' && <p>Confirmation email sent. Check your inbox.</p>}
          {emailStatus === 'error' && <p>Failed to update email. Please try again.</p>}
          <button type="submit">Update email</button>
        </form>
      </section>

      {!user?.emailVerified && (
        <section>
          <h2>Email Verification</h2>
          <p>Your email address is not yet verified.</p>
          {resendStatus === 'sent' && <p>Verification email resent. Check your inbox.</p>}
          {resendStatus === 'error' && <p>Could not resend. Please try again in a few minutes.</p>}
          <button onClick={handleResend} disabled={resendStatus === 'sent'}>Resend verification email</button>
        </section>
      )}
    </main>
  );
}
