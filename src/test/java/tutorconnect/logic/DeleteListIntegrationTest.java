package tutorconnect.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static tutorconnect.testutil.Assert.assertThrows;
import static tutorconnect.testutil.TypicalStudents.getTypicalStudentDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.node.TextNode;

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
import tutorconnect.model.student.StudentFields;
import tutorconnect.model.student.WeeklySlot;
import tutorconnect.model.student.WeeklySlotField;
import tutorconnect.storage.JsonStudentDirectoryStorage;
import tutorconnect.storage.JsonUserPrefsStorage;
import tutorconnect.storage.StorageManager;
import tutorconnect.testutil.StudentBuilder;

/** Exercises commands through parsing, persistence, and the published model. */
class DeleteListIntegrationTest {
    @TempDir
    Path directory;

    private Logic logic(Model model, JsonStudentDirectoryStorage storage) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(directory.resolve("prefs.json"))));
    }

    @Test
    void delete_firstMiddleLast_persistCompleteRemainingProfiles() throws Exception {
        for (int index : List.of(1, 4, 7)) {
            Model model = new ModelManager(getTypicalStudentDirectory(), new UserPrefs());
            List<Student> expected = new ArrayList<>(model.getFilteredStudentList());
            Student removed = expected.remove(index - 1);
            JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(directory.resolve("register.json"));
            var result = logic(model, storage).execute("delete " + index);
            assertEquals("Student deleted: " + removed.getName() + ".", result.getFeedbackToUser());
            assertEquals(SelectionAction.PRESERVE, result.getSelectionAction());
            assertEquals(expected, model.getFilteredStudentList());
            assertEquals(expected, storage.readStudentDirectory().orElseThrow().getStudentList());
            assertFalse(storage.readStudentDirectory().orElseThrow().getStudentList().contains(removed));
        }
    }

    @Test
    void delete_filteredList_targetsVisibleRowAndRetainsSurvivingObjects() throws Exception {
        Model model = new ModelManager(getTypicalStudentDirectory(), new UserPrefs());
        Student first = model.getFilteredStudentList().get(0);
        Student target = model.getFilteredStudentList().get(3);
        Student survivor = model.getFilteredStudentList().get(5);
        model.updateFilteredStudentList(p -> p == target || p == survivor);
        logic(model, new JsonStudentDirectoryStorage(directory.resolve("register.json"))).execute("delete 1");
        assertEquals(List.of(survivor), model.getFilteredStudentList());
        assertSame(survivor, model.getFilteredStudentList().get(0));
        assertSame(first, model.getStudentDirectory().getStudentList().get(0));
    }

    @Test
    void delete_saveFailure_preservesStorageModelAndView() throws Exception {
        Model model = new ModelManager(getTypicalStudentDirectory(), new UserPrefs());
        Path path = directory.resolve("register.json");
        new JsonStudentDirectoryStorage(path).saveStudentDirectory(model.getStudentDirectory());
        String original = Files.readString(path);
        StudentDirectory before = new StudentDirectory(model.getStudentDirectory());
        Student visible = model.getFilteredStudentList().get(3);
        model.updateFilteredStudentList(p -> p == visible);
        model.selectStudent(visible.getId());
        JsonStudentDirectoryStorage failing = new JsonStudentDirectoryStorage(path) {
            @Override
            public void saveStudentDirectory(ReadOnlyStudentDirectory proposed) throws IOException {
                assertEquals(before, model.getStudentDirectory());
                assertEquals(6, proposed.getStudentList().size());
                throw new IOException("save failed");
            }
        };
        assertThrows(CommandException.class, LogicManager.MESSAGE_SAVE_FAILURE, () ->
                logic(model, failing).execute("delete 1"));
        assertEquals(before, model.getStudentDirectory());
        assertEquals(List.of(visible), model.getFilteredStudentList());
        assertEquals(visible.getId(), model.getSelectedStudentId());
        assertEquals(original, Files.readString(path));
    }

    @Test
    void list_loadsStoredOrderClearsSelectionAndDoesNotSave() throws Exception {
        Model model = new ModelManager();
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

    @Test
    void delete_profileWithAttachedData_removedAfterReload() throws Exception {
        Model model = new ModelManager();
        Student target = new StudentBuilder().withName("Student Target")
                .withTags("math", "weekly").build();
        model.addStudent(target);
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(directory.resolve("register.json"));
        storage.saveStudentDirectory(model.getStudentDirectory());
        assertEquals(target, storage.readStudentDirectory().orElseThrow().getStudentList().get(0));
        logic(model, storage).execute("delete 1");
        assertEquals(List.of(), storage.readStudentDirectory().orElseThrow().getStudentList());
    }

    @Test
    void delete_studentEnvelope_removedFromProposedStateWithoutChangingSurvivor() throws Exception {
        Model model = new ModelManager();
        StudentFields fields = new StudentFields(Map.of("subject", TextNode.valueOf("Math"),
                "guardianPhone", TextNode.valueOf("+6591234567")));
        Student target = new StudentBuilder().withName("Target").build().withStudentFields(fields);
        Student survivor = new StudentBuilder().withName("Survivor").build().withStudentFields(fields);
        model.addStudent(target);
        model.addStudent(survivor);
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(directory.resolve("register.json")) {
            @Override
            public void saveStudentDirectory(ReadOnlyStudentDirectory proposed) {
                assertEquals(List.of(survivor), proposed.getStudentList());
                assertEquals(fields, proposed.getStudentList().get(0).getStudentFields());
            }
        };
        logic(model, storage).execute("delete 1");
        assertEquals(List.of(survivor), model.getFilteredStudentList());
    }


    @Test
    void delete_duplicateNames_preservesOtherProfileAndSelectionById() throws Exception {
        Model model = new ModelManager();
        StudentFields fields = new StudentFields(Map.of("subject", TextNode.valueOf("Math"),
                "guardianPhone", TextNode.valueOf("+6591234567"),
                "educationLevel", TextNode.valueOf("Primary 1"),
                "hourlyRate", TextNode.valueOf("40.00")))
                .with(WeeklySlotField.INSTANCE, new WeeklySlot(DayOfWeek.MONDAY, LocalTime.of(16, 0)));
        Student target = new StudentBuilder().withName("Same Name").withPhone("81234567")
                .build().withStudentFields(fields);
        Student survivor = new StudentBuilder().withName("Same Name").withPhone("91234567")
                .build().withStudentFields(fields);
        model.addStudent(target);
        model.addStudent(survivor);
        JsonStudentDirectoryStorage storage = new JsonStudentDirectoryStorage(directory.resolve("register.json"));
        Logic logic = logic(model, storage);
        logic.execute("delete 1");
        assertEquals(survivor.getId(), model.getSelectedStudentId());
        Student reloaded = storage.readStudentDirectory().orElseThrow().getStudentList().get(0);
        assertEquals(survivor.getId(), reloaded.getId());
        assertEquals(fields, reloaded.getStudentFields());
        logic.execute("delete 1");
        assertNull(model.getSelectedStudentId());
        assertEquals(List.of(), storage.readStudentDirectory().orElseThrow().getStudentList());
    }

}
