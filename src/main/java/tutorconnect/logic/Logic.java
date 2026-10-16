package tutorconnect.logic;

import java.util.UUID;

import javafx.collections.ObservableList;
import tutorconnect.commons.core.GuiSettings;
import tutorconnect.logic.commands.CommandResult;
import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.logic.parser.exceptions.ParseException;
import tutorconnect.model.student.Student;

/**
 * API of the Logic component
 */
public interface Logic {
    /**
     * Executes the command and returns the result.
     * @param commandText The command as entered by the user.
     * @return the result of the command execution.
     * @throws CommandException If an error occurs during command execution.
     * @throws ParseException If an error occurs during parsing.
     */
    CommandResult execute(String commandText) throws CommandException, ParseException;

    /** Returns an unmodifiable view of the filtered list of students */
    ObservableList<Student> getFilteredStudentList();

    /** Returns scheduled students in an independent weekday/time/insertion ordering. */
    ObservableList<Student> getWeeklySchedule();

    /**
     * Returns the user prefs' GUI settings.
     */
    GuiSettings getGuiSettings();

    /**
     * Set the user prefs' GUI settings.
     */
    void setGuiSettings(GuiSettings guiSettings);

    UUID getSelectedStudentId();

    void selectStudent(UUID id);
}
