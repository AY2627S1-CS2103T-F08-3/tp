package tutorconnect.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tutorconnect.testutil.TypicalStudents.ALICE;
import static tutorconnect.testutil.TypicalStudents.BENSON;

import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import javafx.collections.FXCollections;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.exceptions.DuplicateStudentException;

public class ModelSelectionResetTest {
    private final ModelManager model = new ModelManager();

    @Test
    public void reset_emptyDirectory_clearsSelectionAndAllowsNextSnapshot() {
        model.addStudent(ALICE);
        model.setStudentDirectory(new StudentDirectory());
        assertNull(model.getSelectedStudentId());
        assertEquals(List.of(), model.getFilteredStudentList());
        assertNull(model.copyForCommand().getSelectedStudentId());
    }

    @Test
    public void reset_unrelatedDirectory_doesNotSelectReplacementStudent() {
        model.addStudent(ALICE);
        model.setStudentDirectory(directoryOf(BENSON));
        assertNull(model.getSelectedStudentId());
        assertEquals(List.of(BENSON), model.getFilteredStudentList());
        assertNull(model.copyForCommand().getSelectedStudentId());
    }

    @Test
    public void reset_sameVisibleUuid_retainsSelectionAndUpdatedDetails() {
        model.addStudent(ALICE);
        Student updated = ALICE.withDetails(ALICE.getName(), ALICE.getPhone(), ALICE.getEmail(),
                BENSON.getAddress(), ALICE.getTags());
        model.setStudentDirectory(directoryOf(updated));
        assertEquals(ALICE.getId(), model.getSelectedStudentId());
        assertEquals(List.of(updated), model.getFilteredStudentList());
        assertEquals(ALICE.getId(), model.copyForCommand().getSelectedStudentId());
    }

    @Test
    public void reset_selectedUuidHiddenByExistingFilter_clearsSelection() {
        model.addStudent(ALICE);
        Predicate<Student> predicate = student -> student.getName().equals(ALICE.getName());
        model.updateFilteredStudentList(predicate);
        model.selectStudent(ALICE.getId());
        Student hidden = new Student(ALICE.getId(), BENSON.getName(), BENSON.getPhone(), BENSON.getEmail(),
                BENSON.getAddress(), BENSON.getTags(), BENSON.getStudentFields());
        model.setStudentDirectory(directoryOf(hidden));
        assertNull(model.getSelectedStudentId());
        assertEquals(predicate, model.getStudentPredicate());
        assertEquals(List.of(), model.getFilteredStudentList());
        assertNull(model.copyForCommand().getSelectedStudentId());
    }

    @Test
    public void reset_invalidReplacement_preservesSelectionAndDirectory() {
        model.addStudent(ALICE);
        ReadOnlyStudentDirectory invalid = () -> FXCollections.observableArrayList(BENSON, BENSON);
        assertThrows(DuplicateStudentException.class, () -> model.setStudentDirectory(invalid));
        assertEquals(ALICE.getId(), model.getSelectedStudentId());
        assertEquals(List.of(ALICE), model.getFilteredStudentList());
        assertEquals(ALICE.getId(), model.copyForCommand().getSelectedStudentId());
    }

    @Test
    public void reset_nullReplacement_preservesSelectionAndDirectory() {
        model.addStudent(ALICE);
        assertThrows(NullPointerException.class, () -> model.setStudentDirectory(null));
        assertEquals(ALICE.getId(), model.getSelectedStudentId());
        assertEquals(List.of(ALICE), model.getFilteredStudentList());
    }

    @Test
    public void reset_noSelection_keepsSelectionUnset() {
        model.setStudentDirectory(directoryOf(ALICE));
        assertNull(model.getSelectedStudentId());
        assertEquals(List.of(ALICE), model.getFilteredStudentList());
    }

    private static StudentDirectory directoryOf(Student student) {
        StudentDirectory directory = new StudentDirectory();
        directory.addStudent(student);
        return directory;
    }
}
