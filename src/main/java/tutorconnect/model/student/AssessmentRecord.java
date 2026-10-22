package tutorconnect.model.student;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import tutorconnect.commons.util.AppUtil;
import tutorconnect.commons.util.StudentText;

/**
 * One dated assessment result, such as an exam or a test. The grade is free text ("A1", "85/100", "B+") and is
 * never interpreted as a number, so different grading systems are not implied to be comparable.
 */
public final class AssessmentRecord {

    public static final int MAX_TITLE_LENGTH = 60;
    public static final int MAX_GRADE_LENGTH = 20;

    public static final String MESSAGE_INVALID_DATE =
            "Error: Invalid date. Use a real date in yyyy-MM-dd format, e.g. 2026-10-31.";
    public static final String MESSAGE_INVALID_TITLE =
            "Error: Invalid title. Use 1-60 letters, numbers, spaces, or the symbols & / ' ( ) - . , : #.";
    public static final String MESSAGE_INVALID_GRADE =
            "Error: Invalid grade. Use 1-20 letters, numbers, spaces, or the symbols / . % + -.";

    private static final Pattern DATE_SHAPE = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}");
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);
    private static final Pattern TITLE_CHARACTERS = Pattern.compile("[\\p{L}\\p{Nd} &/'()\\-.,:#]+");
    private static final Pattern GRADE_CHARACTERS = Pattern.compile("[\\p{L}\\p{Nd} /.%+\\-]+");

    private final LocalDate date;
    private final String title;
    private final String grade;

    /**
     * Constructs a record from an already parsed date and the title and grade as typed.
     *
     * @throws IllegalArgumentException with the title or grade message if either is invalid.
     */
    public AssessmentRecord(LocalDate date, String title, String grade) {
        requireNonNull(date);
        this.date = date;
        this.title = normalizeText(title, MAX_TITLE_LENGTH, TITLE_CHARACTERS, MESSAGE_INVALID_TITLE);
        this.grade = normalizeText(grade, MAX_GRADE_LENGTH, GRADE_CHARACTERS, MESSAGE_INVALID_GRADE);
    }

    /**
     * Builds a record from raw text, validating the date, then the title, then the grade.
     *
     * @throws IllegalArgumentException with the message of the first invalid part.
     */
    public static AssessmentRecord of(String date, String title, String grade) {
        return new AssessmentRecord(parseDate(date), title, grade);
    }

    /**
     * Parses a zero-padded {@code yyyy-MM-dd} date that exists in the calendar.
     *
     * @throws IllegalArgumentException if the text is not such a date.
     */
    public static LocalDate parseDate(String text) {
        requireNonNull(text);
        String normalized;
        try {
            normalized = StudentText.normalize(text);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(MESSAGE_INVALID_DATE, e);
        }
        AppUtil.checkArgument(DATE_SHAPE.matcher(normalized).matches(), MESSAGE_INVALID_DATE);
        try {
            return LocalDate.parse(normalized, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(MESSAGE_INVALID_DATE, e);
        }
    }

    private static String normalizeText(String text, int maxLength, Pattern allowed, String message) {
        requireNonNull(text);
        String normalized;
        try {
            normalized = StudentText.normalize(text);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(message, e);
        }
        int length = StudentText.length(normalized);
        AppUtil.checkArgument(length >= 1 && length <= maxLength && allowed.matcher(normalized).matches()
                && normalized.codePoints().anyMatch(Character::isLetterOrDigit), message);
        return normalized;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getTitle() {
        return title;
    }

    public String getGrade() {
        return grade;
    }

    /** Two records are the same assessment if they share a date and a title, ignoring case and spacing. */
    public boolean isSameAssessment(AssessmentRecord other) {
        return other != null && date.equals(other.date) && title.toLowerCase(Locale.ROOT)
                .equals(other.title.toLowerCase(Locale.ROOT));
    }

    @Override
    public String toString() {
        return date + " " + title + ": " + grade;
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof AssessmentRecord otherRecord && date.equals(otherRecord.date)
                && title.equals(otherRecord.title) && grade.equals(otherRecord.grade);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, title, grade);
    }
}
