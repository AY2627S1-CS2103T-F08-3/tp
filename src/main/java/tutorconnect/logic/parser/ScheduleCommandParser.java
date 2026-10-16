package tutorconnect.logic.parser;

import java.time.DayOfWeek;
import java.time.LocalTime;

import tutorconnect.commons.util.StudentText;
import tutorconnect.logic.commands.ScheduleCommand;
import tutorconnect.logic.parser.exceptions.ParseException;
import tutorconnect.model.student.WeeklySlot;

/**
 * Validates structure, then index, day and time in documentation order regardless of prefix order.
 */
public class ScheduleCommandParser implements Parser<ScheduleCommand> {
    @Override
    public ScheduleCommand parse(String args) throws ParseException {
        if (StudentText.hasProhibitedCharacters(args)) {
            throw new ParseException(StudentParameters.UNEXPECTED_TEXT);
        }
        StudentParameters.IndexedParameters parameters = StudentParameters.parseIndexed(args, "d/DAY", "t/TIME");
        VisibleIndex index = VisibleIndex.parse(StudentText.normalize(parameters.index()));
        try {
            DayOfWeek day = WeeklySlot.parseDay(parameters.values().get("d/"));
            LocalTime time = WeeklySlot.parseTime(parameters.values().get("t/"));
            return new ScheduleCommand(index, new WeeklySlot(day, time));
        } catch (IllegalArgumentException e) {
            throw new ParseException(e.getMessage(), e);
        }
    }
}
