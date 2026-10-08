package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class StudentValidationTest {
    @Test
    public void names_unicodePunctuationAndLengthBoundaries() {
        for (String valid : new String[]{"A", "李小明", "O'Neil", "Jean-Luc", "Dr. Tan", "D’Arcy",
            "A".repeat(70), "𐐀".repeat(70)}) {
            assertTrue(Name.isValidName(valid), valid);
        }
        for (String invalid : new String[]{"", "   ", "-'..", "123", "A1", "A/B", "A@B", "A".repeat(71),
            "𐐀".repeat(71), "\u0301", "A\n", "A\t", "A\u0000", "A\u2029"}) {
            assertFalse(Name.isValidName(invalid), invalid);
        }
        assertEquals("Éva Tan", new Name("  Ｅ́va\u00a0　Tan  ").fullName);
    }

    @Test
    public void phones_normalizeOrReject() {
        for (String valid : new String[]{"81234567", "+6581234567", "+65 8123 4567", "+65-8123-4567",
            "  ８１２３４５６７  ", "8123   4567"}) {
            assertEquals("+6581234567", new Phone(valid).value);
        }
        assertTrue(Phone.isValidPhone("61234567"));
        assertTrue(Phone.isValidPhone("91234567"));
        for (String invalid : new String[]{"", "71234567", "12345678", "8123456", "812345678", "+1 81234567",
            "6581234567", "+65--8123-4567", "81234567-", "8123/4567", "8123\n4567", "8123\t4567"}) {
            assertFalse(Phone.isValidPhone(invalid), invalid);
        }
    }

    @Test
    public void addresses_punctuationAndLengthBoundaries() {
        for (String valid : new String[]{"A", "1", "Blk 21, #04-18 / A's (East) & B.", "裕廊西街 3",
            "A".repeat(200)}) {
            assertTrue(Address.isValidAddress(valid), valid);
        }
        for (String invalid : new String[]{"", " ", "#,-/'()&.", "A".repeat(201), "A;B", "A:B", "A@B",
            "A\n", "\rA", "A\tB", "A\u0000", "A\u2028", "A\u200b"}) {
            assertFalse(Address.isValidAddress(invalid), invalid);
        }
        assertEquals("21 Clementi Ave", new Address("  ２１　Clementi  Ave ").value);
    }
}
