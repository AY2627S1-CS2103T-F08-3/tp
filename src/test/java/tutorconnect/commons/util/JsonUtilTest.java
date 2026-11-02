package tutorconnect.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tutorconnect.testutil.Assert.assertThrows;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import tutorconnect.model.UserPrefs;
import tutorconnect.testutil.SerializableTestClass;
import tutorconnect.testutil.TestUtil;

/**
 * Tests JSON Read and Write
 */
public class JsonUtilTest {

    private static final Path SERIALIZATION_FILE = TestUtil.getFilePathInSandboxFolder("serialize.json");

    @Test
    public void serializeObjectToJsonFile_noExceptionThrown() throws IOException {
        SerializableTestClass serializableTestClass = new SerializableTestClass();
        serializableTestClass.setTestValues();

        JsonUtil.serializeObjectToJsonFile(SERIALIZATION_FILE, serializableTestClass);

        assertEquals(FileUtil.readFromFile(SERIALIZATION_FILE), SerializableTestClass.JSON_STRING_REPRESENTATION);
    }

    @Test
    public void deserializeObjectFromJsonFile_noExceptionThrown() throws IOException {
        FileUtil.writeToFile(SERIALIZATION_FILE, SerializableTestClass.JSON_STRING_REPRESENTATION);

        SerializableTestClass serializableTestClass = JsonUtil
                .deserializeObjectFromJsonFile(SERIALIZATION_FILE, SerializableTestClass.class);

        assertEquals(serializableTestClass.getName(), SerializableTestClass.getNameTestValue());
        assertEquals(serializableTestClass.getListOfLocalDateTimes(), SerializableTestClass.getListTestValues());
        assertEquals(serializableTestClass.getMapOfIntegerToString(), SerializableTestClass.getHashMapTestValues());
    }

    @Test
    public void fromJsonString_nestingAtLimit_accepted() throws IOException {
        String json = "[".repeat(JsonUtil.MAX_NESTING_DEPTH) + "0" + "]".repeat(JsonUtil.MAX_NESTING_DEPTH);
        JsonNode value = JsonUtil.fromJsonString(json, JsonNode.class);
        for (int i = 0; i < JsonUtil.MAX_NESTING_DEPTH; i++) {
            value = value.get(0);
        }
        assertEquals(0, value.asInt());
    }

    @Test
    public void fromJsonString_nestingOverLimit_rejected() {
        int depth = JsonUtil.MAX_NESTING_DEPTH + 1;
        String json = "[".repeat(depth) + "0" + "]".repeat(depth);
        assertThrows(IOException.class, () -> JsonUtil.fromJsonString(json, JsonNode.class));
    }

    @Test
    public void fromJsonString_deepIgnoredPreference_rejected() {
        String json = "{\"future\":" + "[".repeat(10000) + "0" + "]".repeat(10000) + "}";
        assertThrows(IOException.class, () -> JsonUtil.fromJsonString(json, UserPrefs.class));
    }

    @Test
    public void fromJsonString_bracketsInsideString_doNotCountAsNesting() throws IOException {
        String text = "[{}]".repeat(10000);
        assertEquals(text, JsonUtil.fromJsonString("\"" + text + "\"", String.class));
    }

}
