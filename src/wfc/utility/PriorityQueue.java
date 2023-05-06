package wfc.utility;



/**
 * @author Frederik Dahl
 * 18/04/2023
 */


public class PriorityQueue<T extends Comparable<T>> {

    private T[] nodes;
    private int count;

    @SuppressWarnings("unchecked")
    public PriorityQueue(int capacity) {
        capacity = nextPowerOfTwo(capacity);
        nodes = (T[]) new Comparable[capacity];
    }

    public void enqueue(T item) {
        if (item == null) throw new IllegalArgumentException("null arg");
        if (count == nodes.length) grow();
        nodes[count] = item;
        up(count++);
    }

    public T dequeue() {
        T item = nodes[0];
        nodes[0] = nodes[--count];
        nodes[count] = null;
        if (count > 0) down(0);
        return item;
    }

    public boolean remove(T item, boolean identity) {
        if (count > 0) {
            for (int i = 0; i < count; i++) {
                T listed = nodes[i];
                if (identity) {
                    if (listed == item) {
                        T moved = nodes[--count];
                        nodes[count] = null;
                        if (moved != listed){
                            nodes[i] = moved;
                            down(i);
                        } return true;
                    }
                } else if (listed.equals(item)) {
                    T moved = nodes[--count];
                    nodes[count] = null;
                    if (moved != listed){
                        nodes[i] = moved;
                        down(i);
                    } return true;
                }
            }
        } return false;
    }

    private T peak() {
        return nodes[0];
    }

    public int size() {
        return count;
    }

    public boolean isEmpty () {
        return count == 0;
    }

    public boolean notEmpty () {
        return count > 0;
    }

    private void up(int index) {
        T node = nodes[index];
        while (index > 0) {
            int parent_index = parent(index);
            T parent = nodes[parent_index];
            if (node.compareTo(parent) > 0) {
                nodes[index] = parent;
                index = parent_index;
            } else break;
        } nodes[index] = node;
    }

    private void down(int index) {
        final T node = nodes[index];
        while (true) {
            int left_index = 1 + (index * 2);
            int right_index = left_index + 1;
            if (left_index >= count) break;
            if (right_index < count) {
                T right_node = nodes[right_index];
                T left_node = nodes[left_index];
                if (right_node.compareTo(left_node) > 0) {
                    if (node.compareTo(right_node) >= 0) break;
                    nodes[index] = right_node;
                    index = right_index;
                } else {
                    if (node.compareTo(left_node) >= 0) break;
                    nodes[index] = left_node;
                    index = left_index;
                }
            } else {
                T left_node = nodes[left_index];
                if (node.compareTo(left_node) >= 0) break;
                nodes[index] = left_node;
                index = left_index;
            }
        } nodes[index] = node;
    }

    @SuppressWarnings("unchecked")
    private void grow() {
        T[] old_nodes = nodes;
        nodes = (T[])new Comparable[count << 1];
        System.arraycopy(old_nodes, 0, nodes, 0, count);
    }

    private int nextPowerOfTwo(int value) {
        if (value-- == 0) return 1;
        value |= value >>> 1;
        value |= value >>> 2;
        value |= value >>> 4;
        value |= value >>> 8;
        value |= value >>> 16;
        return value + 1;
    }

    private int parent(int index) {
        return (index - 1) / 2;
    }

    private int right(int index) {
        return 2 + (index * 2);
    }

    private int left(int index) {
        return 1 + (index * 2);
    }
}
