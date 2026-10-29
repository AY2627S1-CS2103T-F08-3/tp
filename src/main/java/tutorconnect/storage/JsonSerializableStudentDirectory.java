package tutorconnect.storage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

import tutorconnect.commons.exceptions.IllegalValueException;
import tutorconnect.model.ReadOnlyStudentDirectory;
import tutorconnect.model.StudentDirectory;
import tutorconnect.model.student.Student;

/**
 * An Immutable StudentDirectory that is serializable to JSON format.
 */
@JsonRootName(value = "addressbook")
class JsonSerializableStudentDirectory {

    public static final String MESSAGE_DUPLICATE_STUDENT = "Students list contains duplicate student(s).";

    // Java names change, but the persisted key remains compatible with existing data.
    @JsonProperty("persons")
    private final List<JsonAdaptedStudent> students = new ArrayList<>();

    /**
     * Constructs a {@code JsonSerializableStudentDirectory} with the given students.
     */
    @JsonCreator
    public JsonSerializableStudentDirectory(@JsonProperty("persons") List<JsonAdaptedStudent> students) {
        if (students == null) {
            throw new IllegalArgumentException("The persons property must be a JSON array.");
        }
        this.students.addAll(students);
    }

    /**
     * Converts a given {@code ReadOnlyStudentDirectory} into this class for Jackson use.
     *
     * @param source future changes to this will not affect the created {@code JsonSerializableStudentDirectory}.
     */
    public JsonSerializableStudentDirectory(ReadOnlyStudentDirectory source) {
        students.addAll(source.getStudentList().stream().map(JsonAdaptedStudent::new).collect(Collectors.toList()));
    }

    /**
     * Converts this student directory into the model's {@code StudentDirectory} object.
     *
     * @throws IllegalValueException if there were any data constraints violated.
     */
    public StudentDirectory toModelType() throws IllegalValueException {
        StudentDirectory studentDirectory = new StudentDirectory();
        Set<UUID> ids = new HashSet<>();
        for (JsonAdaptedStudent jsonAdaptedStudent : students) {
            if (jsonAdaptedStudent == null) {
                throw new IllegalValueException("Student records must not be null.");
            }
            Student student = jsonAdaptedStudent.toModelType();
            if (studentDirectory.hasStudent(student) || !ids.add(student.getId())) {
                throw new IllegalValueException(MESSAGE_DUPLICATE_STUDENT);
            }
            studentDirectory.addStudent(student);
        }
        return studentDirectory;
    }

}
