package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import seedu.address.logic.LogicManager;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.person.Address;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.Phone;
import seedu.address.model.person.WeeklySlot;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.JavaFxTestUtil;

/**
 * Real JavaFX acceptance flow; opt in on a desktop or virtual display with F08_UI_TESTS=true.
 */
@EnabledIfEnvironmentVariable(named = "F08_UI_TESTS", matches = "true")
public class WeeklyScheduleUiTest {
    @TempDir
    public Path directory;

    @BeforeAll
    public static void startToolkit() {
        JavaFxTestUtil.startToolkit();
    }

    @Test
    public void commandBox_slotsSummaryDetailsAndFailures_followPublishedState() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            ModelManager model = new ModelManager();
            Person zoe = new Person(new Name("Zoe"), new Phone("81234567"), new Address("21 Clementi Ave"));
            Person amy = new Person(new Name("Amy"), new Phone("81234567"), new Address("22 Clementi Ave"));
            model.addPerson(zoe);
            model.addPerson(amy);
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
            storage.saveAddressBook(model.getAddressBook());
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
                @SuppressWarnings("unchecked")
                ListView<Person> summary = (ListView<Person>) stage.getScene().lookup("#weeklyScheduleList");
                StudentDetailsPanel details = (StudentDetailsPanel) stage.getScene().lookup("#studentDetails");
                rows.getSelectionModel().select(0);
                assertTrue(labels(details).contains("Weekly slot: —"));
                enter(input, "schedule 1 d/Sun t/23:59");
                assertEquals("Weekly lesson set for Zoe: Sunday 23:59.", status.getText());
                assertTrue(labels(details).contains("Weekly slot: Sunday 23:59"));
                enter(input, "schedule 2 t/09:00 d/Mon");
                assertEquals(List.of("Amy", "Zoe"), names(summary));
                assertEquals(List.of("Zoe", "Amy"), names(rows));
                assertEquals(amy.getId(), model.getSelectedPersonId());
                Person selected = rows.getSelectionModel().getSelectedItem();
                List<Node> detailNodes = List.copyOf(details.getChildren());
                List<Person> scheduled = List.copyOf(summary.getItems());
                AtomicInteger changes = new AtomicInteger();
                summary.getItems().addListener((ListChangeListener<Person>) change -> changes.incrementAndGet());
                enter(input, "schedule 1 d/sUN t/２３：５９");
                assertEquals("Weekly lesson for Zoe is already Sunday 23:59.", status.getText());
                assertSame(selected, rows.getSelectionModel().getSelectedItem());
                assertEquals(detailNodes, details.getChildren());
                enter(input, "schedule 999 t/24:00 d/Funday");
                assertEquals(WeeklySlot.MESSAGE_INVALID_DAY, status.getText());
                enter(input, "schedule 1 d/Mon t/12:60");
                assertEquals(WeeklySlot.MESSAGE_INVALID_TIME, status.getText());
                String disk = Files.readString(storage.getAddressBookFilePath());
                failSave.set(true);
                enter(input, "schedule 1 d/Tue t/19:00");
                assertEquals(LogicManager.MESSAGE_SAVE_FAILURE, status.getText());
                assertSame(selected, rows.getSelectionModel().getSelectedItem());
                assertEquals(detailNodes, details.getChildren());
                assertEquals(scheduled, summary.getItems());
                assertEquals(0, changes.get());
                assertEquals(disk, Files.readString(storage.getAddressBookFilePath()));
                failSave.set(false);
                enter(input, "schedule 1 d/Tue t/19:00");
                assertEquals("Weekly lesson updated for Zoe: Sunday 23:59 -> Tuesday 19:00.", status.getText());
                assertTrue(labels(details).contains("Weekly slot: Tuesday 19:00"));
                assertEquals(zoe.getId(), model.getSelectedPersonId());
                enter(input, "schedule 2 d/Tue t/19:00");
                assertEquals(List.of("Zoe", "Amy"), names(summary));
                enter(input, "delete 1");
                assertEquals(List.of("Amy"), names(summary));
                assertEquals(amy.getId(), model.getSelectedPersonId());
                enter(input, "delete 1");
                assertTrue(summary.getItems().isEmpty());
                assertNull(rows.getSelectionModel().getSelectedItem());
                assertNull(model.getSelectedPersonId());
                assertTrue(storage.readAddressBook().orElseThrow().getPersonList().isEmpty());
            } finally {
                stage.hide();
            }
            return null;
        });
        Platform.runLater(task);
        task.get(30, TimeUnit.SECONDS);
    }

    private List<String> labels(StudentDetailsPanel details) {
        return details.getChildren().stream().map(node -> ((Label) node).getText()).toList();
    }

    private List<String> names(ListView<Person> rows) {
        return rows.getItems().stream().map(person -> person.getName().toString()).toList();
    }

    private void enter(TextField input, String command) {
        input.setText(command);
        input.fireEvent(new ActionEvent());
    }
}
