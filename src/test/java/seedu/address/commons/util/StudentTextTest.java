package seedu.address.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class StudentTextTest {
    @Test
    public void normalize_unicodeAndSpaces() {
        assertEquals("Alex Tan", StudentText.normalize("  Ａlex\u00a0  Tan  "));
        assertEquals("é", StudentText.normalize("e\u0301"));
    }

    @Test
    public void normalize_rejectsControlsBeforeTrimming() {
        for (String value : new String[]{"Alex\n", "\tAlex", "A\u0000B", "A\u2028B", "A\u200bB"}) {
            assertThrows(IllegalArgumentException.class, () -> StudentText.normalize(value));
        }
    }
}
