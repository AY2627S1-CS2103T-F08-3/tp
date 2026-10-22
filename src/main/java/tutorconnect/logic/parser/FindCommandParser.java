package tutorconnect.logic.parser;

import static tutorconnect.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import java.util.List;
import java.util.logging.Logger;

import tutorconnect.commons.core.LogsCenter;
import tutorconnect.commons.util.StudentText;
import tutorconnect.logic.commands.FindCommand;
import tutorconnect.logic.parser.exceptions.ParseException;
import tutorconnect.model.student.NameContainsKeywordsPredicate;

/**
 * Parses input arguments and creates a new FindCommand object
 */
public class FindCommandParser implements Parser<FindCommand> {
    private static final Logger logger = LogsCenter.getLogger(FindCommandParser.class);

    /**
     * Parses the given {@code String} of arguments in the context of the FindCommand
     * and returns a FindCommand object for execution.
     * @throws ParseException if the user input does not conform to the expected format
     */
    public FindCommand parse(String args) throws ParseException {
        String trimmedArgs;
        try {
            // Keep legacy whitespace separators, then apply the same normalization as stored names.
            trimmedArgs = StudentText.normalize(args.replaceAll("\\s+", " "));
        } catch (IllegalArgumentException e) {
            logger.fine("Name query rejected: prohibited text");
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE), e);
        }
        if (trimmedArgs.isEmpty()) {
            logger.fine("Name query rejected: missing keywords");
            throw new ParseException(
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE));
        }

        String[] nameKeywords = trimmedArgs.split("\\s+");

        return new FindCommand(new NameContainsKeywordsPredicate(List.of(nameKeywords)));
    }

}
