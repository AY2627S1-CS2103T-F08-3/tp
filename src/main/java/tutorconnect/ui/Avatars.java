package tutorconnect.ui;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Locale;

/** Initials and a stable colour for the circular avatars shown beside students. */
public final class Avatars {
    private static final int COLOUR_COUNT = 6;

    private Avatars() {}

    /** Returns up to two initials, from the first and last words of {@code name}. */
    public static String initials(String name) {
        String[] words = Arrays.stream(requireNonNull(name).strip().split("\\s+"))
                .filter(word -> word.codePoints().anyMatch(Character::isLetter))
                .toArray(String[]::new);
        if (words.length == 0) {
            return "?";
        }
        String first = firstLetter(words[0]);
        return words.length == 1 ? first : first + firstLetter(words[words.length - 1]);
    }

    /** Returns the style class of the avatar colour for {@code name}; the same name always gets the same colour. */
    public static String styleClass(String name) {
        return "avatar-" + Math.floorMod(requireNonNull(name).toLowerCase(Locale.ROOT).hashCode(), COLOUR_COUNT);
    }

    private static String firstLetter(String word) {
        int letter = word.codePoints().filter(Character::isLetter).findFirst().orElseThrow();
        return new String(Character.toChars(letter)).toUpperCase(Locale.ROOT);
    }
}
