package tutorconnect.logic.parser;

import tutorconnect.logic.commands.ClearCommand;
import tutorconnect.logic.parser.exceptions.ParseException;

/** Parses the parameter-free clear command; stray text is refused because clear cannot be undone. */
public class ClearCommandParser implements Parser<ClearCommand> {
    public static final String MESSAGE_PARAMETERS = "Error: Clear does not accept parameters.";

    @Override
    public ClearCommand parse(String args) throws ParseException {
        if (!args.matches("(?U)\\s*")) {
            throw new ParseException(MESSAGE_PARAMETERS);
        }
        return new ClearCommand();
    }
}
