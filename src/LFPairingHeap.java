import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * A lock-free pairing heap of weightings on a type T. Right now, only
 * decreaseKey() and insert() are lock-free.
 * 
 * @author Charles Newton
 */
public class LFPairingHeap<T> {
	private volatile Weighted<T> ele;
	private ConcurrentLinkedDeque<LFPairingHeap<T>> subHeaps;
	private AtomicStampedReference<LFPairingHeap<T>> root;
	private volatile LFPairingHeap<T> parent;

	public class NewHeapAndValue {
		public Weighted<T> value;
		public LFPairingHeap<T> heap;

		private NewHeapAndValue(LFPairingHeap<T> heap, Weighted<T> value) {
			this.heap = heap;
			this.value = value;
		}
	}

	/**
	 * Creates a new (empty) pairing heap.
	 */
	public LFPairingHeap() {
		subHeaps = new ConcurrentLinkedDeque<LFPairingHeap<T>>();
		ele = null;
		parent = null;
	}

	/**
	 * Creates a trivial heap containing only one element.
	 */
	private LFPairingHeap(Weighted<T> ele) {
		this();
		this.ele = ele;
	}

	public boolean isEmpty() {
		return ele == null;
	}

	/**
	 * Merges two heaps together. The larger of rhs of lhs will become the child
	 * of the smaller. Makes a stateful change to which ever the smaller of lhs
	 * and rhs are.
	 * 
	 * @return the smallest heap (which now contains both heaps.)
	 */
	private LFPairingHeap<T> merge(LFPairingHeap<T> lhs, LFPairingHeap<T> rhs) {
		// Trivial cases
		if (lhs.isEmpty())
			return rhs;
		if (rhs.isEmpty())
			return lhs;

		LFPairingHeap<T> small;
		LFPairingHeap<T> large;
		if (lhs.ele.compareTo(rhs.ele) <= 0) {
			small = lhs;
			large = rhs;
		} else {
			small = rhs;
			large = lhs;
		}

		small.subHeaps.push(large);
		large.parent = small;

		return small;
	}

	private LFPairingHeap<T> merger(ConcurrentLinkedDeque<LFPairingHeap<T>> list) {
		if (list.size() == 0)
			return new LFPairingHeap<T>();
		if (list.size() == 1)
			return list.pop();

		return merge(merge(list.pop(), list.pop()), merger(list));
	}

	/**
	 * Inserts e into the heap. Returns the key associated with e.
	 */
	public LFPairingHeap<T> insert(Weighted<T> e) {
		LFPairingHeap<T> ret = new LFPairingHeap<T>(e);
		while (true) {
			LFPairingHeap<T> expectedRoot;
			int[] expectedStamp = new int[1];
			expectedRoot = root.get(expectedStamp);
			LFPairingHeap<T> newRoot = new LFPairingHeap<T>(expectedRoot.ele);
			newRoot.parent = expectedRoot.parent;
			newRoot.subHeaps = expectedRoot.subHeaps;
			newRoot = merge(newRoot, ret);
			if (root.compareAndSet(expectedRoot, newRoot, expectedStamp[0],
					expectedStamp[0] + 1)) {

				break;
			}
			else if (newRoot == ret)
				ret.subHeaps.remove(expectedRoot);
			
		}
		return ret;
		// return merge(new LFPairingHeap<T>(e), this);
	}

	public NewHeapAndValue deleteMin() {
		return new NewHeapAndValue(merger(subHeaps), ele);
	}

	public void decreaseKey(LFPairingHeap<T> key, int delta) {
		while (key == root.getReference()) {
			LFPairingHeap<T> expectedRoot;
			int[] expectedStamp = new int[1];
			expectedRoot = root.get(expectedStamp);
			Weighted<T> newWeight = expectedRoot.ele.clone();
			LFPairingHeap<T> newRoot = new LFPairingHeap<T>(newWeight);
			newRoot.subHeaps = expectedRoot.subHeaps;
			if (root.compareAndSet(expectedRoot, newRoot, expectedStamp[0], expectedStamp[0] + 1))
				return;
		}

		// No changes to the tree structure needed in this case.
		if (key.parent.ele.getWeight() < key.ele.getWeight() - delta) {
			key.ele.setWeight(key.ele.getWeight() - delta);
			return;
		}

		// Delink the node from its parent and update its weight.
		key.parent.subHeaps.remove(key);
		key.ele.setWeight(key.ele.getWeight() - delta);

		while (true) {
			LFPairingHeap<T> expectedRoot;
			int[] expectedStamp = new int[1];
			expectedRoot = root.get(expectedStamp);
			LFPairingHeap<T> newRoot = new LFPairingHeap<T>(expectedRoot.ele);
			newRoot.parent = expectedRoot.parent;
			newRoot.subHeaps = expectedRoot.subHeaps;
			newRoot = merge(newRoot, key);
			if (root.compareAndSet(expectedRoot, newRoot, expectedStamp[0], expectedStamp[0] + 1)) {
				
				return;
			}
			// Cleanup our failure.
			else if (newRoot == key)
				key.subHeaps.removeLastOccurrence(expectedRoot);
		}
	}
}