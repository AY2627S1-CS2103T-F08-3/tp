package tutorconnect.model;

import static java.util.Objects.requireNonNull;
import static tutorconnect.commons.util.CollectionUtil.requireAllNonNull;

import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Logger;

import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import tutorconnect.commons.core.GuiSettings;
import tutorconnect.commons.core.LogsCenter;
import tutorconnect.model.student.Student;

/**
 * Represents the in-memory model of the address book data.
 */
public class ModelManager implements Model {
    private static final Logger logger = LogsCenter.getLogger(ModelManager.class);

    private final StudentDirectory studentDirectory;
    private final UserPrefs userPrefs;
    private final FilteredList<Student> filteredStudents;
    private UUID selectedStudentId;
    private Predicate<Student> studentPredicate = PREDICATE_SHOW_ALL_STUDENTS;

    /**
     * Initializes a ModelManager with the given studentDirectory and userPrefs.
     */
    public ModelManager(ReadOnlyStudentDirectory studentDirectory, ReadOnlyUserPrefs userPrefs) {
        requireAllNonNull(studentDirectory, userPrefs);

        logger.fine("Initializing with address book: " + studentDirectory + " and user prefs " + userPrefs);

        this.studentDirectory = new StudentDirectory(studentDirectory);
        this.userPrefs = new UserPrefs(userPrefs);
        filteredStudents = new FilteredList<>(this.studentDirectory.getStudentList(), PREDICATE_SHOW_ALL_STUDENTS);
    }

    public ModelManager() {
        this(new StudentDirectory(), new UserPrefs());
    }

    //=========== UserPrefs ==================================================================================

    @Override
    public ReadOnlyUserPrefs getUserPrefs() {
        return userPrefs;
    }

    @Override
    public GuiSettings getGuiSettings() {
        return userPrefs.getGuiSettings();
    }

    @Override
    public void setGuiSettings(GuiSettings guiSettings) {
        requireNonNull(guiSettings);
        userPrefs.setGuiSettings(guiSettings);
    }

    //=========== StudentDirectory ================================================================================

    @Override
    public void setStudentDirectory(ReadOnlyStudentDirectory studentDirectory) {
        this.studentDirectory.resetData(studentDirectory);
        if (selectedStudentId != null
                && filteredStudents.stream().noneMatch(student -> student.getId().equals(selectedStudentId))) {
            selectedStudentId = null;
        }
    }

    @Override
    public ReadOnlyStudentDirectory getStudentDirectory() {
        return studentDirectory;
    }

    @Override
    public boolean hasStudent(Student student) {
        requireNonNull(student);
        return studentDirectory.hasStudent(student);
    }

    @Override
    public void deleteStudent(Student target) {
        studentDirectory.removeStudent(target);
        if (target.getId().equals(selectedStudentId)) {
            selectedStudentId = null;
        }
    }

    @Override
    public void addStudent(Student student) {
        studentDirectory.addStudent(student);
        updateFilteredStudentList(PREDICATE_SHOW_ALL_STUDENTS);
        selectedStudentId = student.getId();
    }

    @Override
    public void setStudent(Student target, Student editedStudent) {
        requireAllNonNull(target, editedStudent);

        studentDirectory.setStudent(target, editedStudent);
        selectedStudentId = editedStudent.getId();
    }

    //=========== Filtered Student List Accessors =============================================================

    /**
     * Returns an unmodifiable view of the list of {@code Student} backed by the internal list of
     * {@code studentDirectory}
     */
    @Override
    public ObservableList<Student> getFilteredStudentList() {
        return filteredStudents;
    }

    @Override
    public void updateFilteredStudentList(Predicate<Student> predicate) {
        requireNonNull(predicate);
        filteredStudents.setPredicate(predicate);
        studentPredicate = predicate;
        selectedStudentId = null;
    }

    @Override
    public Predicate<Student> getStudentPredicate() {
        return studentPredicate;
    }

    @Override
    public UUID getSelectedStudentId() {
        return selectedStudentId;
    }

    @Override
    public void selectStudent(UUID id) {
        if (id != null && filteredStudents.stream().noneMatch(student -> student.getId().equals(id))) {
            throw new IllegalArgumentException("Selection must refer to a displayed student");
        }
        selectedStudentId = id;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof ModelManager otherModelManager)) {
            return false;
        }

        return studentDirectory.equals(otherModelManager.studentDirectory)
                && userPrefs.equals(otherModelManager.userPrefs)
                && filteredStudents.equals(otherModelManager.filteredStudents);
    }

}
