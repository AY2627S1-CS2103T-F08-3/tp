package tutorconnect.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tutorconnect.logic.LogicManager;
import tutorconnect.model.ModelManager;
import tutorconnect.model.ReadOnlyStudentDirectory;
import tutorconnect.model.UserPrefs;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.StudentFields;
import tutorconnect.model.student.Subject;
import tutorconnect.storage.JsonStudentDirectoryStorage;
import tutorconnect.storage.JsonUserPrefsStorage;
import tutorconnect.storage.StorageManager;
import tutorconnect.testutil.JavaFxTestUtil;
import tutorconnect.testutil.StudentBuilder;

/** Real F06 JavaFX rendering and command-box acceptance; opt in with F06_UI_TESTS=true. */
@EnabledIfEnvironmentVariable(named = "F06_UI_TESTS", matches = "true")
public class SubjectUiTest {
    @TempDir
    private Path directory;

    @BeforeAll
    public static void startToolkit() {
        JavaFxTestUtil.startToolkit();
    }

    @Test
    public void storedNoncanonicalSubject_cardAndDetailsUseNormalizedDisplayWithoutMutatingData() throws Exception {
        onFxThread(() -> {
            StudentFields fields = new StudentFields(Map.of("subject", TextNode.valueOf("  Ｈ２   Math  ")));
            Student student = new StudentBuilder().build().withStudentFields(fields);
            assertCardAndDetails(student, "H2 Math");
            assertEquals("  Ｈ２   Math  ", fields.toStorage().get("subject").asText());
            return null;
        });
    }

    @Test
    public void unsetOrUnreadableSubject_cardAndDetailsShowEmDash() throws Exception {
        onFxThread(() -> {
            assertCardAndDetails(new StudentBuilder().build(), "—");
            for (JsonNode value : List.of(TextNode.valueOf("@@"), IntNode.valueOf(12))) {
                Student student = new StudentBuilder().build().withStudentFields(
                        new StudentFields(Map.of("subject", value)));
                assertCardAndDetails(student, "—");
                assertEquals(value, student.getStudentFields().toStorage().get("subject"));
            }
            return null;
        });
    }

    @Test
    public void commandBox_subjectLifecycleFilteredIndexFailureAndRestart_followPublishedState() throws Exception {
        onFxThread(() -> {
            ModelManager model = new ModelManager();
            model.addStudent(new StudentBuilder().withName("First Student").build());
            model.addStudent(new StudentBuilder().withName("Alex Tan").build());
            AtomicBoolean failSave = new AtomicBoolean();
            JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(directory.resolve("students.json")) {
                @Override
                public void saveStudentDirectory(ReadOnlyStudentDirectory proposed) throws IOException {
                    if (failSave.get()) {
                        throw new IOException("Injected F06 UI save failure");
                    }
                    super.saveStudentDirectory(proposed);
                }
            };
            Stage stage = open(model, storage);
            try {
                TextField input = (TextField) stage.getScene().lookup("#commandTextField");
                TextArea status = (TextArea) stage.getScene().lookup("#resultDisplay");
                ListView<Student> rows = rows(stage);
                rows.getSelectionModel().select(1);
                assertRendered(stage, "—");
                enter(input, "subject 2 s/Ｈ２   Math");
                assertEquals("Subject set for Alex Tan: H2 Math.", status.getText());
                assertRendered(stage, "H2 Math");
                Student target = rows.getItems().getLast();
                rows.getSelectionModel().select(0);
                Student selected = rows.getSelectionModel().getSelectedItem();
                List<String> details = detailLabels(stage);
                String disk = Files.readString(storage.getStudentDirectoryFilePath());
                failSave.set(true);
                enter(input, "subject 2 s/h2 math");
                assertEquals("Subject for Alex Tan is already H2 Math.", status.getText());
                assertSame(selected, rows.getSelectionModel().getSelectedItem());
                assertEquals(details, detailLabels(stage));
                assertSame(target, rows.getItems().getLast());
                enter(input, "subject 2 s/Physics & Chemistry");
                assertEquals(LogicManager.MESSAGE_SAVE_FAILURE, status.getText());
                assertSame(selected, rows.getSelectionModel().getSelectedItem());
                assertSame(target, rows.getItems().getLast());
                assertEquals(details, detailLabels(stage));
                assertEquals(disk, Files.readString(storage.getStudentDirectoryFilePath()));
                enter(input, "subject 999 s/@");
                assertEquals(Subject.MESSAGE_CONSTRAINTS, status.getText());
                assertEquals(details, detailLabels(stage));
                failSave.set(false);
                enter(input, "find Alex");
                assertEquals(1, rows.getItems().size());
                enter(input, "subject 1 s/Physics & Chemistry");
                assertEquals("Subject updated for Alex Tan: H2 Math -> Physics & Chemistry.", status.getText());
                assertEquals(target.getId(), rows.getSelectionModel().getSelectedItem().getId());
                assertSame(selected, model.getStudentDirectory().getStudentList().getFirst());
                assertRendered(stage, "Physics & Chemistry");
                enter(input, "subject 2 s/English Language");
                assertEquals("Error: No student exists at index 2.", status.getText());
                assertRendered(stage, "Physics & Chemistry");
            } finally {
                stage.hide();
            }

            ModelManager restarted = new ModelManager(storage.readStudentDirectory().orElseThrow(), new UserPrefs());
            Stage reopened = open(restarted, storage);
            try {
                rows(reopened).getSelectionModel().select(1);
                assertRendered(reopened, "Physics & Chemistry");
                rows(reopened).getSelectionModel().select(0);
                assertRendered(reopened, "—");
            } finally {
                reopened.hide();
            }
            return null;
        });
    }

    private Stage open(ModelManager model, JsonStudentDirectoryStorage storage) {
        LogicManager logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
        Stage stage = new Stage();
        MainWindow window = new MainWindow(stage, logic, storage.getStudentDirectoryFilePath());
        window.fillInnerParts();
        window.show();
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().layout();
        return stage;
    }

    @SuppressWarnings("unchecked")
    private ListView<Student> rows(Stage stage) {
        return (ListView<Student>) stage.getScene().lookup("#studentListView");
    }

    private List<String> detailLabels(Stage stage) {
        StudentDetailsPanel details = (StudentDetailsPanel) stage.getScene().lookup("#studentDetails");
        return details.getChildren().stream().map(node -> ((Label) node).getText()).toList();
    }

    private void assertRendered(Stage stage, String subject) {
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().layout();
        assertTrue(detailLabels(stage).contains("Subject: " + subject));
        assertTrue(rows(stage).lookupAll("#subject").stream()
                .anyMatch(node -> ((Label) node).getText().equals("Subject: " + subject)));
    }

    private void assertCardAndDetails(Student student, String subject) {
        StudentCard card = new StudentCard(student, 1);
        assertEquals("Subject: " + subject, ((Label) card.getRoot().lookup("#subject")).getText());
        StudentDetailsPanel details = new StudentDetailsPanel();
        details.showStudent(student);
        assertTrue(details.getChildren().stream()
                .anyMatch(node -> ((Label) node).getText().equals("Subject: " + subject)));
    }

    private void enter(TextField input, String command) {
        input.setText(command);
        input.fireEvent(new ActionEvent());
    }

    private void onFxThread(Callable<Void> action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(action);
        Platform.runLater(task);
        task.get(30, TimeUnit.SECONDS);
    }
}
