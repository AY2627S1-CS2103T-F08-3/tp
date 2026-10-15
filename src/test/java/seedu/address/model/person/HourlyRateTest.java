package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;

/** Monetary validation applies to every entry point, including owner codecs and legacy JSON. */
public class HourlyRateTest {
    @Test
    public void constructor_boundariesAndCentAmounts_areExactAndCanonical() {
        for (String amount : List.of("1", "1.0", "1.00", "1.01", "19.99", "999.99", "1000.00")) {
            HourlyRate rate = new HourlyRate(new BigDecimal(amount));
            assertEquals(new BigDecimal(amount).setScale(2), rate.value);
            assertEquals(2, rate.value.scale());
            assertEquals(rate.value.toPlainString(), rate.toString());
            assertTrue(HourlyRate.isValid(new BigDecimal(amount)));
        }
    }

    @Test
    public void constructor_invalidRangeAndPrecision_rejectedBeforeNormalization() {
        for (String amount : List.of("-1", "0", "0.99", "1000.01", "9999999999999999999999",
                "1.001", "40.000", "1000.000", "1E+100", "1E-100")) {
            BigDecimal value = new BigDecimal(amount);
            assertFalse(HourlyRate.isValid(value), amount);
            IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () ->
                    new HourlyRate(value), amount);
            assertEquals(HourlyRate.MESSAGE_CONSTRAINTS, failure.getMessage());
        }
        assertFalse(HourlyRate.isValid(null));
        assertThrows(NullPointerException.class, () -> new HourlyRate(null));
    }

    @Test
    public void equals_equivalentAmounts_haveEqualHashCodes() {
        HourlyRate whole = new HourlyRate(new BigDecimal("40"));
        for (String amount : List.of("40.0", "40.00", "4E+1")) {
            HourlyRate equivalent = new HourlyRate(new BigDecimal(amount));
            assertEquals(whole, equivalent);
            assertEquals(whole.hashCode(), equivalent.hashCode());
        }
        assertNotEquals(whole, new HourlyRate(new BigDecimal("40.01")));
        assertNotEquals(whole, null);
        assertNotEquals(whole, "40.00");
    }

    @Test
    public void codec_textAndDecimalNodes_roundTripWithoutFloatingPoint() {
        for (String amount : List.of("1", "19.99", "999.99", "1000")) {
            HourlyRate expected = new HourlyRate(new BigDecimal(amount));
            assertEquals(expected, HourlyRate.FIELD.decode(TextNode.valueOf(amount)));
            assertEquals(expected, HourlyRate.FIELD.decode(new DecimalNode(new BigDecimal(amount))));
            assertEquals(TextNode.valueOf(expected.toString()), HourlyRate.FIELD.encode(expected));
        }
        for (var invalid : List.of(TextNode.valueOf("40.000"), new DecimalNode(new BigDecimal("40.000")),
                TextNode.valueOf("NaN"), JsonNodeFactory.instance.booleanNode(true),
                JsonNodeFactory.instance.objectNode(), JsonNodeFactory.instance.arrayNode(),
                JsonNodeFactory.instance.nullNode())) {
            assertThrows(IllegalArgumentException.class, () -> HourlyRate.FIELD.decode(invalid));
        }
    }
}
