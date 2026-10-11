package tutorconnect.logic.commands;

import static java.util.Objects.requireNonNull;
import static tutorconnect.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static tutorconnect.logic.parser.CliSyntax.PREFIX_NAME;
import static tutorconnect.logic.parser.CliSyntax.PREFIX_PHONE;

import tutorconnect.commons.util.ToStringBuilder;
import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.model.Model;
import tutorconnect.model.student.Student;

/**
 * Adds a student to the address book.
 */
public class AddCommand extends Command {

    public static final String COMMAND_WORD = "add";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Adds a student. "
            + "Parameters: "
            + PREFIX_NAME + "NAME "
            + PREFIX_PHONE + "PHONE "
            + PREFIX_ADDRESS + "ADDRESS\n"
            + "Example: " + COMMAND_WORD + " "
            + PREFIX_NAME + "John Doe "
            + PREFIX_PHONE + "98765432 "
            + PREFIX_ADDRESS + "311, Clementi Ave 2, #02-25";

    public static final String MESSAGE_SUCCESS = "Student added: %1$s.";
    public static final String MESSAGE_DUPLICATE_STUDENT =
            "Error: This student already exists with the same name and phone.";
    public static final String MESSAGE_SHARED_PHONE = "Warning: Another student uses this phone number.";

    private final Student toAdd;

    /**
     * Creates an AddCommand to add the specified {@code Student}
     */
    public AddCommand(Student student) {
        requireNonNull(student);
        toAdd = student;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);

        if (model.hasStudent(toAdd)) {
            throw new CommandException(MESSAGE_DUPLICATE_STUDENT);
        }

        boolean sharedPhone = model.getStudentDirectory().getStudentList().stream()
                .anyMatch(student -> student.getPhone().equals(toAdd.getPhone()));
        model.addStudent(toAdd);
        String message = String.format(MESSAGE_SUCCESS, toAdd.getName());
        return new CommandResult(message + (sharedPhone ? " " + MESSAGE_SHARED_PHONE : ""));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof AddCommand otherAddCommand)) {
            return false;
        }

        return toAdd.equals(otherAddCommand.toAdd);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("toAdd", toAdd)
                .toString();
    }
}
