package tutorconnect;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.stage.Stage;
import tutorconnect.commons.core.LogsCenter;
import tutorconnect.commons.exceptions.DataLoadingException;
import tutorconnect.commons.util.StringUtil;
import tutorconnect.logic.Logic;
import tutorconnect.logic.LogicManager;
import tutorconnect.model.Model;
import tutorconnect.model.ModelManager;
import tutorconnect.model.ReadOnlyStudentDirectory;
import tutorconnect.model.ReadOnlyUserPrefs;
import tutorconnect.model.StudentDirectory;
import tutorconnect.model.UserPrefs;
import tutorconnect.model.util.SampleDataUtil;
import tutorconnect.storage.JsonStudentDirectoryStorage;
import tutorconnect.storage.JsonUserPrefsStorage;
import tutorconnect.storage.Storage;
import tutorconnect.storage.StorageManager;
import tutorconnect.ui.Ui;
import tutorconnect.ui.UiManager;

/**
 * Runs the application.
 */
public class TutorConnectApp extends Application {

    public static final String VERSION = "1.5.0-ea";

    private static final Logger logger = LogsCenter.getLogger(TutorConnectApp.class);
    private static final Path USER_PREFS_FILE_PATH = Paths.get("preferences.json");
    private static final Path STUDENT_DIRECTORY_FILE_PATH = Paths.get("data", "addressbook.json");
    protected Ui ui;
    protected Logic logic;
    protected Storage storage;
    protected Model model;

    private final Path userPrefsFilePath;
    private final Path studentDirectoryFilePath;

    /** Uses the existing data locations so upgrading does not hide saved students. */
    public TutorConnectApp() {
        this(USER_PREFS_FILE_PATH, STUDENT_DIRECTORY_FILE_PATH);
    }

    /** Allows lifecycle verification with isolated data, without changing global working directories. */
    TutorConnectApp(Path userPrefsFilePath, Path studentDirectoryFilePath) {
        this.userPrefsFilePath = userPrefsFilePath;
        this.studentDirectoryFilePath = studentDirectoryFilePath;
    }

    @Override
    public void init() throws Exception {
        logger.info("=============================[ Initializing TutorConnect ]===========================");
        super.init();

        JsonUserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(userPrefsFilePath);
        UserPrefs userPrefs = initPrefs(userPrefsStorage);
        JsonStudentDirectoryStorage studentDirectoryStorage = new JsonStudentDirectoryStorage(studentDirectoryFilePath);
        storage = new StorageManager(studentDirectoryStorage, userPrefsStorage);

        model = initModelManager(storage, userPrefs);

        logic = new LogicManager(model, storage);

        ui = new UiManager(logic, storage.getStudentDirectoryFilePath());
    }

    /**
     * Returns a {@code ModelManager} with the data from {@code storage}'s address book and {@code userPrefs}. <br>
     * The data from the sample address book will be used instead if {@code storage}'s address book is not found,
     * or an empty address book will be used instead if errors occur when reading {@code storage}'s address book.
     */
    private Model initModelManager(Storage storage, ReadOnlyUserPrefs userPrefs) {
        logger.info("Using data file : " + storage.getStudentDirectoryFilePath());

        Optional<ReadOnlyStudentDirectory> studentDirectoryOptional;
        ReadOnlyStudentDirectory initialData;
        try {
            studentDirectoryOptional = storage.readStudentDirectory();
            if (studentDirectoryOptional.isEmpty()) {
                logger.info("Creating a new data file " + storage.getStudentDirectoryFilePath()
                        + " populated with a sample StudentDirectory.");
            }
            initialData = studentDirectoryOptional.orElseGet(SampleDataUtil::getSampleStudentDirectory);
        } catch (DataLoadingException e) {
            logger.warning("Data file at " + storage.getStudentDirectoryFilePath() + " could not be loaded."
                    + " Will be starting with an empty StudentDirectory.");
            initialData = new StudentDirectory();
        }

        return new ModelManager(initialData, userPrefs);
    }

    /**
     * Returns a {@code UserPrefs} using the file at {@code storage}'s user prefs file path,
     * or a new {@code UserPrefs} with default configuration if errors occur when
     * reading from the file.
     */
    protected UserPrefs initPrefs(JsonUserPrefsStorage storage) {
        Path prefsFilePath = storage.getUserPrefsFilePath();
        logger.info("Using preference file : " + prefsFilePath);

        UserPrefs initializedPrefs;
        try {
            Optional<UserPrefs> prefsOptional = storage.readUserPrefs();
            if (prefsOptional.isEmpty()) {
                logger.info("Creating new preference file " + prefsFilePath);
            }
            initializedPrefs = prefsOptional.orElse(new UserPrefs());
        } catch (DataLoadingException e) {
            logger.warning("Preference file at " + prefsFilePath + " could not be loaded."
                    + " Using default preferences.");
            initializedPrefs = new UserPrefs();
        }

        //Update prefs file in case it was missing to begin with or there are new/unused fields
        try {
            storage.saveUserPrefs(initializedPrefs);
        } catch (IOException e) {
            logger.warning("Failed to save preference file : " + StringUtil.getDetails(e));
        }

        return initializedPrefs;
    }

    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting TutorConnect " + TutorConnectApp.VERSION);
        ui.start(primaryStage);
    }

    @Override
    public void stop() {
        logger.info("============================ [ Stopping TutorConnect ] =============================");
        try {
            storage.saveUserPrefs(model.getUserPrefs());
        } catch (IOException e) {
            logger.severe("Failed to save preferences " + StringUtil.getDetails(e));
        }
    }
}
