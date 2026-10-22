package tutorconnect.logic.parser;

import static java.util.Objects.requireNonNull;
import static tutorconnect.logic.Messages.MESSAGE_MISSING_REQUIRED_PARAMETER;
import static tutorconnect.logic.Messages.MESSAGE_UNEXPECTED_TEXT;
import static tutorconnect.logic.Messages.MESSAGE_UNKNOWN_PARAMETER;
import static tutorconnect.logic.parser.CliSyntax.PREFIX_NOTE;

import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tutorconnect.commons.core.index.Index;
import tutorconnect.logic.commands.NoteCommand;
import tutorconnect.logic.parser.exceptions.ParseException;
import tutorconnect.model.student.AcademicNote;

/**
 * Parses {@code note INDEX m/NOTE}. The command has one parameter, so everything after the first {@code m/}
 * is the note and may contain slashes such as {@code and/or}, {@code w/} or {@code m/s}.
 */
public class NoteCommandParser implements Parser<NoteCommand> {

    private static final Pattern NOTE_PREFIX = Pattern.compile("(?:^|\\s)" + Pattern.quote(PREFIX_NOTE.getPrefix()));
    private static final Pattern PREFIX_TOKEN = Pattern.compile("(?:^|\\s)([A-Za-z][A-Za-z0-9]*/)");

    @Override
    public NoteCommand parse(String arguments) throws ParseException {
        requireNonNull(arguments);
        String normalized = Normalizer.normalize(arguments, Normalizer.Form.NFKC).stripLeading();

        Matcher noteMatcher = NOTE_PREFIX.matcher(normalized);
        boolean hasNote = noteMatcher.find();
        String beforeNote = hasNote ? normalized.substring(0, noteMatcher.start()) : normalized;

        Matcher unknown = PREFIX_TOKEN.matcher(beforeNote);
        if (unknown.find()) {
            throw new ParseException(String.format(MESSAGE_UNKNOWN_PARAMETER, unknown.group(1)));
        }
        String[] tokens = beforeNote.isBlank() ? new String[0] : beforeNote.strip().split("\\s+");
        if (tokens.length > 1) {
            throw new ParseException(MESSAGE_UNEXPECTED_TEXT);
        }
        if (!hasNote) {
            throw new ParseException(String.format(MESSAGE_MISSING_REQUIRED_PARAMETER, "m/NOTE"));
        }

        Index index = ParserUtil.parseIndex(tokens.length == 0 ? "" : tokens[0]);
        try {
            return new NoteCommand(index, new AcademicNote(normalized.substring(noteMatcher.end())));
        } catch (IllegalArgumentException e) {
            throw new ParseException(AcademicNote.MESSAGE_CONSTRAINTS, e);
        }
    }
}
