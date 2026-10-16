package tutorconnect.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import tutorconnect.commons.exceptions.IllegalValueException;
import tutorconnect.commons.util.JsonUtil;
import tutorconnect.model.student.HourlyRate;

public class HourlyRateValidationTest {
    @Test
    public void json_overPrecisionTextAndNumbers_areRejectedWithoutRounding() throws Exception {
        for (String value : List.of("40.000", "40.001", "1000.000")) {
            for (String jsonValue : List.of(value, "\"" + value + "\"")) {
                String json = "{\"name\":\"Alex Tan\",\"phone\":\"81234567\",\"address\":\"21 Street\","
                        + "\"hourlyRate\":" + jsonValue + "}";
                JsonAdaptedStudent adapter = JsonUtil.fromJsonString(json, JsonAdaptedStudent.class);
                IllegalValueException failure = assertThrows(IllegalValueException.class, adapter::toModelType);
                assertEquals(HourlyRate.MESSAGE_CONSTRAINTS, failure.getMessage());
            }
        }
    }
}
