package seedu.address.logic.parser;

import java.text.Normalizer;

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
        if (args.codePoints().anyMatch(c -> Character.isISOControl(c)
                || Character.getType(c) == Character.FORMAT || c == 0x2028 || c == 0x2029)) {
            throw new ParseException(VisibleIndex.MESSAGE_INVALID);
        }
        String value = Normalizer.normalize(args, Normalizer.Form.NFKC).strip();
        if (value.isEmpty()) {
            throw new ParseException(MESSAGE_MISSING);
        }
        if (value.split(" +").length > 1) {
            throw new ParseException(MESSAGE_MULTIPLE);
        }
        return new DeleteCommand(VisibleIndex.parse(value));
    }

}
