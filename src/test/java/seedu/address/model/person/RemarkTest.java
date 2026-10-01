package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class RemarkTest {
    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Remark(null));
    }

    @Test
    public void constructor_anyNonNullText_preservesValue() {
        assertEquals("", new Remark("").value);
        assertEquals("  Likes 咖啡!\n", new Remark("  Likes 咖啡!\n").value);
    }

    @Test
    public void equals() {
        Remark remark = new Remark("Likes swimming");
        assertTrue(remark.equals(remark));
        assertTrue(remark.equals(new Remark("Likes swimming")));
        assertFalse(remark.equals(new Remark("")));
        assertFalse(remark.equals(null));
        assertFalse(remark.equals("Likes swimming"));
    }

    @Test
    public void hashCode_sameValue_sameHashCode() {
        assertEquals(new Remark("Likes swimming").hashCode(), new Remark("Likes swimming").hashCode());
    }

    @Test
    public void toStringMethod() {
        assertEquals("Likes swimming", new Remark("Likes swimming").toString());
    }
}
