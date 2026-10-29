package tutorconnect.model.student;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import tutorconnect.logic.parser.ParserUtil;
import tutorconnect.logic.parser.exceptions.ParseException;

public class EmailValidationPerformanceTest {
    @Test
    public void invalidLongDomain_rejectsPromptlyWithoutBlockingContactEditing() {
        assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
            for (String domain : new String[]{"a".repeat(10_000), "a-".repeat(5_000) + "b",
                "ab.".repeat(5_000) + "cd"}) {
                String email = "a@" + domain + "!";
                assertFalse(Email.isValidEmail(email));
                assertThrows(ParseException.class, () -> ParserUtil.parseEmail(email));
            }
        });
    }

    @Test
    public void finalDomainLabel_acceptsInternalHyphenAndRequiresTwoCharacters() {
        assertTrue(Email.isValidEmail("a@a-b"));
        assertTrue(Email.isValidEmail("a@example.a-b"));
        assertTrue(Email.isValidEmail("a@ab"));
        assertFalse(Email.isValidEmail("a@a"));
        assertFalse(Email.isValidEmail("a@a-"));
        assertFalse(Email.isValidEmail("a@-a"));
    }
}
