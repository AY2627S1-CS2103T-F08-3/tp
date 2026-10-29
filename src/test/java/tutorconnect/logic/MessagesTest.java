package tutorconnect.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class MessagesTest {
    @Test
    public void formatSentence_valueEndsWithFullStop_keepsOneFullStop() {
        assertEquals("Student added: Tan Jr.", Messages.formatSentence("Student added: %1$s.", "Tan Jr."));
    }

    @Test
    public void formatSentence_ordinaryValue_addsFullStop() {
        assertEquals("Student added: Amy.", Messages.formatSentence("Student added: %1$s.", "Amy"));
    }

    @Test
    public void formatSentence_ellipsisOrMidSentenceName_unchanged() {
        assertEquals("Exiting ...", Messages.formatSentence("Exiting ..."));
        assertEquals("Subject for Tan Jr. is already Math.",
                Messages.formatSentence("Subject for %1$s is already %2$s.", "Tan Jr.", "Math"));
    }
}
