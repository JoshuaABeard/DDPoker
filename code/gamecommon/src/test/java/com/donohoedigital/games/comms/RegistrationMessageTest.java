/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 Joshua Beard and contributors
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.comms;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for RegistrationMessage - represents user registration information
 */
class RegistrationMessageTest {

    // =================================================================
    // Constructor Tests
    // =================================================================

    @Test
    void should_CreateMessage_When_GivenCategory() {
        RegistrationMessage msg = new RegistrationMessage(EngineMessage.CAT_USER_REG);

        assertThat(msg).isNotNull();
        assertThat(msg.getCategory()).isEqualTo(EngineMessage.CAT_USER_REG);
    }

    @Test
    void should_SetOSAndJava_When_Created() {
        RegistrationMessage msg = new RegistrationMessage(EngineMessage.CAT_USER_REG);

        assertThat(msg.getOS()).isNotNull();
        assertThat(msg.getJava()).isNotNull();
    }

    @Test
    void should_CreateFromEngineMessage_When_CopyConstructorUsed() {
        EngineMessage original = new EngineMessage(EngineMessage.GAME_NOTDEFINED, EngineMessage.PLAYER_NOTDEFINED,
                EngineMessage.CAT_USER_REG);
        original.setString(RegistrationMessage.PARAM_NAME, "TestUser");

        RegistrationMessage msg = new RegistrationMessage(original);

        assertThat(msg.getName()).isEqualTo("TestUser");
    }

    // =================================================================
    // Email Tests
    // =================================================================

    @Test
    void should_ReturnNull_When_EmailNotSet() {
        RegistrationMessage msg = new RegistrationMessage(EngineMessage.CAT_USER_REG);

        assertThat(msg.getEmail()).isNull();
    }

    @Test
    void should_ReturnEmail_When_EmailSet() {
        RegistrationMessage msg = new RegistrationMessage(EngineMessage.CAT_USER_REG);
        msg.setString(RegistrationMessage.PARAM_REG_EMAIL, "user@example.com");

        assertThat(msg.getEmail()).isEqualTo("user@example.com");
    }

    // =================================================================
    // Activation Tests
    // =================================================================

    @Test
    void should_ReturnTrue_When_IsActivationCalledWithNoEmail() {
        RegistrationMessage msg = new RegistrationMessage(EngineMessage.CAT_USER_REG);

        assertThat(msg.isActivation()).isTrue();
    }

    @Test
    void should_ReturnFalse_When_IsActivationCalledWithEmail() {
        RegistrationMessage msg = new RegistrationMessage(EngineMessage.CAT_USER_REG);
        msg.setString(RegistrationMessage.PARAM_REG_EMAIL, "user@example.com");

        assertThat(msg.isActivation()).isFalse();
    }

    // =================================================================
    // Patch Tests
    // =================================================================

    @Test
    void should_ReturnFalse_When_PatchNotSet() {
        RegistrationMessage msg = new RegistrationMessage(EngineMessage.CAT_USER_REG);

        assertThat(msg.isPatch()).isFalse();
    }

    @Test
    void should_ReturnTrue_When_PatchSetToTrue() {
        RegistrationMessage msg = new RegistrationMessage(EngineMessage.CAT_USER_REG);
        msg.setPatch(true);

        assertThat(msg.isPatch()).isTrue();
    }

    @Test
    void should_ReturnFalse_When_PatchSetToFalse() {
        RegistrationMessage msg = new RegistrationMessage(EngineMessage.CAT_USER_REG);
        msg.setPatch(false);

        assertThat(msg.isPatch()).isFalse();
    }

    @Test
    void should_TogglePatch_When_SetMultipleTimes() {
        RegistrationMessage msg = new RegistrationMessage(EngineMessage.CAT_USER_REG);

        msg.setPatch(true);
        assertThat(msg.isPatch()).isTrue();

        msg.setPatch(false);
        assertThat(msg.isPatch()).isFalse();
    }

    // =================================================================
    // Name Tests
    // =================================================================

    @Test
    void should_ReturnNull_When_NameNotSet() {
        RegistrationMessage msg = new RegistrationMessage(EngineMessage.CAT_USER_REG);

        assertThat(msg.getName()).isNull();
    }

    @Test
    void should_ReturnName_When_NameSet() {
        RegistrationMessage msg = new RegistrationMessage(EngineMessage.CAT_USER_REG);
        msg.setString(RegistrationMessage.PARAM_NAME, "John Doe");

        assertThat(msg.getName()).isEqualTo("John Doe");
    }

    // =================================================================
    // Address Tests
    // =================================================================

    @Test
    void should_ReturnNull_When_AddressNotSet() {
        RegistrationMessage msg = new RegistrationMessage(EngineMessage.CAT_USER_REG);

        assertThat(msg.getAddress()).isNull();
    }

    @Test
    void should_ReturnAddress_When_AddressSet() {
        RegistrationMessage msg = new RegistrationMessage(EngineMessage.CAT_USER_REG);
        msg.setString(RegistrationMessage.PARAM_ADDRESS, "123 Main St");

        assertThat(msg.getAddress()).isEqualTo("123 Main St");
    }

    // =================================================================
    // City Tests
    // =================================================================

    @Test
    void should_ReturnNull_When_CityNotSet() {
        RegistrationMessage msg = new RegistrationMessage(EngineMessage.CAT_USER_REG);

        assertThat(msg.getCity()).isNull();
    }

    @Test
    void should_ReturnCity_When_CitySet() {
        RegistrationMessage msg = new RegistrationMessage(EngineMessage.CAT_USER_REG);
        msg.setString(RegistrationMessage.PARAM_CITY, "San Francisco");

        assertThat(msg.getCity()).isEqualTo("San Francisco");
    }

    // =================================================================
    // State Tests
    // =================================================================

    @Test
    void should_ReturnNull_When_StateNotSet() {
        RegistrationMessage msg = new RegistrationMessage(EngineMessage.CAT_USER_REG);

        assertThat(msg.getState()).isNull();
    }

    @Test
    void should_ReturnState_When_StateSet() {
        RegistrationMessage msg = new RegistrationMessage(EngineMessage.CAT_USER_REG);
        msg.setString(RegistrationMessage.PARAM_STATE, "CA");

        assertThat(msg.getState()).isEqualTo("CA");
    }

    // =================================================================
    // Postal Code Tests
    // =================================================================

    @Test
    void should_ReturnNull_When_PostalNotSet() {
        RegistrationMessage msg = new RegistrationMessage(EngineMessage.CAT_USER_REG);

        assertThat(msg.getPostal()).isNull();
    }

    @Test
    void should_ReturnPostal_When_PostalSet() {
        RegistrationMessage msg = new RegistrationMessage(EngineMessage.CAT_USER_REG);
        msg.setString(RegistrationMessage.PARAM_POSTAL, "94102");

        assertThat(msg.getPostal()).isEqualTo("94102");
    }

    // =================================================================
    // Country Tests
    // =================================================================

    @Test
    void should_ReturnNull_When_CountryNotSet() {
        RegistrationMessage msg = new RegistrationMessage(EngineMessage.CAT_USER_REG);

        assertThat(msg.getCountry()).isNull();
    }

    @Test
    void should_ReturnCountry_When_CountrySet() {
        RegistrationMessage msg = new RegistrationMessage(EngineMessage.CAT_USER_REG);
        msg.setString(RegistrationMessage.PARAM_COUNTRY, "USA");

        assertThat(msg.getCountry()).isEqualTo("USA");
    }

    // =================================================================
    // OS and Java Version Tests
    // =================================================================

    @Test
    void should_HaveOSValue_When_MessageCreated() {
        RegistrationMessage msg = new RegistrationMessage(EngineMessage.CAT_USER_REG);

        String os = msg.getOS();
        assertThat(os).isNotNull();
        assertThat(os).isNotEmpty();
    }

    @Test
    void should_HaveJavaVersion_When_MessageCreated() {
        RegistrationMessage msg = new RegistrationMessage(EngineMessage.CAT_USER_REG);

        String java = msg.getJava();
        assertThat(java).isNotNull();
        assertThat(java).isNotEmpty();
    }

    // =================================================================
    // Full Registration Scenario Tests
    // =================================================================

    @Test
    void should_StoreAllFields_When_CompleteRegistration() {
        RegistrationMessage msg = new RegistrationMessage(EngineMessage.CAT_USER_REG);

        msg.setString(RegistrationMessage.PARAM_NAME, "Alice Johnson");
        msg.setString(RegistrationMessage.PARAM_REG_EMAIL, "alice@example.com");
        msg.setString(RegistrationMessage.PARAM_ADDRESS, "456 Oak Ave");
        msg.setString(RegistrationMessage.PARAM_CITY, "Portland");
        msg.setString(RegistrationMessage.PARAM_STATE, "OR");
        msg.setString(RegistrationMessage.PARAM_POSTAL, "97201");
        msg.setString(RegistrationMessage.PARAM_COUNTRY, "USA");
        msg.setPatch(false);

        assertThat(msg.getName()).isEqualTo("Alice Johnson");
        assertThat(msg.getEmail()).isEqualTo("alice@example.com");
        assertThat(msg.getAddress()).isEqualTo("456 Oak Ave");
        assertThat(msg.getCity()).isEqualTo("Portland");
        assertThat(msg.getState()).isEqualTo("OR");
        assertThat(msg.getPostal()).isEqualTo("97201");
        assertThat(msg.getCountry()).isEqualTo("USA");
        assertThat(msg.isPatch()).isFalse();
        assertThat(msg.isActivation()).isFalse();
    }

    @Test
    void should_IndicateActivation_When_EmailNotProvided() {
        RegistrationMessage msg = new RegistrationMessage(EngineMessage.CAT_USER_REG);

        msg.setString(RegistrationMessage.PARAM_NAME, "Bob Smith");
        msg.setPatch(true);

        assertThat(msg.isActivation()).isTrue();
        assertThat(msg.isPatch()).isTrue();
    }

    // =================================================================
    // Edge Case Tests
    // =================================================================

    @Test
    void should_HandleEmptyStrings_When_FieldsSetToEmpty() {
        RegistrationMessage msg = new RegistrationMessage(EngineMessage.CAT_USER_REG);

        msg.setString(RegistrationMessage.PARAM_NAME, "");
        msg.setString(RegistrationMessage.PARAM_REG_EMAIL, "");

        assertThat(msg.getName()).isEqualTo("");
        assertThat(msg.getEmail()).isEqualTo("");
        assertThat(msg.isActivation()).isFalse(); // Empty string is not null
    }

    @Test
    void should_HandleSpecialCharacters_When_InFields() {
        RegistrationMessage msg = new RegistrationMessage(EngineMessage.CAT_USER_REG);

        msg.setString(RegistrationMessage.PARAM_NAME, "O'Brien & Sons");
        msg.setString(RegistrationMessage.PARAM_ADDRESS, "123 Main St., Apt #5");

        assertThat(msg.getName()).isEqualTo("O'Brien & Sons");
        assertThat(msg.getAddress()).isEqualTo("123 Main St., Apt #5");
    }

    @Test
    void should_HandleInternationalCharacters_When_InFields() {
        RegistrationMessage msg = new RegistrationMessage(EngineMessage.CAT_USER_REG);

        msg.setString(RegistrationMessage.PARAM_NAME, "José García");
        msg.setString(RegistrationMessage.PARAM_CITY, "São Paulo");

        assertThat(msg.getName()).isEqualTo("José García");
        assertThat(msg.getCity()).isEqualTo("São Paulo");
    }
}
