package tutorconnect.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static tutorconnect.testutil.TypicalStudents.ALICE;

import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tutorconnect.logic.LogicManager;
import tutorconnect.model.ModelManager;
import tutorconnect.model.StudentDirectory;
import tutorconnect.model.student.Student;

public class ReloadIdentityIntegrityTest {
    @TempDir
    private Path folder;

    @Test
    public void list_identityOnlyFileChange_refreshesModelAndNextScheduledSave() throws Exception {
        ModelManager model = new ModelManager();
        model.addStudent(ALICE);
        Student replacement = new Student(UUID.randomUUID(), ALICE.getName(), ALICE.getPhone(), ALICE.getEmail(),
                ALICE.getAddress(), ALICE.getTags(), ALICE.getStudentFields());
        StudentDirectory onDisk = new StudentDirectory();
        onDisk.addStudent(replacement);
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(folder.resolve("students.json"));
        storage.saveStudentDirectory(onDisk);
        LogicManager logic = new LogicManager(model,
                new StorageManager(storage, new JsonUserPrefsStorage(folder.resolve("prefs.json"))));
        logic.execute("list");
        assertEquals(replacement.getId(), model.getStudentDirectory().getStudentList().getFirst().getId());
        assertNull(model.getSelectedStudentId());
        logic.execute("schedule 1 d/Mon t/09:00");
        assertEquals(replacement.getId(), model.getSelectedStudentId());
        assertEquals(replacement.getId(), storage.readStudentDirectory().orElseThrow()
                .getStudentList().getFirst().getId());
    }
}
