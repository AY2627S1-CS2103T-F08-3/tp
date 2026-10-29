package tutorconnect.model.util;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import tutorconnect.model.ReadOnlyStudentDirectory;
import tutorconnect.model.StudentDirectory;
import tutorconnect.model.student.Address;
import tutorconnect.model.student.EducationLevel;
import tutorconnect.model.student.Email;
import tutorconnect.model.student.Name;
import tutorconnect.model.student.Phone;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.StudentFields;
import tutorconnect.model.student.Subject;
import tutorconnect.model.tag.Tag;

/**
 * Contains utility methods for populating {@code StudentDirectory} with sample data.
 */
public class SampleDataUtil {
    public static Student[] getSampleStudents() {
        return new Student[] {
            new Student(new Name("Alex Yeoh"), new Phone("87438807"), new Email("alexyeoh@example.com"),
                new Address("Blk 30 Geylang Street 29, #06-40"),
                getTagSet("examPrep")).withStudentFields(tutoringFields("Secondary 4", "Mathematics")),
            new Student(new Name("Bernice Yu"), new Phone("99272758"), new Email("berniceyu@example.com"),
                new Address("Blk 30 Lorong 3 Serangoon Gardens, #07-18"),
                getTagSet("revision")).withStudentFields(tutoringFields("Primary 6", "Science")),
            new Student(new Name("Charlotte Oliveiro"), new Phone("93210283"), new Email("charlotte@example.com"),
                new Address("Blk 11 Ang Mo Kio Street 74, #11-04"),
                getTagSet("examPrep")).withStudentFields(tutoringFields("Secondary 4", "English")),
            new Student(new Name("David Li"), new Phone("91031282"), new Email("lidavid@example.com"),
                new Address("Blk 436 Serangoon Gardens Street 26, #16-43"),
                getTagSet("revision")).withStudentFields(tutoringFields("JC 1", "Mathematics")),
            new Student(new Name("Irfan Ibrahim"), new Phone("92492021"), new Email("irfan@example.com"),
                new Address("Blk 47 Tampines Street 20, #17-35"),
                getTagSet("foundation")).withStudentFields(tutoringFields("Primary 3", "English")),
            new Student(new Name("Roy Balakrishnan"), new Phone("92624417"), new Email("royb@example.com"),
                new Address("Blk 45 Aljunied Street 85, #11-31"),
                getTagSet("newStudent"))
        };
    }

    /** Creates validated tutoring fields; Roy remains an example with optional fields unset. */
    private static StudentFields tutoringFields(String level, String subject) {
        return new StudentFields()
                .with(EducationLevel.FIELD, new EducationLevel(level))
                .with(Subject.FIELD, new Subject(subject));
    }

    public static ReadOnlyStudentDirectory getSampleStudentDirectory() {
        StudentDirectory sampleAb = new StudentDirectory();
        for (Student sampleStudent : getSampleStudents()) {
            sampleAb.addStudent(sampleStudent);
        }
        return sampleAb;
    }

    /**
     * Returns a tag set containing the list of strings given.
     */
    public static Set<Tag> getTagSet(String... strings) {
        return Arrays.stream(strings)
                .map(Tag::new)
                .collect(Collectors.toSet());
    }

}
