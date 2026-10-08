package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
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
import seedu.address.testutil.PersonBuilder;

/** Real JavaFX smoke test; opt in on a desktop or under a virtual display with F01_UI_TESTS=true. */
@EnabledIfEnvironmentVariable(named = "F01_UI_TESTS", matches = "true")
public class AddStudentUiTest {
    @TempDir
    private Path directory;

    @BeforeAll
    public static void startToolkit() {
        Platform.startup(() -> Platform.setImplicitExit(false));
    }

    @Test
    public void commandBox_addSelectsDetailsAndErrorsPreserveSelection() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            ModelManager model = new ModelManager();
            AtomicBoolean failSave = new AtomicBoolean();
            JsonAddressBookStorage storage = new JsonAddressBookStorage(directory.resolve("students.json")) {
                @Override
                public void saveAddressBook(ReadOnlyAddressBook proposed) throws IOException {
                    if (failSave.get()) {
                        throw new IOException("Injected UI save failure");
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
                stage.getScene().getRoot().applyCss();
                stage.getScene().getRoot().layout();
                TextField input = (TextField) stage.getScene().lookup("#commandTextField");
                TextArea status = (TextArea) stage.getScene().lookup("#resultDisplay");
                @SuppressWarnings("unchecked")
                ListView<Person> rows = (ListView<Person>) stage.getScene().lookup("#personListView");
                enter(input, "add n/Alex Tan p/81234567 a/21 Clementi Ave 3 #04-18");
                // Materialize virtualized list cells before checking card rendering/coverage.
                stage.getScene().getRoot().applyCss();
                stage.getScene().getRoot().layout();
                assertEquals("Student added: Alex Tan.", status.getText());
                assertEquals(0, rows.getSelectionModel().getSelectedIndex());
                assertEquals(model.getSelectedPersonId(), rows.getSelectionModel().getSelectedItem().getId());
                StudentDetailsPanel details = (StudentDetailsPanel) stage.getScene().lookup("#studentDetails");
                List<String> text = details.getChildren().stream().map(node -> ((Label) node).getText()).toList();
                assertTrue(text.contains("Phone: +6581234567"));
                assertEquals(5, text.stream().filter(line -> line.endsWith("—")).count());
                Person selected = rows.getSelectionModel().getSelectedItem();
                Label email = (Label) rows.lookup("#email");
                assertFalse(email.isVisible());
                assertFalse(email.isManaged());
                PersonCard legacyCard = new PersonCard(new PersonBuilder().build(), 1);
                assertTrue(legacyCard.getRoot().lookup("#email").isVisible());
                assertTrue(legacyCard.getRoot().lookup("#email").isManaged());
                PersonListPanel compatibilityPanel = new PersonListPanel(rows.getItems());
                assertEquals(rows.getItems(), ((ListView<?>) compatibilityPanel.getRoot()
                        .lookup("#personListView")).getItems());
                enter(input, "add n/123 p/81234567 a/A");
                assertTrue(status.getText().startsWith("Error: Invalid name."));
                assertEquals(selected, rows.getSelectionModel().getSelectedItem());
                failSave.set(true);
                enter(input, "add n/Nur Aisyah p/92345678 a/8 Jalan Besar");
                assertEquals(LogicManager.MESSAGE_SAVE_FAILURE, status.getText());
                assertEquals(List.of(selected), rows.getItems());
                assertEquals(selected, rows.getSelectionModel().getSelectedItem());
                failSave.set(false);
                enter(input, "add n/Nur Aisyah p/92345678 a/8 Jalan Besar");
                assertEquals(1, rows.getSelectionModel().getSelectedIndex());
                assertEquals("Name: Nur Aisyah", ((Label) details.getChildren().getFirst()).getText());
                enter(input, "list");
                assertNull(rows.getSelectionModel().getSelectedItem());
                assertNull(model.getSelectedPersonId());
            } finally {
                stage.hide();
            }
            return null;
        });
        Platform.runLater(task);
        task.get(30, TimeUnit.SECONDS);
    }

    private void enter(TextField input, String command) {
        input.setText(command);
        input.fireEvent(new ActionEvent());
    }
}
