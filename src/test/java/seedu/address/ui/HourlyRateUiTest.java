package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import seedu.address.logic.LogicManager;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.JavaFxTestUtil;
import seedu.address.testutil.PersonBuilder;

/** F07 through the actual JavaFX command box, list cells and selected-student details. */
@EnabledIfEnvironmentVariable(named = "F01_UI_TESTS", matches = "true")
public class HourlyRateUiTest {
    @TempDir
    private Path directory;

    @BeforeAll
    public static void startToolkit() {
        JavaFxTestUtil.startToolkit();
    }

    @Test
    public void rateCommands_renderCurrencyAndPreservePublishedUiOnFailure() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            ModelManager model = new ModelManager();
            model.addPerson(new PersonBuilder().withName("Alex Tan").build());
            model.addPerson(new PersonBuilder().withName("Other Student").withPhone("91234567").build());
            AtomicBoolean failSave = new AtomicBoolean();
            JsonAddressBookStorage storage = new JsonAddressBookStorage(directory.resolve("students.json")) {
                @Override
                public void saveAddressBook(ReadOnlyAddressBook proposed) throws IOException {
                    if (failSave.get()) {
                        throw new IOException("Injected rate UI save failure");
                    }
                    super.saveAddressBook(proposed);
                }
            };
            LogicManager logic = new LogicManager(model, new StorageManager(storage,
                    new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
            Stage stage = new Stage();
            try {
                MainWindow window = new MainWindow(stage, logic, storage.getAddressBookFilePath());
                window.fillInnerParts();
                window.show();
                layout(stage);
                TextField input = (TextField) stage.getScene().lookup("#commandTextField");
                TextArea status = (TextArea) stage.getScene().lookup("#resultDisplay");
                @SuppressWarnings("unchecked")
                ListView<Person> rows = (ListView<Person>) stage.getScene().lookup("#personListView");
                StudentDetailsPanel details = (StudentDetailsPanel) stage.getScene().lookup("#studentDetails");
                rows.getSelectionModel().select(0);
                assertTrue(detailText(details).contains("Hourly rate: —"));
                assertEquals("Hourly rate: —", cardRate(rows.getItems().getFirst()));

                enter(input, "rate 1 r/1");
                assertEquals("Hourly rate set for Alex Tan: S$1.00.", status.getText());
                assertTrue(detailText(details).contains("Hourly rate: S$1.00"));
                layout(stage);
                assertTrue(rows.lookupAll("#hourlyRate").stream()
                        .anyMatch(node -> ((Label) node).getText().equals("Hourly rate: S$1.00")));
                enter(input, "rate 1 r/1000.00");
                assertEquals("Hourly rate updated for Alex Tan: S$1.00 -> S$1000.00.", status.getText());
                assertTrue(detailText(details).contains("Hourly rate: S$1000.00"));
                assertEquals("Hourly rate: S$1000.00", cardRate(rows.getItems().getFirst()));

                rows.getSelectionModel().select(1);
                Person selected = rows.getSelectionModel().getSelectedItem();
                List<String> beforeNoChange = detailText(details);
                failSave.set(true);
                enter(input, "rate 1 r/1000.0");
                assertEquals("Hourly rate for Alex Tan is already S$1000.00.", status.getText());
                assertSame(selected, rows.getSelectionModel().getSelectedItem());
                assertEquals(beforeNoChange, detailText(details));
                failSave.set(false);

                enter(input, "find Other");
                assertEquals(1, rows.getItems().size());
                enter(input, "rate 1 r/19.99");
                assertEquals(selected.getId(), rows.getSelectionModel().getSelectedItem().getId());
                assertEquals("Hourly rate set for Other Student: S$19.99.", status.getText());
                assertTrue(detailText(details).contains("Hourly rate: S$19.99"));
                assertEquals("Hourly rate: S$19.99", cardRate(rows.getItems().getFirst()));

                Person published = rows.getSelectionModel().getSelectedItem();
                List<String> beforeFailure = detailText(details);
                byte[] saved = Files.readAllBytes(storage.getAddressBookFilePath());
                enter(input, "rate 1 r/19.999");
                assertTrue(status.getText().startsWith("Error: Invalid rate."));
                assertSame(published, rows.getSelectionModel().getSelectedItem());
                assertEquals(beforeFailure, detailText(details));
                failSave.set(true);
                enter(input, "rate 1 r/20");
                assertEquals(LogicManager.MESSAGE_SAVE_FAILURE, status.getText());
                assertSame(published, rows.getSelectionModel().getSelectedItem());
                assertEquals(beforeFailure, detailText(details));
                assertEquals("Hourly rate: S$19.99", cardRate(rows.getItems().getFirst()));
                assertArrayEquals(saved, Files.readAllBytes(storage.getAddressBookFilePath()));
                failSave.set(false);

                enter(input, "list");
                rows.getSelectionModel().select(0);
                assertTrue(detailText(details).contains("Hourly rate: S$1000.00"));
                rows.getSelectionModel().select(1);
                assertTrue(detailText(details).contains("Hourly rate: S$19.99"));
                assertEquals("Hourly rate: S$19.99", cardRate(rows.getItems().getLast()));
            } finally {
                stage.hide();
            }
            return null;
        });
        Platform.runLater(task);
        task.get(30, TimeUnit.SECONDS);
    }

    private String cardRate(Person student) {
        return ((Label) new PersonCard(student, 1).getRoot().lookup("#hourlyRate")).getText();
    }

    private List<String> detailText(StudentDetailsPanel details) {
        return details.getChildren().stream().map(node -> ((Label) node).getText()).toList();
    }

    private void layout(Stage stage) {
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().layout();
    }

    private void enter(TextField input, String command) {
        input.setText(command);
        input.fireEvent(new ActionEvent());
    }
}
