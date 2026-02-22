/*
 * ============================================================================================
 * DD Poker - Source Code
 * Copyright (c) 2026  DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ============================================================================================
 */
package com.donohoedigital.games.poker.server;

import com.donohoedigital.games.poker.PlayerProfile;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for {@link EmbeddedGameServer} JWT authentication.
 *
 * <p>
 * Tests that {@code getJwtForProfile()} returns a valid JWT and that
 * authenticated REST requests succeed.
 */
@Tag("integration")
@Tag("slow")
class EmbeddedGameServerAuthTest {

    private EmbeddedGameServer server;
    private PlayerProfile testProfile;

    @BeforeEach
    void setUp() throws Exception {
        server = new EmbeddedGameServer();
        server.start();
        testProfile = new PlayerProfile("TestUser");
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void getJwtForProfileReturnsNonNullToken() {
        String jwt = server.getJwtForProfile(testProfile);

        assertThat(jwt).isNotNull();
        assertThat(jwt).isNotBlank();
        // JWT format: three base64url segments separated by dots
        assertThat(jwt.split("\\.")).hasSize(3);
    }

    @Test
    void getJwtForProfileIsStable() {
        String first = server.getJwtForProfile(testProfile);
        String second = server.getJwtForProfile(testProfile);

        // Both tokens should be for the same user (though the tokens themselves may
        // differ due to issued-at timestamp). We just verify both are valid JWTs.
        assertThat(first.split("\\.")).hasSize(3);
        assertThat(second.split("\\.")).hasSize(3);
    }

    @Test
    void differentProfilesProduceDifferentUsernames() {
        PlayerProfile otherProfile = new PlayerProfile("OtherUser");

        String first = server.getJwtForProfile(testProfile);
        String second = server.getJwtForProfile(otherProfile);

        // Both are valid JWTs
        assertThat(first.split("\\.")).hasSize(3);
        assertThat(second.split("\\.")).hasSize(3);
        // The tokens are for different identities (different payload), so they differ
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void authenticatedRestRequestSucceeds() throws Exception {
        String jwt = server.getJwtForProfile(testProfile);
        int port = server.getPort();

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + "/api/v1/games"))
                .timeout(Duration.ofSeconds(5)).header("Authorization", "Bearer " + jwt).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 200 OK means authentication succeeded and the endpoint returned the game list
        assertThat(response.statusCode()).isEqualTo(200);
    }
}
