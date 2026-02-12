package com.donohoedigital.games.poker.service.impl;

import com.donohoedigital.games.poker.service.PasswordHashingService;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

/**
 * Implementation of PasswordHashingService using jBCrypt library.
 *
 * Uses BCrypt.hashpw() for hashing with auto-generated salts.
 * Uses BCrypt.checkpw() for password verification.
 */
@Service
public class PasswordHashingServiceImpl implements PasswordHashingService {

    @Override
    public String hashPassword(String plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("Plaintext password cannot be null");
        }
        return BCrypt.hashpw(plaintext, BCrypt.gensalt());
    }

    @Override
    public boolean checkPassword(String plaintext, String hash) {
        // Handle null inputs safely
        if (plaintext == null || hash == null) {
            return false;
        }

        try {
            return BCrypt.checkpw(plaintext, hash);
        } catch (IllegalArgumentException e) {
            // BCrypt.checkpw throws IllegalArgumentException for invalid hash format
            return false;
        }
    }
}
