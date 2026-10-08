package seedu.address.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.model.person.Address;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.Phone;
import seedu.address.model.person.WeeklySlot;
import seedu.address.model.person.WeeklySlotField;

public class WeeklyScheduleTest {
    @Test
    public void summary_ordersWeekdayThenTimeWithoutChangingRegisterOrFilter() {
        ModelManager model = new ModelManager();
        Person zoe = scheduled("Zoe", DayOfWeek.SUNDAY, 0, 0);
        Person amy = scheduled("Amy", DayOfWeek.MONDAY, 18, 30);
        Person ben = scheduled("Ben", DayOfWeek.MONDAY, 9, 0);
        model.addPerson(zoe);
        model.addPerson(amy);
        model.addPerson(ben);
        WeeklySchedule summary = new WeeklySchedule(model.getAddressBook().getPersonList());
        model.updateFilteredPersonList(person -> person.getId().equals(zoe.getId()));
        assertEquals(List.of(ben, amy, zoe), summary.getEntries());
        assertEquals(List.of(zoe, amy, ben), model.getAddressBook().getPersonList());
        assertEquals(List.of(zoe), model.getFilteredPersonList());
        assertThrows(UnsupportedOperationException.class, () -> summary.getEntries().clear());
    }

    @Test
    public void summary_tiesUseInsertionOrder_andUpdatesAndDeletionRefreshImmediately() {
        ModelManager model = new ModelManager();
        Person zoe = scheduled("Zoe", DayOfWeek.SATURDAY, 9, 30);
        Person amy = scheduled("Amy", DayOfWeek.SATURDAY, 9, 30);
        Person ben = scheduled("Ben", DayOfWeek.SATURDAY, 9, 30);
        model.addPerson(zoe);
        model.addPerson(amy);
        model.addPerson(ben);
        WeeklySchedule summary = new WeeklySchedule(model.getAddressBook().getPersonList());
        assertEquals(List.of(zoe, amy, ben), summary.getEntries());
        Person earlierBen = ben.withStudentFields(ben.getStudentFields().with(WeeklySlotField.INSTANCE,
                new WeeklySlot(DayOfWeek.FRIDAY, LocalTime.of(23, 59))));
        model.setPerson(ben, earlierBen);
        assertEquals(List.of(earlierBen, zoe, amy), summary.getEntries());
        assertEquals(List.of(zoe, amy, earlierBen), model.getAddressBook().getPersonList());
        model.deletePerson(zoe);
        assertEquals(List.of(earlierBen, amy), summary.getEntries());
        model.deletePerson(earlierBen);
        model.deletePerson(amy);
        assertTrue(summary.getEntries().isEmpty());
    }

    private Person scheduled(String name, DayOfWeek day, int hour, int minute) {
        Person person = new Person(new Name(name), new Phone("81234567"), new Address("21 Clementi Ave"));
        return person.withStudentFields(person.getStudentFields().with(WeeklySlotField.INSTANCE,
                new WeeklySlot(day, LocalTime.of(hour, minute))));
    }
}
