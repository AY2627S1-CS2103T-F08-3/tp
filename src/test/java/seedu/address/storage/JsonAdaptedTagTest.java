package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import seedu.address.commons.exceptions.IllegalValueException;

public class JsonAdaptedTagTest {
    @Test
    public void toModelType_nullTagName_reportsValidationFailure() {
        JsonAdaptedTag tag = new JsonAdaptedTag((String) null);
        assertThrows(IllegalValueException.class, tag::toModelType);
    }
}
