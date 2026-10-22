package tutorconnect.model.student;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.YearMonth;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class PaymentHistoryTest {
    @Test
    public void parseMonth_acceptsStrictCalendarMonths() {
        assertEquals(YearMonth.of(2026, 10), PaymentHistory.parseMonth("2026-10"));
        assertEquals(YearMonth.of(2024, 2), PaymentHistory.parseMonth("2024-02"));
        for (String invalid : new String[] {"", "2026-1", "26-10", "2026-00", "2026-13", "2026/10",
            "2026-02-01", " 2026-10", "2026-10 "}) {
            assertThrows(IllegalArgumentException.class, () -> PaymentHistory.parseMonth(invalid), invalid);
        }
    }

    @Test
    public void fieldCodec_roundTripsSortedStatuses() {
        PaymentHistory source = new PaymentHistory(Map.of(
                YearMonth.of(2026, 12), PaymentStatus.COMPLETED,
                YearMonth.of(2026, 2), PaymentStatus.PENDING));
        JsonNode encoded = PaymentHistory.FIELD.encode(source);

        assertEquals("pending", encoded.get("2026-02").asText());
        assertEquals("completed", encoded.get("2026-12").asText());
        assertEquals(source, PaymentHistory.FIELD.decode(encoded));
        assertThrows(UnsupportedOperationException.class, () ->
                source.asMap().put(YearMonth.of(2026, 3), PaymentStatus.PENDING));
    }

    @Test
    public void fieldCodec_rejectsMalformedStoredValues() {
        var json = JsonNodeFactory.instance;
        for (JsonNode invalid : new JsonNode[] {
                json.textNode("pending"), json.arrayNode(), json.objectNode().put("2026-13", "pending"),
                json.objectNode().put("2026-10", "PENDING"), json.objectNode().put("2026-10", "unknown"),
                json.objectNode().put("2026-10", 1)
        }) {
            assertThrows(IllegalArgumentException.class, ()
                    -> PaymentHistory.FIELD.decode(invalid));
        }
    }
}
