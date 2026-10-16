package tutorconnect.model.student;

import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import tutorconnect.commons.util.AppUtil;

/** An hourly rate in SGD, stored with exactly two decimal places. */
public final class HourlyRate {

    public static final String MESSAGE_CONSTRAINTS =
            "Hourly rate must be from 1.00 to 1000.00 SGD with at most two decimal places.";
    private static final BigDecimal MINIMUM = new BigDecimal("1.00");
    private static final BigDecimal MAXIMUM = new BigDecimal("1000.00");

    public static final StudentField<HourlyRate> FIELD = new StudentField<>() {
        @Override
        public String key() {
            return "hourlyRate";
        }

        @Override
        public HourlyRate decode(JsonNode value) {
            AppUtil.checkArgument(value.isTextual() || value.isNumber(), MESSAGE_CONSTRAINTS);
            try {
                return new HourlyRate(new BigDecimal(value.asText()));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(MESSAGE_CONSTRAINTS, exception);
            }
        }

        @Override
        public JsonNode encode(HourlyRate value) {
            return TextNode.valueOf(value.value.toPlainString());
        }
    };

    public final BigDecimal value;

    /** Constructs an hourly rate without rounding its value. */
    public HourlyRate(BigDecimal amount) {
        requireNonNull(amount);
        AppUtil.checkArgument(isValid(amount), MESSAGE_CONSTRAINTS);
        value = amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    /** Returns whether {@code amount} is within the allowed SGD range and has cent precision. */
    public static boolean isValid(BigDecimal amount) {
        return amount != null
                && amount.compareTo(MINIMUM) >= 0
                && amount.compareTo(MAXIMUM) <= 0
                && amount.scale() <= 2;
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof HourlyRate otherRate && value.equals(otherRate.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
