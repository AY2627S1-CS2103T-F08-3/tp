package tutorconnect.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tutorconnect.testutil.TypicalStudents.ALICE;
import static tutorconnect.testutil.TypicalStudents.BENSON;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import tutorconnect.model.student.Student;
import tutorconnect.model.student.UniqueStudentList;
import tutorconnect.model.student.exceptions.DuplicateStudentException;

public class ModelSnapshotIntegrityTest {
    @Test
    public void reset_ownDirectory_preservesRecordsAndIds() {
        StudentDirectory directory = directoryOf(ALICE, BENSON);
        directory.resetData(directory);
        assertEquals(List.of(ALICE, BENSON), directory.getStudentList());
        assertEquals(List.of(ALICE.getId(), BENSON.getId()),
                directory.getStudentList().stream().map(Student::getId).toList());
    }

    @Test
    public void setStudents_ownViewOrSublist_snapshotsBeforeReplacement() {
        StudentDirectory directory = directoryOf(ALICE, BENSON);
        directory.setStudents(directory.getStudentList());
        assertEquals(List.of(ALICE, BENSON), directory.getStudentList());
        directory.setStudents(directory.getStudentList().subList(1, 2));
        assertEquals(List.of(BENSON), directory.getStudentList());
    }

    @Test
    public void setStudents_ownUniqueList_preservesRecords() {
        UniqueStudentList students = new UniqueStudentList();
        students.setStudents(List.of(ALICE, BENSON));
        students.setStudents(students);
        assertEquals(List.of(ALICE, BENSON), students.asUnmodifiableObservableList());
    }

    @Test
    public void setStudent_editMovesRowOutsideFilter_clearsSelectionAndAllowsNextSnapshot() {
        ModelManager model = new ModelManager(directoryOf(ALICE), new UserPrefs());
        model.updateFilteredStudentList(student -> student.getName().equals(ALICE.getName()));
        model.selectStudent(ALICE.getId());
        Student edited = ALICE.withDetails(BENSON.getName(), ALICE.getPhone(), ALICE.getEmail(),
                ALICE.getAddress(), ALICE.getTags());
        model.setStudent(ALICE, edited);
        assertNull(model.getSelectedStudentId());
        assertEquals(List.of(), model.getFilteredStudentList());
        assertEquals(List.of(edited), model.getStudentDirectory().getStudentList());
        assertNull(model.copyForCommand().getSelectedStudentId());
    }

    @Test
    public void setStudent_rejectedEdit_preservesSelectionAndDirectory() {
        ModelManager model = new ModelManager(directoryOf(ALICE, BENSON), new UserPrefs());
        model.selectStudent(ALICE.getId());
        assertThrows(DuplicateStudentException.class, () -> model.setStudent(ALICE, BENSON));
        assertEquals(ALICE.getId(), model.getSelectedStudentId());
        assertEquals(List.of(ALICE, BENSON), model.getFilteredStudentList());
    }

    @Test
    public void publish_equalContactDataWithNewId_adoptsPublishedIdentityAndSelection() {
        ModelManager model = new ModelManager(directoryOf(ALICE), new UserPrefs());
        Model proposed = model.copyForCommand();
        Student replacement = new Student(UUID.randomUUID(), ALICE.getName(), ALICE.getPhone(), ALICE.getEmail(),
                ALICE.getAddress(), ALICE.getTags(), ALICE.getStudentFields());
        proposed.setStudent(ALICE, replacement);
        model.publish(proposed);
        assertEquals(replacement.getId(), model.getStudentDirectory().getStudentList().getFirst().getId());
        assertEquals(replacement.getId(), model.getSelectedStudentId());
        assertEquals(replacement.getId(), model.copyForCommand().getSelectedStudentId());
    }

    @Test
    public void publish_unchangedSnapshot_retainsExistingRowsAndSelection() {
        ModelManager model = new ModelManager(directoryOf(ALICE, BENSON), new UserPrefs());
        model.selectStudent(BENSON.getId());
        model.publish(model.copyForCommand());
        assertEquals(List.of(ALICE, BENSON), model.getFilteredStudentList());
        assertEquals(BENSON.getId(), model.getSelectedStudentId());
    }

    private static StudentDirectory directoryOf(Student... students) {
        StudentDirectory directory = new StudentDirectory();
        directory.setStudents(List.of(students));
        return directory;
    }
}
