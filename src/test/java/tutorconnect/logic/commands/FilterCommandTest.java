package tutorconnect.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import tutorconnect.logic.parser.TutorConnectParser;
import tutorconnect.model.Model;
import tutorconnect.model.ModelManager;
import tutorconnect.model.StudentDirectory;
import tutorconnect.model.student.EducationLevel;
import tutorconnect.model.student.Student;
import tutorconnect.testutil.StudentBuilder;

class FilterCommandTest {
    @Test
    void execute_levelChangeOutsideFilter_clearsSelectionWithoutSelectingNextRow() throws Exception {
        Model model = new ModelManager();
        Student target = new StudentBuilder().build();
        target = target.withStudentFields(target.getStudentFields()
                .with(EducationLevel.FIELD, new EducationLevel("p1")));
        model.addStudent(target);
        TutorConnectParser parser = new TutorConnectParser();
        parser.parseCommand("filter l/p1").execute(model);
        model.selectStudent(target.getId());
        CommandResult result = parser.parseCommand("level 1 l/p2").execute(model);
        assertEquals(List.of(), model.getFilteredStudentList());
        assertNull(model.getSelectedStudentId());
        assertEquals(CommandResult.SelectionAction.CLEAR, result.getSelectionAction());
        assertEquals(java.util.Optional.empty(), result.getSelectedStudentIndex());
        // A staged command must remain publishable after its target leaves the filtered view.
        Model published = new ModelManager();
        published.publish(model);
        assertEquals(target.getId(), published.getStudentDirectory().getStudentList().getFirst().getId());
    }

    @Test
    void execute_replacesViewPreservesOrderAndRecords() throws Exception {
        Model model = new ModelManager();
        Student first = new StudentBuilder().withName("Zoe").build();
        Student second = new StudentBuilder().withName("Amy").build();
        first = first.withStudentFields(first.getStudentFields().with(EducationLevel.FIELD, new EducationLevel("p1")));
        second = second.withStudentFields(second.getStudentFields()
                .with(EducationLevel.FIELD, new EducationLevel("p1")));
        model.addStudent(first);
        model.addStudent(second);
        StudentDirectory before = new StudentDirectory(model.getStudentDirectory());
        model.updateFilteredStudentList(student -> false);
        Command command = new TutorConnectParser().parseCommand("filter l/p1");
        CommandResult result = command.execute(model);
        assertEquals(List.of(first, second), model.getFilteredStudentList());
        assertEquals("2 student(s) listed!", result.getFeedbackToUser());
        assertEquals(CommandResult.SelectionAction.CLEAR, result.getSelectionAction());
        assertNull(model.getSelectedStudentId());
        assertEquals(before, model.getStudentDirectory());

        result = new TutorConnectParser().parseCommand("filter l/jc2").execute(model);
        assertEquals("0 student(s) listed!", result.getFeedbackToUser());
        assertEquals(List.of(), model.getFilteredStudentList());
        assertEquals(before, model.getStudentDirectory());
    }
}
