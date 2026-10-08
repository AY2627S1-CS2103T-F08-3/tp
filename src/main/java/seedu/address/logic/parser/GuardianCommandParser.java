package seedu.address.logic.parser;

import static seedu.address.logic.parser.CliSyntax.PREFIX_GUARDIAN_PHONE;

import seedu.address.logic.commands.GuardianCommand;
import seedu.address.logic.parser.IndexedParameterParserUtil.ParsedParameter;
import seedu.address.logic.parser.exceptions.ParseException;

/** Parses input arguments for the {@code guardian} command. */
public class GuardianCommandParser implements Parser<GuardianCommand> {

    @Override
    public GuardianCommand parse(String arguments) throws ParseException {
        ParsedParameter parsed = IndexedParameterParserUtil.parse(arguments, PREFIX_GUARDIAN_PHONE, "g/PHONE");
        return new GuardianCommand(parsed.index(), ParserUtil.parseGuardianPhone(parsed.value()));
    }
}
