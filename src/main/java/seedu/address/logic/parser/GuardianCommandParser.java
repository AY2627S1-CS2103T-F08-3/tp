package seedu.address.logic.parser;

import seedu.address.commons.core.index.Index;
import seedu.address.commons.util.StudentText;
import seedu.address.logic.commands.GuardianCommand;
import seedu.address.logic.parser.StudentParameters.IndexedParameters;
import seedu.address.logic.parser.exceptions.ParseException;

/** Parses input arguments for the {@code guardian} command. */
public class GuardianCommandParser implements Parser<GuardianCommand> {

    @Override
    public GuardianCommand parse(String arguments) throws ParseException {
        IndexedParameters parsed = StudentParameters.parseIndexedValues(arguments, "g/PHONE");
        Index index;
        try {
            index = ParserUtil.parseIndex(StudentText.normalize(parsed.index()));
        } catch (IllegalArgumentException e) {
            throw new ParseException(ParserUtil.MESSAGE_INVALID_INDEX, e);
        }
        return new GuardianCommand(index, ParserUtil.parseGuardianPhone(parsed.values().get("g/")));
    }
}
