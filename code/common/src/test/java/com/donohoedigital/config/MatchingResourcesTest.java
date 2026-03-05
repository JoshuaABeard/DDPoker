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
package com.donohoedigital.config;

import org.apache.logging.log4j.*;
import org.junit.jupiter.api.*;
import org.springframework.core.io.*;

import java.lang.annotation.*;
import java.net.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Created by IntelliJ IDEA. User: donohoe Date: Apr 6, 2008 Time: 3:19:51 PM To
 * change this template use File | Settings | File Templates.
 */
class MatchingResourcesTest {
    Logger logger = LogManager.getLogger(MatchingResourcesTest.class);

    @Test
    void testFindResources() {
        MatchingResources mr = new MatchingResources("classpath*:com/donohoedigital/config/ConfigUtils.class");
        URL[] match = mr.getAllMatchesURL();
        assertThat(match.length).isEqualTo(1);

        logger.info("URL: " + match[0]);
        assertThat(mr.getURL(null)).isNull();

        Resource[] none = new MatchingResources("classpath*:com/donohoedigital/config/NoSuchFile.class")
                .getAllMatches();
        assertThat(none.length).isEqualTo(0);
    }

    @Test
    void testFindResource() {
        Resource thiz = new MatchingResources("classpath*:com/donohoedigital/config/MatchingResourcesTest.class")
                .getSingleRequiredResource();
        assertThat(thiz).isNotNull();

        // test required
        assertThatThrownBy(() -> new MatchingResources("classpath*:com/donohoedigital/config/NoSuchFile.class")
                .getSingleRequiredResource()).isInstanceOf(Exception.class);

        // test multiple matches
        assertThatThrownBy(
                () -> new MatchingResources("classpath*:com/donohoedigital/config/*.class").getSingleResource())
                        .isInstanceOf(Exception.class);
    }

    @Test
    void testToString() {
        MatchingResources mr = new MatchingResources("classpath*:com/donohoedigital/config/*.class");
        assertThat(mr.getAllMatches().length).isGreaterThan(0);

        logger.info("URLs:\n" + mr);
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface AnnoMatchTest {
    }

    private interface MatchTest {
    }

    private interface MatchTest2 extends MatchTest {
    }

    @AnnoMatchTest
    private static class MatchTestImpl implements MatchTest {
    }

    private static class MatchTestSubImpl extends MatchTestImpl {
    }

    @AnnoMatchTest
    private static class MatchTestSubImpl2 extends MatchTestImpl implements MatchTest {
    }

    private static class MatchTestSubSubImpl extends MatchTestSubImpl {
    }

    private static class MatchTest2Impl implements MatchTest2 {
    }

    private static class NoMatchTest {
    }

    @Test
    void testMatchingAnno() {
        MatchingResources mr = new MatchingResources("classpath*:com/donohoedigital/config/*.class");
        Set<Class<?>> matches = mr.getAnnotatedMatches(AnnoMatchTest.class);
        assertThat(matches).contains(MatchTestImpl.class);
        assertThat(matches).contains(MatchTestSubImpl2.class);
        assertThat(matches).hasSize(2);
    }

    @Test
    void testMatchingImpl() {
        MatchingResources mr = new MatchingResources("classpath*:com/donohoedigital/config/*.class");
        Set<Class<?>> matches = mr.getImplementingMatches(MatchTest.class);
        assertThat(matches).contains(MatchTestImpl.class);
        assertThat(matches).contains(MatchTestSubImpl.class);
        assertThat(matches).contains(MatchTest2Impl.class);
        assertThat(matches).contains(MatchTestSubImpl2.class);
        assertThat(matches).contains(MatchTestSubSubImpl.class);
        assertThat(matches).hasSize(5);
        assertThat(matches).doesNotContain(NoMatchTest.class);
        assertThat(matches).doesNotContain(MatchTest.class);
        assertThat(matches).doesNotContain(MatchTest2.class);
    }

    @Test
    void testGetSubclasses() {
        MatchingResources mr = new MatchingResources("classpath*:com/donohoedigital/config/*.class");
        Set<Class<?>> matches = mr.getSubclasses(MatchTestImpl.class);
        assertThat(matches).contains(MatchTestSubImpl.class);
        assertThat(matches).contains(MatchTestSubSubImpl.class);
        assertThat(matches).contains(MatchTestSubImpl2.class);
        assertThat(matches).hasSize(3);
    }
}
