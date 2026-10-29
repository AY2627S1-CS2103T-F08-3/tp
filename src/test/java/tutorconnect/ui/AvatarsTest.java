package tutorconnect.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class AvatarsTest {
    @Test
    public void initials_useFirstAndLastWords() {
        assertEquals("SC", Avatars.initials("Sophie Chen"));
        assertEquals("OJ", Avatars.initials("O'Brien-Smith Jr."));
        assertEquals("NA", Avatars.initials("  nur   binte   aisyah "));
        assertEquals("A", Avatars.initials("Amy"));
        assertEquals("ÉL", Avatars.initials("élise Lim"));
        assertEquals("?", Avatars.initials("..."));
    }

    @Test
    public void styleClass_isStableAndInRange() {
        assertEquals(Avatars.styleClass("Amy Tan"), Avatars.styleClass("amy tan"));
        assertTrue(Avatars.styleClass("Amy Tan").matches("avatar-[0-5]"));
    }
}
