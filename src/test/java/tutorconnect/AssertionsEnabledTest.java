package tutorconnect;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Guards the build setting that runs tests with Java assertions enabled. */
public class AssertionsEnabledTest {
    @Test
    public void assertStatement_failingCondition_throwsAssertionError() {
        assertThrows(AssertionError.class, () -> {
            assert false : "Assertions must be enabled in build.gradle";
        }, "Tests are running without -ea; set enableAssertions = true for the test task");
    }
}
