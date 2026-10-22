package tutorconnect.model.student;

import java.util.Locale;

/** A manually recorded monthly payment state. */
public enum PaymentStatus {
    PENDING,
    COMPLETED;

    public static final String MESSAGE_INVALID = "Error: Invalid payment status. Use pending or completed.";

    /** Parses the exact lowercase command/storage representation. */
    public static PaymentStatus parse(String value) {
        return switch (value) {
            case "pending" -> PENDING;
            case "completed" -> COMPLETED;
            default -> throw new IllegalArgumentException(MESSAGE_INVALID);
        };
    }

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
