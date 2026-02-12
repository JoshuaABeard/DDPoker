package com.donohoedigital.games.poker.model;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Simple test to verify password hash getter/setter works
 */
public class OnlineProfilePasswordTest {

    @Test
    public void should_StoreAndRetrievePasswordHash_When_SetPasswordHashCalled() {
        // Given: a profile
        OnlineProfile profile = new OnlineProfile("TestUser");

        // When: setting a password hash
        String testHash = "$2a$10$abcdefghijklmnopqrstuv";
        profile.setPasswordHash(testHash);

        // Then: should be able to retrieve it
        assertEquals("getPasswordHash() should return the value set by setPasswordHash()", testHash,
                profile.getPasswordHash());
    }

    @Test
    public void should_ReturnNull_When_PasswordHashNotSet() {
        // Given: a profile with no password hash set
        OnlineProfile profile = new OnlineProfile("TestUser");

        // When: getting password hash
        String hash = profile.getPasswordHash();

        // Then: should be null
        assertNull("getPasswordHash() should return null when not set", hash);
    }
}
