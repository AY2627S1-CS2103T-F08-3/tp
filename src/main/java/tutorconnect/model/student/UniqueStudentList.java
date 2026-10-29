package tutorconnect.model.student;

import static java.util.Objects.requireNonNull;
import static tutorconnect.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Iterator;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import tutorconnect.model.student.exceptions.DuplicateStudentException;
import tutorconnect.model.student.exceptions.StudentNotFoundException;

/**
 * A list of students that enforces uniqueness between its elements and does not allow nulls.
 * Adding and updating reject a student with the same UUID or matching contact identity, as defined by
 * {@code Student#isSameStudent(Student)}. Removal uses {@code Student#equals(Object)} so that only the student
 * with exactly the same fields will be removed.
 *
 * Supports a minimal set of list operations.
 *
 * @see Student#isSameStudent(Student)
 */
public class UniqueStudentList implements Iterable<Student> {

    private final ObservableList<Student> internalList = FXCollections.observableArrayList();
    private final ObservableList<Student> internalUnmodifiableList =
            FXCollections.unmodifiableObservableList(internalList);

    /**
     * Returns true if the list contains an equivalent student as the given argument.
     */
    public boolean contains(Student toCheck) {
        requireNonNull(toCheck);
        return internalList.stream().anyMatch(toCheck::isSameStudent);
    }

    /**
     * Adds a student to the list.
     * The student must not already exist in the list.
     */
    public void add(Student toAdd) {
        requireNonNull(toAdd);
        if (containsExcept(toAdd, -1)) {
            throw new DuplicateStudentException();
        }
        internalList.add(toAdd);
    }

    /**
     * Replaces the student {@code target} in the list with {@code editedStudent}.
     * {@code target} must exist in the list.
     * The student identity of {@code editedStudent} must not be the same as another existing student in the list.
     */
    public void setStudent(Student target, Student editedStudent) {
        requireAllNonNull(target, editedStudent);

        int index = internalList.indexOf(target);
        if (index == -1) {
            throw new StudentNotFoundException();
        }

        if (containsExcept(editedStudent, index)) {
            throw new DuplicateStudentException();
        }

        internalList.set(index, editedStudent);
    }

    /**
     * Removes the equivalent student from the list.
     * The student must exist in the list.
     */
    public void remove(Student toRemove) {
        requireNonNull(toRemove);
        if (!internalList.remove(toRemove)) {
            throw new StudentNotFoundException();
        }
    }

    public void setStudents(UniqueStudentList replacement) {
        requireNonNull(replacement);
        internalList.setAll(List.copyOf(replacement.internalList));
    }

    /**
     * Replaces the contents of this list with {@code students}.
     * {@code students} must not contain duplicate students.
     */
    public void setStudents(List<Student> students) {
        requireAllNonNull(students);
        // The supplied list may be this list's own unmodifiable view or a sublist of it.
        List<Student> snapshot = List.copyOf(students);
        if (!studentsAreUnique(snapshot)) {
            throw new DuplicateStudentException();
        }

        internalList.setAll(snapshot);
    }

    /**
     * Returns the backing list as an unmodifiable {@code ObservableList}.
     */
    public ObservableList<Student> asUnmodifiableObservableList() {
        return internalUnmodifiableList;
    }

    @Override
    public Iterator<Student> iterator() {
        return internalList.iterator();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof UniqueStudentList otherUniqueStudentList)) {
            return false;
        }

        return internalList.equals(otherUniqueStudentList.internalList);
    }

    @Override
    public int hashCode() {
        return internalList.hashCode();
    }

    @Override
    public String toString() {
        return internalList.toString();
    }

    /**
     * Returns true if {@code students} contains only unique students.
     */
    private boolean studentsAreUnique(List<Student> students) {
        for (int i = 0; i < students.size() - 1; i++) {
            for (int j = i + 1; j < students.size(); j++) {
                if (hasSameIdentity(students.get(i), students.get(j))) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Checks every row except the one being replaced, so ID-preserving edits remain valid. */
    private boolean containsExcept(Student toCheck, int excludedIndex) {
        for (int i = 0; i < internalList.size(); i++) {
            if (i != excludedIndex && hasSameIdentity(toCheck, internalList.get(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasSameIdentity(Student first, Student second) {
        return first.getId().equals(second.getId()) || first.isSameStudent(second);
    }
}
