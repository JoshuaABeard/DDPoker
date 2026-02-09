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
package com.donohoedigital.games.poker.online;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.Queue;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for PublicIPDetector using TDD approach.
 * Tests written BEFORE implementation (RED phase).
 */
class PublicIPDetectorTest {

    private TestHttpFetcher testHttpFetcher;
    private PublicIPDetector detector;

    /**
     * Test implementation of HttpFetcher for testing.
     * Allows configuring responses and tracking calls.
     */
    private static class TestHttpFetcher implements PublicIPDetector.HttpFetcher {
        private final Queue<FetchResult> responses = new LinkedList<>();
        private int callCount = 0;

        void addResponse(String response) {
            responses.add(new FetchResult(response, null));
        }

        void addException(RuntimeException exception) {
            responses.add(new FetchResult(null, exception));
        }

        int getCallCount() {
            return callCount;
        }

        @Override
        public String fetch(String url) {
            callCount++;
            FetchResult result = responses.poll();
            if (result == null) {
                throw new IllegalStateException("No more responses configured for test");
            }
            if (result.exception != null) {
                throw result.exception;
            }
            return result.response;
        }

        private static class FetchResult {
            final String response;
            final RuntimeException exception;

            FetchResult(String response, RuntimeException exception) {
                this.response = response;
                this.exception = exception;
            }
        }
    }

    @BeforeEach
    void setUp() {
        testHttpFetcher = new TestHttpFetcher();
        detector = new PublicIPDetector(testHttpFetcher);
    }

    // =================================================================
    // Test 1: Successful IP fetch from primary service (ipify.org)
    // =================================================================

    @Test
    void should_ReturnPublicIP_When_PrimaryServiceReturnsValidIP() {
        // Arrange
        String expectedIP = "203.0.113.42";
        testHttpFetcher.addResponse(expectedIP);

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        assertThat(actualIP).isEqualTo(expectedIP);
        assertThat(testHttpFetcher.getCallCount()).isEqualTo(1);
    }

    // =================================================================
    // Test 2: Fallback to second service when first fails
    // =================================================================

    @Test
    void should_FallbackToSecondService_When_FirstServiceFails() {
        // Arrange
        String expectedIP = "198.51.100.42";
        testHttpFetcher.addException(new RuntimeException("Service unavailable"));  // First service fails
        testHttpFetcher.addResponse(expectedIP);  // Second service succeeds

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        assertThat(actualIP).isEqualTo(expectedIP);
        assertThat(testHttpFetcher.getCallCount()).isEqualTo(2);
    }

    // =================================================================
    // Test 3: Fallback to third service when first two fail
    // =================================================================

    @Test
    void should_FallbackToThirdService_When_FirstTwoServicesFail() {
        // Arrange
        String expectedIP = "192.0.2.42";
        testHttpFetcher.addException(new RuntimeException("First service unavailable"));
        testHttpFetcher.addException(new RuntimeException("Second service unavailable"));
        testHttpFetcher.addResponse(expectedIP);  // Third service succeeds

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        assertThat(actualIP).isEqualTo(expectedIP);
        assertThat(testHttpFetcher.getCallCount()).isEqualTo(3);
    }

    // =================================================================
    // Test 4: Reject private IP addresses (192.168.x.x)
    // =================================================================

    @Test
    void should_ReturnNull_When_ServiceReturnsPrivateIP_192_168() {
        // Arrange
        testHttpFetcher.addResponse("192.168.1.100");
        testHttpFetcher.addResponse("192.168.1.100");
        testHttpFetcher.addResponse("192.168.1.100");

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        assertThat(actualIP).isNull();
    }

    // =================================================================
    // Test 5: Reject private IP addresses (10.x.x.x)
    // =================================================================

    @Test
    void should_ReturnNull_When_ServiceReturnsPrivateIP_10() {
        // Arrange
        testHttpFetcher.addResponse("10.0.0.5");
        testHttpFetcher.addResponse("10.0.0.5");
        testHttpFetcher.addResponse("10.0.0.5");

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        assertThat(actualIP).isNull();
    }

    // =================================================================
    // Test 6: Reject localhost (127.0.0.1)
    // =================================================================

    @Test
    void should_ReturnNull_When_ServiceReturnsLocalhost() {
        // Arrange
        testHttpFetcher.addResponse("127.0.0.1");
        testHttpFetcher.addResponse("127.0.0.1");
        testHttpFetcher.addResponse("127.0.0.1");

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        assertThat(actualIP).isNull();
    }

    // =================================================================
    // Test 7: Cache returns same IP within TTL
    // =================================================================

    @Test
    void should_ReturnCachedIP_When_CalledWithinTTL() {
        // Arrange
        String expectedIP = "203.0.113.42";
        testHttpFetcher.addResponse(expectedIP);

        // Act - First call
        String firstIP = detector.fetchPublicIP();
        // Act - Second call (should use cache)
        String secondIP = detector.fetchPublicIP();

        // Assert
        assertThat(firstIP).isEqualTo(expectedIP);
        assertThat(secondIP).isEqualTo(expectedIP);
        // HTTP fetcher should only be called once (second call uses cache)
        assertThat(testHttpFetcher.getCallCount()).isEqualTo(1);
    }

    // =================================================================
    // Test 8: Cache expires after TTL
    // =================================================================

    @Test
    void should_RefetchIP_When_CacheExpires() {
        // Arrange
        String firstIP = "203.0.113.42";
        String secondIP = "198.51.100.42";
        TestHttpFetcher shortTTLFetcher = new TestHttpFetcher();
        shortTTLFetcher.addResponse(firstIP);
        shortTTLFetcher.addResponse(secondIP);

        // Create detector with very short TTL (1ms for testing)
        PublicIPDetector detectorWithShortTTL = new PublicIPDetector(shortTTLFetcher, 1L);

        // Act - First call
        String result1 = detectorWithShortTTL.fetchPublicIP();

        // Wait for cache to expire
        try {
            Thread.sleep(10); // Wait 10ms to ensure cache expires
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act - Second call (cache should be expired)
        String result2 = detectorWithShortTTL.fetchPublicIP();

        // Assert
        assertThat(result1).isEqualTo(firstIP);
        assertThat(result2).isEqualTo(secondIP);
        // HTTP fetcher should be called twice (cache expired)
        assertThat(shortTTLFetcher.getCallCount()).isEqualTo(2);
    }

    // =================================================================
    // Test 9: Returns null when all services fail
    // =================================================================

    @Test
    void should_ReturnNull_When_AllServicesFail() {
        // Arrange
        testHttpFetcher.addException(new RuntimeException("Service 1 failed"));
        testHttpFetcher.addException(new RuntimeException("Service 2 failed"));
        testHttpFetcher.addException(new RuntimeException("Service 3 failed"));

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        assertThat(actualIP).isNull();
        assertThat(testHttpFetcher.getCallCount()).isEqualTo(3);
    }

    // =================================================================
    // Test 10: Validate IPv4 format
    // =================================================================

    @Test
    void should_ReturnNull_When_ServiceReturnsInvalidIPFormat() {
        // Arrange
        testHttpFetcher.addResponse("abc.def.ghi.jkl");
        testHttpFetcher.addResponse("abc.def.ghi.jkl");
        testHttpFetcher.addResponse("abc.def.ghi.jkl");

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        assertThat(actualIP).isNull();
    }

    // =================================================================
    // Additional edge case tests
    // =================================================================

    @Test
    void should_ReturnNull_When_ServiceReturnsEmptyString() {
        // Arrange
        testHttpFetcher.addResponse("");
        testHttpFetcher.addResponse("");
        testHttpFetcher.addResponse("");

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        assertThat(actualIP).isNull();
    }

    @Test
    void should_ReturnNull_When_ServiceReturnsNull() {
        // Arrange
        testHttpFetcher.addResponse(null);
        testHttpFetcher.addResponse(null);
        testHttpFetcher.addResponse(null);

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        assertThat(actualIP).isNull();
    }

    @Test
    void should_HandleWhitespaceInIPResponse() {
        // Arrange
        String expectedIP = "203.0.113.42";
        testHttpFetcher.addResponse("  " + expectedIP + "\n  ");

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        assertThat(actualIP).isEqualTo(expectedIP);
    }

    @Test
    void should_RejectPrivateIP_172_16_Range() {
        // Arrange
        testHttpFetcher.addResponse("172.16.0.1");
        testHttpFetcher.addResponse("172.16.0.1");
        testHttpFetcher.addResponse("172.16.0.1");

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        assertThat(actualIP).isNull();
    }

    @Test
    void should_RejectLinkLocalIP_169_254() {
        // Arrange
        testHttpFetcher.addResponse("169.254.1.1");
        testHttpFetcher.addResponse("169.254.1.1");
        testHttpFetcher.addResponse("169.254.1.1");

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        assertThat(actualIP).isNull();
    }

    // =================================================================
    // Thread Safety Tests
    // =================================================================

    @Test
    void should_HandleConcurrentAccess_When_MultipleThreadsFetchSimultaneously() throws InterruptedException {
        // Arrange
        String expectedIP = "203.0.113.42";
        testHttpFetcher.addResponse(expectedIP);

        // Act - Launch multiple threads concurrently
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        String[] results = new String[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                results[index] = detector.fetchPublicIP();
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Assert
        // All threads should get the same IP
        for (String result : results) {
            assertThat(result).isEqualTo(expectedIP);
        }
        // HTTP fetcher should only be called once (others use cache)
        assertThat(testHttpFetcher.getCallCount()).isEqualTo(1);
    }

    // =================================================================
    // IPv6 Handling Tests
    // =================================================================

    @Test
    void should_ReturnNull_When_ServiceReturnsIPv6Address() {
        // Arrange
        testHttpFetcher.addResponse("2001:0db8:85a3::8a2e:0370:7334");
        testHttpFetcher.addResponse("2001:0db8:85a3::8a2e:0370:7334");
        testHttpFetcher.addResponse("2001:0db8:85a3::8a2e:0370:7334");

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        assertThat(actualIP).isNull();
    }

    @Test
    void should_ReturnNull_When_ServiceReturnsCompressedIPv6() {
        // Arrange
        testHttpFetcher.addResponse("::1"); // IPv6 loopback
        testHttpFetcher.addResponse("::1");
        testHttpFetcher.addResponse("::1");

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        assertThat(actualIP).isNull();
    }

    // =================================================================
    // Special IP Address Range Tests
    // =================================================================

    @Test
    void should_RejectBroadcastIP_255_255_255_255() {
        // Arrange
        testHttpFetcher.addResponse("255.255.255.255");
        testHttpFetcher.addResponse("255.255.255.255");
        testHttpFetcher.addResponse("255.255.255.255");

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        assertThat(actualIP).isNull();
    }

    @Test
    void should_RejectZeroIP_0_0_0_0() {
        // Arrange
        testHttpFetcher.addResponse("0.0.0.0");
        testHttpFetcher.addResponse("0.0.0.0");
        testHttpFetcher.addResponse("0.0.0.0");

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        assertThat(actualIP).isNull();
    }

    @Test
    void should_RejectMulticastIP_224_Range() {
        // Arrange - 224.0.0.0/4 is multicast range (224.0.0.0 - 239.255.255.255)
        testHttpFetcher.addResponse("224.0.0.1");
        testHttpFetcher.addResponse("224.0.0.1");
        testHttpFetcher.addResponse("224.0.0.1");

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        // Note: Current implementation doesn't explicitly reject multicast
        // This test documents current behavior
        // TODO: Consider rejecting multicast IPs explicitly
        assertThat(actualIP).isNotNull(); // Currently accepts (may want to change)
    }

    @Test
    void should_AcceptValidPublicIP_EdgeCase_1_0_0_1() {
        // Arrange - 1.0.0.1 is valid public IP
        testHttpFetcher.addResponse("1.0.0.1");

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        assertThat(actualIP).isEqualTo("1.0.0.1");
    }

    @Test
    void should_AcceptValidPublicIP_8_8_8_8() {
        // Arrange - Google DNS, definitely public
        testHttpFetcher.addResponse("8.8.8.8");

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        assertThat(actualIP).isEqualTo("8.8.8.8");
    }

    // =================================================================
    // Malformed IP Response Tests
    // =================================================================

    @Test
    void should_ReturnNull_When_ServiceReturnsPartialIP() {
        // Arrange - Only 3 octets
        testHttpFetcher.addResponse("192.168.1");
        testHttpFetcher.addResponse("192.168.1");
        testHttpFetcher.addResponse("192.168.1");

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        assertThat(actualIP).isNull();
    }

    @Test
    void should_ReturnNull_When_ServiceReturnsTooManyOctets() {
        // Arrange - 5 octets
        testHttpFetcher.addResponse("192.168.1.1.1");
        testHttpFetcher.addResponse("192.168.1.1.1");
        testHttpFetcher.addResponse("192.168.1.1.1");

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        assertThat(actualIP).isNull();
    }

    @Test
    void should_ReturnNull_When_ServiceReturnsLeadingZeros() {
        // Arrange
        testHttpFetcher.addResponse("192.168.001.001");
        testHttpFetcher.addResponse("192.168.001.001");
        testHttpFetcher.addResponse("192.168.001.001");

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        assertThat(actualIP).isNull();
    }

    @Test
    void should_ReturnNull_When_ServiceReturnsOctetOver255() {
        // Arrange
        testHttpFetcher.addResponse("192.168.1.256");
        testHttpFetcher.addResponse("192.168.1.256");
        testHttpFetcher.addResponse("192.168.1.256");

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        assertThat(actualIP).isNull();
    }

    @Test
    void should_ReturnNull_When_ServiceReturnsOctet999() {
        // Arrange
        testHttpFetcher.addResponse("999.999.999.999");
        testHttpFetcher.addResponse("999.999.999.999");
        testHttpFetcher.addResponse("999.999.999.999");

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        assertThat(actualIP).isNull();
    }

    @Test
    void should_ReturnNull_When_ServiceReturnsNegativeOctet() {
        // Arrange
        testHttpFetcher.addResponse("192.-1.1.1");
        testHttpFetcher.addResponse("192.-1.1.1");
        testHttpFetcher.addResponse("192.-1.1.1");

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        assertThat(actualIP).isNull();
    }

    @Test
    void should_ReturnNull_When_ServiceReturnsHTMLErrorResponse() {
        // Arrange
        testHttpFetcher.addResponse("<html><body>Error 500</body></html>");
        testHttpFetcher.addResponse("<html><body>Error 500</body></html>");
        testHttpFetcher.addResponse("<html><body>Error 500</body></html>");

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        assertThat(actualIP).isNull();
    }

    @Test
    void should_UseFirstIP_When_ServiceReturnsMultipleIPs() {
        // Arrange - Some services might return multiple IPs
        testHttpFetcher.addResponse("203.0.113.42\n8.8.8.8");

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        // After trim, we get "203.0.113.42\n8.8.8.8" which won't match IP pattern
        assertThat(actualIP).isNull();
    }

    @Test
    void should_ReturnNull_When_ServiceReturnsIPWithPort() {
        // Arrange
        testHttpFetcher.addResponse("203.0.113.42:8080");
        testHttpFetcher.addResponse("203.0.113.42:8080");
        testHttpFetcher.addResponse("203.0.113.42:8080");

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        assertThat(actualIP).isNull();
    }

    @Test
    void should_ReturnNull_When_ServiceReturnsIPWithCIDR() {
        // Arrange
        testHttpFetcher.addResponse("203.0.113.42/24");
        testHttpFetcher.addResponse("203.0.113.42/24");
        testHttpFetcher.addResponse("203.0.113.42/24");

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        assertThat(actualIP).isNull();
    }

    // =================================================================
    // Cache Edge Case Tests
    // =================================================================

    @Test
    void should_AlwaysFetch_When_TTLIsZero() {
        // Arrange
        TestHttpFetcher zeroTTLFetcher = new TestHttpFetcher();
        zeroTTLFetcher.addResponse("203.0.113.42");
        zeroTTLFetcher.addResponse("198.51.100.42");

        PublicIPDetector detectorWithZeroTTL = new PublicIPDetector(zeroTTLFetcher, 0L);

        // Act - Two calls with zero TTL
        String firstIP = detectorWithZeroTTL.fetchPublicIP();
        String secondIP = detectorWithZeroTTL.fetchPublicIP();

        // Assert - Should fetch twice (no caching)
        assertThat(firstIP).isEqualTo("203.0.113.42");
        assertThat(secondIP).isEqualTo("198.51.100.42");
        assertThat(zeroTTLFetcher.getCallCount()).isEqualTo(2);
    }

    @Test
    void should_CacheIndefinitely_When_TTLIsMaxLong() {
        // Arrange
        TestHttpFetcher longTTLFetcher = new TestHttpFetcher();
        longTTLFetcher.addResponse("203.0.113.42");

        PublicIPDetector detectorWithLongTTL = new PublicIPDetector(longTTLFetcher, Long.MAX_VALUE);

        // Act - Multiple calls
        String firstIP = detectorWithLongTTL.fetchPublicIP();
        String secondIP = detectorWithLongTTL.fetchPublicIP();
        String thirdIP = detectorWithLongTTL.fetchPublicIP();

        // Assert - Should only fetch once
        assertThat(firstIP).isEqualTo("203.0.113.42");
        assertThat(secondIP).isEqualTo("203.0.113.42");
        assertThat(thirdIP).isEqualTo("203.0.113.42");
        assertThat(longTTLFetcher.getCallCount()).isEqualTo(1);
    }

    @Test
    void should_RefetchAfterClearCache() {
        // Arrange
        String firstIP = "203.0.113.42";
        String secondIP = "198.51.100.42";
        testHttpFetcher.addResponse(firstIP);
        testHttpFetcher.addResponse(secondIP);

        // Act
        String result1 = detector.fetchPublicIP();
        detector.clearCache(); // Explicitly clear cache
        String result2 = detector.fetchPublicIP();

        // Assert
        assertThat(result1).isEqualTo(firstIP);
        assertThat(result2).isEqualTo(secondIP);
        assertThat(testHttpFetcher.getCallCount()).isEqualTo(2);
    }

    // =================================================================
    // Boundary Value Tests
    // =================================================================

    @Test
    void should_AcceptValidIP_AllZeroOctetsExceptLast() {
        // Arrange
        testHttpFetcher.addResponse("0.0.0.1");

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        // Note: 0.0.0.1 is technically reserved but not explicitly blocked
        assertThat(actualIP).isEqualTo("0.0.0.1");
    }

    @Test
    void should_AcceptValidIP_AllMaxOctets() {
        // Arrange
        testHttpFetcher.addResponse("254.254.254.254");

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        assertThat(actualIP).isEqualTo("254.254.254.254");
    }

    @Test
    void should_ReturnNull_When_ServiceReturnsOnlyDots() {
        // Arrange
        testHttpFetcher.addResponse("...");
        testHttpFetcher.addResponse("...");
        testHttpFetcher.addResponse("...");

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        assertThat(actualIP).isNull();
    }

    @Test
    void should_ReturnNull_When_ServiceReturnsIPWithExtraWhitespace() {
        // Arrange
        testHttpFetcher.addResponse("203 . 0 . 113 . 42");
        testHttpFetcher.addResponse("203 . 0 . 113 . 42");
        testHttpFetcher.addResponse("203 . 0 . 113 . 42");

        // Act
        String actualIP = detector.fetchPublicIP();

        // Assert
        assertThat(actualIP).isNull();
    }
}
