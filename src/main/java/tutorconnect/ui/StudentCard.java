package tutorconnect.ui;

import java.util.Comparator;

import javafx.fxml.FXML;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import tutorconnect.model.student.EducationLevel;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.Subject;
import tutorconnect.model.student.WeeklySlotField;

/**
 * A UI component that displays information of a {@code Student}.
 */
public class StudentCard extends UiPart<Region> {

    private static final String FXML = "StudentListCard.fxml";
    /** Rows narrower than this hide the lesson and rate columns, which the profile panel still shows. */
    private static final double COMPACT_WIDTH = 520;

    /**
     * Note: Certain keywords such as "location" and "resources" are reserved keywords in JavaFX.
     * As a consequence, UI elements' variable names cannot be set to such keywords
     * or an exception will be thrown by JavaFX during runtime.
     *
     * @see <a href="https://github.com/se-edu/addressbook-level4/issues/336">The issue on AddressBook level 4</a>
     */

    public final Student student;

    @FXML
    private HBox cardPane;
    @FXML
    private Label name;
    @FXML
    private Label id;
    @FXML
    private StackPane avatar;
    @FXML
    private Label avatarText;
    @FXML
    private Label phone;
    @FXML
    private Label address;
    @FXML
    private Label subject;
    @FXML
    private Label email;
    @FXML
    private Label weeklySlot;
    @FXML
    private Label educationLevel;
    @FXML
    private Label hourlyRate;
    @FXML
    private FlowPane tags;
    @FXML
    private VBox lessonColumn;
    @FXML
    private VBox rateColumn;

    /**
     * Creates a {@code StudentCard} with the given {@code Student} and index to display.
     */
    public StudentCard(Student student, int displayedIndex) {
        super(FXML);
        this.student = student;
        id.setText(displayedIndex + ". ");
        name.setText(student.getName().fullName);
        avatarText.setText(Avatars.initials(student.getName().fullName));
        avatar.getStyleClass().add(Avatars.styleClass(student.getName().fullName));
        phone.setText(student.getPhone().value);
        address.setText(student.getAddress().value);
        email.setText(student.getEmail().value);
        email.setVisible(!student.getEmail().value.isEmpty());
        email.setManaged(email.isVisible());
        String level = EducationLevel.display(student.getStudentFields());
        String subjectName = Subject.display(student.getStudentFields());
        String slot = WeeklySlotField.display(student);
        String rate = student.getHourlyRate().map(value -> "S$" + value).orElse("—");
        showValue(educationLevel, "Level: " + level, level.equals("—") ? "No level" : level, "card-meta");
        showValue(subject, "Subject: " + subjectName, subjectName.equals("—") ? "No subject" : subjectName,
                "card-meta");
        showValue(weeklySlot, "Weekly lesson: " + slot, slot, "card-value");
        showValue(hourlyRate, "Hourly rate: " + rate, rate, "card-value");
        student.getTags().stream()
                .sorted(Comparator.comparing(tag -> tag.tagName))
                .forEach(tag -> tags.getChildren().add(new Label(tag.tagName)));
        tags.setVisible(!tags.getChildren().isEmpty());
        tags.setManaged(tags.isVisible());
        cardPane.widthProperty().addListener((unused, oldWidth, width) -> showColumns(width.doubleValue()));
    }

    /**
     * Keeps {@code text} as the label's text while drawing only {@code value}. The prefixed text remains the card's
     * readable contract, but the row reads like a table rather than a list of "Key: value" lines.
     */
    private static void showValue(Label label, String text, String value, String styleClass) {
        label.setText(text);
        Text shown = new Text(value);
        shown.getStyleClass().add(styleClass);
        if (value.equals("—") || value.startsWith("No ")) {
            shown.getStyleClass().add("card-unset");
        }
        label.setGraphic(shown);
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }

    private void showColumns(double width) {
        boolean isWide = width >= COMPACT_WIDTH;
        for (VBox column : new VBox[] {lessonColumn, rateColumn}) {
            column.setVisible(isWide);
            column.setManaged(isWide);
        }
    }
}
