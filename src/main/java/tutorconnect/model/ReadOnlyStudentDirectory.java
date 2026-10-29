package tutorconnect.model;

import javafx.collections.ObservableList;
import tutorconnect.model.student.Student;

/**
 * Unmodifiable view of an student directory
 */
public interface ReadOnlyStudentDirectory {

    /**
     * Returns an unmodifiable view of the students list.
     * This list will not contain any duplicate students.
     */
    ObservableList<Student> getStudentList();

}
