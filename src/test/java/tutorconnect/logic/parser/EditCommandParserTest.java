package tutorconnect.logic.parser;

import static tutorconnect.logic.commands.CommandTestUtil.ADDRESS_DESC_AMY;
import static tutorconnect.logic.commands.CommandTestUtil.ADDRESS_DESC_BOB;
import static tutorconnect.logic.commands.CommandTestUtil.EMAIL_DESC_AMY;
import static tutorconnect.logic.commands.CommandTestUtil.EMAIL_DESC_BOB;
import static tutorconnect.logic.commands.CommandTestUtil.INVALID_ADDRESS_DESC;
import static tutorconnect.logic.commands.CommandTestUtil.INVALID_EMAIL_DESC;
import static tutorconnect.logic.commands.CommandTestUtil.INVALID_NAME_DESC;
import static tutorconnect.logic.commands.CommandTestUtil.INVALID_PHONE_DESC;
import static tutorconnect.logic.commands.CommandTestUtil.INVALID_TAG_DESC;
import static tutorconnect.logic.commands.CommandTestUtil.NAME_DESC_AMY;
import static tutorconnect.logic.commands.CommandTestUtil.PHONE_DESC_AMY;
import static tutorconnect.logic.commands.CommandTestUtil.PHONE_DESC_BOB;
import static tutorconnect.logic.commands.CommandTestUtil.TAG_DESC_FRIEND;
import static tutorconnect.logic.commands.CommandTestUtil.TAG_DESC_HUSBAND;
import static tutorconnect.logic.commands.CommandTestUtil.VALID_ADDRESS_AMY;
import static tutorconnect.logic.commands.CommandTestUtil.VALID_EMAIL_AMY;
import static tutorconnect.logic.commands.CommandTestUtil.VALID_NAME_AMY;
import static tutorconnect.logic.commands.CommandTestUtil.VALID_PHONE_AMY;
import static tutorconnect.logic.commands.CommandTestUtil.VALID_PHONE_BOB;
import static tutorconnect.logic.commands.CommandTestUtil.VALID_TAG_FRIEND;
import static tutorconnect.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;
import static tutorconnect.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static tutorconnect.logic.parser.CliSyntax.PREFIX_EMAIL;
import static tutorconnect.logic.parser.CliSyntax.PREFIX_PHONE;
import static tutorconnect.logic.parser.CliSyntax.PREFIX_TAG;
import static tutorconnect.logic.parser.CommandParserTestUtil.assertParseFailure;
import static tutorconnect.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static tutorconnect.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;
import static tutorconnect.testutil.TypicalIndexes.INDEX_SECOND_STUDENT;
import static tutorconnect.testutil.TypicalIndexes.INDEX_THIRD_STUDENT;

import org.junit.jupiter.api.Test;

import tutorconnect.commons.core.index.Index;
import tutorconnect.logic.Messages;
import tutorconnect.logic.commands.EditCommand;
import tutorconnect.logic.commands.EditCommand.EditStudentDescriptor;
import tutorconnect.model.student.Address;
import tutorconnect.model.student.Email;
import tutorconnect.model.student.Name;
import tutorconnect.model.student.Phone;
import tutorconnect.model.tag.Tag;
import tutorconnect.testutil.EditStudentDescriptorBuilder;

public class EditCommandParserTest {

    private static final String TAG_EMPTY = " " + PREFIX_TAG;

    private static final String MESSAGE_INVALID_FORMAT = ParserUtil.MESSAGE_INVALID_INDEX;

    private EditCommandParser parser = new EditCommandParser();

    @Test
    public void parse_unknownPrefixInsideAddress_failure() {
        assertParseFailure(parser, "1 a/21 Clementi x/accidental", "Error: Unknown parameter: x/.");
        assertParseFailure(parser, "1 a/21 Clementi wrong/value", "Error: Unknown parameter: wrong/.");
        assertParseFailure(parser, "1 N/Alex", "Error: Unknown parameter: N/.");
    }

    @Test
    public void parse_controlAtValueBoundary_failure() {
        for (String control : new String[] {"\n", "\r", "\t", "\u0000", "\u200b", "\u2028"}) {
            assertParseFailure(parser, "1 n/Alex" + control, StudentParameters.UNEXPECTED_TEXT);
            assertParseFailure(parser, "1 a/" + control + "21 Clementi", StudentParameters.UNEXPECTED_TEXT);
            assertParseFailure(parser, "1 e/alex@example.com" + control, StudentParameters.UNEXPECTED_TEXT);
            assertParseFailure(parser, "1 t/friend" + control, StudentParameters.UNEXPECTED_TEXT);
        }
    }

    @Test
    public void parse_missingParts_failure() {
        // no index specified
        assertParseFailure(parser, VALID_NAME_AMY, StudentParameters.UNEXPECTED_TEXT);

        // no field specified
        assertParseFailure(parser, "1", EditCommand.MESSAGE_NOT_EDITED);

        // no index and no field specified
        assertParseFailure(parser, "", MESSAGE_INVALID_FORMAT);
    }

    @Test
    public void parse_invalidPreamble_failure() {
        // negative index
        assertParseFailure(parser, "-5" + NAME_DESC_AMY, MESSAGE_INVALID_FORMAT);

        // zero index
        assertParseFailure(parser, "0" + NAME_DESC_AMY, MESSAGE_INVALID_FORMAT);

        // invalid arguments being parsed as preamble
        assertParseFailure(parser, "1 some random string", StudentParameters.UNEXPECTED_TEXT);

        // invalid prefix being parsed as preamble
        assertParseFailure(parser, "1 i/ string", "Error: Unknown parameter: i/.");
    }

    @Test
    public void parse_invalidValue_failure() {
        assertParseFailure(parser, "1" + INVALID_NAME_DESC, Name.MESSAGE_CONSTRAINTS); // invalid name
        assertParseFailure(parser, "1" + INVALID_PHONE_DESC, Phone.MESSAGE_CONSTRAINTS); // invalid phone
        assertParseFailure(parser, "1" + INVALID_EMAIL_DESC, Email.MESSAGE_CONSTRAINTS); // invalid email
        assertParseFailure(parser, "1" + INVALID_ADDRESS_DESC, Address.MESSAGE_CONSTRAINTS); // invalid address
        assertParseFailure(parser, "1" + INVALID_TAG_DESC, Tag.MESSAGE_CONSTRAINTS); // invalid tag

        // invalid phone followed by valid email
        assertParseFailure(parser, "1" + INVALID_PHONE_DESC + EMAIL_DESC_AMY, Phone.MESSAGE_CONSTRAINTS);

        // while parsing {@code PREFIX_TAG} alone will reset the tags of the {@code Student} being edited,
        // parsing it together with a valid tag results in error
        assertParseFailure(parser, "1" + TAG_DESC_FRIEND + TAG_DESC_HUSBAND + TAG_EMPTY, Tag.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, "1" + TAG_DESC_FRIEND + TAG_EMPTY + TAG_DESC_HUSBAND, Tag.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, "1" + TAG_EMPTY + TAG_DESC_FRIEND + TAG_DESC_HUSBAND, Tag.MESSAGE_CONSTRAINTS);

        // multiple invalid values, but only the first invalid value is captured
        assertParseFailure(parser, "1" + INVALID_NAME_DESC + INVALID_EMAIL_DESC + VALID_ADDRESS_AMY + VALID_PHONE_AMY,
                Name.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void parse_allFieldsSpecified_success() {
        Index targetIndex = INDEX_SECOND_STUDENT;
        String userInput = targetIndex.getOneBased() + PHONE_DESC_BOB + TAG_DESC_HUSBAND
                + EMAIL_DESC_AMY + ADDRESS_DESC_AMY + NAME_DESC_AMY + TAG_DESC_FRIEND;

        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder().withName(VALID_NAME_AMY)
                .withPhone(VALID_PHONE_BOB).withEmail(VALID_EMAIL_AMY).withAddress(VALID_ADDRESS_AMY)
                .withTags(VALID_TAG_HUSBAND, VALID_TAG_FRIEND).build();
        EditCommand expectedCommand = new EditCommand(targetIndex, descriptor);

        assertParseSuccess(parser, userInput, expectedCommand);
    }

    @Test
    public void parse_someFieldsSpecified_success() {
        Index targetIndex = INDEX_FIRST_STUDENT;
        String userInput = targetIndex.getOneBased() + PHONE_DESC_BOB + EMAIL_DESC_AMY;

        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder().withPhone(VALID_PHONE_BOB)
                .withEmail(VALID_EMAIL_AMY).build();
        EditCommand expectedCommand = new EditCommand(targetIndex, descriptor);

        assertParseSuccess(parser, userInput, expectedCommand);
    }

    @Test
    public void parse_oneFieldSpecified_success() {
        // name
        Index targetIndex = INDEX_THIRD_STUDENT;
        String userInput = targetIndex.getOneBased() + NAME_DESC_AMY;
        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder().withName(VALID_NAME_AMY).build();
        EditCommand expectedCommand = new EditCommand(targetIndex, descriptor);
        assertParseSuccess(parser, userInput, expectedCommand);

        // phone
        userInput = targetIndex.getOneBased() + PHONE_DESC_AMY;
        descriptor = new EditStudentDescriptorBuilder().withPhone(VALID_PHONE_AMY).build();
        expectedCommand = new EditCommand(targetIndex, descriptor);
        assertParseSuccess(parser, userInput, expectedCommand);

        // email
        userInput = targetIndex.getOneBased() + EMAIL_DESC_AMY;
        descriptor = new EditStudentDescriptorBuilder().withEmail(VALID_EMAIL_AMY).build();
        expectedCommand = new EditCommand(targetIndex, descriptor);
        assertParseSuccess(parser, userInput, expectedCommand);

        // address
        userInput = targetIndex.getOneBased() + ADDRESS_DESC_AMY;
        descriptor = new EditStudentDescriptorBuilder().withAddress(VALID_ADDRESS_AMY).build();
        expectedCommand = new EditCommand(targetIndex, descriptor);
        assertParseSuccess(parser, userInput, expectedCommand);

        // tags
        userInput = targetIndex.getOneBased() + TAG_DESC_FRIEND;
        descriptor = new EditStudentDescriptorBuilder().withTags(VALID_TAG_FRIEND).build();
        expectedCommand = new EditCommand(targetIndex, descriptor);
        assertParseSuccess(parser, userInput, expectedCommand);
    }

    @Test
    public void parse_multipleRepeatedFields_failure() {
        // More extensive testing of duplicate parameter detections is done in
        // AddCommandParserTest#parse_repeatedNonTagValue_failure()

        // valid followed by invalid
        Index targetIndex = INDEX_FIRST_STUDENT;
        String userInput = targetIndex.getOneBased() + INVALID_PHONE_DESC + PHONE_DESC_BOB;

        assertParseFailure(parser, userInput, Messages.getErrorMessageForDuplicatePrefixes(PREFIX_PHONE));

        // invalid followed by valid
        userInput = targetIndex.getOneBased() + PHONE_DESC_BOB + INVALID_PHONE_DESC;

        assertParseFailure(parser, userInput, Messages.getErrorMessageForDuplicatePrefixes(PREFIX_PHONE));

        // multiple valid fields repeated
        userInput = targetIndex.getOneBased() + PHONE_DESC_AMY + ADDRESS_DESC_AMY + EMAIL_DESC_AMY
                + TAG_DESC_FRIEND + PHONE_DESC_AMY + ADDRESS_DESC_AMY + EMAIL_DESC_AMY + TAG_DESC_FRIEND
                + PHONE_DESC_BOB + ADDRESS_DESC_BOB + EMAIL_DESC_BOB + TAG_DESC_HUSBAND;

        assertParseFailure(parser, userInput,
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_PHONE, PREFIX_EMAIL, PREFIX_ADDRESS));

        // multiple invalid values
        userInput = targetIndex.getOneBased() + INVALID_PHONE_DESC + INVALID_ADDRESS_DESC + INVALID_EMAIL_DESC
                + INVALID_PHONE_DESC + INVALID_ADDRESS_DESC + INVALID_EMAIL_DESC;

        assertParseFailure(parser, userInput,
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_PHONE, PREFIX_EMAIL, PREFIX_ADDRESS));
    }

    @Test
    public void parse_resetTags_success() {
        Index targetIndex = INDEX_THIRD_STUDENT;
        String userInput = targetIndex.getOneBased() + TAG_EMPTY;

        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder().withTags().build();
        EditCommand expectedCommand = new EditCommand(targetIndex, descriptor);

        assertParseSuccess(parser, userInput, expectedCommand);
    }

    @Test
    public void parse_unicodeSpacesAndNormalizedValues_success() {
        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder().withName("Alex Tan")
                .withPhone("81234567").withAddress("12/3 Street").withTags("friend").build();
        assertParseSuccess(parser, "\u00a01\u2003n/Ａｌｅｘ  Tan\u2003p/+65 8123 4567"
                + "\u3000a/12/3 Street\u00a0t/friend\u3000t/friend",
                new EditCommand(INDEX_FIRST_STUDENT, descriptor));
        assertParseSuccess(parser, "1\u00a0t/\u3000",
                new EditCommand(INDEX_FIRST_STUDENT, new EditStudentDescriptorBuilder().withTags().build()));
    }

    @Test
    public void parse_structuralErrors_precedeValuesAndIndex() {
        assertParseFailure(parser, "0 p/bad x/value", "Error: Unknown parameter: x/.");
        assertParseFailure(parser, "0 extra n/Alex", StudentParameters.UNEXPECTED_TEXT);
        assertParseFailure(parser, "0 p/bad p/81234567",
                Messages.getErrorMessageForDuplicatePrefixes(PREFIX_PHONE));
        assertParseFailure(parser, "1 n/", Name.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, "1 p/", Phone.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void parse_invalidAndOversizedIndices_handledWithoutNarrowing() {
        for (String index : new String[] {"01", "+1", "1.0", "-1"}) {
            assertParseFailure(parser, index + " n/Alex", MESSAGE_INVALID_FORMAT);
        }
        String huge = "9".repeat(100);
        assertParseSuccess(parser, huge + " n/Alex", new EditCommand(Index.fromOneBased(huge),
                new EditStudentDescriptorBuilder().withName("Alex").build()));
    }
}
