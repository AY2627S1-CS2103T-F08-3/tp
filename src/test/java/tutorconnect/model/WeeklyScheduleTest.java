package tutorconnect.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tutorconnect.testutil.Assert.assertThrows;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import tutorconnect.model.student.Address;
import tutorconnect.model.student.Name;
import tutorconnect.model.student.Phone;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.WeeklySlot;
import tutorconnect.model.student.WeeklySlotField;

public class WeeklyScheduleTest {
    @Test
    public void summary_ordersWeekdayThenTimeWithoutChangingRegisterOrFilter() {
        ModelManager model = new ModelManager();
        Student zoe = scheduled("Zoe", DayOfWeek.SUNDAY, 0, 0);
        Student amy = scheduled("Amy", DayOfWeek.MONDAY, 18, 30);
        Student ben = scheduled("Ben", DayOfWeek.MONDAY, 9, 0);
        model.addStudent(zoe);
        model.addStudent(amy);
        model.addStudent(ben);
        WeeklySchedule summary = new WeeklySchedule(model.getStudentDirectory().getStudentList());
        model.updateFilteredStudentList(student -> student.getId().equals(zoe.getId()));
        assertEquals(List.of(ben, amy, zoe), summary.getEntries());
        assertEquals(List.of(zoe, amy, ben), model.getStudentDirectory().getStudentList());
        assertEquals(List.of(zoe), model.getFilteredStudentList());
        assertThrows(UnsupportedOperationException.class, () -> summary.getEntries().clear());
    }

    @Test
    public void summary_tiesUseInsertionOrder_andUpdatesAndDeletionRefreshImmediately() {
        ModelManager model = new ModelManager();
        Student zoe = scheduled("Zoe", DayOfWeek.SATURDAY, 9, 30);
        Student amy = scheduled("Amy", DayOfWeek.SATURDAY, 9, 30);
        Student ben = scheduled("Ben", DayOfWeek.SATURDAY, 9, 30);
        model.addStudent(zoe);
        model.addStudent(amy);
        model.addStudent(ben);
        WeeklySchedule summary = new WeeklySchedule(model.getStudentDirectory().getStudentList());
        assertEquals(List.of(zoe, amy, ben), summary.getEntries());
        Student earlierBen = ben.withStudentFields(ben.getStudentFields().with(WeeklySlotField.INSTANCE,
                new WeeklySlot(DayOfWeek.FRIDAY, LocalTime.of(23, 59))));
        model.setStudent(ben, earlierBen);
        assertEquals(List.of(earlierBen, zoe, amy), summary.getEntries());
        assertEquals(List.of(zoe, amy, earlierBen), model.getStudentDirectory().getStudentList());
        model.deleteStudent(zoe);
        assertEquals(List.of(earlierBen, amy), summary.getEntries());
        model.deleteStudent(earlierBen);
        model.deleteStudent(amy);
        assertTrue(summary.getEntries().isEmpty());
    }

    private Student scheduled(String name, DayOfWeek day, int hour, int minute) {
        Student student = new Student(new Name(name), new Phone("81234567"), new Address("21 Clementi Ave"));
        return student.withStudentFields(student.getStudentFields().with(WeeklySlotField.INSTANCE,
                new WeeklySlot(day, LocalTime.of(hour, minute))));
    }
}
