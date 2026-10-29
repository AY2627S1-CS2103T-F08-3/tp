package tutorconnect.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tutorconnect.testutil.TypicalStudents.ALICE;
import static tutorconnect.testutil.TypicalStudents.BENSON;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tutorconnect.model.StudentDirectory;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.exceptions.DuplicateStudentException;

public class StudentIdUniquenessPersistenceTest {
    @TempDir
    private Path directory;

    @Test
    public void rejectedDuplicate_leavesPersistedDirectoryReloadable() throws Exception {
        StudentDirectory students = new StudentDirectory();
        students.addStudent(ALICE);
        Path file = directory.resolve("students.json");
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(file);
        storage.saveStudentDirectory(students);
        String before = Files.readString(file);
        Student duplicate = new Student(ALICE.getId(), BENSON.getName(), BENSON.getPhone(), BENSON.getEmail(),
                BENSON.getAddress(), BENSON.getTags(), BENSON.getStudentFields());
        assertThrows(DuplicateStudentException.class, () -> students.addStudent(duplicate));
        assertEquals(before, Files.readString(file));
        storage.saveStudentDirectory(students);
        var loaded = storage.readStudentDirectory().orElseThrow().getStudentList();
        assertEquals(List.of(ALICE), loaded);
        assertEquals(ALICE.getId(), loaded.getFirst().getId());
    }

    @Test
    public void identityPreservingEdit_roundTripsWithDistinctIds() throws Exception {
        StudentDirectory students = new StudentDirectory();
        students.setStudents(List.of(ALICE, BENSON));
        Student edited = ALICE.withDetails(ALICE.getName(), ALICE.getPhone(), ALICE.getEmail(),
                BENSON.getAddress(), ALICE.getTags());
        students.setStudent(ALICE, edited);
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(directory.resolve("edited.json"));
        storage.saveStudentDirectory(students);
        var loaded = storage.readStudentDirectory().orElseThrow().getStudentList();
        assertEquals(List.of(edited, BENSON), loaded);
        assertEquals(ALICE.getId(), loaded.getFirst().getId());
        assertEquals(BENSON.getId(), loaded.get(1).getId());
    }
}
