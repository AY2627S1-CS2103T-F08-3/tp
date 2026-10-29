package tutorconnect.ui;

import static java.util.Objects.requireNonNull;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;

/**
 * A UI component that displays the result of a command execution.
 */
public class ResultDisplay extends UiPart<Region> {

    static final String ERROR_STYLE_CLASS = "result-error";
    static final String WARNING_STYLE_CLASS = "result-warning";
    private static final String FXML = "ResultDisplay.fxml";

    @FXML
    private TextArea resultDisplay;

    public ResultDisplay() {
        super(FXML);
    }

    /** Shows {@code feedbackToUser}, tinting the box for messages that start with "Error:" or "Warning:". */
    public void setFeedbackToUser(String feedbackToUser) {
        requireNonNull(feedbackToUser);
        resultDisplay.setText(feedbackToUser);
        resultDisplay.getStyleClass().removeAll(ERROR_STYLE_CLASS, WARNING_STYLE_CLASS);
        if (feedbackToUser.startsWith("Error")) {
            resultDisplay.getStyleClass().add(ERROR_STYLE_CLASS);
        } else if (feedbackToUser.contains("Warning:")) {
            resultDisplay.getStyleClass().add(WARNING_STYLE_CLASS);
        }
    }

}
