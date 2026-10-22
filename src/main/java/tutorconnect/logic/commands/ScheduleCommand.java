package tutorconnect.logic.commands;

import static tutorconnect.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

import tutorconnect.commons.core.LogsCenter;
import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.logic.parser.VisibleIndex;
import tutorconnect.model.Model;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.WeeklySlot;
import tutorconnect.model.student.WeeklySlotField;

/**
 * Sets or replaces a student's one complete recurring weekly lesson.
 */
public class ScheduleCommand extends Command {
    public static final String COMMAND_WORD = "schedule";
    public static final String MESSAGE_USAGE = "schedule INDEX d/DAY t/TIME\n"
            + "Example: schedule 1 d/Tuesday t/19:00";
    public static final String MESSAGE_SET = "Weekly lesson set for %s: %s.";
    public static final String MESSAGE_UPDATED = "Weekly lesson updated for %s: %s -> %s.";
    public static final String MESSAGE_UNCHANGED = "Weekly lesson for %s is already %s.";
    private static final Logger logger = LogsCenter.getLogger(ScheduleCommand.class);

    private final VisibleIndex index;
    private final WeeklySlot slot;

    /**
     * Creates a complete slot update for a displayed-list index.
     */
    public ScheduleCommand(VisibleIndex index, WeeklySlot slot) {
        requireAllNonNull(index, slot);
        this.index = index;
        this.slot = slot;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        Objects.requireNonNull(model);
        Student target = index.resolve(model.getFilteredStudentList());
        Optional<WeeklySlot> previous = target.getStudentFields().get(WeeklySlotField.INSTANCE);
        if (previous.filter(slot::equals).isPresent()) {
            logger.fine("Weekly slot unchanged for student " + target.getId());
            return new CommandResult(String.format(MESSAGE_UNCHANGED, target.getName(), slot));
        }
        Student updated = target.withStudentFields(target.getStudentFields().with(WeeklySlotField.INSTANCE, slot));
        model.setStudent(target, updated);
        model.selectStudent(updated.getId());
        logger.fine((previous.isPresent() ? "Replaced" : "Set") + " weekly slot for student " + target.getId());
        String message = previous.map(old -> String.format(MESSAGE_UPDATED, target.getName(), old, slot))
                .orElseGet(() -> String.format(MESSAGE_SET, target.getName(), slot));
        return new CommandResult(message);
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof ScheduleCommand command
                && index.equals(command.index) && slot.equals(command.slot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, slot);
    }
}
