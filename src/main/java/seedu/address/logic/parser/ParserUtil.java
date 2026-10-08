package seedu.address.logic.parser;

import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.Address;
import seedu.address.model.person.Email;
import seedu.address.model.person.GuardianPhone;
import seedu.address.model.person.HourlyRate;
import seedu.address.model.person.Name;
import seedu.address.model.person.Phone;
import seedu.address.model.tag.Tag;

/**
 * Contains utility methods used for parsing strings in the various *Parser classes.
 */
public class ParserUtil {

    public static final String MESSAGE_INVALID_INDEX =
            "Error: Invalid index. Enter a positive whole number without leading zeroes.";
    public static final String MESSAGE_INVALID_GUARDIAN_PHONE = "Error: Invalid guardian phone. Use an 8-digit "
            + "Singapore number beginning with 6, 8, or 9, optionally prefixed by +65.";
    public static final String MESSAGE_INVALID_HOURLY_RATE = "Error: Invalid rate. Enter an SGD amount from 1.00 "
            + "to 1000.00 with at most two decimal places and no currency symbol.";
    private static final Pattern VISIBLE_INDEX_PATTERN = Pattern.compile("[1-9][0-9]*");
    private static final Pattern GUARDIAN_PHONE_INPUT_PATTERN =
            Pattern.compile("(?:\\+65[ -]?)?[689](?:[ -]?[0-9]){7}");
    private static final Pattern HOURLY_RATE_INPUT_PATTERN =
            Pattern.compile("(?:0|[1-9][0-9]*)(?:\\.[0-9]{1,2})?");

    /**
     * Parses {@code oneBasedIndex} into an {@code Index} and returns it. Leading and trailing whitespaces will be
     * trimmed.
     * @throws ParseException if the specified index is invalid (not a non-zero unsigned integer).
     */
    public static Index parseIndex(String oneBasedIndex) throws ParseException {
        requireNonNull(oneBasedIndex);
        String trimmedIndex = oneBasedIndex.trim();
        if (!VISIBLE_INDEX_PATTERN.matcher(trimmedIndex).matches()) {
            throw new ParseException(MESSAGE_INVALID_INDEX);
        }
        return Index.fromOneBased(trimmedIndex);
    }

    /** Parses and canonicalizes an optional-prefix Singapore guardian phone number. */
    public static GuardianPhone parseGuardianPhone(String phone) throws ParseException {
        requireNonNull(phone);
        String nfkcPhone = Normalizer.normalize(phone, Normalizer.Form.NFKC);
        if (containsControlCharacter(nfkcPhone)) {
            throw new ParseException(MESSAGE_INVALID_GUARDIAN_PHONE);
        }
        String normalizedPhone = nfkcPhone.trim().replaceAll(" {2,}", " ");
        if (!GUARDIAN_PHONE_INPUT_PATTERN.matcher(normalizedPhone).matches()) {
            throw new ParseException(MESSAGE_INVALID_GUARDIAN_PHONE);
        }
        String compactPhone = normalizedPhone.replace(" ", "").replace("-", "");
        String digits = compactPhone.startsWith("+65") ? compactPhone.substring(3) : compactPhone;
        return new GuardianPhone("+65" + digits);
    }

    /** Parses a plain SGD amount and stores it with two decimal places without rounding. */
    public static HourlyRate parseHourlyRate(String rate) throws ParseException {
        requireNonNull(rate);
        String nfkcRate = Normalizer.normalize(rate, Normalizer.Form.NFKC);
        if (containsControlCharacter(nfkcRate)) {
            throw new ParseException(MESSAGE_INVALID_HOURLY_RATE);
        }
        String normalizedRate = nfkcRate.trim();
        if (!HOURLY_RATE_INPUT_PATTERN.matcher(normalizedRate).matches()) {
            throw new ParseException(MESSAGE_INVALID_HOURLY_RATE);
        }
        try {
            return new HourlyRate(new BigDecimal(normalizedRate));
        } catch (IllegalArgumentException exception) {
            throw new ParseException(MESSAGE_INVALID_HOURLY_RATE, exception);
        }
    }

    private static boolean containsControlCharacter(String value) {
        return value.codePoints().anyMatch(Character::isISOControl);
    }

    /**
     * Parses a {@code String name} into a {@code Name}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code name} is invalid.
     */
    public static Name parseName(String name) throws ParseException {
        requireNonNull(name);
        if (!Name.isValidName(name)) {
            throw new ParseException(Name.MESSAGE_CONSTRAINTS);
        }
        return new Name(name);
    }

    /**
     * Parses a {@code String phone} into a {@code Phone}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code phone} is invalid.
     */
    public static Phone parsePhone(String phone) throws ParseException {
        requireNonNull(phone);
        if (!Phone.isValidPhone(phone)) {
            throw new ParseException(Phone.MESSAGE_CONSTRAINTS);
        }
        return new Phone(phone);
    }

    /**
     * Parses a {@code String address} into an {@code Address}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code address} is invalid.
     */
    public static Address parseAddress(String address) throws ParseException {
        requireNonNull(address);
        if (!Address.isValidAddress(address)) {
            throw new ParseException(Address.MESSAGE_CONSTRAINTS);
        }
        return new Address(address);
    }

    /**
     * Parses a {@code String email} into an {@code Email}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code email} is invalid.
     */
    public static Email parseEmail(String email) throws ParseException {
        requireNonNull(email);
        String trimmedEmail = email.trim();
        if (!Email.isValidEmail(trimmedEmail)) {
            throw new ParseException(Email.MESSAGE_CONSTRAINTS);
        }
        return new Email(trimmedEmail);
    }

    /**
     * Parses a {@code String tag} into a {@code Tag}.
     * Leading and trailing whitespaces will be trimmed.
     *
     * @throws ParseException if the given {@code tag} is invalid.
     */
    public static Tag parseTag(String tag) throws ParseException {
        requireNonNull(tag);
        String trimmedTag = tag.trim();
        if (!Tag.isValidTagName(trimmedTag)) {
            throw new ParseException(Tag.MESSAGE_CONSTRAINTS);
        }
        return new Tag(trimmedTag);
    }

    /**
     * Parses {@code Collection<String> tags} into a {@code Set<Tag>}.
     */
    public static Set<Tag> parseTags(Collection<String> tags) throws ParseException {
        requireNonNull(tags);
        final Set<Tag> tagSet = new HashSet<>();
        for (String tagName : tags) {
            tagSet.add(parseTag(tagName));
        }
        return tagSet;
    }
}
