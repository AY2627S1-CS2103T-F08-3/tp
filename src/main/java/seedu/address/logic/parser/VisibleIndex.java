package seedu.address.logic.parser;

import java.util.List;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;

/** Validates and resolves a one-based position in the currently displayed list. */
public final class VisibleIndex {
    public static final String MESSAGE_INVALID =
            "Error: Invalid index. Enter a positive whole number without leading zeroes.";

    private final String value;

    private VisibleIndex(String value) {
        this.value = value;
    }

    /** Keeps decimal text so even arbitrarily large, syntactically valid indices are safe. */
    public static VisibleIndex parse(String value) throws ParseException {
        if (!value.matches("[1-9][0-9]*")) {
            throw new ParseException(MESSAGE_INVALID);
        }
        return new VisibleIndex(value);
    }

    /** Resolves against the visible list, never against the unfiltered register. */
    public <T> T resolve(List<T> displayed) throws CommandException {
        String size = Integer.toString(displayed.size());
        if (value.length() > size.length()
                || value.length() == size.length() && value.compareTo(size) > 0) {
            throw new CommandException("Error: No student exists at index " + value + ".");
        }
        return displayed.get(Integer.parseInt(value) - 1);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof VisibleIndex index && value.equals(index.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
