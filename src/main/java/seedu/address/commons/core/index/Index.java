package seedu.address.commons.core.index;

import java.math.BigInteger;

import seedu.address.commons.util.ToStringBuilder;

/**
 * Represents a zero-based or one-based index.
 *
 * {@code Index} should be used right from the start (when parsing in a new user input), so that if the current
 * component wants to communicate with another component, it can send an {@code Index} to avoid having to know what
 * base the other component is using for its index. However, after receiving the {@code Index}, that component can
 * convert it back to an int if the index will not be passed to a different component again.
 */
public class Index {
    private final BigInteger zeroBasedIndex;

    /**
     * Index can only be created by calling {@link Index#fromZeroBased(int)} or
     * {@link Index#fromOneBased(int)}.
     */
    private Index(BigInteger zeroBasedIndex) {
        if (zeroBasedIndex.signum() < 0) {
            throw new IndexOutOfBoundsException();
        }

        this.zeroBasedIndex = zeroBasedIndex;
    }

    public int getZeroBased() {
        return zeroBasedIndex.intValueExact();
    }

    public int getOneBased() {
        return zeroBasedIndex.add(BigInteger.ONE).intValueExact();
    }

    /** Returns the one-based index as entered, without narrowing it to an integer. */
    public String getOneBasedString() {
        return zeroBasedIndex.add(BigInteger.ONE).toString();
    }

    /** Returns whether this index refers to an item in a list of the given size. */
    public boolean isWithinSize(int size) {
        return zeroBasedIndex.compareTo(BigInteger.valueOf(size)) < 0;
    }

    /** Returns the zero-based value after callers have established that it fits an int. */
    public int getZeroBasedExact() {
        return zeroBasedIndex.intValueExact();
    }

    /**
     * Creates a new {@code Index} using a zero-based index.
     */
    public static Index fromZeroBased(int zeroBasedIndex) {
        return new Index(BigInteger.valueOf(zeroBasedIndex));
    }

    /**
     * Creates a new {@code Index} using a one-based index.
     */
    public static Index fromOneBased(int oneBasedIndex) {
        return new Index(BigInteger.valueOf(oneBasedIndex).subtract(BigInteger.ONE));
    }

    /** Creates an index from a positive decimal string of any size. */
    public static Index fromOneBased(String oneBasedIndex) {
        return new Index(new BigInteger(oneBasedIndex).subtract(BigInteger.ONE));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Index otherIndex)) {
            return false;
        }

        return zeroBasedIndex.equals(otherIndex.zeroBasedIndex);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("zeroBasedIndex", zeroBasedIndex).toString();
    }
}
