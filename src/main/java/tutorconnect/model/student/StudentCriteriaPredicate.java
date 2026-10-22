package tutorconnect.model.student;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.function.Predicate;

/** Matches every supplied criterion, using the field owners' canonical comparison rules. */
public record StudentCriteriaPredicate(Optional<EducationLevel> level, Optional<Subject> subject)
        implements Predicate<Student> {

    /** Creates a query requiring at least one non-null optional criterion. */
    public StudentCriteriaPredicate {
        requireNonNull(level);
        requireNonNull(subject);
        if (level.isEmpty() && subject.isEmpty()) {
            throw new IllegalArgumentException("At least one criterion is required");
        }
    }

    @Override
    public boolean test(Student student) {
        return matches(student, EducationLevel.FIELD, level) && matches(student, Subject.FIELD, subject);
    }

    private static <T> boolean matches(Student student, StudentField<T> field, Optional<T> criterion) {
        if (criterion.isEmpty()) {
            return true;
        }
        try {
            return student.getStudentFields().get(field).filter(criterion.get()::equals).isPresent();
        } catch (IllegalArgumentException e) {
            // Unreadable legacy fields are treated as unset, just as in the details display.
            return false;
        }
    }
}
