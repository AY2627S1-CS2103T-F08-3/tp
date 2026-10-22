package tutorconnect.logic.parser;

import static tutorconnect.logic.parser.CliSyntax.PREFIX_RECORD;

import java.util.regex.Pattern;

import tutorconnect.commons.core.index.Index;
import tutorconnect.logic.commands.UnassessCommand;
import tutorconnect.logic.parser.IndexedParameterParserUtil.ParsedParameter;
import tutorconnect.logic.parser.exceptions.ParseException;

/** Parses {@code unassess INDEX i/RECORD}, where RECORD is a positive whole number of any size. */
public class UnassessCommandParser implements Parser<UnassessCommand> {

    private static final Pattern RECORD_NUMBER = Pattern.compile("[1-9][0-9]*");

    @Override
    public UnassessCommand parse(String arguments) throws ParseException {
        ParsedParameter parsed = IndexedParameterParserUtil.parse(arguments, PREFIX_RECORD, "i/RECORD");
        String record = parsed.value().strip();
        if (!RECORD_NUMBER.matcher(record).matches()) {
            throw new ParseException(UnassessCommand.MESSAGE_INVALID_RECORD);
        }
        return new UnassessCommand(parsed.index(), Index.fromOneBased(record));
    }
}
