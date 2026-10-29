package tutorconnect.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.logging.Logger;

import tutorconnect.commons.core.LogsCenter;
import tutorconnect.logic.Messages;
import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.logic.parser.VisibleIndex;
import tutorconnect.model.Model;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.WeeklySlotField;

/** Removes a complete weekly slot while keeping its student and all other details. */
public class UnscheduleCommand extends Command {
    public static final String COMMAND_WORD = "unschedule";
    private static final Logger logger = LogsCenter.getLogger(UnscheduleCommand.class);
    private final VisibleIndex index;

    public UnscheduleCommand(VisibleIndex index) {
        this.index = requireNonNull(index);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        Student target = index.resolve(requireNonNull(model).getFilteredStudentList());
        var slot = WeeklySlotField.get(target);
        if (slot.isEmpty()) {
            logger.fine("No weekly slot to remove for student " + target.getId());
            return new CommandResult(
                    Messages.formatSentence("No weekly lesson is scheduled for %s.", target.getName()));
        }
        model.setStudent(target, target.withStudentFields(target.getStudentFields().without(WeeklySlotField.INSTANCE)));
        logger.fine("Removed weekly slot for student " + target.getId());
        return new CommandResult("Weekly lesson removed for " + target.getName() + ": " + slot.get() + ".");
    }
}
