package tutorconnect.logic;

/**
 * Container for user visible messages.
 */
public class Messages {

    public static final String MESSAGE_UNKNOWN_COMMAND =
            "Error: Unknown command. Type help to view available commands.";
    public static final String MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX =
            "Error: No student exists at index %1$s.";
    public static final String MESSAGE_STUDENTS_LISTED_OVERVIEW = "%1$d student(s) listed!";
    public static final String MESSAGE_UNKNOWN_PARAMETER = "Error: Unknown parameter: %1$s.";
    public static final String MESSAGE_MISSING_REQUIRED_PARAMETER = "Error: Missing required parameter: %1$s.";
    public static final String MESSAGE_PARAMETER_SPECIFIED_ONLY_ONCE =
            "Error: Parameter %1$s may be specified only once.";
    public static final String MESSAGE_UNEXPECTED_TEXT = "Error: Unexpected text after command.";

    /**
     * Formats a sentence that ends with a full stop, keeping only one when the last value already ends with one.
     * For example, a student named {@code Tan Jr.} gives {@code Student added: Tan Jr.} rather than {@code Jr..}.
     */
    public static String formatSentence(String template, Object... values) {
        String sentence = String.format(template, values);
        return sentence.endsWith("..") && !sentence.endsWith("...")
                ? sentence.substring(0, sentence.length() - 1)
                : sentence;
    }
}
