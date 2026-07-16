package com.emall.search.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.exception.BusinessException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class SearchCursorCodecTest {
    private static final String SECRET = "test-search-cursor-secret-with-32-characters";
    private static final Instant NOW = Instant.parse("2026-07-15T08:00:00Z");
    private final SearchCursorCodec codec = new SearchCursorCodec(
            JsonMapper.builder().addModule(new JavaTimeModule()).build(), SECRET, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void shouldRoundTripSignedCursor() {
        SearchCursorState state =
                new SearchCursorState("pit-1", List.of(9.5, 1_700L, 30001L), "fingerprint", 20, NOW.plusSeconds(120));

        SearchCursorState decoded = codec.decode(codec.encode(state));

        assertThat(decoded.pitId()).isEqualTo("pit-1");
        assertThat(decoded.sortValues()).containsExactly(9.5, 1_700, 30001);
        assertThat(decoded.seen()).isEqualTo(20);
    }

    @Test
    void shouldRejectTamperedOrExpiredCursor() {
        SearchCursorState valid = new SearchCursorState("pit-1", List.of(1L), "fingerprint", 1, NOW.plusSeconds(30));
        SearchCursorState expired = new SearchCursorState("pit-1", List.of(1L), "fingerprint", 1, NOW.minusSeconds(1));
        String token = codec.encode(valid);
        String tampered = (token.charAt(0) == 'A' ? 'B' : 'A') + token.substring(1);

        assertThatThrownBy(() -> codec.decode(tampered)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("invalid search cursor");
        assertThatThrownBy(() -> codec.decode(codec.encode(expired))).isInstanceOf(BusinessException.class)
                .hasMessageContaining("expired");
    }
}
