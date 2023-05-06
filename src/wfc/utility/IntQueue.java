package wfc.utility;

import java.util.Collection;
import java.util.EmptyStackException;

/**
 * Simple auto-growing circular queue structure for primitives.
 *
 * @author Frederik Dahl
 * 30/05/2022
 */


public class IntQueue {
    
    private int[] q;
    private int f,r,p;
    
    public IntQueue(int capacity) {
        q = new int[Math.max(0,capacity)];
    }
    
    public IntQueue() {
        this(0);
    }
    
    /**
     * Ensure space for n additional elements. This is useful before enqueuing, as the underlying
     * array would only need to "grow" once instead of potentially multiple times.
     * @param n additional elements
     */
    public void makeRoom(int n) {
        ensureCapacity(n + p);
    }
    
    public void ensureCapacity(int size) {
        if (size > q.length) {
            int[] tmp = q;
            q = new int[size];
            if (isEmpty()) return;
            for (int v = 0; v < p; v++)
                q[v] = tmp[(f+v) % tmp.length];
            r = p; f = 0;
        }
    }
    
    public void enqueue(int i) {
        if (p == q.length) {
            int[] tmp = q;
            q = new int[p * 2 + 1];
            for (int v = 0; v < p; v++)
                q[v] = tmp[(f+v) % p];
            r = p; f = 0;
        } q[r] = i;
        r = (r+1) % q.length;
        p++;
    }
    
    /**
     * Also calls method makeRoom, before enqueuing
     * @param i the array to enqueue
     */
    public void enqueue(int[] i) {
        if (i != null) { makeRoom(i.length);
            for (int v : i) enqueue(v);
        }
    }
    
    /**
     * Also calls method makeRoom, before enqueuing
     * @param c the collection to enqueue
     */
    public void enqueue(Collection<Integer> c) {
        if (c != null) { makeRoom(c.size());
            for (Integer i : c) enqueue(i);
        }
    }
    
    /**
     * Throws EmptyStackException. Use with: isEmpty()
     * @return top element value
     */
    
    public int dequeue() {
        if (p == 0) throw new EmptyStackException();
        int v = q[f];
        if (--p == 0) f = r = p;
        else f = (f+1) % q.length;
        return v;
    }
    
    /**
     * Fit underlying array to match its size.
     */
    public void fit(int min) {
        int size = Math.max(p,min);
        if (q.length > size) {
            int n = p;
            int[] tmp = new int[size];
            for (int i = 0; i < n; i++) {
                tmp[i] = dequeue();
            } q = tmp; p = n;
        }
    }
    
    /**
     * @return raw array (could be circular);
     */
    public int[] array() {
        return q;
    }
    
    public void clear() {
        f = r = p = 0;
    }
    
    public int size() {
        return p;
    }
    
    public int sizeBytes() {
        return p * Integer.BYTES;
    }
    
    public int capacity() {
        return q.length;
    }
    
    public boolean isEmpty() {
        return p == 0;
    }
    
}
