package tutorconnect.logic.parser;

import static java.util.Objects.requireNonNull;
import static tutorconnect.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static tutorconnect.logic.parser.CliSyntax.PREFIX_EMAIL;
import static tutorconnect.logic.parser.CliSyntax.PREFIX_NAME;
import static tutorconnect.logic.parser.CliSyntax.PREFIX_PHONE;
import static tutorconnect.logic.parser.CliSyntax.PREFIX_TAG;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tutorconnect.commons.core.index.Index;
import tutorconnect.commons.util.StudentText;
import tutorconnect.logic.commands.EditCommand;
import tutorconnect.logic.commands.EditCommand.EditStudentDescriptor;
import tutorconnect.logic.parser.exceptions.ParseException;
import tutorconnect.model.tag.Tag;

/**
 * Parses input arguments and creates a new EditCommand object
 */
public class EditCommandParser implements Parser<EditCommand> {
    private static final Pattern PARAMETER_BOUNDARY = Pattern.compile("(?<!\\S)([A-Za-z]+/)");
    private static final Set<String> ALLOWED_PREFIXES = Set.of(PREFIX_NAME.getPrefix(), PREFIX_PHONE.getPrefix(),
            PREFIX_EMAIL.getPrefix(), PREFIX_ADDRESS.getPrefix(), PREFIX_TAG.getPrefix());

    /**
     * Parses the given {@code String} of arguments in the context of the EditCommand
     * and returns an EditCommand object for execution.
     * @throws ParseException if the user input does not conform to the expected format
     */
    public EditCommand parse(String args) throws ParseException {
        requireNonNull(args);
        // Check raw controls before the legacy tokenizer trims any field boundaries.
        if (StudentText.hasProhibitedCharacters(args)) {
            throw new ParseException(StudentParameters.UNEXPECTED_TEXT);
        }
        String spacedArgs = args.replaceAll("\\p{Zs}+", " ");
        Matcher boundary = PARAMETER_BOUNDARY.matcher(spacedArgs);
        while (boundary.find()) {
            String prefix = boundary.group(1);
            if (!ALLOWED_PREFIXES.contains(prefix)) {
                throw new ParseException("Error: Unknown parameter: " + prefix + ".");
            }
        }
        ArgumentMultimap argMultimap = ArgumentTokenizer.tokenize(spacedArgs,
                PREFIX_NAME, PREFIX_PHONE, PREFIX_EMAIL, PREFIX_ADDRESS, PREFIX_TAG);
        if (argMultimap.getPreamble().contains(" ")) {
            throw new ParseException(StudentParameters.UNEXPECTED_TEXT);
        }

        argMultimap.verifyNoDuplicatePrefixesFor(PREFIX_NAME, PREFIX_PHONE, PREFIX_EMAIL, PREFIX_ADDRESS);
        Index index = ParserUtil.parseIndex(argMultimap.getPreamble());

        EditStudentDescriptor editStudentDescriptor = new EditStudentDescriptor();

        if (argMultimap.getValue(PREFIX_NAME).isPresent()) {
            editStudentDescriptor.setName(ParserUtil.parseName(argMultimap.getValue(PREFIX_NAME).get()));
        }
        if (argMultimap.getValue(PREFIX_PHONE).isPresent()) {
            editStudentDescriptor.setPhone(ParserUtil.parsePhone(argMultimap.getValue(PREFIX_PHONE).get()));
        }
        if (argMultimap.getValue(PREFIX_EMAIL).isPresent()) {
            editStudentDescriptor.setEmail(ParserUtil.parseEmail(argMultimap.getValue(PREFIX_EMAIL).get()));
        }
        if (argMultimap.getValue(PREFIX_ADDRESS).isPresent()) {
            editStudentDescriptor.setAddress(ParserUtil.parseAddress(argMultimap.getValue(PREFIX_ADDRESS).get()));
        }
        parseTagsForEdit(argMultimap.getAllValues(PREFIX_TAG)).ifPresent(editStudentDescriptor::setTags);

        if (!editStudentDescriptor.isAnyFieldEdited()) {
            throw new ParseException(EditCommand.MESSAGE_NOT_EDITED);
        }

        return new EditCommand(index, editStudentDescriptor);
    }

    /**
     * Parses {@code Collection<String> tags} into a {@code Set<Tag>} if {@code tags} is non-empty.
     * If {@code tags} contains only one element which is an empty string, it will be parsed into a
     * {@code Set<Tag>} containing zero tags.
     */
    private Optional<Set<Tag>> parseTagsForEdit(Collection<String> tags) throws ParseException {
        assert tags != null;

        if (tags.isEmpty()) {
            return Optional.empty();
        }
        Collection<String> tagSet = tags.size() == 1 && tags.contains("") ? Collections.emptySet() : tags;
        return Optional.of(ParserUtil.parseTags(tagSet));
    }

}
