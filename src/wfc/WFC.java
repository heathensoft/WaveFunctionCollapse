package wfc;

import wfc.utility.BitSet;
import wfc.utility.HeapSet;
import wfc.utility.IntStack;
import wfc.utility.PriorityQueue;

import java.nio.IntBuffer;

/**
 *
 * Notes:
 *
 * "Option / Pattern / State" is used interchangeably.
 * It is the index of a specific 3x3 pattern.
 * "Value" on the other hand refer to the actual value in the output.
 * In a 3x3 Pattern, the "Value" is the center tile.
 *
 * Directions are represented by [0,1,2,3] -> [N,E,S,W]
 *
 * @author Frederik Dahl
 * 05/05/2023
 */


public class WFC {
    
    
    private final Patterns patterns; // All possible patterns used to generate output
    private final int[][] cardinals_array; // Directional offset array
    private final int[] negate_direction; // maps directions to their opposites
    private int noise_position; // internal position of the random generator
    private int noise_seed; // seed of the random generator
    
    
    /**
     * @param training_input input to process
     * @param seed seed used by internal random generator
     * @param allow_pattern_permutations allow pattern rotations and reflections
     */
    public WFC(int[][] training_input, int seed, boolean allow_pattern_permutations) {
        patterns = new Patterns(training_input,allow_pattern_permutations);
        cardinals_array = new int[][]{{0,1},{1,0},{0,-1},{-1,0}}; // [N,E,S,W]
        negate_direction = new int[]{ 2, 3, 0, 1 }; // [S,W,N,E]
        noise_position = 1337;
        noise_seed = seed;
    }
    
    /**
     * Runs wave function collapse and stores the result in output.
     * output is continuously updated and could be rendered while running
     * if you are processing an image.
     * @param output storage
     * @param failure_limit tolerated collisions before termination
     * @param wrap_around If you want the results edges connected
     * @return true if successful
     */
    public boolean generate(int[][] output, int failure_limit, boolean wrap_around) {
        
        boolean success;
        int collisions = 0;
        int width = output[0].length;
        int height = output.length;
        /*
            IntBuffers: Not a very clear way of doing this. But ok.
            These buffers are used throughout the algorithm
            to store cell pattern indexes. Their capacity is the total number of patterns.
            And we "borrow" them from the Patterns object, where they are initialized.
            Just make sure they don't override each other.
         */
        IntBuffer TMP_BUFFER_0 = patterns.borrow_buffer_0();
        IntBuffer TMP_BUFFER_1 = patterns.borrow_buffer_1();
        
        Cell[][] cells = new Cell[height][width];
        IntStack propagation_stack = new IntStack(64);
        // HeapSet is a priority queue optimized for updating the order of its elements
        HeapSet<Cell> priority_queue = new HeapSet<>(width * height);
        // Regular priority queue, no ordering updates of elements. Just Push and Pop
        // Sorts adjacent cells by entropy, before propagating by least entropy
        PriorityQueue<AdjacentCell> adjacent_queue = new PriorityQueue<>(4);
    
        while (collisions != failure_limit) {
        
            if (collisions == 0) {
                // initialize the WFC, Populate Cell[][] and priority queue
                for (int r = 0; r < height; r++) {
                    for (int c = 0; c < width; c++) {
                        Cell cell = new Cell(c, r, patterns);
                        cells[r][c] = cell;
                        cell.introduce_noise_to_entropy(white_noise() * 0.0001f);
                        priority_queue.set(cell);
                    }
                }
            } else { // Reached a contradiction and we must reset the WFC
                while (priority_queue.notEmpty()) priority_queue.pop();
                while (adjacent_queue.notEmpty()) adjacent_queue.dequeue();
                propagation_stack.clear();
                for (int r = 0; r < height; r++) {
                    for (int c = 0; c < width; c++) {
                        Cell cell = cells[r][c];
                        cell.reset(patterns);
                        cell.introduce_noise_to_entropy(white_noise() * 0.0001f);
                        priority_queue.set(cell);
                    }
                }
            }
            success = true;
        
            collision:
        
            while (priority_queue.notEmpty()) {
                
                Cell popped_cell = priority_queue.pop();
                int remaining = popped_cell.remaining_options();
                if (remaining <= 0) throw new IllegalStateException("Unreachable");
                if (remaining == 1) {
                    int pattern = popped_cell.collect_options(TMP_BUFFER_0).get(0);
                    output[popped_cell.y()][popped_cell.x()] = patterns.value_of(pattern);
                } else { // remaining > 1 atp.
                    
                    // Collapse the Cell with the least amount of entropy,
                    // and push it onto the propagation stack
                    int pattern = collapse_cell(popped_cell,TMP_BUFFER_0);
                    output[popped_cell.y()][popped_cell.x()] = patterns.value_of(pattern);
                    // instead of the Cell object itself, we push its position
                    // and the direction it was propagated FROM.
                    // We don't want to propagate back in the opposite direction.
                    // We could but it's not necessary
                    propagation_stack.push(4); // 4 = NO_DIRECTION
                    propagation_stack.push(popped_cell.position());
                
                    while (!propagation_stack.isEmpty()) {
                        
                        int position = propagation_stack.pop();
                        int from_direction = propagation_stack.pop();
                        int cx = position & 0xFFFF;
                        int cy = (position >> 16) & 0xFFFF;
                        Cell current_cell = cells[cy][cx];
                        IntBuffer options = current_cell.collect_options(TMP_BUFFER_1);
                    
                        for (int to_direction = 0; to_direction < 4; to_direction++) {
                            if (from_direction == to_direction) continue;
                            int[] dir_offset = cardinals_array[to_direction];
                            int nx = cx + dir_offset[0];
                            int ny = cy + dir_offset[1];
                            if (wrap_around) {
                                nx = nx < 0 ? (width - 1) : nx % width;
                                ny = ny < 0 ? (height - 1) : ny % height;
                            } else if (nx < 0 || nx == width || ny < 0 || ny == height) continue;
                            Cell adjacent_cell = cells[ny][nx];
                            if (adjacent_cell.remaining_options() == 1) continue;
                        
                            // sort adjacent cells by entropy (The one with the least will be propagated first)
                            BitSet propagation_mask = patterns.valid_adjacent_patterns(options, to_direction);
                            if (propagate_cell(adjacent_cell, propagation_mask,TMP_BUFFER_0)) {
                                remaining = adjacent_cell.remaining_options();
                                if (remaining == 0) {
                                    collisions++;
                                    success = false;
                                    break collision;
                                }
                                adjacent_queue.enqueue(wrap(adjacent_cell, negate_direction[to_direction]));
                                priority_queue.set(adjacent_cell);
                            }
                        }
                        while (adjacent_queue.notEmpty()) {
                            AdjacentCell wrapper = adjacent_queue.dequeue();
                            propagation_stack.push(wrapper.direction());
                            propagation_stack.push(wrapper.cell().position());
                        }
                    }
                }
            }
            if (success) return true;
        }
        return false;
    }
    
    
    public void set_noise_seed(int seed) { this.noise_seed = seed; }
    
    public void reset_noise_position() { noise_position = 1337; }
    
    
    /**
     * Propagates the cell. The mask is the set of the updated possible options
     * this Cell can be. If the Cell's remaining options were reduced, A new entropy
     * is calculated.
     * @param cell cell to propagate
     * @param propagation_mask possible states of the cell
     * @param buffer temp internal buffer, used to collect the options from the bitset
     * @return true if options were reduced as a consequence of propagation
     */
    private boolean propagate_cell(Cell cell, BitSet propagation_mask, IntBuffer buffer) {
        int remaining_prior = cell.remaining_options();
        BitSet options_mask = cell.options_mask();
        options_mask.and(propagation_mask);
        cell.set_remaining_options(options_mask.cardinality());
        int remaining = cell.remaining_options();
        if (remaining < remaining_prior) {
            if (remaining > 0) {
                if (remaining == 1) {
                    cell.set_entropy(0.0f);
                } else { int W = 0;
                    cell.collect_options(buffer);
                    int buffer_position = buffer.position();
                    int buffer_limit = buffer.limit();
                    for (int i = buffer_position; i < buffer_limit; i++) {
                        W += patterns.frequency_of(buffer.get(i));
                    } double S = 0;
                    for (int i = buffer_position; i < buffer_limit; i++) {
                        int w = patterns.frequency_of(buffer.get(i));
                        S += w * Math.log(w);
                    } cell.set_entropy((float) (Math.log(W) - S / W));
                    // log(W) - (w1*log(w1) + w2*log(w2) + ... + wn*log(wn)) / W
                    cell.introduce_noise_to_entropy(white_noise() * 0.0001f);
                }
            } return true;
        } return false;
    }
    
    /**
     * Collapses the cell. Selects randomly (weighted) one of its remaining options,
     * and commits the cell to that option / pattern / state.
     * @param cell Cell to collapse
     * @param buffer temp internal buffer, used to collect the options from the bitset
     * @return The option the Cell collapsed to
     */
    private int collapse_cell(Cell cell, IntBuffer buffer) {
        cell.collect_options(buffer);
        int buffer_position = buffer.position();
        int buffer_limit = buffer.limit();
        int accumulated = 0;
        int sum_weight = 0;
        for (int i = buffer_position; i < buffer_limit; i++) {
            sum_weight += patterns.frequency_of(buffer.get(i));
        } int rand = positive_integer(sum_weight);
        for (int i = buffer_position; i < buffer_limit; i++) {
            int pattern = buffer.get(i);
            accumulated += patterns.frequency_of(buffer.get(i));
            if (rand <= accumulated) {
                cell.commit_to_option(pattern);
                return pattern;
            } // Exception never thrown
        } throw new IllegalStateException("Unreachable");
    }
    
    private int positive_integer(int max) { return positive_integer() % (max + 1); }
    
    private int positive_integer() { return hash(++noise_position, noise_seed) & 0x7FFF_FFFF;}
    
    private float white_noise() { return positive_integer() / (float) 0x7FFF_FFFF; }
    
    private int hash(int value, int seed) {
        long m = (long) value & 0xFFFFFFFFL;
        m *= 0xB5297AAD;
        m += seed;
        m ^= (m >> 8);
        m += 0x68E31DA4;
        m ^= (m << 8);
        m *= 0x1B56C4E9;
        m ^= (m >> 8);
        return (int) m;
    }
    
    private void print(int[][] array) {
        int rows = array.length;
        int cols = array[0].length;
        StringBuilder builder = new StringBuilder();
        for (int r = 0; r < rows; r++) {
            builder.append("\n");
            for (int c = 0; c < cols; c++)
                builder.append(array[r][c]).append("  ");
        } System.out.println(builder);
    }
    
    private static final class Cell implements Comparable<Cell> {
        private final BitSet options; // remaining options represented by a set of bits
        private final int position; // position of cell (16 bit x/ 16 bit y)
        private int remaining; // options remaining count (number of set bits in options)
        private float entropy; // the calculated shannon entropy (0 when the cell is collapsed)
        Cell(int x, int y, Patterns patterns) {
            this.position = ((x & 0xFFFF) | (y & 0xFFFF) << 16);
            this.options = new BitSet(patterns.super_position());
            this.remaining = patterns.count();
            this.entropy = patterns.super_entropy();
        }
        void reset(Patterns patterns) {
            if (remaining != patterns.count()) {
                options.or(patterns.super_position());
                remaining = patterns.count();
                entropy = patterns.super_entropy();
            }
        }
        void set_entropy(float entropy) { this.entropy = entropy; }
        void introduce_noise_to_entropy(float noise) { entropy += noise; }
        void set_remaining_options(int remaining) { this.remaining = remaining; }
        void commit_to_option(int option) {
            options.clear();
            options.set(option);
            entropy = 0.0f;
            remaining = 1;
        }
        IntBuffer collect_options(IntBuffer dst) {
            options.indices(dst.clear(), remaining);
            return dst.flip();
        }
        BitSet options_mask() { return options; }
        int remaining_options() { return remaining; }
        int position() { return position; }
        float entropy() { return entropy; }
        int x() { return position & 0xFFFF; }
        int y() { return (position >> 16) & 0xFFFF; }
        public int compareTo(Cell o) {
            return Float.compare(o.entropy,entropy);
        }
    }
    
     /*
        Wrapper object used to sort adjacent propagated Cells, before pushing
        them onto the propagation stack. The Cell with the highest entropy,
        will be pushed first. (So that the one with the least entropy will be popped first)
     */
    
    private final static class AdjacentCell implements Comparable<AdjacentCell> {
        private Cell cell;
        private int direction;
        AdjacentCell set(Cell cell, int direction) {
            this.cell = cell;
            this.direction = direction;
            return this;
        } Cell cell() { return cell; }
        int direction() { return direction; }
        public int compareTo(AdjacentCell o) {
            return o.cell.compareTo(cell);
        }
    }
    
    private final AdjacentCell[] _wrp_objects = new AdjacentCell[] {
            new AdjacentCell(), new AdjacentCell(),
            new AdjacentCell(), new AdjacentCell()
    };
    
    // "pool" of wrapper objects (never more than 4 used at any given time)
    private int _next_wrp_obj = -1;
    private AdjacentCell wrap(Cell cell, int direction) {
        _next_wrp_obj = (++_next_wrp_obj) & 0x7FFF_FFFF;
        return _wrp_objects[_next_wrp_obj %4].set(cell,direction);
    }
    
}
