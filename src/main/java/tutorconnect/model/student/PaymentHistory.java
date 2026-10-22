package tutorconnect.model.student;

import static java.util.Objects.requireNonNull;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Immutable payment statuses recorded for a student's months. */
public final class PaymentHistory {
    public static final String MESSAGE_INVALID_MONTH = "Error: Invalid month. Enter a valid month in YYYY-MM format.";
    private static final String MESSAGE_INVALID_HISTORY =
            "Payment history must map YYYY-MM months to pending or completed.";
    private static final Pattern MONTH_PATTERN = Pattern.compile("[0-9]{4}-(0[1-9]|1[0-2])");

    /** Typed storage codec for the optional JSON key {@code payments}. */
    public static final StudentField<PaymentHistory> FIELD = new StudentField<>() {
        @Override
        public String key() {
            return "payments";
        }

        @Override
        public PaymentHistory decode(JsonNode value) {
            if (!value.isObject()) {
                throw new IllegalArgumentException(MESSAGE_INVALID_HISTORY);
            }
            NavigableMap<YearMonth, PaymentStatus> result = new TreeMap<>();
            value.fields().forEachRemaining(entry -> {
                YearMonth month = parseMonth(entry.getKey());
                if (!entry.getValue().isTextual()) {
                    throw new IllegalArgumentException(MESSAGE_INVALID_HISTORY);
                }
                PaymentStatus status;
                try {
                    status = PaymentStatus.parse(entry.getValue().asText());
                } catch (IllegalArgumentException exception) {
                    throw new IllegalArgumentException(MESSAGE_INVALID_HISTORY, exception);
                }
                result.put(month, status);
            });
            return new PaymentHistory(result);
        }

        @Override
        public JsonNode encode(PaymentHistory value) {
            ObjectNode encoded = JsonNodeFactory.instance.objectNode();
            value.statuses.forEach((month, status) -> encoded.put(month.toString(), status.toString()));
            return encoded;
        }
    };

    private final NavigableMap<YearMonth, PaymentStatus> statuses;

    /** Creates a sorted, immutable snapshot of recorded month statuses. */
    public PaymentHistory(Map<YearMonth, PaymentStatus> statuses) {
        requireNonNull(statuses);
        TreeMap<YearMonth, PaymentStatus> copy = new TreeMap<>();
        statuses.forEach((month, status) -> copy.put(requireNonNull(month), requireNonNull(status)));
        this.statuses = Collections.unmodifiableNavigableMap(copy);
    }

    /** Returns an empty history for a student with no recorded months. */
    public static PaymentHistory empty() {
        return new PaymentHistory(Map.of());
    }

    /** Formats recorded months in chronological order for the selected-student details panel. */
    public static List<String> detailLines(StudentFields fields) {
        Optional<PaymentHistory> history;
        try {
            history = fields.get(FIELD);
        } catch (IllegalArgumentException exception) {
            return List.of("Payment history: stored data could not be read");
        }
        if (history.isEmpty() || history.get().statuses.isEmpty()) {
            return List.of("Payment history: —");
        }
        List<String> lines = new ArrayList<>();
        lines.add("Payment history:");
        history.get().statuses.forEach((month, status) -> lines.add(month + ": " + status));
        return List.copyOf(lines);
    }

    /** Parses a strict four-digit year and two-digit calendar month. */
    public static YearMonth parseMonth(String value) {
        if (value == null || !MONTH_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(MESSAGE_INVALID_MONTH);
        }
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(MESSAGE_INVALID_MONTH, exception);
        }
    }

    public Optional<PaymentStatus> getStatus(YearMonth month) {
        return Optional.ofNullable(statuses.get(requireNonNull(month)));
    }

    /** Returns a sorted immutable view for display and testing. */
    public NavigableMap<YearMonth, PaymentStatus> asMap() {
        return statuses;
    }

    /** Returns a new history with only the requested month replaced. */
    public PaymentHistory withStatus(YearMonth month, PaymentStatus status) {
        TreeMap<YearMonth, PaymentStatus> copy = new TreeMap<>(statuses);
        copy.put(requireNonNull(month), requireNonNull(status));
        return new PaymentHistory(copy);
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof PaymentHistory history && statuses.equals(history.statuses);
    }

    @Override
    public int hashCode() {
        return statuses.hashCode();
    }

    @Override
    public String toString() {
        return statuses.toString();
    }
}
