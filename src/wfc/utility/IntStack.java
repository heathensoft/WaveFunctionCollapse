
package wfc.utility;



public class IntStack {
	
	private int[] s;
	private int p;
	
	public IntStack(int cap) {
		if (cap < 0) throw new NegativeArraySizeException("cap < 0: " + cap);
		s = new int[cap];
	}
	
	public IntStack() {
		this(0);
	}
	
	public void makeRoom(int n) {
		ensureCapacity(n + p);
	}
	
	public void ensureCapacity(int size) {
		if (size > s.length) {
			int[] tmp = s;
			s = new int[size];
			System.arraycopy(tmp,0, s,0,p);
		}
	}
	
	public void push(int i) {
		if (p == s.length) {
			int[] tmp = s;
			s = new int[s.length * 2 + 1];
			System.arraycopy(tmp,0, s,0,tmp.length);
		} s[p++] = i;
	}
	
	public void push(int[] i) {
		if (i != null) { makeRoom(i.length);
			for (int v : i) s[p++] = v;
		}
	}
	
	public int pop() {
		return s[--p];
	}
	
	public void fit(int min) {
		int cap = Math.max(Math.max(0,min),p);
		int[] tmp = s;
		s = new int[cap];
		System.arraycopy(tmp,0, s,0, p);
	}
	
	public int[] array() {
		return s;
	}
	
	public void clear() {
		p = 0;
	}
	
	public int size() {
		return p;
	}
	
	public int sizeBytes() {
		return p * Integer.BYTES;
	}
	
	public int capacity() {
		return s.length;
	}
	
	public boolean isEmpty() {
		return p == 0;
	}
	
}