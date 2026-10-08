package seedu.address.logic.parser;

import static seedu.address.logic.parser.CliSyntax.PREFIX_HOURLY_RATE;

import seedu.address.logic.commands.RateCommand;
import seedu.address.logic.parser.IndexedParameterParserUtil.ParsedParameter;
import seedu.address.logic.parser.exceptions.ParseException;

/** Parses input arguments for the {@code rate} command. */
public class RateCommandParser implements Parser<RateCommand> {

    @Override
    public RateCommand parse(String arguments) throws ParseException {
        ParsedParameter parsed = IndexedParameterParserUtil.parse(arguments, PREFIX_HOURLY_RATE, "r/RATE");
        return new RateCommand(parsed.index(), ParserUtil.parseHourlyRate(parsed.value()));
    }
}
