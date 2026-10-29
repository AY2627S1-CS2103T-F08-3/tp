package tutorconnect;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import tutorconnect.commons.core.GuiSettings;
import tutorconnect.model.ReadOnlyUserPrefs;
import tutorconnect.model.UserPrefs;
import tutorconnect.model.student.Phone;
import tutorconnect.model.student.Student;
import tutorconnect.storage.JsonStudentDirectoryStorage;
import tutorconnect.storage.JsonUserPrefsStorage;
import tutorconnect.storage.StorageManager;
import tutorconnect.testutil.JavaFxTestUtil;

/** Exercises renamed startup and FXML wiring without touching a user's saved data. */
@EnabledIfEnvironmentVariable(named = "F01_UI_TESTS", matches = "true")
public class TutorConnectAppTest {
    @TempDir
    private Path directory;

    @BeforeAll
    public static void startToolkit() {
        JavaFxTestUtil.startToolkit();
    }

    @Test
    public void init_missingFiles_loadsValidSamplesAndCreatesPreferences() throws Exception {
        onFxThread(() -> {
            assertNotNull(new TutorConnectApp()); // JavaFX still has its public no-argument constructor.
            TutorConnectApp app = app();
            app.init();
            var samples = app.model.getStudentDirectory().getStudentList();
            assertEquals(List.of("Alex Yeoh", "Bernice Yu", "Charlotte Oliveiro", "David Li", "Irfan Ibrahim",
                    "Roy Balakrishnan"), samples.stream().map(student -> student.getName().fullName).toList());
            assertEquals(6, samples.stream().map(Student::getId).distinct().count());
            for (Student student : samples) {
                assertTrue(Phone.isValidPhone(student.getPhone().value));
                assertTrue(student.getPhone().value.startsWith("+65"));
                assertTrue(student.getStudentFields().toStorage().isEmpty());
                assertFalse(student.getTags().isEmpty());
            }
            assertTrue(Files.exists(directory.resolve("preferences.json")));
            assertFalse(Files.exists(directory.resolve("addressbook.json")));
            app.stop();
            return null;
        });
    }

    @Test
    public void init_existingLegacyData_opensRenamedWindowAndKeepsPreferences() throws Exception {
        Files.writeString(directory.resolve("addressbook.json"), """
                {"persons":[{"name":"Alex Tan","phone":"81234567","address":"21 Street",
                "guardianPhone":"+6591234567","subject":"H2 Math","hourlyRate":"80.00"}]}
                """);
        UserPrefs prefs = new UserPrefs();
        prefs.setGuiSettings(new GuiSettings(900, 650, 20, 20));
        new JsonUserPrefsStorage(directory.resolve("preferences.json")).saveUserPrefs(prefs);
        onFxThread(() -> {
            TutorConnectApp app = app();
            app.init();
            assertEquals(directory.resolve("addressbook.json"), app.storage.getStudentDirectoryFilePath());
            assertEquals(prefs, app.model.getUserPrefs());
            Student student = app.model.getStudentDirectory().getStudentList().getFirst();
            assertEquals("+6591234567", student.getGuardianPhone().orElseThrow().value);
            Stage stage = new Stage();
            try {
                app.start(stage);
                assertTrue(stage.isShowing());
                assertEquals("TutorConnect", stage.getTitle());
                assertNotNull(stage.getScene().lookup("#studentListView"));
                app.logic.execute("guardian 1 g/67778899");
                app.stop();
                Student saved = new JsonStudentDirectoryStorage(directory.resolve("addressbook.json"))
                        .readStudentDirectory().orElseThrow().getStudentList().getFirst();
                assertEquals(student.getId(), saved.getId());
                assertEquals("+6567778899", saved.getGuardianPhone().orElseThrow().value);
                assertEquals("H2 Math", saved.getStudentFields().display("subject"));
                assertEquals("80.00", saved.getHourlyRate().orElseThrow().toString());
            } finally {
                stage.hide();
            }
            return null;
        });
    }

    @Test
    public void init_invalidFiles_fallsBackWithoutOverwritingStudentData() throws Exception {
        String invalid = "{\"persons\":[null]}";
        Files.writeString(directory.resolve("addressbook.json"), invalid);
        Files.writeString(directory.resolve("preferences.json"), "not JSON");
        onFxThread(() -> {
            TutorConnectApp app = app();
            app.init();
            assertTrue(app.model.getStudentDirectory().getStudentList().isEmpty());
            assertEquals(new UserPrefs(), app.model.getUserPrefs());
            app.stop();
            assertEquals(invalid, Files.readString(directory.resolve("addressbook.json")));
            return null;
        });
    }

    @Test
    public void start_unreadableData_warnsAndKeepsCopyAfterNextSave() throws Exception {
        String unreadable = "{\"persons\":[{\"name\":\"Amy\",\"phone\":\"+6581234567\",\"address\":\"1 St\"},"
                + "{\"name\":\"Ben\",\"phone\":\"12\",\"address\":\"2 St\"}]}";
        Files.writeString(directory.resolve("addressbook.json"), unreadable);
        onFxThread(() -> {
            TutorConnectApp app = app();
            app.init();
            List<Path> backups;
            try (var files = Files.list(directory)) {
                backups = files.filter(file -> file.getFileName().toString().endsWith(".bak")).toList();
            }
            assertEquals(1, backups.size());
            assertEquals(unreadable, Files.readString(backups.getFirst()));
            String warning = app.getStartupWarning();
            assertTrue(warning.startsWith("Warning: "));
            assertTrue(warning.contains(backups.getFirst().getFileName().toString()));
            Stage stage = new Stage();
            try {
                app.start(stage);
                TextArea result = (TextArea) stage.getScene().lookup("#resultDisplay");
                assertEquals(warning, result.getText());
                app.logic.execute("add n/Cara p/83456789 a/3 St");
                assertEquals(unreadable, Files.readString(backups.getFirst()));
                app.stop();
            } finally {
                stage.hide();
            }
            return null;
        });
    }

    @Test
    public void init_readableData_hasNoStartupWarning() throws Exception {
        onFxThread(() -> {
            TutorConnectApp app = app();
            app.init();
            assertNull(app.getStartupWarning());
            return null;
        });
    }

    @Test
    public void preferenceSaveFailures_doNotCrashInitializationOrShutdown() throws Exception {
        onFxThread(() -> {
            TutorConnectApp app = app();
            app.init();
            AtomicInteger attempts = new AtomicInteger();
            JsonUserPrefsStorage failing = new JsonUserPrefsStorage(directory.resolve("preferences.json")) {
                @Override
                public void saveUserPrefs(ReadOnlyUserPrefs prefs) throws IOException {
                    attempts.incrementAndGet();
                    throw new IOException("Injected preference write failure");
                }
            };
            assertEquals(new UserPrefs(), app.initPrefs(failing));
            app.storage = new StorageManager(new JsonStudentDirectoryStorage(directory.resolve("addressbook.json")),
                    failing);
            assertDoesNotThrow(app::stop);
            assertEquals(2, attempts.get());
            return null;
        });
    }

    private TutorConnectApp app() {
        return new TutorConnectApp(directory.resolve("preferences.json"), directory.resolve("addressbook.json"));
    }

    private void onFxThread(Callable<Void> action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(action);
        Platform.runLater(task);
        task.get(30, TimeUnit.SECONDS);
    }
}
