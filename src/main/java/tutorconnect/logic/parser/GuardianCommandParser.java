package tutorconnect.logic.parser;

import static tutorconnect.logic.parser.CliSyntax.PREFIX_GUARDIAN_PHONE;

import tutorconnect.logic.commands.GuardianCommand;
import tutorconnect.logic.parser.IndexedParameterParserUtil.ParsedParameter;
import tutorconnect.logic.parser.exceptions.ParseException;

/** Parses input arguments for the {@code guardian} command. */
public class GuardianCommandParser implements Parser<GuardianCommand> {

    @Override
    public GuardianCommand parse(String arguments) throws ParseException {
        ParsedParameter parsed = IndexedParameterParserUtil.parse(arguments, PREFIX_GUARDIAN_PHONE, "g/PHONE");
        return new GuardianCommand(parsed.index(), ParserUtil.parseGuardianPhone(parsed.value()));
    }
}
