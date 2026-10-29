package tutorconnect.logic;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.collections.ObservableList;
import tutorconnect.commons.core.GuiSettings;
import tutorconnect.commons.core.LogsCenter;
import tutorconnect.commons.exceptions.DataLoadingException;
import tutorconnect.logic.commands.Command;
import tutorconnect.logic.commands.CommandResult;
import tutorconnect.logic.commands.ListCommand;
import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.logic.parser.TutorConnectParser;
import tutorconnect.logic.parser.exceptions.ParseException;
import tutorconnect.model.Model;
import tutorconnect.model.WeeklySchedule;
import tutorconnect.model.student.Student;
import tutorconnect.storage.Storage;

/**
 * The main LogicManager of the app.
 */
public class LogicManager implements Logic {
    public static final String MESSAGE_SAVE_FAILURE = "Error: Changes could not be saved. No changes were made.";

    private final Logger logger = LogsCenter.getLogger(LogicManager.class);

    private final Model model;
    private final Storage storage;
    private final TutorConnectParser tutorConnectParser;
    private final WeeklySchedule weeklySchedule;

    /**
     * Constructs a {@code LogicManager} with the given {@code Model} and {@code Storage}.
     */
    public LogicManager(Model model, Storage storage) {
        this.model = model;
        this.storage = storage;
        tutorConnectParser = new TutorConnectParser();
        weeklySchedule = new WeeklySchedule(model.getStudentDirectory().getStudentList());
    }

    @Override
    public CommandResult execute(String commandText) throws CommandException, ParseException {
        logger.fine("Command received");
        Command command = parse(commandText);
        String commandName = command.getClass().getSimpleName();
        logger.fine("Parsed " + commandName + "; executing against a staged copy of the model");

        Model proposed = model.copyForCommand();
        if (command instanceof ListCommand) {
            reloadSavedStudents(proposed);
        }
        CommandResult commandResult;
        try {
            commandResult = command.execute(proposed);
        } catch (CommandException exception) {
            logger.fine(commandName + " rejected during execution; staged changes discarded");
            throw exception;
        }
        if (!(command instanceof ListCommand)) {
            saveIfChanged(commandName, proposed);
        }
        model.publish(proposed);
        logger.fine(commandName + " completed; staged model published");
        return commandResult;
    }

    private Command parse(String commandText) throws ParseException {
        try {
            return tutorConnectParser.parseCommand(commandText);
        } catch (ParseException exception) {
            logger.fine("Command rejected during parsing; model unchanged");
            throw exception;
        }
    }

    /** Replaces the staged register with the saved one, which {@code list} shows; a missing file keeps it. */
    private void reloadSavedStudents(Model proposed) throws CommandException {
        try {
            storage.readStudentDirectory().ifPresent(proposed::setStudentDirectory);
        } catch (DataLoadingException exception) {
            logger.log(Level.WARNING, "Could not reload student data for list; keeping the current register",
                    exception);
            throw new CommandException(ListCommand.MESSAGE_LOAD_FAILURE, exception);
        }
    }

    /** Saves the staged register before it is published, unless the command left the student data unchanged. */
    private void saveIfChanged(String commandName, Model proposed) throws CommandException {
        if (model.getStudentDirectory().equals(proposed.getStudentDirectory())) {
            logger.fine(commandName + " left student data unchanged; nothing was saved");
            return;
        }
        try {
            storage.saveStudentDirectory(proposed.getStudentDirectory());
        } catch (IOException ioe) {
            logger.log(Level.WARNING, commandName + " could not be saved; staged changes were discarded", ioe);
            throw new CommandException(MESSAGE_SAVE_FAILURE, ioe);
        }
        logger.info(commandName + " saved " + proposed.getStudentDirectory().getStudentList().size()
                + " student(s)");
    }

    @Override
    public UUID getSelectedStudentId() {
        return model.getSelectedStudentId();
    }

    @Override
    public void selectStudent(UUID id) {
        model.selectStudent(id);
    }

    @Override
    public ObservableList<Student> getFilteredStudentList() {
        return model.getFilteredStudentList();
    }

    @Override
    public ObservableList<Student> getWeeklySchedule() {
        return weeklySchedule.getEntries();
    }

    @Override
    public GuiSettings getGuiSettings() {
        return model.getGuiSettings();
    }

    @Override
    public void setGuiSettings(GuiSettings guiSettings) {
        model.setGuiSettings(guiSettings);
    }
}
