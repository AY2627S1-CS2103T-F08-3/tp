package tutorconnect.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.model.ModelManager;
import tutorconnect.storage.JsonStudentDirectoryStorage;
import tutorconnect.storage.JsonUserPrefsStorage;
import tutorconnect.storage.StorageManager;

/** Messages that end with a student's name must not show a double full stop when the name ends with one. */
public class NameEndingFullStopTest {
    @TempDir
    private Path directory;

    @Test
    public void commands_nameEndingInFullStop_reportOneFullStop() throws Exception {
        LogicManager logic = new LogicManager(new ModelManager(), new StorageManager(
                new JsonStudentDirectoryStorage(directory.resolve("students.json")),
                new JsonUserPrefsStorage(directory.resolve("prefs.json"))));

        assertEquals("Student added: O'Brien-Smith Jr.",
                logic.execute("add n/O'Brien-Smith Jr. p/91234567 a/Blk 1 #01-02").getFeedbackToUser());
        assertEquals("Student added: Tan Sr. Warning: Another student uses this phone number.",
                logic.execute("add n/Tan Sr. p/91234567 a/2 Road").getFeedbackToUser());
        assertEquals("Note set for O'Brien-Smith Jr.", logic.execute("note 1 m/Revise").getFeedbackToUser());
        assertEquals("Note cleared for O'Brien-Smith Jr.", logic.execute("clearnote 1").getFeedbackToUser());
        assertEquals("No weekly lesson is scheduled for O'Brien-Smith Jr.",
                logic.execute("unschedule 1").getFeedbackToUser());
        assertEquals("Error: No assessment record 1 for O'Brien-Smith Jr.",
                assertThrows(CommandException.class, () -> logic.execute("unassess 1 i/1")).getMessage());
        assertEquals("Student updated: O'Brien-Smith Jr.", logic.execute("edit 1 a/3 Road").getFeedbackToUser());
        assertEquals("Student deleted: O'Brien-Smith Jr.", logic.execute("delete 1").getFeedbackToUser());
    }
}
