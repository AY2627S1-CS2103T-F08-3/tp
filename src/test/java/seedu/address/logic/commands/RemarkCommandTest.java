package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandFailure;
import static seedu.address.logic.commands.CommandTestUtil.showPersonAtIndex;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static seedu.address.testutil.TypicalIndexes.INDEX_SECOND_PERSON;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import org.junit.jupiter.api.Test;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.Messages;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Remark;
import seedu.address.testutil.PersonBuilder;

public class RemarkCommandTest {
    private final Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());

    @Test
    public void execute_addReplaceClear_success() throws Exception {
        Person original = model.getFilteredPersonList().get(0);
        for (String value : new String[] {"Likes swimming", "Prefers evenings", ""}) {
            RemarkCommand command = new RemarkCommand(INDEX_FIRST_PERSON, new Remark(value));
            Person expected = new PersonBuilder(original).withRemark(value).build();
            String message = value.isEmpty() ? RemarkCommand.MESSAGE_DELETE_REMARK_SUCCESS
                    : RemarkCommand.MESSAGE_ADD_REMARK_SUCCESS;
            assertEquals(new CommandResult(String.format(message, Messages.format(expected))), command.execute(model));
            assertEquals(expected, model.getFilteredPersonList().get(0));
        }
    }

    @Test
    public void execute_filteredList_editsDisplayedPerson() throws Exception {
        Person target = model.getFilteredPersonList().get(1);
        Person untouched = model.getFilteredPersonList().get(0);
        showPersonAtIndex(model, INDEX_SECOND_PERSON);
        new RemarkCommand(INDEX_FIRST_PERSON, new Remark("Filtered")).execute(model);
        assertEquals(untouched, model.getAddressBook().getPersonList().get(0));
        assertEquals(new PersonBuilder(target).withRemark("Filtered").build(),
                model.getAddressBook().getPersonList().get(1));
        assertEquals(model.getAddressBook().getPersonList(), model.getFilteredPersonList());
    }

    @Test
    public void execute_invalidIndex_failureWithoutMutation() {
        Index outside = Index.fromZeroBased(model.getFilteredPersonList().size());
        assertCommandFailure(new RemarkCommand(outside, new Remark("x")), model,
                Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
        showPersonAtIndex(model, INDEX_FIRST_PERSON);
        assertCommandFailure(new RemarkCommand(INDEX_SECOND_PERSON, new Remark("x")), model,
                Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void execute_editOtherFields_preservesRemark() throws Exception {
        new RemarkCommand(INDEX_FIRST_PERSON, new Remark("Keep me")).execute(model);
        EditCommand.EditPersonDescriptor descriptor = new EditCommand.EditPersonDescriptor();
        descriptor.setPhone(new Phone("91234567"));
        new EditCommand(INDEX_FIRST_PERSON, descriptor).execute(model);
        assertEquals(new Remark("Keep me"), model.getFilteredPersonList().get(0).getRemark());
    }

    @Test
    public void constructor_nullArguments_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new RemarkCommand(null, new Remark("")));
        assertThrows(NullPointerException.class, () -> new RemarkCommand(INDEX_FIRST_PERSON, null));
    }

    @Test
    public void equals_comparesIndexAndRemark() {
        RemarkCommand command = new RemarkCommand(INDEX_FIRST_PERSON, new Remark("x"));
        assertEquals(command, command);
        assertEquals(command, new RemarkCommand(INDEX_FIRST_PERSON, new Remark("x")));
        assertNotEquals(command, new RemarkCommand(INDEX_SECOND_PERSON, new Remark("x")));
        assertNotEquals(command, new RemarkCommand(INDEX_FIRST_PERSON, new Remark("y")));
        assertNotEquals(command, null);
        assertNotEquals(command, "x");
    }
}
