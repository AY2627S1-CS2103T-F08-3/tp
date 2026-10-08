package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.commons.util.JsonUtil;
import seedu.address.model.AddressBook;
import seedu.address.model.person.Person;
import seedu.address.model.person.StudentFields;
import seedu.address.model.person.WeeklySlot;
import seedu.address.model.person.WeeklySlotField;

public class WeeklySlotPersistenceTest {
    @TempDir
    public Path directory;

    @Test
    public void saveAndReload_allWeekdays_preservesTypedSlotIdentityAndUnrelatedFields() throws Exception {
        Path file = directory.resolve("students.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        StudentFields unrelated = new StudentFields(Map.of("subject", JsonNodeFactory.instance.textNode("Math/Science"),
                "guardianPhone", JsonNodeFactory.instance.textNode("+6581234567"),
                "hourlyRate", JsonNodeFactory.instance.textNode("65.00")));
        for (DayOfWeek day : DayOfWeek.values()) {
            WeeklySlot slot = new WeeklySlot(day, LocalTime.of(9, 0));
            Person scheduled = ALICE.withStudentFields(unrelated.with(WeeklySlotField.INSTANCE, slot));
            AddressBook book = new AddressBook();
            book.addPerson(scheduled);
            storage.saveAddressBook(book);
            Person loaded = storage.readAddressBook().orElseThrow().getPersonList().getFirst();
            assertEquals(scheduled, loaded);
            assertEquals(ALICE.getId(), loaded.getId());
            assertEquals(slot, WeeklySlotField.get(loaded).orElseThrow());
            assertEquals("Math/Science", loaded.getStudentFields().display("subject"));
            JsonNode json = JsonUtil.fromJsonString(Files.readString(file), JsonNode.class);
            assertEquals(day.name(), json.get("persons").get(0).get("weeklySlot").get("day").asText());
            assertEquals("09:00", json.get("persons").get(0).get("weeklySlot").get("time").asText());
        }
    }

    @Test
    public void read_legacyMissingOrNullSlot_remainsUnset() throws Exception {
        for (boolean explicitNull : List.of(false, true)) {
            ObjectNode record = JsonUtil.fromJsonString(JsonUtil.toJsonString(new JsonAdaptedPerson(ALICE)),
                    ObjectNode.class);
            record.remove("id");
            record.remove("weeklySlot");
            if (explicitNull) {
                record.putNull("weeklySlot");
            }
            Path file = directory.resolve("legacy.json");
            Files.writeString(file, JsonNodeFactory.instance.objectNode()
                    .set("persons", JsonNodeFactory.instance.arrayNode().add(record)).toString());
            JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
            Person loaded = storage.readAddressBook().orElseThrow().getPersonList().getFirst();
            assertTrue(WeeklySlotField.get(loaded).isEmpty());
            assertEquals("—", WeeklySlotField.display(loaded));
            assertEquals(loaded.getId(), storage.readAddressBook().orElseThrow().getPersonList().getFirst().getId());
        }
    }

    @Test
    public void read_partialInvalidOrWronglyTypedSlots_rejectsEntireRecord() throws Exception {
        JsonNodeFactory json = JsonNodeFactory.instance;
        for (JsonNode slot : List.of(json.objectNode().put("day", "MONDAY"),
                json.objectNode().put("time", "09:00"), json.objectNode(), json.textNode("Monday 09:00"),
                json.objectNode().put("day", "Funday").put("time", "09:00"),
                json.objectNode().put("day", "MONDAY").put("time", "9:00"),
                json.objectNode().put("day", "MONDAY").put("time", "24:00"))) {
            ObjectNode record = JsonUtil.fromJsonString(JsonUtil.toJsonString(new JsonAdaptedPerson(ALICE)),
                    ObjectNode.class);
            record.set("weeklySlot", slot);
            Path file = directory.resolve("invalid.json");
            String content = json.objectNode().set("persons", json.arrayNode().add(record)).toString();
            Files.writeString(file, content);
            assertThrows(DataLoadingException.class, () -> new JsonAddressBookStorage(file).readAddressBook());
            assertEquals(content, Files.readString(file));
        }
    }
}
