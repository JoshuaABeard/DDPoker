/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file
 * in the root directory of this project.
 *
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images,
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials)
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 *
 * For inquiries regarding commercial licensing of this source code or
 * the use of names, logos, images, text, or other assets, please contact
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.server;

import com.donohoedigital.games.poker.model.OnlineProfile;
import com.donohoedigital.games.poker.service.OnlineProfileService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for OnlineProfileService business logic beyond simple DAO pass-through
 * operations.
 */
@Tag("slow")
@SpringJUnitConfig(locations = {"/app-context-pokerservertests.xml"})
@Transactional
class OnlineProfileServiceTest {
    @Autowired
    private OnlineProfileService service;

    @Test
    @Rollback
    void should_RejectDisallowedNames_When_ValidatingProfileName() {
        // assumes disallowed.txt is read, should be false
        assertThat(service.isNameValid("ddpoker")).isFalse();
        assertThat(service.isNameValid("???")).isFalse();
    }

    @Test
    @Rollback
    void should_PreventDuplicates_When_SavingProfile() {
        OnlineProfile profile = PokerTestData.createOnlineProfile("Dexter");
        assertThat(service.saveOnlineProfile(profile)).isTrue();
        assertThat(profile.getId()).isNotNull();

        // check dup fails
        OnlineProfile dup = PokerTestData.createOnlineProfile("Dexter");
        assertThat(service.saveOnlineProfile(dup)).isFalse();
    }

    @Test
    @Rollback
    void should_AuthenticateProfile_When_CredentialsAreValid() {
        OnlineProfile profile = PokerTestData.createOnlineProfile("Dexter");
        assertThat(service.saveOnlineProfile(profile)).isTrue();
        assertThat(profile.getId()).isNotNull();

        // check auth succeeds with correct credentials
        OnlineProfile dup = PokerTestData.createOnlineProfile("Dexter");
        assertThat(service.authenticateOnlineProfile(dup)).isNotNull();

        // check auth fails without password
        dup.setPassword(null);
        assertThat(service.authenticateOnlineProfile(dup)).isNull();

        // check auth fails with different name
        dup.setName("Zorro");
        assertThat(service.authenticateOnlineProfile(dup)).isNull();
    }

    // ========================================
    // Password Generation
    // ========================================

    @Test
    void should_GenerateNonEmptyPassword_When_GeneratePasswordCalled() {
        String password = service.generatePassword();
        assertThat(password).isNotNull().isNotEmpty();
    }

    @Test
    void should_GenerateDifferentPasswords_When_CalledMultipleTimes() {
        String password1 = service.generatePassword();
        String password2 = service.generatePassword();
        assertThat(password1).isNotEqualTo(password2);
    }

    // ========================================
    // Query Methods - getOnlineProfileById
    // ========================================

    @Test
    @Rollback
    void should_ReturnProfile_When_ValidIdProvided() {
        OnlineProfile profile = PokerTestData.createOnlineProfile("TestUser");
        service.saveOnlineProfile(profile);

        OnlineProfile fetched = service.getOnlineProfileById(profile.getId());
        assertThat(fetched).isNotNull();
        assertThat(fetched.getName()).isEqualTo("TestUser");
    }

    @Test
    @Rollback
    void should_ReturnNull_When_InvalidIdProvided() {
        OnlineProfile fetched = service.getOnlineProfileById(99999L);
        assertThat(fetched).isNull();
    }

    // ========================================
    // Query Methods - getOnlineProfileByName
    // ========================================

    @Test
    @Rollback
    void should_ReturnProfile_When_ValidNameProvided() {
        OnlineProfile profile = PokerTestData.createOnlineProfile("FindMe");
        service.saveOnlineProfile(profile);

        OnlineProfile fetched = service.getOnlineProfileByName("FindMe");
        assertThat(fetched).isNotNull();
        assertThat(fetched.getName()).isEqualTo("FindMe");
    }

    @Test
    @Rollback
    void should_ReturnNull_When_ProfileNameNotFound() {
        OnlineProfile fetched = service.getOnlineProfileByName("NonExistent");
        assertThat(fetched).isNull();
    }

    // ========================================
    // Query Methods - getMatchingOnlineProfilesCount
    // ========================================

    @Test
    @Rollback
    void should_ReturnZeroCount_When_NoMatchingProfiles() {
        int count = service.getMatchingOnlineProfilesCount("NonExistent", null, null, false);
        assertThat(count).isZero();
    }

    @Test
    @Rollback
    void should_ReturnCorrectCount_When_ProfilesMatch() {
        service.saveOnlineProfile(PokerTestData.createOnlineProfile("Alice"));
        service.saveOnlineProfile(PokerTestData.createOnlineProfile("Bob"));
        service.saveOnlineProfile(PokerTestData.createOnlineProfile("AliceInWonderland"));

        int count = service.getMatchingOnlineProfilesCount("Alice", null, null, false);
        assertThat(count).isEqualTo(2);
    }

    // ========================================
    // Query Methods - getMatchingOnlineProfiles
    // ========================================

    @Test
    @Rollback
    void should_ReturnEmptyList_When_NoMatchingProfiles() {
        List<OnlineProfile> profiles = service.getMatchingOnlineProfiles(null, 0, 10, "NonExistent", null, null, false);
        assertThat(profiles).isEmpty();
    }

    @Test
    @Rollback
    void should_ReturnMatchingProfiles_When_NameSearchProvided() {
        service.saveOnlineProfile(PokerTestData.createOnlineProfile("Alice"));
        service.saveOnlineProfile(PokerTestData.createOnlineProfile("Bob"));
        service.saveOnlineProfile(PokerTestData.createOnlineProfile("AliceInWonderland"));

        List<OnlineProfile> profiles = service.getMatchingOnlineProfiles(null, 0, 10, "Alice", null, null, false);
        assertThat(profiles).hasSize(2);
    }

    // ========================================
    // Query Methods - getAllOnlineProfilesForEmail
    // ========================================

    @Test
    @Rollback
    void should_ReturnEmptyList_When_NoProfilesForEmail() {
        List<OnlineProfile> profiles = service.getAllOnlineProfilesForEmail("nobody@example.com", null);
        assertThat(profiles).isEmpty();
    }

    @Test
    @Rollback
    void should_ReturnProfiles_When_EmailMatches() {
        OnlineProfile profile1 = PokerTestData.createOnlineProfile("User1");
        profile1.setEmail("shared@example.com");
        service.saveOnlineProfile(profile1);

        OnlineProfile profile2 = PokerTestData.createOnlineProfile("User2");
        profile2.setEmail("shared@example.com");
        service.saveOnlineProfile(profile2);

        List<OnlineProfile> profiles = service.getAllOnlineProfilesForEmail("shared@example.com", null);
        assertThat(profiles).hasSize(2);
    }

    @Test
    @Rollback
    void should_ExcludeProfile_When_ExcludeNameProvided() {
        OnlineProfile profile1 = PokerTestData.createOnlineProfile("User1");
        profile1.setEmail("shared@example.com");
        service.saveOnlineProfile(profile1);

        OnlineProfile profile2 = PokerTestData.createOnlineProfile("User2");
        profile2.setEmail("shared@example.com");
        service.saveOnlineProfile(profile2);

        List<OnlineProfile> profiles = service.getAllOnlineProfilesForEmail("shared@example.com", "User1");
        assertThat(profiles).hasSize(1);
        assertThat(profiles.get(0).getName()).isEqualTo("User2");
    }

    // ========================================
    // Mutation Methods - retire
    // ========================================

    @Test
    @Rollback
    void should_RetireProfile_When_RetireCalled() {
        OnlineProfile profile = PokerTestData.createOnlineProfile("ToRetire");
        service.saveOnlineProfile(profile);

        service.retire("ToRetire");

        OnlineProfile fetched = service.getOnlineProfileByName("ToRetire");
        assertThat(fetched.isRetired()).isTrue();
    }

    // ========================================
    // Mutation Methods - updateOnlineProfile
    // ========================================

    @Test
    @Rollback
    void should_UpdateProfile_When_ValidProfileProvided() {
        OnlineProfile profile = PokerTestData.createOnlineProfile("Original");
        service.saveOnlineProfile(profile);

        profile.setEmail("updated@example.com");
        OnlineProfile updated = service.updateOnlineProfile(profile);

        assertThat(updated).isNotNull();
        assertThat(updated.getEmail()).isEqualTo("updated@example.com");
    }

    @Test
    @Rollback
    void should_CreateProfile_When_UpdatingNonExistentProfile() {
        OnlineProfile profile = PokerTestData.createOnlineProfile("NonExistent");
        OnlineProfile updated = service.updateOnlineProfile(profile);
        // Service creates the profile if it doesn't exist
        assertThat(updated).isNotNull();
        assertThat(updated.getId()).isNotNull();
    }

    // ========================================
    // Mutation Methods - deleteOnlineProfile
    // ========================================

    @Test
    @Rollback
    void should_DeleteProfile_When_DeleteOnlineProfileCalled() {
        OnlineProfile profile = PokerTestData.createOnlineProfile("ToDelete");
        service.saveOnlineProfile(profile);
        assertThat(profile.getId()).isNotNull();

        service.deleteOnlineProfile(profile);

        OnlineProfile fetched = service.getOnlineProfileById(profile.getId());
        assertThat(fetched).isNull();
    }

    // ========================================
    // Mutation Methods - deleteOnlineProfiles
    // ========================================

    @Test
    @Rollback
    void should_DeleteMultipleProfiles_When_DeleteOnlineProfilesCalled() {
        OnlineProfile profile1 = PokerTestData.createOnlineProfile("Delete1");
        OnlineProfile profile2 = PokerTestData.createOnlineProfile("Delete2");
        service.saveOnlineProfile(profile1);
        service.saveOnlineProfile(profile2);

        List<OnlineProfile> toDelete = new java.util.ArrayList<>();
        toDelete.add(profile1);
        toDelete.add(profile2);

        service.deleteOnlineProfiles(toDelete);

        assertThat(service.getOnlineProfileById(profile1.getId())).isNull();
        assertThat(service.getOnlineProfileById(profile2.getId())).isNull();
    }

    // ========================================
    // Name Validation Edge Cases
    // ========================================

    @Test
    @Rollback
    void should_AcceptValidNames_When_ValidatingProfileName() {
        assertThat(service.isNameValid("ValidUser123")).isTrue();
        assertThat(service.isNameValid("Player_One")).isTrue();
    }

    @Test
    @Rollback
    void should_ThrowException_When_ValidatingNullName() {
        assertThatThrownBy(() -> service.isNameValid(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @Rollback
    void should_RejectEmptyName_When_ValidatingProfileName() {
        assertThat(service.isNameValid("")).isFalse();
    }

    // ========================================
    // Authentication Edge Cases
    // ========================================

    @Test
    @Rollback
    void should_ReturnNull_When_AuthenticatingWithWrongPassword() {
        OnlineProfile profile = PokerTestData.createOnlineProfile("Secure");
        service.saveOnlineProfile(profile);

        OnlineProfile attemptLogin = PokerTestData.createOnlineProfile("Secure");
        attemptLogin.setPassword("WrongPassword");
        assertThat(service.authenticateOnlineProfile(attemptLogin)).isNull();
    }

    @Test
    @Rollback
    void should_ReturnNull_When_AuthenticatingRetiredProfile() {
        OnlineProfile profile = PokerTestData.createOnlineProfile("Retired");
        service.saveOnlineProfile(profile);
        service.retire("Retired");

        OnlineProfile attemptLogin = PokerTestData.createOnlineProfile("Retired");
        assertThat(service.authenticateOnlineProfile(attemptLogin)).isNull();
    }

    // ========================================
    // Query Methods - getOnlineProfileSummariesForEmail
    // ========================================

    @Test
    @Rollback
    void should_ReturnEmptyList_When_NoProfileSummariesForEmail() {
        var summaries = service.getOnlineProfileSummariesForEmail("nobody@example.com");
        assertThat(summaries).isEmpty();
    }

    @Test
    @Rollback
    void should_ReturnSummaries_When_EmailHasProfiles() {
        OnlineProfile profile1 = PokerTestData.createOnlineProfile("SummaryUser1");
        profile1.setEmail("summary@example.com");
        service.saveOnlineProfile(profile1);

        OnlineProfile profile2 = PokerTestData.createOnlineProfile("SummaryUser2");
        profile2.setEmail("summary@example.com");
        service.saveOnlineProfile(profile2);

        var summaries = service.getOnlineProfileSummariesForEmail("summary@example.com");
        assertThat(summaries).hasSize(2);
    }

    // ========================================
    // Additional Edge Cases
    // ========================================

    @Test
    @Rollback
    void should_HandleNullEmail_When_QueryingSummariesForEmail() {
        var summaries = service.getOnlineProfileSummariesForEmail(null);
        assertThat(summaries).isEmpty();
    }

    @Test
    @Rollback
    void should_ReturnProfile_When_UpdatingWithNewEmail() {
        OnlineProfile profile = PokerTestData.createOnlineProfile("UpdateEmail");
        service.saveOnlineProfile(profile);

        // Change email
        profile.setEmail("newemail@example.com");
        OnlineProfile updated = service.updateOnlineProfile(profile);

        assertThat(updated).isNotNull();
        assertThat(updated.getEmail()).isEqualTo("newemail@example.com");

        // Verify by fetching again
        OnlineProfile fetched = service.getOnlineProfileById(profile.getId());
        assertThat(fetched.getEmail()).isEqualTo("newemail@example.com");
    }

    // ========================================
    // Additional Edge Cases and Pagination Tests
    // ========================================

    @Test
    @Rollback
    void should_HandlePagination_When_GettingMatchingProfiles() {
        // Create multiple profiles with similar names
        for (int i = 0; i < 5; i++) {
            service.saveOnlineProfile(PokerTestData.createOnlineProfile("PageUser" + i));
        }

        // Get first page
        List<OnlineProfile> page1 = service.getMatchingOnlineProfiles(null, 0, 2, "PageUser", null, null, false);
        assertThat(page1).hasSizeLessThanOrEqualTo(2);

        // Get second page
        List<OnlineProfile> page2 = service.getMatchingOnlineProfiles(null, 2, 2, "PageUser", null, null, false);
        assertThat(page2).isNotNull();
    }

    @Test
    @Rollback
    void should_IncludeRetiredProfiles_When_FlagIsTrue() {
        OnlineProfile profile1 = PokerTestData.createOnlineProfile("RetireTest1");
        service.saveOnlineProfile(profile1);
        service.retire("RetireTest1");

        OnlineProfile profile2 = PokerTestData.createOnlineProfile("RetireTest2");
        service.saveOnlineProfile(profile2);

        // Without retired
        int countWithout = service.getMatchingOnlineProfilesCount("RetireTest", null, null, false);
        assertThat(countWithout).isEqualTo(1);

        // With retired
        int countWith = service.getMatchingOnlineProfilesCount("RetireTest", null, null, true);
        assertThat(countWith).isEqualTo(2);
    }

    @Test
    @Rollback
    void should_SearchByEmail_When_EmailSearchProvided() {
        OnlineProfile profile1 = PokerTestData.createOnlineProfile("EmailSearch1");
        profile1.setEmail("test@example.com");
        service.saveOnlineProfile(profile1);

        OnlineProfile profile2 = PokerTestData.createOnlineProfile("EmailSearch2");
        profile2.setEmail("test@example.org");
        service.saveOnlineProfile(profile2);

        int count = service.getMatchingOnlineProfilesCount(null, "test@example", null, false);
        assertThat(count).isGreaterThanOrEqualTo(2);
    }

    @Test
    @Rollback
    void should_ReturnMultipleSummaries_When_EmailHasManyProfiles() {
        String sharedEmail = "shared@example.com";
        for (int i = 0; i < 4; i++) {
            OnlineProfile profile = PokerTestData.createOnlineProfile("SharedEmail" + i);
            profile.setEmail(sharedEmail);
            service.saveOnlineProfile(profile);
        }

        var summaries = service.getOnlineProfileSummariesForEmail(sharedEmail);
        assertThat(summaries).hasSizeGreaterThanOrEqualTo(4);
    }

    @Test
    @Rollback
    void should_GenerateUniquePasswords_When_CalledMultipleTimes() {
        java.util.Set<String> passwords = new java.util.HashSet<>();
        for (int i = 0; i < 10; i++) {
            passwords.add(service.generatePassword());
        }

        // All passwords should be unique
        assertThat(passwords).hasSize(10);
    }

    @Test
    @Rollback
    void should_PreservePasswordHash_When_UpdatingProfile() {
        OnlineProfile profile = PokerTestData.createOnlineProfile("PasswordTest");
        String originalPassword = profile.getPassword();
        service.saveOnlineProfile(profile);

        // Update without changing password
        profile.setEmail("newpassword@example.com");
        OnlineProfile updated = service.updateOnlineProfile(profile);

        // Verify can still authenticate with original credentials
        OnlineProfile authTest = PokerTestData.createOnlineProfile("PasswordTest");
        authTest.setPassword(originalPassword);
        OnlineProfile authenticated = service.authenticateOnlineProfile(authTest);
        assertThat(authenticated).isNotNull();
    }

    @Test
    @Rollback
    void should_DeleteAllProfiles_When_ListProvided() {
        // Create multiple profiles
        OnlineProfile profile1 = PokerTestData.createOnlineProfile("BulkDelete1");
        OnlineProfile profile2 = PokerTestData.createOnlineProfile("BulkDelete2");
        OnlineProfile profile3 = PokerTestData.createOnlineProfile("BulkDelete3");

        service.saveOnlineProfile(profile1);
        service.saveOnlineProfile(profile2);
        service.saveOnlineProfile(profile3);

        // Delete all three
        List<OnlineProfile> toDelete = new java.util.ArrayList<>();
        toDelete.add(profile1);
        toDelete.add(profile2);
        toDelete.add(profile3);

        service.deleteOnlineProfiles(toDelete);

        // Verify all deleted
        assertThat(service.getOnlineProfileById(profile1.getId())).isNull();
        assertThat(service.getOnlineProfileById(profile2.getId())).isNull();
        assertThat(service.getOnlineProfileById(profile3.getId())).isNull();
    }
}
