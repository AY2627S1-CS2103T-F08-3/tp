package tutorconnect.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import tutorconnect.model.student.Address;
import tutorconnect.model.student.Name;
import tutorconnect.model.student.Phone;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.WeeklySlot;
import tutorconnect.model.student.WeeklySlotField;

public class WeeklyScheduleMutationTest {
    @Test
    public void mixedMutations_keepScheduleOrderedAndObserversConsistent() {
        Thread thread = Thread.currentThread();
        Thread.UncaughtExceptionHandler original = thread.getUncaughtExceptionHandler();
        List<Throwable> observerFailures = new ArrayList<>();
        thread.setUncaughtExceptionHandler((ignored, failure) -> observerFailures.add(failure));
        try {
            for (long seed : new long[]{83, 2103, 2026, 999}) {
                exerciseMutations(seed, observerFailures);
            }
        } finally {
            thread.setUncaughtExceptionHandler(original);
        }
    }

    private void exerciseMutations(long seed, List<Throwable> observerFailures) {
        Random random = new Random(seed);
        StudentDirectory directory = new StudentDirectory();
        WeeklySchedule schedule = new WeeklySchedule(directory.getStudentList());
        int nextName = 0;
        for (int operation = 0; operation < 300; operation++) {
            var register = directory.getStudentList();
            int action = register.isEmpty() ? 0 : random.nextInt(5);
            if (action == 0) {
                String suffix = String.valueOf((char) ('A' + nextName % 26)).repeat(nextName / 26 + 1);
                nextName++;
                Student added = new Student(new Name("Student " + suffix), new Phone("81234567"), new Address("A"));
                directory.addStudent(withRandomSlot(added, random));
            } else if (action == 1) {
                Student target = register.get(random.nextInt(register.size()));
                directory.setStudent(target, withRandomSlot(target, random));
            } else if (action == 2) {
                directory.removeStudent(register.get(random.nextInt(register.size())));
            } else if (action == 3) {
                List<Student> shuffled = new ArrayList<>(register);
                Collections.shuffle(shuffled, random);
                directory.setStudents(shuffled);
            } else {
                Student target = register.get(random.nextInt(register.size()));
                directory.setStudent(target, target.withStudentFields(
                        target.getStudentFields().without(WeeklySlotField.INSTANCE)));
            }
            assertTrue(observerFailures.isEmpty(), () -> "Observable-list failure: " + observerFailures);
            List<Student> expected = register.stream().filter(student -> WeeklySlotField.get(student).isPresent())
                    .sorted(Comparator.comparingInt(this::minuteOfWeek)).toList();
            assertEquals(expected.stream().map(Student::getId).toList(),
                    schedule.getEntries().stream().map(Student::getId).toList(),
                    "Seed " + seed + ", step " + operation);
        }
    }

    private Student withRandomSlot(Student student, Random random) {
        // A small range deliberately creates ties and tests source-order preservation.
        WeeklySlot slot = new WeeklySlot(DayOfWeek.of(random.nextInt(3) + 1),
                LocalTime.of(9, random.nextInt(2) * 30));
        return student.withStudentFields(student.getStudentFields().with(WeeklySlotField.INSTANCE, slot));
    }

    private int minuteOfWeek(Student student) {
        WeeklySlot slot = WeeklySlotField.get(student).orElseThrow();
        return (slot.getDay().getValue() - 1) * 1440 + slot.getTime().getHour() * 60 + slot.getTime().getMinute();
    }
}
