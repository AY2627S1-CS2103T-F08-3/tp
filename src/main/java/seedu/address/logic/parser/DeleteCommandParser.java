package seedu.address.logic.parser;

import seedu.address.commons.core.index.Index;
import seedu.address.commons.util.StudentText;
import seedu.address.logic.commands.DeleteCommand;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * Parses input arguments and creates a new DeleteCommand object
 */
public class DeleteCommandParser implements Parser<DeleteCommand> {
    public static final String MESSAGE_MISSING = "Error: Missing required parameter: INDEX.";
    public static final String MESSAGE_MULTIPLE = "Error: Delete accepts exactly one index.";

    /**
     * Parses the given {@code String} of arguments in the context of the DeleteCommand
     * and returns a DeleteCommand object for execution.
     * @throws ParseException if the user input does not conform to the expected format
     */
    public DeleteCommand parse(String args) throws ParseException {
        final String normalized;
        try {
            normalized = StudentText.normalize(args);
        } catch (IllegalArgumentException exception) {
            throw new ParseException(ParserUtil.MESSAGE_INVALID_INDEX, exception);
        }
        if (normalized.isEmpty()) {
            throw new ParseException(MESSAGE_MISSING);
        }
        if (normalized.contains(" ")) {
            throw new ParseException(MESSAGE_MULTIPLE);
        }
        Index index = ParserUtil.parseIndex(normalized);
        return new DeleteCommand(index);
    }

}
