package tutorconnect.ui;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Optional;

/**
 * Read-only reference of command formats used by the command bar's completion and hints and by the command rail.
 * It only describes the existing commands; parsing and validation remain in {@code TutorConnectParser}.
 */
public final class CommandGuide {
    /** One command word with its format and the group it is listed under. */
    public record Entry(String word, String format, String group) {}

    public static final List<String> GROUPS = List.of("Students", "Details", "Lessons", "Records", "Payments", "App");

    private static final List<Entry> ENTRIES = List.of(
            new Entry("add", "add n/NAME p/PHONE a/ADDRESS", "Students"),
            new Entry("edit", "edit INDEX [n/NAME] [p/PHONE] [e/EMAIL] [a/ADDRESS] [t/TAG]...", "Students"),
            new Entry("delete", "delete INDEX", "Students"),
            new Entry("list", "list", "Students"),
            new Entry("find", "find KEYWORD [MORE_KEYWORDS]", "Students"),
            new Entry("filter", "filter [l/LEVEL] [s/SUBJECT]", "Students"),
            new Entry("clear", "clear", "Students"),
            new Entry("guardian", "guardian INDEX g/PHONE", "Details"),
            new Entry("level", "level INDEX l/LEVEL", "Details"),
            new Entry("subject", "subject INDEX s/SUBJECT", "Details"),
            new Entry("rate", "rate INDEX r/RATE", "Details"),
            new Entry("note", "note INDEX m/NOTE", "Details"),
            new Entry("clearnote", "clearnote INDEX", "Details"),
            new Entry("schedule", "schedule INDEX d/DAY t/TIME", "Lessons"),
            new Entry("unschedule", "unschedule INDEX", "Lessons"),
            new Entry("timetable", "timetable [d/DAY]", "Lessons"),
            new Entry("assess", "assess INDEX d/DATE t/TITLE g/GRADE", "Records"),
            new Entry("assessments", "assessments INDEX", "Records"),
            new Entry("unassess", "unassess INDEX i/RECORD", "Records"),
            new Entry("payment", "payment INDEX m/YYYY-MM s/pending|completed", "Payments"),
            new Entry("payments", "payments m/YYYY-MM [s/pending|completed]", "Payments"),
            new Entry("help", "help", "App"),
            new Entry("exit", "exit", "App"));

    private CommandGuide() {}

    public static List<Entry> entries() {
        return ENTRIES;
    }

    /** Returns the command words starting with {@code prefix}, in guide order. */
    public static List<String> wordsStartingWith(String prefix) {
        requireNonNull(prefix);
        return ENTRIES.stream().map(Entry::word).filter(word -> word.startsWith(prefix)).toList();
    }

    /**
     * Completes the command word being typed. Text that already contains a space is left alone, so completion never
     * rewrites parameters. A single match is completed with a trailing space; several matches are extended to their
     * longest common prefix (an exact word such as {@code payment} is completed even though {@code payments} exists).
     */
    public static Optional<String> complete(String text) {
        requireNonNull(text);
        String typed = text.stripLeading();
        if (typed.isEmpty() || typed.contains(" ")) {
            return Optional.empty();
        }
        List<String> matches = wordsStartingWith(typed);
        if (matches.isEmpty()) {
            return Optional.empty();
        }
        if (matches.size() == 1 || matches.contains(typed)) {
            String word = matches.size() == 1 ? matches.get(0) : typed;
            return Optional.of(word + " ");
        }
        String common = commonPrefix(matches);
        return common.length() > typed.length() ? Optional.of(common) : Optional.empty();
    }

    /**
     * Returns the hint for the command bar: the format of the typed command word, the candidates while a word is
     * still being typed, or an empty string when nothing is typed or no command matches.
     */
    public static String hintFor(String text) {
        requireNonNull(text);
        String typed = text.stripLeading();
        if (typed.isEmpty()) {
            return "";
        }
        String word = typed.split("\\s+", 2)[0];
        Optional<Entry> exact = ENTRIES.stream().filter(entry -> entry.word().equals(word)).findFirst();
        boolean isWordFinished = typed.length() > word.length();
        if (exact.isPresent() && (isWordFinished || wordsStartingWith(word).size() == 1)) {
            return exact.get().format();
        }
        if (isWordFinished) {
            return "";
        }
        List<String> matches = wordsStartingWith(word);
        if (matches.size() == 1) {
            return formatOf(matches.get(0)) + "   (Tab to complete)";
        }
        return matches.isEmpty() ? "" : String.join("   ", matches);
    }

    private static String formatOf(String word) {
        return ENTRIES.stream().filter(entry -> entry.word().equals(word)).findFirst().orElseThrow().format();
    }

    private static String commonPrefix(List<String> words) {
        String prefix = words.get(0);
        for (String word : words) {
            while (!word.startsWith(prefix)) {
                prefix = prefix.substring(0, prefix.length() - 1);
            }
        }
        return prefix;
    }
}
