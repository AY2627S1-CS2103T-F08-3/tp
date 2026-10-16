package tutorconnect.logic.parser;

import static tutorconnect.logic.parser.CliSyntax.PREFIX_HOURLY_RATE;

import tutorconnect.logic.commands.RateCommand;
import tutorconnect.logic.parser.IndexedParameterParserUtil.ParsedParameter;
import tutorconnect.logic.parser.exceptions.ParseException;

/** Parses input arguments for the {@code rate} command. */
public class RateCommandParser implements Parser<RateCommand> {

    @Override
    public RateCommand parse(String arguments) throws ParseException {
        ParsedParameter parsed = IndexedParameterParserUtil.parse(arguments, PREFIX_HOURLY_RATE, "r/RATE");
        return new RateCommand(parsed.index(), ParserUtil.parseHourlyRate(parsed.value()));
    }
}
