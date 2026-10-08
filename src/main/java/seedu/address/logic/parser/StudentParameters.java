package seedu.address.logic.parser;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.address.logic.parser.exceptions.ParseException;

/** Shared structural parser for single-letter student parameters; values remain raw until validated. */
public final class StudentParameters {
    public static final String UNEXPECTED_TEXT = "Error: Unexpected text after command.";
    private static final Pattern BOUNDARY = Pattern.compile("(?<!\\S)([A-Za-z]/)",
            Pattern.UNICODE_CHARACTER_CLASS);

    private StudentParameters() {}

    /**
     * Parses required parameters in documented order, e.g. n/NAME, p/PHONE, a/ADDRESS.
     * Whitespace-delimited single-letter slash tokens are syntax; embedded slashes remain value text.
     */
    public static Map<String, String> parse(String input, String... labels) throws ParseException {
        requireNonNull(input);
        Map<String, List<String>> values = new LinkedHashMap<>();
        for (String label : labels) {
            values.put(label.substring(0, 2), new ArrayList<>());
        }
        Matcher matcher = BOUNDARY.matcher(input);
        String previous = null;
        int start = 0;
        while (matcher.find()) {
            String rawValue = input.substring(start, matcher.start());
            if (previous == null && !rawValue.isBlank()) {
                throw new ParseException(UNEXPECTED_TEXT);
            }
            if (previous != null) {
                values.get(previous).add(rawValue);
            }
            String prefix = matcher.group(1);
            if (!values.containsKey(prefix)) {
                throw new ParseException("Error: Unknown parameter: " + prefix + ".");
            }
            previous = prefix;
            start = matcher.end();
        }
        if (previous == null) {
            if (!input.isBlank()) {
                throw new ParseException(UNEXPECTED_TEXT);
            }
        } else {
            values.get(previous).add(input.substring(start));
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (String label : labels) {
            String prefix = label.substring(0, 2);
            List<String> occurrences = values.get(prefix);
            if (occurrences.isEmpty()) {
                throw new ParseException("Error: Missing required parameter: " + label + ".");
            }
            if (occurrences.size() > 1) {
                throw new ParseException("Error: Parameter " + prefix + " may be specified only once.");
            }
            result.put(prefix, occurrences.getFirst());
        }
        return result;
    }
}
