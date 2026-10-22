package tutorconnect.logic.parser;

import java.util.Optional;

import tutorconnect.commons.util.StudentText;
import tutorconnect.logic.commands.TimetableCommand;
import tutorconnect.logic.parser.exceptions.ParseException;
import tutorconnect.model.student.WeeklySlot;

/** Validates the optional single day using scheduling's existing aliases and normalization. */
public class TimetableCommandParser implements Parser<TimetableCommand> {
    @Override
    public TimetableCommand parse(String args) throws ParseException {
        if (StudentText.hasProhibitedCharacters(args)) {
            throw new ParseException(StudentParameters.UNEXPECTED_TEXT);
        }
        if (args.matches("(?U)\\s*")) {
            return new TimetableCommand(Optional.empty());
        }
        String day = StudentParameters.parseSingleValues(args, "d/DAY").get("d/");
        try {
            return new TimetableCommand(Optional.of(WeeklySlot.parseDay(day)));
        } catch (IllegalArgumentException e) {
            throw new ParseException(WeeklySlot.MESSAGE_INVALID_DAY, e);
        }
    }
}
