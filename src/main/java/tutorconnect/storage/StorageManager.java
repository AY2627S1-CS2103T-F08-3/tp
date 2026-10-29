package tutorconnect.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

import tutorconnect.commons.core.LogsCenter;
import tutorconnect.commons.exceptions.DataLoadingException;
import tutorconnect.model.ReadOnlyStudentDirectory;
import tutorconnect.model.ReadOnlyUserPrefs;
import tutorconnect.model.UserPrefs;

/**
 * Manages storage of StudentDirectory data in local storage.
 */
public class StorageManager implements Storage {

    private static final Logger logger = LogsCenter.getLogger(StorageManager.class);
    private JsonStudentDirectoryStorage studentDirectoryStorage;
    private JsonUserPrefsStorage userPrefsStorage;

    /**
     * Creates a {@code StorageManager} with the given student directory and user prefs storage.
     */
    public StorageManager(JsonStudentDirectoryStorage studentDirectoryStorage, JsonUserPrefsStorage userPrefsStorage) {
        this.studentDirectoryStorage = studentDirectoryStorage;
        this.userPrefsStorage = userPrefsStorage;
    }

    // ================ UserPrefs methods ==============================

    @Override
    public Path getUserPrefsFilePath() {
        return userPrefsStorage.getUserPrefsFilePath();
    }

    @Override
    public Optional<UserPrefs> readUserPrefs() throws DataLoadingException {
        return userPrefsStorage.readUserPrefs();
    }

    @Override
    public void saveUserPrefs(ReadOnlyUserPrefs userPrefs) throws IOException {
        userPrefsStorage.saveUserPrefs(userPrefs);
    }


    // ================ StudentDirectory methods ==============================

    @Override
    public Path getStudentDirectoryFilePath() {
        return studentDirectoryStorage.getStudentDirectoryFilePath();
    }

    @Override
    public Optional<ReadOnlyStudentDirectory> readStudentDirectory() throws DataLoadingException {
        logger.fine("Attempting to read data from file: " + studentDirectoryStorage.getStudentDirectoryFilePath());
        return studentDirectoryStorage.readStudentDirectory();
    }

    @Override
    public void saveStudentDirectory(ReadOnlyStudentDirectory studentDirectory) throws IOException {
        logger.fine("Attempting to write to data file: " + studentDirectoryStorage.getStudentDirectoryFilePath());
        studentDirectoryStorage.saveStudentDirectory(studentDirectory);
    }

}
