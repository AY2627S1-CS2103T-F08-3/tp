package seedu.address.logic.commands;

import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Objects;
import java.util.Optional;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.person.Person;
import seedu.address.model.person.WeeklySlot;
import seedu.address.model.person.WeeklySlotField;

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

    private final Index index;
    private final WeeklySlot slot;

    /**
     * Creates a complete slot update for a displayed-list index.
     */
    public ScheduleCommand(Index index, WeeklySlot slot) {
        requireAllNonNull(index, slot);
        this.index = index;
        this.slot = slot;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        Objects.requireNonNull(model);
        Person target = VisibleIndexResolver.resolvePerson(index, model);
        Optional<WeeklySlot> previous = target.getStudentFields().get(WeeklySlotField.INSTANCE);
        if (previous.filter(slot::equals).isPresent()) {
            return new CommandResult(String.format(MESSAGE_UNCHANGED, target.getName(), slot));
        }
        Person updated = target.withStudentFields(target.getStudentFields().with(WeeklySlotField.INSTANCE, slot));
        model.setPerson(target, updated);
        model.selectPerson(updated.getId());
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
        return Objects.hash(index.getOneBasedString(), slot);
    }
}
