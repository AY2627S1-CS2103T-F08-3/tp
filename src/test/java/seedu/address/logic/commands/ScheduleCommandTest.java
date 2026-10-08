package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BOB;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.VisibleIndex;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.person.Person;
import seedu.address.model.person.StudentFields;
import seedu.address.model.person.WeeklySlot;
import seedu.address.model.person.WeeklySlotField;

public class ScheduleCommandTest {
    private final Model model = new ModelManager();
    private final WeeklySlot tuesday = new WeeklySlot(DayOfWeek.TUESDAY, LocalTime.of(19, 0));

    @Test
    public void execute_setThenReplace_exactMessagesAndCompletePair() throws Exception {
        model.addPerson(ALICE);
        String name = ALICE.getName().toString();
        assertEquals("Weekly lesson set for " + name + ": Tuesday 19:00.",
                new ScheduleCommand(VisibleIndex.parse("1"), tuesday).execute(model).getFeedbackToUser());
        WeeklySlot saturday = new WeeklySlot(DayOfWeek.SATURDAY, LocalTime.of(9, 30));
        assertEquals("Weekly lesson updated for " + name + ": Tuesday 19:00 -> Saturday 09:30.",
                new ScheduleCommand(VisibleIndex.parse("1"), saturday).execute(model).getFeedbackToUser());
        assertEquals(saturday, WeeklySlotField.get(model.getFilteredPersonList().get(0)).orElseThrow());
        assertEquals(ALICE.getId(), model.getSelectedPersonId());
    }

    @Test
    public void execute_noChange_preservesStudentObjectFilterAndSelection() throws Exception {
        model.addPerson(ALICE);
        model.addPerson(BOB);
        new ScheduleCommand(VisibleIndex.parse("1"), tuesday).execute(model);
        model.selectPerson(BOB.getId());
        Person original = model.getFilteredPersonList().get(0);
        var predicate = model.getPersonPredicate();
        assertEquals("Weekly lesson for " + ALICE.getName() + " is already Tuesday 19:00.",
                new ScheduleCommand(VisibleIndex.parse("1"), tuesday).execute(model).getFeedbackToUser());
        assertSame(original, model.getFilteredPersonList().get(0));
        assertSame(predicate, model.getPersonPredicate());
        assertEquals(BOB.getId(), model.getSelectedPersonId());
    }

    @Test
    public void execute_filteredIndex_preservesRegisterPositionAndUnrelatedFields() throws Exception {
        var node = JsonNodeFactory.instance;
        Person target = BOB.withStudentFields(new StudentFields(Map.of("subject", node.textNode("Math/Science"),
                "guardianPhone", node.textNode("+6581234567"), "hourlyRate", node.textNode("65.00"),
                "educationLevel", node.textNode("Secondary 2"))));
        model.addPerson(ALICE);
        model.addPerson(target);
        model.updateFilteredPersonList(person -> person.getId().equals(target.getId()));
        var predicate = model.getPersonPredicate();
        new ScheduleCommand(VisibleIndex.parse("1"), tuesday).execute(model);
        Person updated = model.getFilteredPersonList().get(0);
        assertEquals(target.getId(), updated.getId());
        assertEquals(target.getName(), updated.getName());
        assertEquals(target.getPhone(), updated.getPhone());
        assertEquals(target.getAddress(), updated.getAddress());
        assertEquals(target.getEmail(), updated.getEmail());
        assertEquals(target.getTags(), updated.getTags());
        target.getStudentFields().toStorage().forEach((key, value) ->
                assertEquals(value, updated.getStudentFields().toStorage().get(key)));
        assertEquals(List.of(ALICE.getId(), BOB.getId()),
                model.getAddressBook().getPersonList().stream().map(Person::getId).toList());
        assertSame(predicate, model.getPersonPredicate());
        assertEquals(target.getId(), model.getSelectedPersonId());
    }

    @Test
    public void execute_identicalSlotsForDifferentStudents_acceptsOverlap() throws Exception {
        model.addPerson(ALICE);
        model.addPerson(BOB);
        new ScheduleCommand(VisibleIndex.parse("1"), tuesday).execute(model);
        new ScheduleCommand(VisibleIndex.parse("2"), tuesday).execute(model);
        for (Person person : model.getAddressBook().getPersonList()) {
            assertEquals(tuesday, WeeklySlotField.get(person).orElseThrow());
        }
    }

    @Test
    public void execute_nonexistentAndOversizedIndex_preservesState() throws Exception {
        model.addPerson(ALICE);
        for (String index : List.of("2", "99999999999999999999999999999999999999")) {
            VisibleIndex parsed = VisibleIndex.parse(index);
            assertThrows(CommandException.class, "Error: No student exists at index " + index + ".", ()
                -> new ScheduleCommand(parsed, tuesday).execute(model));
            assertSame(ALICE, model.getFilteredPersonList().get(0));
            assertEquals(ALICE.getId(), model.getSelectedPersonId());
        }
    }
}
