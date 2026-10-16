package tutorconnect.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static tutorconnect.testutil.TypicalStudents.getTypicalStudentDirectory;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tutorconnect.commons.core.GuiSettings;
import tutorconnect.model.ReadOnlyStudentDirectory;
import tutorconnect.model.StudentDirectory;
import tutorconnect.model.UserPrefs;

public class StorageManagerTest {

    @TempDir
    public Path testFolder;

    private StorageManager storageManager;

    @BeforeEach
    public void setUp() {
        JsonStudentDirectoryStorage studentDirectoryStorage = new JsonStudentDirectoryStorage(getTempFilePath("ab"));
        JsonUserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(getTempFilePath("prefs"));
        storageManager = new StorageManager(studentDirectoryStorage, userPrefsStorage);
    }

    private Path getTempFilePath(String fileName) {
        return testFolder.resolve(fileName);
    }

    @Test
    public void prefsReadSave() throws Exception {
        /*
         * Note: This is an integration test that verifies the StorageManager is properly wired to the
         * {@link JsonUserPrefsStorage} class.
         * More extensive testing of UserPref saving/reading is done in {@link JsonUserPrefsStorageTest} class.
         */
        UserPrefs original = new UserPrefs();
        original.setGuiSettings(new GuiSettings(300, 600, 4, 6));
        storageManager.saveUserPrefs(original);
        UserPrefs retrieved = storageManager.readUserPrefs().get();
        assertEquals(original, retrieved);
    }

    @Test
    public void studentDirectoryReadSave() throws Exception {
        /*
         * Note: This is an integration test that verifies the StorageManager is properly wired to the
         * {@link JsonStudentDirectoryStorage} class.
         * More extensive testing of UserPref saving/reading is done in {@link JsonStudentDirectoryStorageTest} class.
         */
        StudentDirectory original = getTypicalStudentDirectory();
        storageManager.saveStudentDirectory(original);
        ReadOnlyStudentDirectory retrieved = storageManager.readStudentDirectory().get();
        assertEquals(original, new StudentDirectory(retrieved));
    }

    @Test
    public void getStudentDirectoryFilePath() {
        assertNotNull(storageManager.getStudentDirectoryFilePath());
    }

}
