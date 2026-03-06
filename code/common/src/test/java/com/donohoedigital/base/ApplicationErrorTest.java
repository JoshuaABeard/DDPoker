/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This file is part of DD Poker, originally created by Doug Donohoe.
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
package com.donohoedigital.base;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for ApplicationError — custom RuntimeException with error codes,
 * details, and assertion helpers.
 */
class ApplicationErrorTest {

    // -----------------------------------------------------------------------
    // Default constructor
    // -----------------------------------------------------------------------

    @Test
    void should_UseDefaultErrorCode_When_DefaultConstructor() {
        ApplicationError error = new ApplicationError();
        assertThat(error.getErrorCode()).isEqualTo(ErrorCodes.ERROR_CODE_ERROR);
        assertThat(error.getMessage()).isEmpty();
        assertThat(error.getDetails()).isEmpty();
        assertThat(error.getSuggestedResolution()).isNull();
        assertThat(error.getException()).isNull();
    }

    // -----------------------------------------------------------------------
    // String constructor
    // -----------------------------------------------------------------------

    @Test
    void should_StoreMessage_When_StringConstructor() {
        ApplicationError error = new ApplicationError("something broke");
        assertThat(error.getErrorCode()).isEqualTo(ErrorCodes.ERROR_CODE_ERROR);
        assertThat(error.getMessage()).isEqualTo("something broke");
        assertThat(error.getDetails()).isEmpty();
        assertThat(error.getException()).isNull();
    }

    // -----------------------------------------------------------------------
    // Full constructor (code, message, details, resolution)
    // -----------------------------------------------------------------------

    @Test
    void should_StoreAllFields_When_FullConstructor() {
        ApplicationError error = new ApplicationError(ErrorCodes.ERROR_VALIDATION, "bad input", "field X is null",
                "provide field X");

        assertThat(error.getErrorCode()).isEqualTo(ErrorCodes.ERROR_VALIDATION);
        assertThat(error.getMessage()).isEqualTo("bad input");
        assertThat(error.getDetails()).isEqualTo("field X is null");
        assertThat(error.getSuggestedResolution()).isEqualTo("provide field X");
        assertThat(error.getException()).isNull();
    }

    @Test
    void should_DefaultNullsToEmpty_When_FullConstructorWithNulls() {
        ApplicationError error = new ApplicationError(ErrorCodes.ERROR_VALIDATION, null, (String) null, null);
        assertThat(error.getMessage()).isEmpty();
        assertThat(error.getDetails()).isEmpty();
        assertThat(error.getSuggestedResolution()).isNull();
    }

    // -----------------------------------------------------------------------
    // Exception wrapping constructors
    // -----------------------------------------------------------------------

    @Test
    void should_WrapException_When_CodeMessageExceptionResolution() {
        RuntimeException cause = new RuntimeException("root cause");
        ApplicationError error = new ApplicationError(ErrorCodes.ERROR_SERVER_IO, "IO failed", cause, "retry later");

        assertThat(error.getErrorCode()).isEqualTo(ErrorCodes.ERROR_SERVER_IO);
        assertThat(error.getMessage()).isEqualTo("IO failed");
        assertThat(error.getDetails()).contains("root cause");
        assertThat(error.getSuggestedResolution()).isEqualTo("retry later");
        assertThat(error.getException()).isSameAs(cause);
    }

    @Test
    void should_WrapException_When_MessageAndException() {
        IllegalStateException cause = new IllegalStateException("bad state");
        ApplicationError error = new ApplicationError("wrapper msg", cause);

        assertThat(error.getErrorCode()).isEqualTo(ErrorCodes.ERROR_UNEXPECTED_EXCEPTION);
        assertThat(error.getMessage()).isEqualTo("wrapper msg");
        assertThat(error.getDetails()).contains("bad state");
        assertThat(error.getException()).isSameAs(cause);
    }

    @Test
    void should_WrapException_When_ExceptionOnly() {
        NullPointerException cause = new NullPointerException("npe");
        ApplicationError error = new ApplicationError(cause);

        assertThat(error.getErrorCode()).isEqualTo(ErrorCodes.ERROR_UNEXPECTED_EXCEPTION);
        assertThat(error.getMessage()).contains("unexpected error");
        assertThat(error.getDetails()).contains("npe");
        assertThat(error.getException()).isSameAs(cause);
    }

    @Test
    void should_WrapException_When_ErrorCodeAndException() {
        Exception cause = new Exception("oops");
        ApplicationError error = new ApplicationError(ErrorCodes.ERROR_CLASS_NOT_FOUND, cause);

        assertThat(error.getErrorCode()).isEqualTo(ErrorCodes.ERROR_CLASS_NOT_FOUND);
        assertThat(error.getMessage()).contains("unexpected error");
        assertThat(error.getDetails()).contains("oops");
        assertThat(error.getException()).isSameAs(cause);
    }

    // -----------------------------------------------------------------------
    // Error code only constructor
    // -----------------------------------------------------------------------

    @Test
    void should_StoreCodeOnly_When_ErrorCodeConstructor() {
        ApplicationError error = new ApplicationError(ErrorCodes.ERROR_UNSUPPORTED);
        assertThat(error.getErrorCode()).isEqualTo(ErrorCodes.ERROR_UNSUPPORTED);
        assertThat(error.getMessage()).isEmpty();
        assertThat(error.getDetails()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // Code + message + resolution constructor (details from stack trace)
    // -----------------------------------------------------------------------

    @Test
    void should_PopulateDetailsFromStackTrace_When_CodeMessageResolution() {
        ApplicationError error = new ApplicationError(ErrorCodes.ERROR_VALIDATION, "validation failed", "check input");

        assertThat(error.getErrorCode()).isEqualTo(ErrorCodes.ERROR_VALIDATION);
        assertThat(error.getMessage()).isEqualTo("validation failed");
        assertThat(error.getSuggestedResolution()).isEqualTo("check input");
        // Details should contain stack trace info from 'this'
        assertThat(error.getDetails()).contains("ApplicationError");
    }

    // -----------------------------------------------------------------------
    // toString
    // -----------------------------------------------------------------------

    @Test
    void should_IncludeAllFields_When_ToStringCalled() {
        ApplicationError error = new ApplicationError(ErrorCodes.ERROR_VALIDATION, "bad input", "field X", "fix it");

        String s = error.toString();
        assertThat(s).contains("Error #" + ErrorCodes.ERROR_VALIDATION);
        assertThat(s).contains("Message: bad input");
        assertThat(s).contains("Details: field X");
        assertThat(s).contains("Suggested Resolution: fix it");
    }

    @Test
    void should_OmitResolution_When_ResolutionIsNull() {
        ApplicationError error = new ApplicationError(ErrorCodes.ERROR_VALIDATION, "bad input", "field X", null);

        String s = error.toString();
        assertThat(s).doesNotContain("Suggested Resolution");
    }

    @Test
    void should_IncludeStackTrace_When_DetailsNotFromException() {
        ApplicationError error = new ApplicationError(ErrorCodes.ERROR_VALIDATION, "msg", "details", null);

        String s = error.toString();
        assertThat(s).contains("stacktrace:");
    }

    @Test
    void should_OmitExtraStackTrace_When_DetailsAreFromException() {
        ApplicationError error = new ApplicationError(new RuntimeException("cause"));

        String s = error.toString();
        // When details come from an exception, no extra stacktrace appended
        assertThat(s).doesNotContain("stacktrace:");
    }

    // -----------------------------------------------------------------------
    // toStringNoStackTrace
    // -----------------------------------------------------------------------

    @Test
    void should_IncludeFieldsWithoutStackTrace_When_ToStringNoStackTraceCalled() {
        ApplicationError error = new ApplicationError(ErrorCodes.ERROR_VALIDATION, "bad input", "field X", "fix it");

        String s = error.toStringNoStackTrace();
        assertThat(s).contains("Error #" + ErrorCodes.ERROR_VALIDATION);
        assertThat(s).contains("Message: bad input");
        assertThat(s).contains("Details: field X");
        assertThat(s).contains("Suggested Resolution: fix it");
        assertThat(s).doesNotContain("stacktrace:");
    }

    @Test
    void should_OmitDetails_When_ToStringNoStackTraceAndDetailsFromException() {
        ApplicationError error = new ApplicationError(new RuntimeException("cause"));

        String s = error.toStringNoStackTrace();
        assertThat(s).contains("Error #" + ErrorCodes.ERROR_UNEXPECTED_EXCEPTION);
        // Details section is suppressed when it comes from an exception
        assertThat(s).doesNotContain("Details:");
    }

    // -----------------------------------------------------------------------
    // assertNotNull
    // -----------------------------------------------------------------------

    @Test
    void should_NotThrow_When_AssertNotNullWithNonNull() {
        assertThatCode(() -> ApplicationError.assertNotNull("value", "test")).doesNotThrowAnyException();
    }

    @Test
    void should_Throw_When_AssertNotNullWithNull() {
        assertThatThrownBy(() -> ApplicationError.assertNotNull(null, "myField")).isInstanceOf(ApplicationError.class)
                .hasMessageContaining("null value").hasMessageContaining("myField");
    }

    @Test
    void should_NotThrow_When_AssertNotNullWithInfoAndNonNull() {
        assertThatCode(() -> ApplicationError.assertNotNull("value", "test", "extra info")).doesNotThrowAnyException();
    }

    @Test
    void should_ThrowWithDetails_When_AssertNotNullWithInfoAndNull() {
        assertThatThrownBy(() -> ApplicationError.assertNotNull(null, "myField", "context"))
                .isInstanceOf(ApplicationError.class).satisfies(e -> {
                    ApplicationError ae = (ApplicationError) e;
                    assertThat(ae.getErrorCode()).isEqualTo(ErrorCodes.ERROR_NULL);
                    assertThat(ae.getDetails()).isEqualTo("context");
                });
    }

    @Test
    void should_UseEmptyDetails_When_AssertNotNullWithNullInfo() {
        assertThatThrownBy(() -> ApplicationError.assertNotNull(null, "field", null))
                .isInstanceOf(ApplicationError.class).satisfies(e -> {
                    ApplicationError ae = (ApplicationError) e;
                    assertThat(ae.getDetails()).isEmpty();
                });
    }

    // -----------------------------------------------------------------------
    // assertNull
    // -----------------------------------------------------------------------

    @Test
    void should_NotThrow_When_AssertNullWithNull() {
        assertThatCode(() -> ApplicationError.assertNull(null, "test")).doesNotThrowAnyException();
    }

    @Test
    void should_Throw_When_AssertNullWithNonNull() {
        assertThatThrownBy(() -> ApplicationError.assertNull("oops", "myField")).isInstanceOf(ApplicationError.class)
                .hasMessageContaining("non-null").hasMessageContaining("myField");
    }

    @Test
    void should_NotThrow_When_AssertNullWithInfoAndNull() {
        assertThatCode(() -> ApplicationError.assertNull(null, "test", "extra")).doesNotThrowAnyException();
    }

    @Test
    void should_ThrowWithDetails_When_AssertNullWithInfoAndNonNull() {
        assertThatThrownBy(() -> ApplicationError.assertNull("oops", "myField", "context"))
                .isInstanceOf(ApplicationError.class).satisfies(e -> {
                    ApplicationError ae = (ApplicationError) e;
                    assertThat(ae.getErrorCode()).isEqualTo(ErrorCodes.ERROR_NULL);
                    assertThat(ae.getDetails()).isEqualTo("context");
                });
    }

    // -----------------------------------------------------------------------
    // assertTrue
    // -----------------------------------------------------------------------

    @Test
    void should_NotThrow_When_AssertTrueWithTrue() {
        assertThatCode(() -> ApplicationError.assertTrue(true, "condition")).doesNotThrowAnyException();
    }

    @Test
    void should_Throw_When_AssertTrueWithFalse() {
        assertThatThrownBy(() -> ApplicationError.assertTrue(false, "myCondition")).isInstanceOf(ApplicationError.class)
                .hasMessageContaining("myCondition");
    }

    @Test
    void should_NotThrow_When_AssertTrueWithInfoAndTrue() {
        assertThatCode(() -> ApplicationError.assertTrue(true, "condition", "info")).doesNotThrowAnyException();
    }

    @Test
    void should_ThrowWithDetails_When_AssertTrueWithInfoAndFalse() {
        assertThatThrownBy(() -> ApplicationError.assertTrue(false, "cond", "detail"))
                .isInstanceOf(ApplicationError.class).satisfies(e -> {
                    ApplicationError ae = (ApplicationError) e;
                    assertThat(ae.getErrorCode()).isEqualTo(ErrorCodes.ERROR_ASSERTION_FAILED);
                    assertThat(ae.getDetails()).isEqualTo("detail");
                });
    }

    // -----------------------------------------------------------------------
    // fail
    // -----------------------------------------------------------------------

    @Test
    void should_AlwaysThrow_When_FailCalled() {
        assertThatThrownBy(() -> ApplicationError.fail("something")).isInstanceOf(ApplicationError.class)
                .hasMessageContaining("something");
    }

    @Test
    void should_IncludeInfo_When_FailCalledWithInfo() {
        assertThatThrownBy(() -> ApplicationError.fail("something", "info")).isInstanceOf(ApplicationError.class)
                .satisfies(e -> {
                    ApplicationError ae = (ApplicationError) e;
                    assertThat(ae.getErrorCode()).isEqualTo(ErrorCodes.ERROR_ASSERTION_FAILED);
                    assertThat(ae.getDetails()).isEqualTo("info");
                });
    }

    @Test
    void should_UseEmptyDetails_When_FailCalledWithNullInfo() {
        assertThatThrownBy(() -> ApplicationError.fail("something", null)).isInstanceOf(ApplicationError.class)
                .satisfies(e -> {
                    ApplicationError ae = (ApplicationError) e;
                    assertThat(ae.getDetails()).isEmpty();
                });
    }

    // -----------------------------------------------------------------------
    // warnNotNull (no exception, just logs)
    // -----------------------------------------------------------------------

    @Test
    void should_NotThrow_When_WarnNotNullWithNull() {
        assertThatCode(() -> ApplicationError.warnNotNull(null, "should not log")).doesNotThrowAnyException();
    }

    @Test
    void should_NotThrow_When_WarnNotNullWithNonNull() {
        // warnNotNull only logs, never throws
        assertThatCode(() -> ApplicationError.warnNotNull("present", "logging a warning")).doesNotThrowAnyException();
    }

    // -----------------------------------------------------------------------
    // RuntimeException inheritance
    // -----------------------------------------------------------------------

    @Test
    void should_BeRuntimeException_When_Created() {
        ApplicationError error = new ApplicationError("test");
        assertThat(error).isInstanceOf(RuntimeException.class);
    }

    @Test
    void should_BeCatchableAsRuntimeException_When_Thrown() {
        assertThatThrownBy(() -> {
            throw new ApplicationError("thrown");
        }).isInstanceOf(RuntimeException.class).isInstanceOf(ApplicationError.class);
    }
}
