package wfc.utility;

import java.nio.IntBuffer;
import java.util.Arrays;

import static java.lang.Long.*;

/**
 * This class works very similar to the Java BitSet class, only slimmer and less safe...
 * Inspired by artemis-odb BitVector, which is a modified version of libgdx Bits.
 * It does not keep track over words in use. (Java Bitset does) There are advantages and disadvantages.
 * For setting and getting single flags, this should be more performant.
 * For comparing "Bitsets", Bitset would be more performant for large sets.
 *
 * @author mzechner (libgdx Bits)
 * @author jshapcott (libgdx Bits)
 * @author junkdog (artemis-odb BitVector)
 * @author Frederik Dahl
 *
 * BitVector:
 * https://github.com/junkdog/artemis-odb/blob/develop/artemis-core/artemis/src/main/java/com/artemis/utils/BitVector.java
 * Bits:
 * https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/utils/Bits.java
 */


public class BitSet {
    
    private long[] words;
    
    public BitSet() {
        words = new long[0];
    }
    
    public BitSet(BitSet bitSet) {
        words = Arrays.copyOf(bitSet.words, bitSet.words.length);
    }
    
    public BitSet(int bits) {
        words = new long[(((Math.max(bits, 0))-1) >>> 6) + 1];
    }
    
    public boolean get(int index) {
        final int word = index >>> 6;
        return word < words.length && (words[word] & (1L << index)) != 0L;
    }
    
    public void set(int index) {
        final int word = index >>> 6;
        checkCapacity(word);
        words[word] |= 1L << index;
    }
    
    public void clear(int index) {
        final int word = index >>> 6;
        if (word < words.length) words[word] &= ~(1L << index);
    }
    
    public void flip(int index) {
        final int word = index >>> 6;
        checkCapacity(word);
        words[word] ^= 1L << index;
    }
    
    public boolean getUnchecked(int index) {
        return (words[index >>> 6] & (1L << index)) != 0L;
    }
    
    public void setUnchecked(int index) {
        words[index >>> 6] |= 1L << index;
    }
    
    public void clearUnchecked(int index) {
        words[index >>> 6] &= ~(1L << index);
    }
    
    public void flipUnsafe(int idx) {
        words[idx >>> 6] ^= 1L << idx;
    }
    
    public boolean getAndClear (int index) {
        final int word = index >>> 6;
        if (word >= words.length) return false;
        long oldBits = words[word];
        words[word] &= ~(1L << (index & 0x3F));
        return words[word] != oldBits;
    }
    
    public boolean getAndSet (int index) {
        final int word = index >>> 6;
        checkCapacity(word);
        long oldBits = words[word];
        words[word] |= 1L << (index & 0x3F);
        return words[word] == oldBits;
    }
    
    
    public void ensureCapacity(int bits) {
        final int word = ((bits-1) >>> 6) + 1;
        if (word > words.length) {
            long[] tmp = new long[word + 1];
            System.arraycopy(words, 0, tmp, 0, words.length);
            words = tmp;
        }
    }
    
    public void and(BitSet o) {
        final int l = words.length;
        final int common = Math.min(l, o.words.length);
        for (int i = 0; common > i; i++) words[i] &= o.words[i];
        if (l > common) for (int i = common; l > i; i++) words[i] = 0L;
    }
    
    public void andNot(BitSet o) {
        int common = Math.min(words.length, o.words.length);
        for (int i = 0; common > i; i++) {
            words[i] &= ~o.words[i];
        }
    }
    
    public void or(BitSet o) {
        final int ol = o.words.length;
        final int common = Math.min(words.length, ol);
        for (int i = 0; common > i; i++) words[i] |= o.words[i];
        if (common < ol) { checkCapacity(ol);
            for (int i = common; ol > i; i++) {
                words[i] = o.words[i];
            }
        }
    }
    
    public void xor(BitSet other) {
        final int ol = other.words.length;
        final int common = Math.min(words.length, ol);
        for (int i = 0; common > i; i++) words[i] ^= other.words[i];
        if (common < ol) { checkCapacity(ol);
            for (int i = common; ol > i; i++) {
                words[i] = other.words[i];
            }
        }
    }
    
    public boolean intersects(BitSet other) {
        long[] bits = this.words;
        long[] otherBits = other.words;
        for (int i = 0, s = Math.min(bits.length, otherBits.length); s > i; i++) {
            if ((bits[i] & otherBits[i]) != 0) return true;
        } return false;
    }
    
    public boolean containsAll(BitSet other) {
        long[] bits = this.words;
        long[] otherBits = other.words;
        final int ol = otherBits.length;
        final int l = bits.length;
        for (int i = l; i < ol; i++)
            if (otherBits[i] != 0) return false;
        for (int i = 0, s = Math.min(l, ol); s > i; i++) {
            if ((bits[i] & otherBits[i]) != otherBits[i]) return false;
        } return true;
    }
    
    public void clear() {
        Arrays.fill(words, 0L);
    }
    
    public int cardinality() {
        int count = 0;
        for (long word : words) count += bitCount(word);
        return count;
    }
    
    public void indices(IntBuffer dst, int count) {
        for (int i = 0, idx = 0; count > idx; i++) {
            long word = words[i];
            int wordBits = i << 6;
            while (word != 0) {
                long t = word & -word;
                dst.put( wordBits + bitCount(t - 1));
                word ^= t;
                idx++;
            }
        }
    }
    
    public int logicalLength() {
        long[] bits = this.words;
        for (int word = bits.length - 1; word >= 0; --word) {
            long wordBits = bits[word];
            if (wordBits != 0)
                return (word << 6) + 64 - numberOfLeadingZeros(wordBits);
        } return 0;
    }
    
    public boolean isEmpty() {
        final int l = words.length;
        for (long word : words) {
            if (word != 0L) return false;
        } return true;
    }
    
    public long word(int index) {
        return index < words.length ? words[index] : 0L;
    }
    
    public long[] array() {
        return words;
    }
    
    public void setWord(int index, long word) {
        if (index >= words.length) {
            long[] newWords = new long[index + 1];
            System.arraycopy(words, 0, newWords, 0, words.length);
            this.words = newWords;
        } words[index] = word;
    }
    
    public int hashCode() {
        final int word = logicalLength() >>> 6; int hash = 0;
        for (int i = 0; word >= i; i++) { hash = 127 * hash + (int) (words[i] ^ (words[i] >>> 32));
        } return hash;
    }
    
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        BitSet other = (BitSet) obj;
        long[] otherBits = other.words;
        int common = Math.min(words.length, otherBits.length);
        for (int i = 0; common > i; i++) {
            if (words[i] != otherBits[i]) return false;
        } if (words.length == otherBits.length) return true;
        return logicalLength() == other.logicalLength();
    }
    
    public String toString() {
        int cardinality = cardinality();
        int end = Math.min(128, cardinality);
        int count = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("Bits[").append(cardinality);
        if (cardinality > 0) {
            sb.append(": {");
            for (int i = nextSetBit(0); end > count && i != -1; i = nextSetBit(i + 1)) {
                if (count != 0) sb.append(", ");
                sb.append(i);
                count++;
            } if (cardinality > end)
                sb.append(" ...");
            sb.append("}");
        } sb.append("]");
        return sb.toString();
    }
    
    private void checkCapacity(int l) {
        if (l >= words.length) {
            long[] tmp = new long[++l];
            System.arraycopy(words, 0, tmp, 0, words.length);
            words = tmp;
        }
    }
    
    /** Returns the index of the first bit that is set to true that occurs on or after the specified starting index. If no such bit
     * exists then -1 is returned. */
    private int nextSetBit(int fromIndex) {
        final int word = fromIndex >>> 6;
        if (word < words.length) {
            long bitmap = words[word] >>> fromIndex;
            if (bitmap == 0) {
                for (int i = 1 + word; i < words.length; i++) {
                    bitmap = words[i];
                    if (bitmap != 0) {
                        return i * 64 + numberOfTrailingZeros(bitmap);
                    }
                }
            } else return fromIndex + numberOfTrailingZeros(bitmap);
        } return -1;
    }
    
}
