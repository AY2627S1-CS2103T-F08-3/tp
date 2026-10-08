package seedu.address.logic;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import javafx.collections.ObservableList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.commands.AtomicCommand;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.logic.commands.Command;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.ListCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.AddressBookParser;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.person.Person;
import seedu.address.storage.Storage;

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
    private final AddressBookParser addressBookParser;

    /**
     * Constructs a {@code LogicManager} with the given {@code Model} and {@code Storage}.
     */
    public LogicManager(Model model, Storage storage) {
        this.model = model;
        this.storage = storage;
        addressBookParser = new AddressBookParser();
    }

    @Override
    public CommandResult execute(String commandText) throws CommandException, ParseException {
        logger.info("----------------[USER COMMAND][" + commandText + "]");

        CommandResult commandResult;
        Command command = addressBookParser.parseCommand(commandText);
        if (command instanceof AtomicCommand) {
            return executeAtomically(command);
        }
        commandResult = command.execute(model);

        try {
            if (!model.getAddressBook().equals(proposed.getAddressBook())) {
                storage.saveAddressBook(proposed.getAddressBook());
            }
        } catch (IOException ioe) {
            throw new CommandException(MESSAGE_SAVE_FAILURE, ioe);
        }
        model.publish(proposed);
        return commandResult;
    }

    /** Executes a state-changing command against a copy, saves it, then publishes it to the live model. */
    private CommandResult executeAtomically(Command command) throws CommandException {
        AddressBook proposedAddressBook = new AddressBook(model.getAddressBook());
        Model proposedModel = new ModelManager(proposedAddressBook, model.getUserPrefs());
        List<Person> currentlyDisplayed = List.copyOf(model.getFilteredPersonList());
        proposedModel.updateFilteredPersonList(person -> currentlyDisplayed.stream()
                .anyMatch(displayedPerson -> displayedPerson.isSamePerson(person)));

        CommandResult result = command.execute(proposedModel);
        if (model.getAddressBook().equals(proposedModel.getAddressBook())) {
            return result;
        }

        try {
            storage.saveAddressBook(proposedModel.getAddressBook());
        } catch (IOException exception) {
            throw new CommandException(MESSAGE_ATOMIC_SAVE_FAILURE, exception);
        }

        model.setAddressBook(proposedModel.getAddressBook());
        return result;
    }

    @Override
    public UUID getSelectedPersonId() {
        return model.getSelectedPersonId();
    }

    @Override
    public void selectPerson(UUID id) {
        model.selectPerson(id);
    }

    @Override
    public ObservableList<Person> getFilteredPersonList() {
        return model.getFilteredPersonList();
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
