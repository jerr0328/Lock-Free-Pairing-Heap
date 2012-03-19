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
	public final AtomicStampedReference<Node<T>> root;
	private final AtomicInteger size;
	
	/**
	 * Creates a new pairing heap.
	 */
	public LFPairingHeap(Node<T> root) {
		root.parent = null;
		this.root = new AtomicStampedReference<Node<T>>(root, 0);
		this.size = new AtomicInteger(1);
	}

	/**
	 * Merges two heaps together. The larger of rhs of lhs will become the child
	 * of the smaller. Makes a stateful change to which ever the smaller of lhs
	 * and rhs are.
	 * 
	 * @return the smallest node (which contains the larger as a subheap)
	 */
	private Node<T> merge(Node<T> lhs, Node<T> rhs) {
		Node<T> small;
		Node<T> large;
		
		if (lhs == null)
			return rhs;
		else if (rhs == null)
			return lhs;
		
		if (lhs.distance <= rhs.distance) {
			small = lhs;
			large = rhs;
		} else {
			small = rhs;
			large = lhs;
		}

		small.subHeaps.add(large);
		large.parent = small;

		return small;
	}

	private Node<T> merger(ConcurrentLinkedQueue<Node<T>> list) {
		/*if (list.size() == 0)
			return null;
		if (list.size() == 1)
			return list.poll();

		return merge(merge(list.poll(), list.poll()), merger(list));*/
		Stack<Node<T>> list2 = new Stack<Node<T>>();
		while (true) {
			Node<T> one = list.poll();
			Node<T> two = list.poll();
			if (one == null)
				break;
			if (two == null) {
				list2.add(one);
				break;
			}
			list2.add(merge(one, two));
		}
		Node<T> ret = list2.pop();
		while (list2.size() > 0)
			ret = merge(ret, list2.pop());

		return ret;
	}

	/**
	 * Inserts e into the heap.
	 */
	public void insert(Node<T> e) {
		Node<T> expectedRoot;
		int[] expectedStamp = new int[1];
		while (true) {
			expectedRoot = root.get(expectedStamp);
			// Check if we can just insert this as a child of the root.
			if (e.distance > expectedRoot.distance) {
				e.parent = expectedRoot;
				expectedRoot.subHeaps.add(e);
				break;
			}
			
			// Otherwise, make a new root.
			Node<T> newRoot = expectedRoot.clone();
			newRoot = merge(newRoot, e);
			if (root.compareAndSet(expectedRoot, newRoot, expectedStamp[0],
					expectedStamp[0] + 1))
				break;
			else if (newRoot == e)
				e.subHeaps.remove(expectedRoot);
			else
				expectedRoot.subHeaps.remove(e);
		}
		size.getAndIncrement();
	}

	// Not lock-free!
	public Node<T> deleteMin() {
		size.getAndDecrement();
		Node<T> ret = root.getReference();
		int stamp = root.getStamp();
		ret.inHeap = false;
		if (ret.subHeaps.size() == 0)
			root.set(null, stamp + 1);
		else {
			root.set(merger(ret.subHeaps), stamp + 1);
			root.getReference().parent = null;
		}
		return ret;
	}

	public int size() {
		return size.get();
	}
	
	public boolean search(int id) {
		return search(id, root.getReference());
	}
	
	private boolean search(int id, Node<T> start) {
		if (start.id == id)
			return true;
		for(Node<T> node : start.subHeaps) {
			if (search(id, node))
				return true;
		}
		return false;
	}
	
	public void decreaseKey(Node<T> key, int newValue) {
		if (!key.inHeap)
			return;
		Node<T> expectedRoot;
		int[] expectedStamp = new int[1];
		// Case 1: Decreasing the root
		while (key == root.getReference()) {
			expectedRoot = root.get(expectedStamp);
			if (key != expectedRoot)
				break;
			Node<T> newRoot = key.clone();
			newRoot.distance = newValue;
			if (root.compareAndSet(expectedRoot, newRoot, expectedStamp[0], expectedStamp[0] + 1))
				return;
		}
		
		// Update the weight.
		key.distance = newValue;
		
		// Case 2: Target is still greater than its parent.
		// (No changes to the tree structure needed in this case.)
		if (key.parent != null) {
			if (key.parent.distance <= key.distance)
				return;
			
			// Delink the node from its parent.
			key.parent.subHeaps.remove(key);
		}
		while (true) {
			// Case 3: Target is greater than the current root.
			expectedRoot = root.get(expectedStamp);
			if (expectedRoot.distance <= key.distance) {
				key.parent = expectedRoot;
				expectedRoot.subHeaps.add(key);
				return;
			}
			
			// Case 4: Update the root.
			key.parent = null;
			Node<T> newRoot = expectedRoot.clone();
			newRoot = merge(newRoot, key);
			if (root.compareAndSet(expectedRoot, newRoot, expectedStamp[0], expectedStamp[0] + 1))
				return;
			// Cleanup our failure.
			else if (newRoot == key) {
				key.subHeaps.remove(expectedRoot);
			}
			else {
				expectedRoot.subHeaps.remove(key);
			}
		}
	}
}