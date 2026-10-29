package tutorconnect.ui;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import tutorconnect.logic.commands.CommandResult;
import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.logic.parser.exceptions.ParseException;

/**
 * The UI component that is responsible for receiving user command inputs.
 */
public class CommandBox extends UiPart<Region> {

    public static final String ERROR_STYLE_CLASS = "error";
    static final String IDLE_HINT = "Start typing a command. The format appears here.";
    private static final String FXML = "CommandBox.fxml";

    private final CommandExecutor commandExecutor;
    private final CommandHistory history = new CommandHistory();

    @FXML
    private TextField commandTextField;

    @FXML
    private Label commandHint;

    /**
     * Creates a {@code CommandBox} with the given {@code CommandExecutor}.
     */
    public CommandBox(CommandExecutor commandExecutor) {
        super(FXML);
        this.commandExecutor = commandExecutor;
        // calls #setStyleToDefault() whenever there is a change to the text of the command box.
        commandTextField.textProperty().addListener((unused1, unused2, unused3) -> setStyleToDefault());
        commandTextField.textProperty().addListener((unused1, unused2, text) -> showHint(text));
        commandTextField.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
    }

    /** Gives the command field keyboard focus, placing the caret at the end. */
    public void focus() {
        commandTextField.requestFocus();
        commandTextField.end();
    }

    /** Focuses the command field and appends {@code typed}, so typing anywhere in the window reaches it. */
    public void focusAndType(String typed) {
        focus();
        commandTextField.appendText(typed);
    }

    /**
     * Handles the keys that make the command bar fast to use: Up/Down browse earlier commands, Tab completes the
     * command word, and Escape clears the field. Each handled key is consumed so focus never leaves the field.
     */
    private void handleKeyPressed(KeyEvent event) {
        if (event.isShortcutDown() || event.isAltDown()) {
            return;
        }
        switch (event.getCode()) {
            case UP -> history.previous(commandTextField.getText()).ifPresent(this::replaceText);
            case DOWN -> history.next().ifPresent(this::replaceText);
            case TAB -> CommandGuide.complete(commandTextField.getText()).ifPresent(this::replaceText);
            case ESCAPE -> {
                commandTextField.clear();
                history.resetPosition();
            }
            default -> {
                return;
            }
        }
        event.consume();
    }

    private void replaceText(String text) {
        commandTextField.setText(text);
        commandTextField.end();
    }

    private void showHint(String text) {
        String hint = CommandGuide.hintFor(text);
        if (text.isBlank()) {
            commandHint.setText(IDLE_HINT);
        } else if (hint.isEmpty()) {
            commandHint.setText("No command starts with \"" + text.strip().split("\\s+", 2)[0]
                    + "\". Type help for the guide.");
        } else {
            commandHint.setText(hint);
        }
    }

    /**
     * Handles the Enter button pressed event.
     */
    @FXML
    private void handleCommandEntered() {
        String commandText = commandTextField.getText();
        if (commandText.equals("")) {
            return;
        }

        history.add(commandText);
        try {
            commandExecutor.execute(commandText);
            commandTextField.setText("");
        } catch (CommandException | ParseException e) {
            setStyleToIndicateCommandFailure();
        }
    }

    /**
     * Sets the command box style to use the default style.
     */
    private void setStyleToDefault() {
        commandTextField.getStyleClass().remove(ERROR_STYLE_CLASS);
    }

    /**
     * Sets the command box style to indicate a failed command.
     */
    private void setStyleToIndicateCommandFailure() {
        ObservableList<String> styleClass = commandTextField.getStyleClass();

        if (styleClass.contains(ERROR_STYLE_CLASS)) {
            return;
        }

        styleClass.add(ERROR_STYLE_CLASS);
    }

    /**
     * Represents a function that can execute commands.
     */
    @FunctionalInterface
    public interface CommandExecutor {
        /**
         * Executes the command and returns the result.
         *
         * @see tutorconnect.logic.Logic#execute(String)
         */
        CommandResult execute(String commandText) throws CommandException, ParseException;
    }

}
