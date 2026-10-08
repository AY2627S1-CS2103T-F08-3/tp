package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static seedu.address.testutil.TypicalIndexes.INDEX_SECOND_PERSON;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.TextNode;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.ModelManager;
import seedu.address.model.person.EducationLevel;
import seedu.address.model.person.Person;
import seedu.address.model.person.StudentFields;
import seedu.address.testutil.PersonBuilder;

public class LevelCommandTest {

    private static final EducationLevel SECONDARY_4 = new EducationLevel("Secondary 4");

    private static ModelManager modelWithOneStudent() {
        ModelManager model = new ModelManager();
        model.addPerson(new PersonBuilder().withName("Amy Bee").build());
        return model;
    }

    @Test
    public void execute_emptyLevel_setsLevelAndSelectsStudent() throws Exception {
        ModelManager model = modelWithOneStudent();

        CommandResult result = new LevelCommand(INDEX_FIRST_PERSON, SECONDARY_4).execute(model);

        assertEquals("Education level set for Amy Bee: Secondary 4.", result.getFeedbackToUser());
        assertEquals(Optional.of(1), result.getSelectedPersonIndex());
        Person student = model.getFilteredPersonList().getFirst();
        assertEquals(Optional.of(SECONDARY_4), student.getStudentFields().get(EducationLevel.FIELD));
        assertEquals(student.getId(), model.getSelectedPersonId());
    }

    @Test
    public void execute_differentLevel_replacesLevel() throws Exception {
        ModelManager model = modelWithOneStudent();
        new LevelCommand(INDEX_FIRST_PERSON, SECONDARY_4).execute(model);

        CommandResult result = new LevelCommand(INDEX_FIRST_PERSON, new EducationLevel("JC1")).execute(model);

        assertEquals("Education level updated for Amy Bee: Secondary 4 -> JC 1.", result.getFeedbackToUser());
        assertEquals("JC 1", model.getFilteredPersonList().getFirst().getStudentFields().display("educationLevel"));
    }

    @Test
    public void execute_sameLevel_isNoChangeAndLeavesTheModelAlone() throws Exception {
        ModelManager model = modelWithOneStudent();
        new LevelCommand(INDEX_FIRST_PERSON, SECONDARY_4).execute(model);
        Person before = model.getFilteredPersonList().getFirst();
        model.selectPerson(null);

        CommandResult result = new LevelCommand(INDEX_FIRST_PERSON, new EducationLevel("sec 4")).execute(model);

        assertEquals("Education level for Amy Bee is already Secondary 4.", result.getFeedbackToUser());
        assertTrue(result.getSelectedPersonIndex().isEmpty());
        assertEquals(null, model.getSelectedPersonId());
        assertEquals(before, model.getFilteredPersonList().getFirst());
    }

    @Test
    public void execute_storedLevelThatCannotBeRead_isTreatedAsUnset() throws Exception {
        ModelManager model = new ModelManager();
        model.addPerson(new PersonBuilder().withName("Amy Bee").build().withStudentFields(
                new StudentFields(java.util.Map.of("educationLevel", TextNode.valueOf("SECONDARY_1")))));

        CommandResult result = new LevelCommand(INDEX_FIRST_PERSON, SECONDARY_4).execute(model);

        assertEquals("Education level set for Amy Bee: Secondary 4.", result.getFeedbackToUser());
    }

    @Test
    public void execute_missingStudent_failsWithoutChangingTheModel() {
        ModelManager model = modelWithOneStudent();
        Person before = model.getFilteredPersonList().getFirst();

        LevelCommand command = new LevelCommand(INDEX_SECOND_PERSON, SECONDARY_4);
        CommandException failure = assertThrows(CommandException.class, () -> command.execute(model));

        assertEquals("Error: No student exists at index 2.", failure.getMessage());
        assertEquals(before, model.getFilteredPersonList().getFirst());
        assertTrue(model.getFilteredPersonList().getFirst().getStudentFields().get(EducationLevel.FIELD).isEmpty());
    }

    @Test
    public void equalsHashCodeAndToString() {
        LevelCommand command = new LevelCommand(INDEX_FIRST_PERSON, SECONDARY_4);
        assertEquals(command, new LevelCommand(INDEX_FIRST_PERSON, new EducationLevel("Sec4")));
        assertEquals(command.hashCode(), new LevelCommand(INDEX_FIRST_PERSON, new EducationLevel("S4")).hashCode());
        assertEquals(command, command);
        assertFalse(command.equals(null));
        assertFalse(command.equals(new ClearCommand()));
        assertFalse(command.equals(new LevelCommand(INDEX_SECOND_PERSON, SECONDARY_4)));
        assertFalse(command.equals(new LevelCommand(INDEX_FIRST_PERSON, new EducationLevel("Sec5"))));
        assertEquals(LevelCommand.class.getCanonicalName() + "{index=" + Index.fromOneBased(1)
                + ", level=Secondary 4}", command.toString());
    }
}
