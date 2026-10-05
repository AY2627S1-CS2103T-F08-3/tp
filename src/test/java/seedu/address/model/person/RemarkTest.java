package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

import seedu.address.testutil.PersonBuilder;

public class RemarkTest {
    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Remark(null));
    }

    @Test
    public void valueObject_acceptsEmptyAndArbitraryText() {
        for (String value : new String[] {"", " ", "Likes swimming! / notes"}) {
            Remark remark = new Remark(value);
            assertEquals(value, remark.toString());
            assertEquals(remark, new Remark(value));
            assertEquals(remark.hashCode(), new Remark(value).hashCode());
            assertNotEquals(remark, null);
            assertNotEquals(remark, value);
        }
        assertNotEquals(new Remark("x"), new Remark("y"));
    }

    @Test
    public void person_remarkAffectsEqualityButNotIdentity() {
        Person original = new PersonBuilder().build();
        Person changed = new PersonBuilder(original).withRemark("Note").build();
        assertFalse(original.equals(changed));
        assertTrue(original.isSamePerson(changed));
        Person copy = new PersonBuilder(changed).build();
        assertEquals(changed, copy);
        assertEquals(changed.hashCode(), copy.hashCode());
    }
}
