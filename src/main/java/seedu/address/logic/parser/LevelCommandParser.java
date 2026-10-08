package seedu.address.logic.parser;

import static seedu.address.logic.parser.CliSyntax.PREFIX_LEVEL;

import seedu.address.logic.commands.LevelCommand;
import seedu.address.logic.parser.IndexedParameterParserUtil.ParsedParameter;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.EducationLevel;

/** Parses input arguments for the {@code level} command. */
public class LevelCommandParser implements Parser<LevelCommand> {

    @Override
    public LevelCommand parse(String arguments) throws ParseException {
        ParsedParameter parsed = IndexedParameterParserUtil.parse(arguments, PREFIX_LEVEL, "l/LEVEL");
        try {
            return new LevelCommand(parsed.index(), new EducationLevel(parsed.value()));
        } catch (IllegalArgumentException e) {
            throw new ParseException(EducationLevel.MESSAGE_CONSTRAINTS, e);
        }
    }
}
