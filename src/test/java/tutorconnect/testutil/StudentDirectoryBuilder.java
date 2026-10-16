package tutorconnect.testutil;

import tutorconnect.model.StudentDirectory;
import tutorconnect.model.student.Student;

/**
 * A utility class to help with building StudentDirectory objects.
 * Example usage: <br>
 *     {@code StudentDirectory ab = new StudentDirectoryBuilder().withStudent("John", "Doe").build();}
 */
public class StudentDirectoryBuilder {

    private StudentDirectory studentDirectory;

    public StudentDirectoryBuilder() {
        studentDirectory = new StudentDirectory();
    }

    public StudentDirectoryBuilder(StudentDirectory studentDirectory) {
        this.studentDirectory = studentDirectory;
    }

    /**
     * Adds a new {@code Student} to the {@code StudentDirectory} that we are building.
     */
    public StudentDirectoryBuilder withStudent(Student student) {
        studentDirectory.addStudent(student);
        return this;
    }

    public StudentDirectory build() {
        return studentDirectory;
    }
}
