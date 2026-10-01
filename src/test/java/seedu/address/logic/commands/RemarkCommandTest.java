package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandFailure;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
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
import seedu.address.model.person.Remark;
import seedu.address.testutil.PersonBuilder;

public class RemarkCommandTest {
    private final Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());

    @Test
    public void constructor_nullArguments_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new RemarkCommand(null, new Remark("")));
        assertThrows(NullPointerException.class, () -> new RemarkCommand(INDEX_FIRST_PERSON, null));
    }

    @Test
    public void execute_nullModel_throwsNullPointerException() {
        RemarkCommand command = new RemarkCommand(INDEX_FIRST_PERSON, new Remark(""));
        assertThrows(NullPointerException.class, () -> command.execute(null));
    }

    @Test
    public void execute_addRemark_success() {
        assertRemarkSuccess("Likes swimming");
    }

    @Test
    public void execute_replaceRemark_success() {
        setExistingRemark();
        assertRemarkSuccess("Likes reading");
    }

    @Test
    public void execute_removeRemark_success() {
        setExistingRemark();
        assertRemarkSuccess("");
    }

    @Test
    public void execute_removeAbsentRemark_success() {
        assertRemarkSuccess("");
    }

    @Test
    public void execute_filteredList_updatesDisplayedPersonAndShowsAll() {
        showPersonAtIndex(model, INDEX_SECOND_PERSON);
        assertRemarkSuccess("This is the second person in the address book");
    }

    @Test
    public void execute_invalidIndexUnfilteredList_throwsCommandException() {
        Index invalidIndex = Index.fromOneBased(model.getFilteredPersonList().size() + 1);
        assertCommandFailure(new RemarkCommand(invalidIndex, new Remark("note")), model,
                Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void execute_invalidIndexFilteredList_throwsCommandException() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);
        assertCommandFailure(new RemarkCommand(INDEX_SECOND_PERSON, new Remark("note")), model,
                Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void equals() {
        RemarkCommand command = new RemarkCommand(INDEX_FIRST_PERSON, new Remark("note"));
        assertTrue(command.equals(command));
        assertTrue(command.equals(new RemarkCommand(INDEX_FIRST_PERSON, new Remark("note"))));
        assertFalse(command.equals(new RemarkCommand(INDEX_SECOND_PERSON, new Remark("note"))));
        assertFalse(command.equals(new RemarkCommand(INDEX_FIRST_PERSON, new Remark("different"))));
        assertFalse(command.equals(null));
        assertFalse(command.equals(new ListCommand()));
    }

    @Test
    public void toStringMethod() {
        RemarkCommand command = new RemarkCommand(INDEX_FIRST_PERSON, new Remark("note"));
        assertEquals(RemarkCommand.class.getCanonicalName() + "{index=" + INDEX_FIRST_PERSON
                + ", remark=note}", command.toString());
    }

    private void setExistingRemark() {
        Person person = model.getFilteredPersonList().getFirst();
        model.setPerson(person, new PersonBuilder(person).withRemark("Old remark").build());
    }

    private void assertRemarkSuccess(String remark) {
        Person person = model.getFilteredPersonList().getFirst();
        Person editedPerson = new PersonBuilder(person).withRemark(remark).build();
        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
        expectedModel.setPerson(person, editedPerson);
        String message = remark.isEmpty() ? RemarkCommand.MESSAGE_DELETE_REMARK_SUCCESS
                : RemarkCommand.MESSAGE_ADD_REMARK_SUCCESS;
        assertCommandSuccess(new RemarkCommand(INDEX_FIRST_PERSON, new Remark(remark)), model,
                String.format(message, Messages.format(editedPerson)), expectedModel);
    }
}
