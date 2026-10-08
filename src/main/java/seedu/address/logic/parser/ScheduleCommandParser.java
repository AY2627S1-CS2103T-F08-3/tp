package seedu.address.logic.parser;

import java.time.DayOfWeek;
import java.time.LocalTime;

import seedu.address.commons.util.StudentText;
import seedu.address.logic.commands.ScheduleCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.WeeklySlot;

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
