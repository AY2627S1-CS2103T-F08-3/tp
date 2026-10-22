package tutorconnect.logic.parser;

import static java.util.Objects.requireNonNull;
import static tutorconnect.logic.Messages.MESSAGE_MISSING_REQUIRED_PARAMETER;
import static tutorconnect.logic.Messages.MESSAGE_UNEXPECTED_TEXT;

import java.text.Normalizer;

import tutorconnect.logic.commands.AssessmentsCommand;
import tutorconnect.logic.parser.exceptions.ParseException;

/** Parses {@code assessments INDEX}: exactly one displayed index and nothing else. */
public class AssessmentsCommandParser implements Parser<AssessmentsCommand> {

    @Override
    public AssessmentsCommand parse(String arguments) throws ParseException {
        requireNonNull(arguments);
        String normalized = Normalizer.normalize(arguments, Normalizer.Form.NFKC).strip();
        if (normalized.isEmpty()) {
            throw new ParseException(String.format(MESSAGE_MISSING_REQUIRED_PARAMETER, "INDEX"));
        }
        if (normalized.split("\\s+").length > 1) {
            throw new ParseException(MESSAGE_UNEXPECTED_TEXT);
        }
        return new AssessmentsCommand(ParserUtil.parseIndex(normalized));
    }
}
