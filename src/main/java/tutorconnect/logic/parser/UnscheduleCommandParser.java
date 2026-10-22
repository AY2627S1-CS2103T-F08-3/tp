package tutorconnect.logic.parser;

import tutorconnect.commons.util.StudentText;
import tutorconnect.logic.commands.UnscheduleCommand;
import tutorconnect.logic.parser.exceptions.ParseException;

/** Validates structure before resolving an arbitrary-size displayed index. */
public class UnscheduleCommandParser implements Parser<UnscheduleCommand> {
    @Override
    public UnscheduleCommand parse(String args) throws ParseException {
        if (StudentText.hasProhibitedCharacters(args)) {
            throw new ParseException(StudentParameters.UNEXPECTED_TEXT);
        }
        var parameters = StudentParameters.parseIndexed(args);
        return new UnscheduleCommand(VisibleIndex.parse(StudentText.normalize(parameters.index())));
    }
}
