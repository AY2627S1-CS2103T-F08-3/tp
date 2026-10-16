package tutorconnect.logic.parser;

import static tutorconnect.logic.parser.CliSyntax.PREFIX_LEVEL;

import tutorconnect.logic.commands.LevelCommand;
import tutorconnect.logic.parser.IndexedParameterParserUtil.ParsedParameter;
import tutorconnect.logic.parser.exceptions.ParseException;
import tutorconnect.model.student.EducationLevel;

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
