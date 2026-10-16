package tutorconnect.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;

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
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import tutorconnect.logic.LogicManager;
import tutorconnect.model.ModelManager;
import tutorconnect.model.ReadOnlyStudentDirectory;
import tutorconnect.model.UserPrefs;
import tutorconnect.model.student.Student;
import tutorconnect.storage.JsonStudentDirectoryStorage;
import tutorconnect.storage.JsonUserPrefsStorage;
import tutorconnect.storage.StorageManager;
import tutorconnect.testutil.JavaFxTestUtil;

/** Real command-box verification of guardian-specific selection, details and restart behavior. */
@EnabledIfEnvironmentVariable(named = "F01_UI_TESTS", matches = "true")
public class GuardianContactUiTest {
    @TempDir
    private Path directory;

    @BeforeAll
    public static void startToolkit() {
        JavaFxTestUtil.startToolkit();
    }

    @Test
    public void commandBox_guardianLifecycleAndFreshWindow_preserveCorrectStudentDetails() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Path file = directory.resolve("students.json");
            AtomicBoolean failSave = new AtomicBoolean();
            JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(file) {
                @Override
                public void saveStudentDirectory(ReadOnlyStudentDirectory proposed) throws IOException {
                    if (failSave.get()) {
                        throw new IOException("Injected guardian UI save failure");
                    }
                    super.saveStudentDirectory(proposed);
                }
            };
            Stage stage = window(new ModelManager(), storage);
            try {
                enter(stage, "add n/Alex Tan p/81234567 a/21 Clementi Ave");
                enter(stage, "add n/Nur Aisyah p/92345678 a/8 Jalan Besar");
                assertGuardian(stage, "—");
                capture(stage, "unset");
                String disk = Files.readString(file);
                failSave.set(true);
                enter(stage, "guardian 1 g/91234567");
                assertStatus(stage, LogicManager.MESSAGE_SAVE_FAILURE);
                assertEquals("Nur Aisyah", selected(stage).getName().fullName);
                assertGuardian(stage, "—");
                assertEquals(disk, Files.readString(file));
                failSave.set(false);
                enter(stage, "find Nur");
                enter(stage, "guardian 1 g/+65 9123 4567");
                assertStatus(stage, "Guardian contact set for Nur Aisyah: +6591234567.");
                assertEquals("Nur Aisyah", selected(stage).getName().fullName);
                assertGuardian(stage, "+6591234567");
                enter(stage, "guardian 1 g/67778899");
                assertStatus(stage, "Guardian contact updated for Nur Aisyah: +6591234567 -> +6567778899.");
                assertGuardian(stage, "+6567778899");
                capture(stage, "replacement-filtered");
                enter(stage, "guardian 2 g/81234567");
                assertStatus(stage, "Error: No student exists at index 2.");
                assertGuardian(stage, "+6567778899");
                enter(stage, "list");
                rows(stage).getSelectionModel().select(0);
                disk = Files.readString(file);
                failSave.set(true);
                enter(stage, "guardian 2 g/+65-6777-8899");
                assertStatus(stage, "Guardian contact for Nur Aisyah is already +6567778899.");
                assertEquals("Alex Tan", selected(stage).getName().fullName);
                assertGuardian(stage, "—");
                enter(stage, "guardian 2 g/81234567");
                assertStatus(stage, LogicManager.MESSAGE_SAVE_FAILURE);
                assertEquals("Alex Tan", selected(stage).getName().fullName);
                assertGuardian(stage, "—");
                assertEquals(disk, Files.readString(file));
            } finally {
                stage.hide();
            }
            // A new model, storage instance and window exercise the startup load boundary.
            JsonStudentDirectoryStorage restartedStorage = new JsonStudentDirectoryStorage(file);
            Stage restarted = window(new ModelManager(restartedStorage.readStudentDirectory().orElseThrow(),
                    new UserPrefs()), restartedStorage);
            try {
                rows(restarted).getSelectionModel().select(0);
                assertGuardian(restarted, "—");
                rows(restarted).getSelectionModel().select(1);
                assertEquals("Nur Aisyah", selected(restarted).getName().fullName);
                assertGuardian(restarted, "+6567778899");
                capture(restarted, "restarted");
                restarted.setWidth(restarted.getMinWidth());
                restarted.setHeight(restarted.getMinHeight());
                capture(restarted, "minimum-window");
            } finally {
                restarted.hide();
            }
            return null;
        });
        Platform.runLater(task);
        task.get(30, TimeUnit.SECONDS);
    }

    private Stage window(ModelManager model, JsonStudentDirectoryStorage storage) {
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

    private void enter(Stage stage, String command) {
        TextField input = (TextField) stage.getScene().lookup("#commandTextField");
        input.setText(command);
        input.fireEvent(new ActionEvent());
    }

    @SuppressWarnings("unchecked")
    private ListView<Student> rows(Stage stage) {
        return (ListView<Student>) stage.getScene().lookup("#studentListView");
    }

    private Student selected(Stage stage) {
        return rows(stage).getSelectionModel().getSelectedItem();
    }

    private void assertStatus(Stage stage, String expected) {
        assertEquals(expected, ((TextArea) stage.getScene().lookup("#resultDisplay")).getText());
    }

    private void assertGuardian(Stage stage, String expected) {
        StudentDetailsPanel panel = (StudentDetailsPanel) stage.getScene().lookup("#studentDetails");
        assertTrue(panel.getChildren().stream().map(node -> ((Label) node).getText())
                .anyMatch(text -> text.equals("Guardian phone: " + expected)));
    }

    /** Captures the actual rendered test scene for optional visual inspection, not a mock-up. */
    private void capture(Stage stage, String name) throws IOException {
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().layout();
        // Let JavaFX complete a render pulse; a synchronous snapshot can retain stale control skins
        // when several commands execute within this test's single FX runnable.
        WritableImage image = null;
        for (int frame = 0; frame < 2; frame++) {
            Object pulse = new Object();
            stage.getScene().snapshot(result -> {
                Platform.exitNestedEventLoop(pulse, result.getImage());
                return null;
            }, null);
            image = (WritableImage) Platform.enterNestedEventLoop(pulse);
        }
        BufferedImage png = new BufferedImage((int) image.getWidth(), (int) image.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < png.getHeight(); y++) {
            for (int x = 0; x < png.getWidth(); x++) {
                png.setRGB(x, y, image.getPixelReader().getArgb(x, y));
            }
        }
        Path output = Path.of("build", "guardian-ui", name + ".png");
        Files.createDirectories(output.getParent());
        ImageIO.write(png, "png", output.toFile());
    }
}
