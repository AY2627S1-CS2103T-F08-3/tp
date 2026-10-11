package tutorconnect.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static tutorconnect.testutil.Assert.assertThrows;
import static tutorconnect.testutil.TypicalStudents.getTypicalStudentDirectory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tutorconnect.logic.commands.CommandResult.SelectionAction;
import tutorconnect.logic.commands.ListCommand;
import tutorconnect.logic.commands.exceptions.CommandException;
import tutorconnect.logic.parser.exceptions.ParseException;
import tutorconnect.model.Model;
import tutorconnect.model.ModelManager;
import tutorconnect.model.ReadOnlyStudentDirectory;
import tutorconnect.model.StudentDirectory;
import tutorconnect.model.UserPrefs;
import tutorconnect.model.student.Student;
import tutorconnect.storage.JsonStudentDirectoryStorage;
import tutorconnect.storage.JsonUserPrefsStorage;
import tutorconnect.storage.StorageManager;

class ListStudentIntegrationTest {
    @TempDir
    Path directory;

    private Logic logic(Model model, JsonStudentDirectoryStorage storage) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
    }

    @Test
    void list_loadsStoredOrderClearsSelectionAndDoesNotSave() throws Exception {
        Model model = new ModelManager(getTypicalStudentDirectory(), new UserPrefs());
        model.selectStudent(model.getFilteredStudentList().get(0).getId());
        Path path = directory.resolve("register.json");
        new JsonStudentDirectoryStorage(path).saveStudentDirectory(getTypicalStudentDirectory());
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(path) {
            @Override
            public void saveStudentDirectory(ReadOnlyStudentDirectory proposed) {
                throw new AssertionError("list must never save");
            }
        };
        var result = logic(model, storage).execute("list");
        assertEquals("Listed 7 students.", result.getFeedbackToUser());
        assertEquals(SelectionAction.CLEAR, result.getSelectionAction());
        assertNull(model.getSelectedStudentId());
        assertEquals(getTypicalStudentDirectory().getStudentList(), model.getFilteredStudentList());
    }

    @Test
    void list_emptyRegister_returnsEmptyMessage() throws Exception {
        Model model = new ModelManager(getTypicalStudentDirectory(), new UserPrefs());
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(directory.resolve("register.json"));
        storage.saveStudentDirectory(new StudentDirectory());
        assertEquals(ListCommand.MESSAGE_EMPTY, logic(model, storage).execute("list").getFeedbackToUser());
        assertEquals(List.of(), model.getFilteredStudentList());
    }

    @Test
    void list_corruptStorageAndInvalidInput_preserveModelAndFilteredView() throws Exception {
        Model model = new ModelManager(getTypicalStudentDirectory(), new UserPrefs());
        StudentDirectory before = new StudentDirectory(model.getStudentDirectory());
        Student visible = model.getFilteredStudentList().get(2);
        model.updateFilteredStudentList(p -> p == visible);
        model.selectStudent(visible.getId());
        Path path = directory.resolve("register.json");
        Files.writeString(path, "invalid json");
        Logic logic = logic(model, new JsonStudentDirectoryStorage(path));
        assertThrows(ParseException.class, "Error: List does not accept parameters.", () -> logic.execute("list 1"));
        assertThrows(CommandException.class, ListCommand.MESSAGE_LOAD_FAILURE, () -> logic.execute("list"));
        assertEquals(before, model.getStudentDirectory());
        assertEquals(List.of(visible), model.getFilteredStudentList());
        assertEquals(visible.getId(), model.getSelectedStudentId());
        assertEquals("invalid json", Files.readString(path));
    }

}
