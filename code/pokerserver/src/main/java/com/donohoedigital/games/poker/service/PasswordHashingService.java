package com.donohoedigital.games.poker.service;

/**
 * Service for secure password hashing using bcrypt. Replaces the old DES
 * encryption approach with industry-standard bcrypt hashing.
 */
public interface PasswordHashingService {

    /**
     * Hash a plaintext password using bcrypt with auto-generated salt.
     *
     * @param plaintext
     *            the plaintext password to hash
     * @return the bcrypt hash (starting with $2a$)
     */
    String hashPassword(String plaintext);

    /**
     * Check if a plaintext password matches a bcrypt hash.
     *
     * @param plaintext
     *            the plaintext password to check
     * @param hash
     *            the bcrypt hash to check against
     * @return true if the password matches, false otherwise (including null inputs)
     */
    boolean checkPassword(String plaintext, String hash);
}
