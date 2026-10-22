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
    public static final String FILE_OPS_ERROR_FORMAT = "Could not save data due to the following error: %s";
    public static final String MESSAGE_SAVE_FAILURE = "Error: Changes could not be saved. No changes were made.";

    public static final String FILE_OPS_PERMISSION_ERROR_FORMAT =
            "Could not save data to file %s due to insufficient permissions to write to the file or the folder.";
    public static final String MESSAGE_ATOMIC_SAVE_FAILURE =
            "Error: Changes could not be saved. No changes were made.";

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
        logger.info("----------------[USER COMMAND][" + commandText + "]");

        Command command = tutorConnectParser.parseCommand(commandText);
        String commandName = command.getClass().getSimpleName();
        logger.fine("Parsed " + commandName + "; executing against a staged copy of the model");
        Model proposed = model.copyForCommand();
        if (command instanceof ListCommand) {
            try {
                storage.readStudentDirectory().ifPresent(proposed::setStudentDirectory);
            } catch (DataLoadingException exception) {
                logger.log(Level.WARNING, "Could not reload student data for list; keeping the current register",
                        exception);
                throw new CommandException(ListCommand.MESSAGE_LOAD_FAILURE, exception);
            }
        }
        CommandResult commandResult = command.execute(proposed);

        boolean isChanged = !(command instanceof ListCommand)
                && !model.getStudentDirectory().equals(proposed.getStudentDirectory());
        if (isChanged) {
            try {
                storage.saveStudentDirectory(proposed.getStudentDirectory());
            } catch (IOException ioe) {
                logger.log(Level.WARNING, commandName + " could not be saved; staged changes were discarded", ioe);
                throw new CommandException(MESSAGE_SAVE_FAILURE, ioe);
            }
            logger.info(commandName + " saved " + proposed.getStudentDirectory().getStudentList().size()
                    + " student(s)");
        } else {
            logger.fine(commandName + " left student data unchanged; nothing was saved");
        }
        model.publish(proposed);
        return commandResult;
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
