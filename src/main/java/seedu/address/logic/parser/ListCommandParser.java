package seedu.address.logic.parser;

import seedu.address.logic.commands.ListCommand;
import seedu.address.logic.parser.exceptions.ParseException;

/** Parses the parameter-free list command. */
public class ListCommandParser implements Parser<ListCommand> {
    public static final String MESSAGE_PARAMETERS = "Error: List does not accept parameters.";

    @Override
    public ListCommand parse(String args) throws ParseException {
        if (!args.matches(" *")) {
            throw new ParseException(MESSAGE_PARAMETERS);
        }
        return new ListCommand();
    }
}
