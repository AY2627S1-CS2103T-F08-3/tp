package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.Optional;

import seedu.address.commons.util.ToStringBuilder;

/**
 * Represents the result of a command execution.
 */
public class CommandResult {
    /** Selection changes are applied only after successful command execution. */
    public enum SelectionAction { UNCHANGED, PRESERVE, CLEAR }

    private SelectionAction selectionAction = SelectionAction.UNCHANGED;

    private final String feedbackToUser;

    /** Help information should be shown to the user. */
    private final boolean showHelp;

    /** The application should exit. */
    private final boolean exit;

    /** One-based index of a person to select after a successful command, if any. */
    private final Integer selectedPersonIndex;

    /**
     * Constructs a {@code CommandResult} with the specified fields.
     */
    public CommandResult(String feedbackToUser, boolean showHelp, boolean exit) {
        this(feedbackToUser, showHelp, exit, null);
    }

    /** Constructs a result that selects the person at the given one-based displayed index. */
    public CommandResult(String feedbackToUser, int selectedPersonIndex) {
        this(feedbackToUser, false, false, selectedPersonIndex);
    }

    private CommandResult(String feedbackToUser, boolean showHelp, boolean exit, Integer selectedPersonIndex) {
        this.feedbackToUser = requireNonNull(feedbackToUser);
        if (selectedPersonIndex != null && selectedPersonIndex <= 0) {
            throw new IllegalArgumentException("Selected person index must be positive.");
        }
        this.showHelp = showHelp;
        this.exit = exit;
        this.selectedPersonIndex = selectedPersonIndex;
    }

    /**
     * Constructs a {@code CommandResult} with the specified {@code feedbackToUser},
     * and other fields set to their default value.
     */
    public CommandResult(String feedbackToUser) {
        this(feedbackToUser, false, false);
    }

    /** Creates a successful result with an explicit UI selection policy. */
    public CommandResult(String feedbackToUser, SelectionAction selectionAction) {
        this(feedbackToUser);
        this.selectionAction = requireNonNull(selectionAction);
    }

    public SelectionAction getSelectionAction() {
        return selectionAction;
    }

    public String getFeedbackToUser() {
        return feedbackToUser;
    }

    public boolean isShowHelp() {
        return showHelp;
    }

    public boolean isExit() {
        return exit;
    }

    public Optional<Integer> getSelectedPersonIndex() {
        return Optional.ofNullable(selectedPersonIndex);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof CommandResult otherCommandResult)) {
            return false;
        }

        return feedbackToUser.equals(otherCommandResult.feedbackToUser)
                && showHelp == otherCommandResult.showHelp
                && exit == otherCommandResult.exit
                && Objects.equals(selectedPersonIndex, otherCommandResult.selectedPersonIndex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(feedbackToUser, showHelp, exit, selectedPersonIndex);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("feedbackToUser", feedbackToUser)
                .add("showHelp", showHelp)
                .add("exit", exit)
                .add("selectedPersonIndex", selectedPersonIndex)
                .toString();
    }

}
