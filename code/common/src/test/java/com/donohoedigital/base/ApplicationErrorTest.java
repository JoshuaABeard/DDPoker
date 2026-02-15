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
package com.donohoedigital.base;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for ApplicationError - custom RuntimeException for application error
 * handling
 */
class ApplicationErrorTest {

    // =================================================================
    // Constructor Tests
    // =================================================================

    @Test
    void should_CreateError_When_DefaultConstructor() {
        ApplicationError error = new ApplicationError();

        assertThat(error.getErrorCode()).isEqualTo(ErrorCodes.ERROR_CODE_ERROR);
        assertThat(error.getMessage()).isEmpty();
        assertThat(error.getDetails()).isEmpty();
        assertThat(error.getSuggestedResolution()).isNull();
    }

    @Test
    void should_CreateError_When_MessageProvided() {
        ApplicationError error = new ApplicationError("Test error message");

        assertThat(error.getErrorCode()).isEqualTo(ErrorCodes.ERROR_CODE_ERROR);
        assertThat(error.getMessage()).isEqualTo("Test error message");
        assertThat(error.getDetails()).isEmpty();
        assertThat(error.getSuggestedResolution()).isNull();
    }

    @Test
    void should_CreateError_When_ErrorCodeProvided() {
        ApplicationError error = new ApplicationError(ErrorCodes.ERROR_NULL);

        assertThat(error.getErrorCode()).isEqualTo(ErrorCodes.ERROR_NULL);
        assertThat(error.getMessage()).isEmpty();
        assertThat(error.getDetails()).isEmpty();
    }

    @Test
    void should_CreateError_When_AllFieldsProvided() {
        ApplicationError error = new ApplicationError(ErrorCodes.ERROR_VALIDATION, "Message", "Details",
                "Suggested Resolution");

        assertThat(error.getErrorCode()).isEqualTo(ErrorCodes.ERROR_VALIDATION);
        assertThat(error.getMessage()).isEqualTo("Message");
        assertThat(error.getDetails()).isEqualTo("Details");
        assertThat(error.getSuggestedResolution()).isEqualTo("Suggested Resolution");
    }

    @Test
    void should_CreateError_When_ExceptionProvided() {
        Exception cause = new RuntimeException("Cause message");

        ApplicationError error = new ApplicationError(cause);

        assertThat(error.getErrorCode()).isEqualTo(ErrorCodes.ERROR_UNEXPECTED_EXCEPTION);
        assertThat(error.getMessage()).isEqualTo("This software received an unexpected error.");
        assertThat(error.getDetails()).contains("Cause message");
        assertThat(error.getException()).isSameAs(cause);
    }

    @Test
    void should_CreateError_When_MessageAndExceptionProvided() {
        Exception cause = new RuntimeException("Cause message");

        ApplicationError error = new ApplicationError("Custom message", cause);

        assertThat(error.getErrorCode()).isEqualTo(ErrorCodes.ERROR_UNEXPECTED_EXCEPTION);
        assertThat(error.getMessage()).isEqualTo("Custom message");
        assertThat(error.getDetails()).contains("Cause message");
        assertThat(error.getException()).isSameAs(cause);
    }

    @Test
    void should_CreateError_When_ErrorCodeMessageExceptionProvided() {
        Exception cause = new RuntimeException("Cause message");

        ApplicationError error = new ApplicationError(ErrorCodes.ERROR_VALIDATION, "Message", cause,
                "Suggested Resolution");

        assertThat(error.getErrorCode()).isEqualTo(ErrorCodes.ERROR_VALIDATION);
        assertThat(error.getMessage()).isEqualTo("Message");
        assertThat(error.getDetails()).contains("Cause message");
        assertThat(error.getSuggestedResolution()).isEqualTo("Suggested Resolution");
        assertThat(error.getException()).isSameAs(cause);
    }

    @Test
    void should_CreateError_When_ErrorCodeAndException() {
        Exception cause = new RuntimeException("Cause message");

        ApplicationError error = new ApplicationError(ErrorCodes.ERROR_NULL, cause);

        assertThat(error.getErrorCode()).isEqualTo(ErrorCodes.ERROR_NULL);
        assertThat(error.getMessage()).isEqualTo("This software received an unexpected error.");
        assertThat(error.getDetails()).contains("Cause message");
        assertThat(error.getException()).isSameAs(cause);
    }

    @Test
    void should_CreateError_When_ErrorCodeMessageResolution() {
        ApplicationError error = new ApplicationError(ErrorCodes.ERROR_VALIDATION, "Message", "Suggested Resolution");

        assertThat(error.getErrorCode()).isEqualTo(ErrorCodes.ERROR_VALIDATION);
        assertThat(error.getMessage()).isEqualTo("Message");
        assertThat(error.getDetails()).isNotEmpty(); // Contains stack trace
        assertThat(error.getSuggestedResolution()).isEqualTo("Suggested Resolution");
    }

    // =================================================================
    // toString Tests
    // =================================================================

    @Test
    void should_FormatToString_When_AllFieldsPresent() {
        ApplicationError error = new ApplicationError(ErrorCodes.ERROR_VALIDATION, "Message", "Details",
                "Suggested Resolution");

        String result = error.toString();

        assertThat(result).contains("Error #" + ErrorCodes.ERROR_VALIDATION);
        assertThat(result).contains("Message: Message");
        assertThat(result).contains("Details: Details");
        assertThat(result).contains("Suggested Resolution: Suggested Resolution");
    }

    @Test
    void should_FormatToString_When_NoSuggestedResolution() {
        ApplicationError error = new ApplicationError(ErrorCodes.ERROR_VALIDATION, "Message", "Details", null);

        String result = error.toString();

        assertThat(result).contains("Error #" + ErrorCodes.ERROR_VALIDATION);
        assertThat(result).contains("Message: Message");
        assertThat(result).contains("Details: Details");
        assertThat(result).doesNotContain("Suggested Resolution:");
    }

    @Test
    void should_IncludeStackTrace_When_NoException() {
        ApplicationError error = new ApplicationError(ErrorCodes.ERROR_VALIDATION, "Message", "Details", null);

        String result = error.toString();

        assertThat(result).contains("stacktrace:");
        assertThat(result).contains("ApplicationError");
    }

    @Test
    void should_NotIncludeStackTrace_When_ExceptionPresent() {
        Exception cause = new RuntimeException("Cause");
        ApplicationError error = new ApplicationError(ErrorCodes.ERROR_VALIDATION, "Message", cause, "Resolution");

        String result = error.toString();

        assertThat(result).doesNotContain("stacktrace:");
    }

    // =================================================================
    // toStringNoStackTrace Tests
    // =================================================================

    @Test
    void should_FormatToStringNoStackTrace_When_Called() {
        ApplicationError error = new ApplicationError(ErrorCodes.ERROR_VALIDATION, "Message", "Details", "Resolution");

        String result = error.toStringNoStackTrace();

        assertThat(result).contains("Error #" + ErrorCodes.ERROR_VALIDATION);
        assertThat(result).contains("Message: Message");
        assertThat(result).contains("Details: Details");
        assertThat(result).contains("Suggested Resolution: Resolution");
        assertThat(result).doesNotContain("stacktrace");
    }

    @Test
    void should_OmitDetails_When_DetailsIsException() {
        Exception cause = new RuntimeException("Cause");
        ApplicationError error = new ApplicationError(ErrorCodes.ERROR_VALIDATION, "Message", cause, null);

        String result = error.toStringNoStackTrace();

        assertThat(result).contains("Error #" + ErrorCodes.ERROR_VALIDATION);
        assertThat(result).contains("Message: Message");
        assertThat(result).doesNotContain("Details:");
    }

    // =================================================================
    // assertNotNull Tests
    // =================================================================

    @Test
    void should_NotThrow_When_assertNotNullWithNonNull() {
        assertThatCode(() -> ApplicationError.assertNotNull(new Object(), "Test object")).doesNotThrowAnyException();
    }

    @Test
    void should_ThrowApplicationError_When_assertNotNullWithNull() {
        assertThatThrownBy(() -> ApplicationError.assertNotNull(null, "Test object"))
                .isInstanceOf(ApplicationError.class).hasMessageContaining("ASSERTION FAILURE")
                .hasMessageContaining("Unexpected null value").hasMessageContaining("Test object");
    }

    @Test
    void should_ThrowWithInfo_When_assertNotNullWithInfo() {
        assertThatThrownBy(() -> ApplicationError.assertNotNull(null, "Test object", "Extra info"))
                .isInstanceOf(ApplicationError.class).hasMessageContaining("ASSERTION FAILURE")
                .hasMessageContaining("Unexpected null value").hasMessageContaining("Test object");
    }

    @Test
    void should_HandleNullInfo_When_assertNotNullWithNullInfo() {
        assertThatThrownBy(() -> ApplicationError.assertNotNull(null, "Test object", null))
                .isInstanceOf(ApplicationError.class).hasMessageContaining("ASSERTION FAILURE");
    }

    // =================================================================
    // assertNull Tests
    // =================================================================

    @Test
    void should_NotThrow_When_assertNullWithNull() {
        assertThatCode(() -> ApplicationError.assertNull(null, "Test object")).doesNotThrowAnyException();
    }

    @Test
    void should_ThrowApplicationError_When_assertNullWithNonNull() {
        assertThatThrownBy(() -> ApplicationError.assertNull(new Object(), "Test object"))
                .isInstanceOf(ApplicationError.class).hasMessageContaining("ASSERTION FAILURE")
                .hasMessageContaining("Unexpected non-null value").hasMessageContaining("Test object");
    }

    @Test
    void should_ThrowWithInfo_When_assertNullWithInfo() {
        assertThatThrownBy(() -> ApplicationError.assertNull(new Object(), "Test object", "Extra info"))
                .isInstanceOf(ApplicationError.class).hasMessageContaining("ASSERTION FAILURE")
                .hasMessageContaining("Unexpected non-null value").hasMessageContaining("Test object");
    }

    @Test
    void should_HandleNullInfo_When_assertNullWithNullInfo() {
        assertThatThrownBy(() -> ApplicationError.assertNull(new Object(), "Test object", null))
                .isInstanceOf(ApplicationError.class).hasMessageContaining("ASSERTION FAILURE");
    }

    // =================================================================
    // assertTrue Tests
    // =================================================================

    @Test
    void should_NotThrow_When_assertTrueWithTrue() {
        assertThatCode(() -> ApplicationError.assertTrue(true, "Test condition")).doesNotThrowAnyException();
    }

    @Test
    void should_ThrowApplicationError_When_assertTrueWithFalse() {
        assertThatThrownBy(() -> ApplicationError.assertTrue(false, "Test condition"))
                .isInstanceOf(ApplicationError.class).hasMessageContaining("ASSERTION FAILURE")
                .hasMessageContaining("Unexpected condition").hasMessageContaining("Test condition");
    }

    @Test
    void should_ThrowWithInfo_When_assertTrueWithInfo() {
        assertThatThrownBy(() -> ApplicationError.assertTrue(false, "Test condition", "Extra info"))
                .isInstanceOf(ApplicationError.class).hasMessageContaining("ASSERTION FAILURE")
                .hasMessageContaining("Unexpected condition").hasMessageContaining("Test condition");
    }

    @Test
    void should_HandleNullInfo_When_assertTrueWithNullInfo() {
        assertThatThrownBy(() -> ApplicationError.assertTrue(false, "Test condition", null))
                .isInstanceOf(ApplicationError.class).hasMessageContaining("ASSERTION FAILURE");
    }

    // =================================================================
    // fail Tests
    // =================================================================

    @Test
    void should_ThrowApplicationError_When_failCalled() {
        assertThatThrownBy(() -> ApplicationError.fail("Test failure")).isInstanceOf(ApplicationError.class)
                .hasMessageContaining("ASSERTION FAILURE").hasMessageContaining("Unexpected condition")
                .hasMessageContaining("Test failure");
    }

    @Test
    void should_ThrowWithInfo_When_failWithInfo() {
        assertThatThrownBy(() -> ApplicationError.fail("Test failure", "Extra info"))
                .isInstanceOf(ApplicationError.class).hasMessageContaining("ASSERTION FAILURE")
                .hasMessageContaining("Unexpected condition").hasMessageContaining("Test failure");
    }

    @Test
    void should_HandleNullInfo_When_failWithNullInfo() {
        assertThatThrownBy(() -> ApplicationError.fail("Test failure", null)).isInstanceOf(ApplicationError.class)
                .hasMessageContaining("ASSERTION FAILURE");
    }

    // =================================================================
    // warnNotNull Tests
    // =================================================================

    @Test
    void should_NotWarn_When_warnNotNullWithNull() {
        // Should not throw or warn
        assertThatCode(() -> ApplicationError.warnNotNull(null, "Test warning")).doesNotThrowAnyException();
    }

    @Test
    void should_LogWarning_When_warnNotNullWithNonNull() {
        // Should log warning but not throw
        assertThatCode(() -> ApplicationError.warnNotNull(new Object(), "Test warning")).doesNotThrowAnyException();
    }

    // =================================================================
    // Edge Cases
    // =================================================================

    @Test
    void should_HandleNullMessage_When_NoMessageProvided() {
        ApplicationError error = new ApplicationError(ErrorCodes.ERROR_NULL, null, "Details", null);

        assertThat(error.getMessage()).isEmpty();
    }

    @Test
    void should_HandleNullDetails_When_NoDetailsProvided() {
        ApplicationError error = new ApplicationError(ErrorCodes.ERROR_NULL, "Message", (String) null, null);

        assertThat(error.getDetails()).isEmpty();
    }

    @Test
    void should_HandleNullException_When_ExceptionIsNull() {
        ApplicationError error = new ApplicationError(ErrorCodes.ERROR_NULL, "Message", (Throwable) null, "Resolution");

        assertThat(error.getException()).isNull();
        assertThat(error.getDetails()).isEmpty();
    }

    @Test
    void should_BeRuntimeException_When_CheckingHierarchy() {
        ApplicationError error = new ApplicationError();

        assertThat(error).isInstanceOf(RuntimeException.class);
    }
}
