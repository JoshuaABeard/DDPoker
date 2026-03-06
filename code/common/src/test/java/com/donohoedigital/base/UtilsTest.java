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
package com.donohoedigital.base;

import org.junit.jupiter.api.*;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class UtilsTest {

    // ========== OS Detection ==========

    @Test
    void testIsOs() {
        assertThat(Utils.isLinux("linux")).isTrue();
        assertThat(Utils.isLinux("LINUX")).isTrue();
        assertThat(Utils.isMacOS("mac os x")).isTrue();
        assertThat(Utils.isMacOS("MAC OS X")).isTrue();
        assertThat(Utils.isWindows("Windows")).isTrue();
        assertThat(Utils.isWindows("windows")).isTrue();

        assertThat(Utils.isLinux("mac os x")).isFalse();
        assertThat(Utils.isLinux("windows")).isFalse();
        assertThat(Utils.isMacOS("linux")).isFalse();
        assertThat(Utils.isMacOS("windows")).isFalse();
        assertThat(Utils.isWindows("linux")).isFalse();
        assertThat(Utils.isWindows("mac os x")).isFalse();
    }

    // ========== joinWithDelimiter ==========

    @Test
    void testJoin() {
        assertThat(Utils.joinWithDelimiter(" | ", "a", null, "b")).isEqualTo("a | b");
    }

    @Test
    void should_ReturnEmpty_When_AllNullOrEmpty() {
        assertThat(Utils.joinWithDelimiter(",", null, "", null)).isEmpty();
    }

    @Test
    void should_ReturnSingleValue_When_OnlyOneNonEmpty() {
        assertThat(Utils.joinWithDelimiter(",", null, "only", "")).isEqualTo("only");
    }

    // ========== encodeHTML ==========

    @Test
    void should_ReturnNull_When_EncodeHtmlNull() {
        assertThat(Utils.encodeHTML(null)).isNull();
    }

    @Test
    void should_ReturnSameString_When_NoSpecialChars() {
        assertThat(Utils.encodeHTML("hello world")).isEqualTo("hello world");
    }

    @Test
    void should_EncodeAmpersand_When_Present() {
        assertThat(Utils.encodeHTML("a&b")).isEqualTo("a&amp;b");
    }

    @Test
    void should_EncodeLessThanAndGreaterThan_When_Present() {
        assertThat(Utils.encodeHTML("<tag>")).isEqualTo("&lt;tag&gt;");
    }

    @Test
    void should_EncodeSlash_When_SlashFlagTrue() {
        assertThat(Utils.encodeHTML("a/b", true)).isEqualTo("a&#47;b");
    }

    @Test
    void should_NotEncodeSlash_When_SlashFlagFalse() {
        assertThat(Utils.encodeHTML("a/b", false)).isEqualTo("a/b");
    }

    @Test
    void should_EncodeNewlineAsBR_When_DefaultHtml() {
        assertThat(Utils.encodeHTML("line1\nline2")).isEqualTo("line1<BR>line2");
    }

    @Test
    void should_EncodeWhitespace_When_WhitespaceFlagTrue() {
        String result = Utils.encodeHTML("a b", true, true);
        assertThat(result).isEqualTo("a&nbsp;b");
    }

    @Test
    void should_EncodeTab_When_WhitespaceFlagTrue() {
        String result = Utils.encodeHTML("a\tb", true, true);
        assertThat(result).isEqualTo("a&nbsp;&nbsp;&nbsp;&nbsp;b");
    }

    // ========== encodeHTMLWhitespace ==========

    @Test
    void should_EncodeSpacesAndTabs_When_CallingEncodeHTMLWhitespace() {
        String result = Utils.encodeHTMLWhitespace("a b\tc");
        assertThat(result).contains("&nbsp;");
    }

    // ========== encodeXML ==========

    @Test
    void should_EncodeXmlSpecialChars_When_Present() {
        assertThat(Utils.encodeXML("a&b<c>d\"e")).isEqualTo("a&amp;b&lt;c&gt;d&quot;e");
    }

    @Test
    void should_ReturnNull_When_EncodeXmlNull() {
        assertThat(Utils.encodeXML(null)).isNull();
    }

    @Test
    void should_NotEncodeSlash_When_EncodeXml() {
        assertThat(Utils.encodeXML("a/b")).isEqualTo("a/b");
    }

    // ========== encodeCSV ==========

    @Test
    void should_ReturnSameString_When_CsvNoSpecialChars() {
        assertThat(Utils.encodeCSV("hello")).isEqualTo("hello");
    }

    @Test
    void should_WrapInQuotes_When_CsvContainsComma() {
        assertThat(Utils.encodeCSV("a,b")).isEqualTo("\"a,b\"");
    }

    @Test
    void should_DoubleQuotes_When_CsvContainsQuotes() {
        assertThat(Utils.encodeCSV("say \"hi\"")).isEqualTo("say \"\"hi\"\"");
    }

    // ========== encodeJavascript ==========

    @Test
    void should_EncodeSingleQuote_When_Javascript() {
        assertThat(Utils.encodeJavascript("it's")).isEqualTo("it&acute;s");
    }

    @Test
    void should_EncodeDoubleQuote_When_Javascript() {
        assertThat(Utils.encodeJavascript("say \"hi\"")).isEqualTo("say &quot;hi&quot;");
    }

    @Test
    void should_EncodeAmpersand_When_Javascript() {
        assertThat(Utils.encodeJavascript("a&b")).isEqualTo("a&amp;b");
    }

    // ========== isEmpty ==========

    @Test
    void should_ReturnTrue_When_Null() {
        assertThat(Utils.isEmpty(null)).isTrue();
    }

    @Test
    void should_ReturnTrue_When_EmptyString() {
        assertThat(Utils.isEmpty("")).isTrue();
    }

    @Test
    void should_ReturnTrue_When_WhitespaceOnly() {
        assertThat(Utils.isEmpty("   ")).isTrue();
    }

    @Test
    void should_ReturnFalse_When_NonEmpty() {
        assertThat(Utils.isEmpty("hello")).isFalse();
    }

    @Test
    void should_ReturnFalse_When_CharSequenceWithContent() {
        assertThat(Utils.isEmpty(new StringBuilder("x"))).isFalse();
    }

    // ========== replace ==========

    @Test
    void should_ReplaceOccurrence_When_PatternFound() {
        assertThat(Utils.replace("hello world", "world", "there")).isEqualTo("hello there");
    }

    @Test
    void should_ReturnOriginal_When_PatternNotFound() {
        assertThat(Utils.replace("hello", "xyz", "abc")).isEqualTo("hello");
    }

    @Test
    void should_ReplaceMultiple_When_MultipleMatches() {
        assertThat(Utils.replace("aaa", "a", "b")).isEqualTo("bbb");
    }

    @Test
    void should_SupportRegex_When_PatternIsRegex() {
        assertThat(Utils.replace("abc123def", "\\d+", "NUM")).isEqualTo("abcNUMdef");
    }

    // ========== toString(String[]) and toString(List) ==========

    @Test
    void should_JoinArray_When_StringArray() {
        assertThat(Utils.toString(new String[]{"a", "b", "c"})).isEqualTo("a, b, c");
    }

    @Test
    void should_ReturnEmpty_When_NullArray() {
        assertThat(Utils.toString((String[]) null)).isEmpty();
    }

    @Test
    void should_ReturnSingle_When_SingleElementArray() {
        assertThat(Utils.toString(new String[]{"only"})).isEqualTo("only");
    }

    @Test
    void should_UseCustomDelimiter_When_Provided() {
        assertThat(Utils.toString(new String[]{"a", "b"}, " | ")).isEqualTo("a | b");
    }

    @Test
    void should_JoinList_When_ListProvided() {
        List<String> list = List.of("x", "y", "z");
        assertThat(Utils.toString(list)).isEqualTo("x, y, z");
    }

    @Test
    void should_ReturnEmpty_When_NullList() {
        assertThat(Utils.toString((List<?>) null)).isEmpty();
    }

    @Test
    void should_StartAtIndex_When_IndexProvided() {
        List<String> list = List.of("a", "b", "c");
        assertThat(Utils.toString(list, 1)).isEqualTo("b, c");
    }

    // ========== getPrintableString ==========

    @Test
    void should_ReturnSameString_When_AllPrintable() {
        assertThat(Utils.getPrintableString("hello", 100)).isEqualTo("hello");
    }

    @Test
    void should_Truncate_When_ExceedsLimit() {
        String result = Utils.getPrintableString("abcdefghij", 5);
        assertThat(result).contains("abcde").contains("[truncated]");
    }

    @Test
    void should_ReplaceNonPrintable_When_ControlChars() {
        // char 1 (SOH) is non-printable, should become ~
        String result = Utils.getPrintableString("a\u0001b", 100);
        assertThat(result).isEqualTo("a~b");
    }

    @Test
    void should_ShowCrLf_When_CarriageReturnLineFeed() {
        String result = Utils.getPrintableString("a\r\nb", 100);
        assertThat(result).isEqualTo("a[Cr][Lf]b");
    }

    // ========== isHTMLString ==========

    @Test
    void should_ReturnTrue_When_StartsWithHtmlTag() {
        assertThat(Utils.isHTMLString("<html>content")).isTrue();
    }

    @Test
    void should_ReturnTrue_When_StartsWithHtmlTagUpperCase() {
        assertThat(Utils.isHTMLString("<HTML>content")).isTrue();
    }

    @Test
    void should_ReturnFalse_When_NotHtmlString() {
        assertThat(Utils.isHTMLString("just text")).isFalse();
    }

    @Test
    void should_ReturnFalse_When_NullHtmlCheck() {
        assertThat(Utils.isHTMLString(null)).isFalse();
    }

    @Test
    void should_ReturnFalse_When_TooShort() {
        assertThat(Utils.isHTMLString("<htm")).isFalse();
    }

    // ========== parseBoolean ==========

    @Test
    void should_ReturnTrue_When_TrueString() {
        assertThat(Utils.parseBoolean("true")).isEqualTo(Boolean.TRUE);
    }

    @Test
    void should_ReturnTrue_When_TrueUpperCase() {
        assertThat(Utils.parseBoolean("TRUE")).isEqualTo(Boolean.TRUE);
    }

    @Test
    void should_ReturnTrue_When_Yes() {
        assertThat(Utils.parseBoolean("yes")).isEqualTo(Boolean.TRUE);
    }

    @Test
    void should_ReturnTrue_When_One() {
        assertThat(Utils.parseBoolean("1")).isEqualTo(Boolean.TRUE);
    }

    @Test
    void should_ReturnTrue_When_Plus() {
        assertThat(Utils.parseBoolean("+")).isEqualTo(Boolean.TRUE);
    }

    @Test
    void should_ReturnFalse_When_FalseString() {
        assertThat(Utils.parseBoolean("false")).isEqualTo(Boolean.FALSE);
    }

    @Test
    void should_ReturnFalse_When_No() {
        assertThat(Utils.parseBoolean("no")).isEqualTo(Boolean.FALSE);
    }

    @Test
    void should_ReturnFalse_When_Zero() {
        assertThat(Utils.parseBoolean("0")).isEqualTo(Boolean.FALSE);
    }

    @Test
    void should_ReturnFalse_When_Minus() {
        assertThat(Utils.parseBoolean("-")).isEqualTo(Boolean.FALSE);
    }

    @Test
    void should_ReturnNull_When_NullBoolean() {
        assertThat(Utils.parseBoolean(null)).isNull();
    }

    @Test
    void should_ReturnNull_When_InvalidBoolean() {
        assertThat(Utils.parseBoolean("maybe")).isNull();
    }

    @Test
    void should_ReturnDefault_When_NullWithDefault() {
        assertThat(Utils.parseBoolean(null, true)).isTrue();
        assertThat(Utils.parseBoolean(null, false)).isFalse();
    }

    @Test
    void should_ReturnParsed_When_ValidWithDefault() {
        assertThat(Utils.parseBoolean("true", false)).isTrue();
        assertThat(Utils.parseBoolean("false", true)).isFalse();
    }

    // ========== parseStringToDouble ==========

    @Test
    void should_ParseDouble_When_NormalNumber() {
        assertThat(Utils.parseStringToDouble("3.14", 100)).isEqualTo(3.14);
    }

    @Test
    void should_ReturnZero_When_NullDouble() {
        assertThat(Utils.parseStringToDouble(null, 100)).isEqualTo(0.0);
    }

    @Test
    void should_ReturnZero_When_EmptyDouble() {
        assertThat(Utils.parseStringToDouble("", 100)).isEqualTo(0.0);
    }

    @Test
    void should_StripNonNumeric_When_MixedString() {
        assertThat(Utils.parseStringToDouble("$1,234.56", 100)).isEqualTo(1234.56);
    }

    @Test
    void should_HandleNegative_When_NegativeNumber() {
        assertThat(Utils.parseStringToDouble("-5.5", 10)).isEqualTo(-5.5);
    }

    @Test
    void should_UseMultiplierOne_When_ZeroMultiplier() {
        assertThat(Utils.parseStringToDouble("3.14159", 0)).isEqualTo(3.0);
    }

    // ========== Color utilities ==========

    @Test
    void should_ReturnHexString_When_RedColor() {
        assertThat(Utils.getHtmlColor(Color.RED)).isEqualTo("#ff0000");
    }

    @Test
    void should_ReturnHexString_When_WhiteColor() {
        assertThat(Utils.getHtmlColor(Color.WHITE)).isEqualTo("#ffffff");
    }

    @Test
    void should_ReturnHexString_When_BlackColor() {
        assertThat(Utils.getHtmlColor(Color.BLACK)).isEqualTo("#000000");
    }

    @Test
    void should_ParseColor_When_HexString() {
        Color c = Utils.getHtmlColor("#ff0000");
        assertThat(c).isEqualTo(Color.RED);
    }

    @Test
    void should_ReturnNull_When_NullColorString() {
        assertThat(Utils.getHtmlColor((String) null)).isNull();
    }

    @Test
    void should_IncludeAlpha_When_GetHtmlColorAlpha() {
        Color c = new Color(255, 0, 0, 128);
        String result = Utils.getHtmlColorAlpha(c);
        assertThat(result).isEqualTo("#ff000080");
    }

    @Test
    void should_ParseAlpha_When_HexStringWithAlpha() {
        Color c = Utils.getHtmlColorAlpha("#ff000080");
        assertThat(c.getRed()).isEqualTo(255);
        assertThat(c.getGreen()).isEqualTo(0);
        assertThat(c.getBlue()).isEqualTo(0);
        assertThat(c.getAlpha()).isEqualTo(128);
    }

    @Test
    void should_ReturnNull_When_NullAlphaColorString() {
        assertThat(Utils.getHtmlColorAlpha((String) null)).isNull();
    }

    @Test
    void should_RoundTrip_When_ColorToStringAndBack() {
        Color original = new Color(100, 150, 200);
        String hex = Utils.getHtmlColor(original);
        Color parsed = Utils.getHtmlColor(hex);
        assertThat(parsed).isEqualTo(original);
    }

    // ========== getTimeString ==========

    @Test
    void should_FormatZero_When_ZeroMillis() {
        assertThat(Utils.getTimeString(0, false)).isEqualTo("00:00:00");
    }

    @Test
    void should_FormatSeconds_When_SmallDuration() {
        assertThat(Utils.getTimeString(5000, false)).isEqualTo("00:00:05");
    }

    @Test
    void should_FormatHoursMinutesSeconds_When_LargeDuration() {
        long millis = (2 * 3600 + 30 * 60 + 15) * 1000L;
        assertThat(Utils.getTimeString(millis, false)).isEqualTo("02:30:15");
    }

    @Test
    void should_IncludeDays_When_OverOneDay() {
        long millis = (26 * 3600 + 30 * 60) * 1000L;
        assertThat(Utils.getTimeString(millis, false)).startsWith("1d ");
    }

    @Test
    void should_IncludeMillis_When_FlagTrue() {
        assertThat(Utils.getTimeString(1234, true)).isEqualTo("00:00:01.234");
    }

    @Test
    void should_ExcludeMillis_When_FlagFalse() {
        String result = Utils.getTimeString(1234, false);
        assertThat(result).doesNotContain(".");
    }

    // ========== formatSizeBytes ==========

    @Test
    void should_FormatBytes_When_SmallValue() {
        assertThat(Utils.formatSizeBytes(500)).isEqualTo("500 B");
    }

    @Test
    void should_FormatKB_When_MediumValue() {
        assertThat(Utils.formatSizeBytes(1024 * 50)).isEqualTo("50 KB");
    }

    @Test
    void should_FormatMB_When_LargeValue() {
        assertThat(Utils.formatSizeBytes(1024L * 1024 * 50)).isEqualTo("50 MB");
    }

    @Test
    void should_FormatGB_When_VeryLargeValue() {
        assertThat(Utils.formatSizeBytes(1024L * 1024 * 1024 * 50)).isEqualTo("50 GB");
    }

    @Test
    void should_FormatKBWithDecimal_When_SmallKBValue() {
        // Between 1KB and 10KB should show one decimal
        assertThat(Utils.formatSizeBytes(1024 * 5 + 512)).contains("KB");
    }

    // ========== Date utilities ==========

    @Test
    void should_ReturnFutureDate_When_PositiveDays() {
        Date now = new Date();
        Date future = Utils.getDateDays(now, 5);
        assertThat(future).isAfter(now);
    }

    @Test
    void should_ReturnPastDate_When_NegativeDays() {
        Date now = new Date();
        Date past = Utils.getDateDays(now, -5);
        assertThat(past).isBefore(now);
    }

    @Test
    void should_ReturnTrue_When_SameDay() {
        Date d1 = new Date();
        Date d2 = new Date(d1.getTime() + 1000); // one second later
        assertThat(Utils.isSameDay(d1, d2)).isTrue();
    }

    @Test
    void should_ReturnFalse_When_DifferentDay() {
        Date d1 = new Date();
        Date d2 = Utils.getDateDays(d1, 1);
        assertThat(Utils.isSameDay(d1, d2)).isFalse();
    }

    @Test
    void should_SetEndOfDay_When_DateProvided() {
        Date d = new Date();
        Date end = Utils.getDateEndOfDay(d);
        Calendar c = Calendar.getInstance();
        c.setTime(end);
        assertThat(c.get(Calendar.HOUR_OF_DAY)).isEqualTo(23);
        assertThat(c.get(Calendar.MINUTE)).isEqualTo(59);
        assertThat(c.get(Calendar.SECOND)).isEqualTo(59);
    }

    @Test
    void should_ReturnNull_When_EndOfDayNull() {
        assertThat(Utils.getDateEndOfDay(null)).isNull();
    }

    @Test
    void should_ZeroTime_When_DateProvided() {
        Date d = new Date();
        Date zeroed = Utils.getDateZeroTime(d);
        Calendar c = Calendar.getInstance();
        c.setTime(zeroed);
        assertThat(c.get(Calendar.HOUR_OF_DAY)).isZero();
        assertThat(c.get(Calendar.MINUTE)).isZero();
        assertThat(c.get(Calendar.SECOND)).isZero();
        assertThat(c.get(Calendar.MILLISECOND)).isZero();
    }

    // ========== encode / decode round trip ==========

    @Test
    void should_RoundTrip_When_EncodeAndDecode() {
        String original = "Hello, World! \u00e9\u00e8\u00ea";
        byte[] encoded = Utils.encode(original);
        String decoded = Utils.decode(encoded);
        assertThat(decoded).isEqualTo(original);
    }

    @Test
    void should_ReturnEmpty_When_DecodeNullOrEmpty() {
        assertThat(Utils.decode(new byte[0], 0, 0)).isEmpty();
        assertThat(Utils.decode(null, 0, 0)).isEmpty();
    }

    @Test
    void should_RoundTrip_When_EncodeBasicAndDecodeBasic() {
        String original = "Hello World";
        byte[] encoded = Utils.encodeBasic(original);
        String decoded = Utils.decodeBasic(encoded);
        assertThat(decoded).isEqualTo(original);
    }

    @Test
    void should_DecodeSubset_When_OffsetAndLengthProvided() {
        String original = "Hello";
        byte[] encoded = Utils.encode(original);
        String partial = Utils.decode(encoded, 0, 3);
        assertThat(partial).isEqualTo("Hel");
    }

    // ========== toHexString ==========

    @Test
    void should_ReturnHex_When_ZeroByte() {
        assertThat(Utils.toHexString((byte) 0)).isEqualTo("0x00");
    }

    @Test
    void should_ReturnHex_When_MaxByte() {
        assertThat(Utils.toHexString((byte) 0xFF)).isEqualTo("0xff");
    }

    @Test
    void should_ReturnHex_When_MidByte() {
        assertThat(Utils.toHexString((byte) 0x0A)).isEqualTo("0x0a");
    }

    @Test
    void should_ReturnHex_When_NegativeByte() {
        // -1 as byte is 0xFF
        assertThat(Utils.toHexString((byte) -1)).isEqualTo("0xff");
    }

    // ========== Exception formatting ==========

    @Test
    void should_FormatException_When_SimpleException() {
        Exception e = new RuntimeException("test error");
        String result = Utils.formatExceptionText(e);
        assertThat(result).contains("RuntimeException");
        assertThat(result).contains("test error");
    }

    @Test
    void should_ReturnNull_When_NullException() {
        assertThat(Utils.formatExceptionText(null)).isEqualTo("null");
    }

    @Test
    void should_IncludeCause_When_ChainedException() {
        Exception cause = new IllegalArgumentException("root cause");
        Exception wrapper = new RuntimeException("wrapper", cause);
        String result = Utils.formatExceptionText(wrapper);
        assertThat(result).contains("root cause");
        assertThat(result).contains("wrapper");
    }

    @Test
    void should_ReturnMessage_When_MessagePresent() {
        Exception e = new RuntimeException("my message");
        assertThat(Utils.getExceptionMessage(e)).isEqualTo("my message");
    }

    @Test
    void should_ReturnClassName_When_NoMessage() {
        Exception e = new RuntimeException();
        assertThat(Utils.getExceptionMessage(e)).isEqualTo("java.lang.RuntimeException");
    }

    @Test
    void should_WrapInPre_When_FormatExceptionHTML() {
        Exception e = new RuntimeException("html error");
        String result = Utils.formatExceptionHTML(e);
        assertThat(result).startsWith("<PRE>");
        assertThat(result).endsWith("</PRE>");
        assertThat(result).contains("html error");
    }

    // ========== fixFilePath ==========

    @Test
    void should_ReturnNull_When_NullPath() {
        assertThat(Utils.fixFilePath(null)).isNull();
    }

    @Test
    void should_RemoveTrailingSlash_When_Present() {
        String result = Utils.fixFilePath("some/path/");
        assertThat(result).doesNotEndWith("/").doesNotEndWith("\\");
    }

    @Test
    void should_NormalizeSeparators_When_BackslashesPresent() {
        String result = Utils.fixFilePath("some\\path\\file.txt");
        // Should use platform separator
        assertThat(result).isEqualTo("some" + File.separator + "path" + File.separator + "file.txt");
    }

    // ========== getFile ==========

    @Test
    void should_ReturnFile_When_DirAndFileProvided() {
        File f = Utils.getFile(System.getProperty("java.io.tmpdir"), "test.txt");
        assertThat(f).isNotNull();
        assertThat(f.getName()).isEqualTo("test.txt");
    }

    @Test
    void should_ReturnFile_When_SinglePathProvided() {
        File f = Utils.getFile(System.getProperty("java.io.tmpdir"));
        assertThat(f).isNotNull();
        assertThat(f.exists()).isTrue();
    }

    // ========== WaitBoolean inner class ==========

    @Test
    void should_BeNotDone_When_NewlyCreated() {
        Object lock = new Object();
        Utils.WaitBoolean wb = new Utils.WaitBoolean(lock);
        assertThat(wb.isDone()).isFalse();
    }

    @Test
    void should_BeDone_When_DoneCalled() {
        Object lock = new Object();
        Utils.WaitBoolean wb = new Utils.WaitBoolean(lock);
        wb.done();
        assertThat(wb.isDone()).isTrue();
    }

    @Test
    void should_ReturnObject_When_ObjectCalled() {
        Object lock = new Object();
        Utils.WaitBoolean wb = new Utils.WaitBoolean(lock);
        assertThat(wb.object()).isSameAs(lock);
    }

    @Test
    void should_BeIdempotent_When_DoneCalledTwice() {
        Object lock = new Object();
        Utils.WaitBoolean wb = new Utils.WaitBoolean(lock);
        wb.done();
        wb.done(); // should not throw
        assertThat(wb.isDone()).isTrue();
    }
}
