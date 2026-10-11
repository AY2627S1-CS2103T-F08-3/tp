package tutorconnect.ui;

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
import tutorconnect.logic.LogicManager;
import tutorconnect.model.ModelManager;
import tutorconnect.model.ReadOnlyStudentDirectory;
import tutorconnect.model.student.Address;
import tutorconnect.model.student.Name;
import tutorconnect.model.student.Phone;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.WeeklySlot;
import tutorconnect.storage.JsonStudentDirectoryStorage;
import tutorconnect.storage.JsonUserPrefsStorage;
import tutorconnect.storage.StorageManager;
import tutorconnect.testutil.JavaFxTestUtil;

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
            Student zoe = new Student(new Name("Zoe"), new Phone("81234567"), new Address("21 Clementi Ave"));
            Student amy = new Student(new Name("Amy"), new Phone("81234567"), new Address("22 Clementi Ave"));
            model.addStudent(zoe);
            model.addStudent(amy);
            AtomicBoolean failSave = new AtomicBoolean();
            JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(directory.resolve("students.json")) {
                @Override
                public void saveStudentDirectory(ReadOnlyStudentDirectory proposed) throws IOException {
                    if (failSave.get()) {
                        throw new IOException("Injected UI save failure");
                    }
                    super.saveStudentDirectory(proposed);
                }
            };
            storage.saveStudentDirectory(model.getStudentDirectory());
            LogicManager logic = new LogicManager(model, new StorageManager(storage,
                    new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
            Stage stage = new Stage();
            try {
                MainWindow window = new MainWindow(stage, logic, storage.getStudentDirectoryFilePath());
                window.fillInnerParts();
                window.show();
                stage.getScene().getRoot().applyCss();
                stage.getScene().getRoot().layout();
                TextField input = (TextField) stage.getScene().lookup("#commandTextField");
                TextArea status = (TextArea) stage.getScene().lookup("#resultDisplay");
                @SuppressWarnings("unchecked")
                ListView<Student> rows = (ListView<Student>) stage.getScene().lookup("#studentListView");
                @SuppressWarnings("unchecked")
                ListView<Student> summary = (ListView<Student>) stage.getScene().lookup("#weeklyScheduleList");
                StudentDetailsPanel details = (StudentDetailsPanel) stage.getScene().lookup("#studentDetails");
                rows.getSelectionModel().select(0);
                assertTrue(labels(details).contains("Weekly slot: —"));
                enter(input, "schedule 1 d/Sun t/23:59");
                assertEquals("Weekly lesson set for Zoe: Sunday 23:59.", status.getText());
                assertTrue(labels(details).contains("Weekly slot: Sunday 23:59"));
                enter(input, "schedule 2 t/09:00 d/Mon");
                assertEquals(List.of("Amy", "Zoe"), names(summary));
                assertEquals(List.of("Zoe", "Amy"), names(rows));
                assertEquals(amy.getId(), model.getSelectedStudentId());
                enter(input, "guardian 1 g/91234567");
                enter(input, "level 1 l/Sec4");
                enter(input, "subject 1 s/Math/Science");
                enter(input, "rate 1 r/65");
                assertEquals(zoe.getId(), model.getSelectedStudentId());
                assertTrue(labels(details).containsAll(List.of("Guardian phone: +6591234567",
                        "Education level: Secondary 4", "Subject: Math/Science", "Hourly rate: 65.00",
                        "Weekly slot: Sunday 23:59")));
                Student complete = rows.getItems().getFirst();
                assertEquals(complete, storage.readStudentDirectory().orElseThrow().getStudentList().getFirst());
                enter(input, "list");
                assertEquals("Listed 2 students.", status.getText());
                assertNull(rows.getSelectionModel().getSelectedItem());
                assertNull(model.getSelectedStudentId());
                assertEquals(List.of("Zoe", "Amy"), names(rows));
                assertEquals(List.of("Amy", "Zoe"), names(summary));
                rows.getSelectionModel().select(1);
                Student selected = rows.getSelectionModel().getSelectedItem();
                List<Node> detailNodes = List.copyOf(details.getChildren());
                List<Student> scheduled = List.copyOf(summary.getItems());
                AtomicInteger changes = new AtomicInteger();
                summary.getItems().addListener((ListChangeListener<Student>) change -> changes.incrementAndGet());
                enter(input, "schedule 1 d/sUN t/２３：５９");
                assertEquals("Weekly lesson for Zoe is already Sunday 23:59.", status.getText());
                assertSame(selected, rows.getSelectionModel().getSelectedItem());
                assertEquals(detailNodes, details.getChildren());
                enter(input, "schedule 999 t/24:00 d/Funday");
                assertEquals(WeeklySlot.MESSAGE_INVALID_DAY, status.getText());
                enter(input, "schedule 1 d/Mon t/12:60");
                assertEquals(WeeklySlot.MESSAGE_INVALID_TIME, status.getText());
                String disk = Files.readString(storage.getStudentDirectoryFilePath());
                failSave.set(true);
                enter(input, "schedule 1 d/Tue t/19:00");
                assertEquals(LogicManager.MESSAGE_SAVE_FAILURE, status.getText());
                assertSame(selected, rows.getSelectionModel().getSelectedItem());
                assertEquals(detailNodes, details.getChildren());
                assertEquals(scheduled, summary.getItems());
                assertEquals(0, changes.get());
                assertEquals(disk, Files.readString(storage.getStudentDirectoryFilePath()));
                failSave.set(false);
                enter(input, "schedule 1 d/Tue t/19:00");
                assertEquals("Weekly lesson updated for Zoe: Sunday 23:59 -> Tuesday 19:00.", status.getText());
                assertTrue(labels(details).contains("Weekly slot: Tuesday 19:00"));
                assertEquals(zoe.getId(), model.getSelectedStudentId());
                enter(input, "schedule 2 d/Tue t/19:00");
                assertEquals(List.of("Zoe", "Amy"), names(summary));
                Student rescheduled = rows.getItems().getFirst();
                for (String key : List.of("guardianPhone", "educationLevel", "subject", "hourlyRate")) {
                    assertEquals(complete.getStudentFields().toStorage().get(key),
                            rescheduled.getStudentFields().toStorage().get(key), key);
                }
                enter(input, "delete 1");
                assertEquals(List.of("Amy"), names(summary));
                assertEquals(amy.getId(), model.getSelectedStudentId());
                enter(input, "delete 1");
                assertTrue(summary.getItems().isEmpty());
                assertNull(rows.getSelectionModel().getSelectedItem());
                assertNull(model.getSelectedStudentId());
                assertTrue(storage.readStudentDirectory().orElseThrow().getStudentList().isEmpty());
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

    private List<String> names(ListView<Student> rows) {
        return rows.getItems().stream().map(student -> student.getName().toString()).toList();
    }

    private void enter(TextField input, String command) {
        input.setText(command);
        input.fireEvent(new ActionEvent());
    }
}
