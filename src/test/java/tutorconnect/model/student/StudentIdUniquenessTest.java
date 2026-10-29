package tutorconnect.model.student;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tutorconnect.testutil.TypicalStudents.ALICE;
import static tutorconnect.testutil.TypicalStudents.BENSON;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import tutorconnect.model.student.exceptions.DuplicateStudentException;

public class StudentIdUniquenessTest {
    private final UniqueStudentList students = new UniqueStudentList();

    @Test
    public void add_duplicateUuidWithDifferentContactDetails_rejectsWithoutMutation() {
        students.add(ALICE);
        Student duplicate = withId(BENSON, ALICE.getId());
        // Contact lookup must not mistake an ID-preserving edit for an existing contact duplicate.
        assertFalse(students.contains(duplicate));
        assertThrows(DuplicateStudentException.class, () -> students.add(duplicate));
        assertEquals(List.of(ALICE), students.asUnmodifiableObservableList());
    }

    @Test
    public void setStudent_duplicateUuid_rejectsWithoutMutation() {
        students.setStudents(List.of(ALICE, BENSON));
        Student duplicate = withId(ALICE, BENSON.getId());
        assertThrows(DuplicateStudentException.class, () -> students.setStudent(ALICE, duplicate));
        assertEquals(List.of(ALICE, BENSON), students.asUnmodifiableObservableList());
    }

    @Test
    public void setStudents_duplicateUuid_rejectsWithoutMutation() {
        students.add(BENSON);
        assertThrows(DuplicateStudentException.class, () ->
                students.setStudents(List.of(ALICE, withId(BENSON, ALICE.getId()))));
        assertEquals(List.of(BENSON), students.asUnmodifiableObservableList());
    }

    @Test
    public void setStudent_retainsOwnUuid_allowsChangedContactDetails() {
        students.add(ALICE);
        Student edited = withId(BENSON, ALICE.getId());
        students.setStudent(ALICE, edited);
        assertEquals(List.of(edited), students.asUnmodifiableObservableList());
        assertEquals(ALICE.getId(), students.asUnmodifiableObservableList().getFirst().getId());
        students.setStudent(edited, edited);
        assertEquals(1, students.asUnmodifiableObservableList().size());
    }

    @Test
    public void add_duplicateContactWithDifferentUuid_stillRejects() {
        students.add(ALICE);
        Student duplicate = withId(ALICE, UUID.randomUUID());
        assertThrows(DuplicateStudentException.class, () -> students.add(duplicate));
        assertEquals(List.of(ALICE), students.asUnmodifiableObservableList());
    }

    private static Student withId(Student source, UUID id) {
        return new Student(id, source.getName(), source.getPhone(), source.getEmail(), source.getAddress(),
                source.getTags(), source.getStudentFields());
    }
}
