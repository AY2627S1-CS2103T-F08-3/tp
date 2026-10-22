package tutorconnect.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
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
import javafx.stage.Stage;
import tutorconnect.logic.LogicManager;
import tutorconnect.model.ModelManager;
import tutorconnect.model.student.Student;
import tutorconnect.storage.JsonStudentDirectoryStorage;
import tutorconnect.storage.JsonUserPrefsStorage;
import tutorconnect.storage.StorageManager;
import tutorconnect.testutil.JavaFxTestUtil;

/** Exercises day/week switching through the real command box, not a second schedule model. */
@EnabledIfEnvironmentVariable(named = "F08_UI_TESTS", matches = "true")
public class TimetableUiTest {
    @TempDir
    private Path directory;

    @BeforeAll
    public static void startToolkit() {
        JavaFxTestUtil.startToolkit();
    }

    @Test
    public void timetable_filtersOnlySummaryAndRefreshesAfterChanges() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            ModelManager model = new ModelManager();
            var storage = new JsonStudentDirectoryStorage(directory.resolve("students.json"));
            var logic = new LogicManager(model, new StorageManager(storage,
                    new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
            Stage stage = new Stage();
            try {
                MainWindow window = new MainWindow(stage, logic, storage.getStudentDirectoryFilePath());
                window.fillInnerParts();
                window.show();
                TextField input = (TextField) stage.getScene().lookup("#commandTextField");
                TextArea status = (TextArea) stage.getScene().lookup("#resultDisplay");
                Label heading = (Label) stage.getScene().lookup("#timetableHeading");
                @SuppressWarnings("unchecked")
                ListView<Student> rows = (ListView<Student>) stage.getScene().lookup("#weeklyScheduleList");
                enter(input, "timetable d/Mon");
                assertEquals("Monday schedule", heading.getText());
                assertEquals("No lessons scheduled for Monday.", ((Label) rows.getPlaceholder()).getText());
                enter(input, "add n/Zoe p/81234567 a/Test Street");
                enter(input, "add n/Amy p/92345678 a/Test Street");
                enter(input, "add n/Ben p/92345678 a/Test Street");
                enter(input, "schedule 1 d/Sun t/23:59");
                enter(input, "schedule 2 d/Mon t/09:00");
                enter(input, "schedule 3 d/Mon t/09:00");
                assertEquals(List.of("Amy", "Ben"), names(rows));
                enter(input, "find Zoe");
                enter(input, "timetable");
                assertEquals(List.of("Amy", "Ben", "Zoe"), names(rows));
                assertEquals(1, model.getFilteredStudentList().size());
                enter(input, "schedule 1 d/Mon t/00:00");
                enter(input, "timetable d/Mon");
                assertEquals(List.of("Zoe", "Amy", "Ben"), names(rows));
                capture(stage, "timetable-monday.png");
                var selected = model.getSelectedStudentId();
                enter(input, "timetable d/nope");
                assertTrue(status.getText().startsWith("Error: Invalid day."));
                assertEquals("Monday schedule", heading.getText());
                assertEquals(selected, model.getSelectedStudentId());
                enter(input, "edit 1 n/Zara");
                assertEquals(List.of("Zara", "Amy", "Ben"), names(rows));
                enter(input, "delete 1");
                assertEquals(List.of("Amy", "Ben"), names(rows));
                enter(input, "timetable d/Sun");
                assertTrue(rows.getItems().isEmpty());
                enter(input, "timetable");
                assertEquals("Weekly schedule", heading.getText());
                enter(input, "clear");
                assertTrue(rows.getItems().isEmpty());
                assertEquals("No weekly lessons scheduled.", ((Label) rows.getPlaceholder()).getText());
            } finally {
                stage.hide();
            }
            return null;
        });
        Platform.runLater(task);
        task.get(30, TimeUnit.SECONDS);
    }

    private List<String> names(ListView<Student> rows) {
        return rows.getItems().stream().map(student -> student.getName().toString()).toList();
    }

    private void capture(Stage stage, String filename) throws Exception {
        String output = System.getenv("F08_SCREENSHOT_DIR");
        if (output == null || output.isBlank()) {
            return;
        }
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().layout();
        var snapshot = stage.getScene().snapshot(null);
        BufferedImage pixels = new BufferedImage((int) snapshot.getWidth(), (int) snapshot.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < pixels.getHeight(); y++) {
            for (int x = 0; x < pixels.getWidth(); x++) {
                pixels.setRGB(x, y, snapshot.getPixelReader().getArgb(x, y));
            }
        }
        Path destination = Path.of(output).resolve(filename);
        Files.createDirectories(destination.getParent());
        assertTrue(ImageIO.write(pixels, "png", destination.toFile()));
    }

    private void enter(TextField input, String command) {
        input.setText(command);
        input.fireEvent(new ActionEvent());
    }
}
