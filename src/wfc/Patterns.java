package wfc;

import wfc.utility.BitSet;

import java.nio.IntBuffer;
import java.util.*;

/**
 *
 * Takes any discrete input (could be colors, tiles or any other values), gather all
 * unique 3x3-patterns (including rotations and reflections) occurring in the input.
 * Then for every pattern, gather frequency, center value and all possible adjacent pattern
 * for each cardinal direction.
 *
 * @author Frederik Dahl
 * 01/05/2023
 */


class Patterns {
    
    private final int count;
    private final int[] value_table; // maps pattern indexes to the 3x3 center value
    private final int[] weight_table; // maps pattern indexes to how many times it occurred in the input
    private final float super_entropy; // the entropy of a Cell that can become any pattern
    private final BitSet super_position; // The set of all patterns [0->(count - 1)]
    private final BitSet intermediary_bitset;
    private final BitSet[] valid_connections_t;
    private final BitSet[] valid_connections_r;
    private final BitSet[] valid_connections_b;
    private final BitSet[] valid_connections_l;
    private final IntBuffer buffer_0; // buffers used to store pattern indexes, their length is equal to
    private final IntBuffer buffer_1; // the total pattern count, and they are used by the WCF object
    
    /**
     * Creates new Pattern Collection
     * @param src input to process
     * @param allow_permutations allow pattern rotations and reflections
     */
    Patterns(int[][] src, boolean allow_permutations) {
        List<Map.Entry<Pattern,int[]>> entries = unique_patterns(src,allow_permutations);
        count = entries.size();
        value_table = new int[count];
        weight_table = new int[count];
        buffer_0 = IntBuffer.allocate(count);
        buffer_1 = IntBuffer.allocate(count);
        super_position = new BitSet(count);
        intermediary_bitset = new BitSet(count);
        valid_connections_t = new BitSet[count];
        valid_connections_r = new BitSet[count];
        valid_connections_b = new BitSet[count];
        valid_connections_l = new BitSet[count];
        int sum_weights_total = 0;
        List<Pattern> pattern_obj_list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Map.Entry<Pattern,int[]> entry = entries.get(i);
            Pattern pattern_obj = entry.getKey();
            pattern_obj_list.add(pattern_obj);
            value_table[i] = pattern_obj.center();
            weight_table[i] = entry.getValue()[0];
            sum_weights_total += weight_table[i];
            super_position.setUnchecked(i);
            valid_connections_t[i] = new BitSet(64);
            valid_connections_r[i] = new BitSet(64);
            valid_connections_b[i] = new BitSet(64);
            valid_connections_l[i] = new BitSet(64);
        } for (int i = 0; i < count; i++) {
            Pattern pattern = pattern_obj_list.get(i);
            for (int j = 0; j < count; j++) {
                Pattern other = pattern_obj_list.get(j);
                if (pattern.top_match(other)) valid_connections_t[i].set(j);
                if (pattern.right_match(other)) valid_connections_r[i].set(j);
                if (pattern.bottom_match(other)) valid_connections_b[i].set(j);
                if (pattern.left_match(other)) valid_connections_l[i].set(j);
            }
        } super_entropy = calculate_cell_initial_entropy(sum_weights_total);
    }
    
    
    BitSet valid_adjacent_patterns(IntBuffer options, int direction) { // to_direction
        intermediary_bitset.clear();
        BitSet[] valid_connections;
        switch (direction) {
            case 0 -> valid_connections = valid_connections_t;
            case 1 -> valid_connections = valid_connections_r;
            case 2 -> valid_connections = valid_connections_b;
            case 3 -> valid_connections = valid_connections_l;
            default -> throw new IllegalStateException("Unexpected value: " + direction);
        } int buffer_position = options.position();
        int buffer_limit = options.limit();
        for (int i = buffer_position; i < buffer_limit; i++) {
            intermediary_bitset.or(valid_connections[options.get(i)]);
        } return intermediary_bitset;
    }
    
    IntBuffer borrow_buffer_0() {
        return buffer_0;
    }
    
    IntBuffer borrow_buffer_1() {
        return buffer_1;
    }
    
    BitSet super_position() {
        return super_position;
    }
    
    float super_entropy() {
        return super_entropy;
    }
    
    int count() {
        return count;
    }
    
    int frequency_of(int pattern) {
        return weight_table[pattern];
    }
    
    int value_of(int pattern) {
        return value_table[pattern];
    }
    
    // see. "Shannon Entropy"
    // - P(x1)*log(P(x1)) - P(x2)*log(P(x2)) - ... - P(xn)*log(P(xn))
    // reformulated as -->
    // log(W) - (w1*log(w1) + w2*log(w2) + ... + wn*log(wn)) / W
    
    private float calculate_cell_initial_entropy(int W) {
        double S = 0;
        for (int i = 0; i < count; i++) {
            S += (weight_table[i] * Math.log(weight_table[i]));
        } return (float) (Math.log(W) - S / W);
    }
    
    private List<Map.Entry<Pattern, int[]>> unique_patterns(int[][] grid, boolean allow_permutations) {
        List<Map.Entry<Pattern, int[]>> list = new ArrayList<>(unique_pattern_map(grid,allow_permutations).entrySet());
        list.sort((o1, o2) -> Integer.compare(o2.getValue()[0], o1.getValue()[0]));
        return list;
    }
    
    private Map<Pattern, int[]> unique_pattern_map(int[][] grid, boolean allow_permutations) {
        List<Pattern> list = all_patterns(grid, allow_permutations);
        Map<Pattern, int[]> map = new HashMap<>(Math.round(list.size() * 1.4f));
        for (Pattern pattern : list) {
            int[] occurrences = map.get(pattern);
            if (occurrences == null) {
                occurrences = new int[]{1};
                map.put(pattern, occurrences);
            } else occurrences[0]++;
        } return map;
    }
    
    private List<Pattern> all_patterns(int[][] grid, boolean allow_permutations) {
        int rows = grid.length;
        int cols = grid[0].length;
        List<Pattern> dst = new ArrayList<>(rows * cols);
        if (rows >= 3 && cols >= 3) {
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int t_idx = (r + 1) % rows;
                    int l_idx = (c - 1) < 0 ? cols - 1 : (c - 1);
                    int b_idx = (r - 1) < 0 ? rows - 1 : (r - 1);
                    int r_idx = (c + 1) % cols;
                    // tl, to, tr, cl, ce, cr, bl, bo, br;
                    Pattern pattern = new Pattern(
                            grid[t_idx][l_idx],
                            grid[t_idx][c],
                            grid[t_idx][r_idx],
                            grid[r][l_idx],
                            grid[r][c],
                            grid[r][r_idx],
                            grid[b_idx][l_idx],
                            grid[b_idx][c],
                            grid[b_idx][r_idx]
                    ); if (allow_permutations)
                        all_permutations(pattern,dst);
                    else dst.add(pattern);
                }
            }
        }
        return dst;
    }
    
    private void all_permutations(Pattern pattern, Collection<Pattern> dst) {
        Pattern r90 = pattern.rotate();
        Pattern r180 = r90.rotate();
        Pattern r270 = r180.rotate();
        dst.add(pattern);
        dst.add(r90);
        dst.add(r180);
        dst.add(r270);
        dst.add(pattern.flip());
        dst.add(r90.flip());
        dst.add(r180.flip());
        dst.add(r270.flip());
    }
    
    private final static class Pattern {
        
        private final int tl, to, tr;
        private final int cl, ce, cr;
        private final int bl, bo, br;
        
        private Pattern(int tl, int to, int tr, int cl, int ce, int cr, int bl, int bo, int br) {
            this.tl = tl; this.to = to; this.tr = tr;
            this.cl = cl; this.ce = ce; this.cr = cr;
            this.bl = bl; this.bo = bo; this.br = br;
        }
        
        boolean top_match(Pattern o) {
            return tl == o.cl && to == o.ce && tr == o.cr && cl == o.bl && ce == o.bo && cr == o.br;
        }
        
        boolean right_match(Pattern o) {
            return to == o.tl && tr == o.to && ce == o.cl && cr == o.ce && bo == o.bl && br == o.bo;
        }
        
        boolean bottom_match(Pattern o) {
            return o.tl == cl && o.to == ce && o.tr == cr && o.cl == bl && o.ce == bo && o.cr == br;
        }
        
        boolean left_match(Pattern o) {
            return o.to == tl && o.tr == to && o.ce == cl && o.cr == ce && o.bo == bl && o.br == bo;
        }
        
        int center() {
            return ce;
        }
    
        private Pattern rotate() { // 90-deg
            return new Pattern(bl, cl, tl, bo, ce, to, br, cr, tr);
        }
        
        private Pattern flip() { // vertical
            return new Pattern(bl,bo,br,cl,ce,cr,tl,to,tr);
        }
        
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pattern p = (Pattern) o;
            return tl == p.tl && to == p.to && tr == p.tr
            && cl == p.cl && ce == p.ce && cr == p.cr
            && bl == p.bl && bo == p.bo && br == p.br;
        }
        
        public int hashCode() {
            return Objects.hash(tl, to, tr, cl, ce, cr, bl, bo, br);
        }
    
        @Override
        public String toString() {
            return tl + "  " + to + "  " + tr + "\n" +
                   cl + "  " + ce + "  " + cr + "\n" +
                   bl + "  " + bo + "  " + br + "\n";
        }
    }
}
