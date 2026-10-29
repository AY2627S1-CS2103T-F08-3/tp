package tutorconnect.logic;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import tutorconnect.logic.parser.Prefix;
import tutorconnect.model.student.Student;

/**
 * Container for user visible messages.
 */
public class Messages {

    public static final String MESSAGE_UNKNOWN_COMMAND =
            "Error: Unknown command. Type help to view available commands.";
    public static final String MESSAGE_INVALID_STUDENT_DISPLAYED_INDEX =
            "Error: No student exists at index %1$s.";
    public static final String MESSAGE_STUDENTS_LISTED_OVERVIEW = "%1$d student(s) listed!";
    public static final String MESSAGE_DUPLICATE_FIELDS =
                "Multiple values specified for the following single-valued field(s): ";
    public static final String MESSAGE_UNKNOWN_PARAMETER = "Error: Unknown parameter: %1$s.";
    public static final String MESSAGE_MISSING_REQUIRED_PARAMETER = "Error: Missing required parameter: %1$s.";
    public static final String MESSAGE_PARAMETER_SPECIFIED_ONLY_ONCE =
            "Error: Parameter %1$s may be specified only once.";
    public static final String MESSAGE_UNEXPECTED_TEXT = "Error: Unexpected text after command.";

    /**
     * Returns an error message indicating the duplicate prefixes.
     */
    public static String getErrorMessageForDuplicatePrefixes(Prefix... duplicatePrefixes) {
        assert duplicatePrefixes.length > 0;

        Set<String> duplicateFields =
                Stream.of(duplicatePrefixes).map(Prefix::toString).collect(Collectors.toSet());

        return MESSAGE_DUPLICATE_FIELDS + String.join(" ", duplicateFields);
    }

    /**
     * Formats the {@code student} for display to the user.
     */
    public static String format(Student student) {
        final StringBuilder builder = new StringBuilder();
        builder.append(student.getName())
                .append("; Phone: ")
                .append(student.getPhone())
                .append("; Email: ")
                .append(student.getEmail())
                .append("; Address: ")
                .append(student.getAddress())
                .append("; Tags: ");
        student.getTags().forEach(builder::append);
        return builder.toString();
    }

}
