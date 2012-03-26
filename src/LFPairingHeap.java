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
	public final AtomicStampedReference<PHNode<T>> root;
	private final AtomicInteger size;

	/**
	 * Creates a new pairing heap.
	 */
	public LFPairingHeap(PHNode<T> root) {
		root.parent = null;
		this.root = new AtomicStampedReference<PHNode<T>>(root, 0);
		this.size = new AtomicInteger(1);
	}

	/**
	 * Merges two heaps together. The larger of rhs of lhs will become the child
	 * of the smaller. Makes a stateful change to which ever the smaller of lhs
	 * and rhs are.
	 * 
	 * @return the smallest node (which contains the larger as a subheap)
	 */
	private PHNode<T> merge(PHNode<T> lhs, PHNode<T> rhs) {
		PHNode<T> small;
		PHNode<T> large;
		
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

		small.subHeaps.add(large.graphNode);
		large.parent = small.graphNode;

		return small;
	}

	private PHNode<T> merger(ConcurrentLinkedQueue<GraphNode<T>> list) {
		/*if (list.size() == 0)
			return null;
		if (list.size() == 1)
			return list.poll();

		return merge(merge(list.poll(), list.poll()), merger(list));*/
		Stack<GraphNode<T>> list2 = new Stack<GraphNode<T>>();
		while (true) {
			GraphNode<T> one = list.poll();
			GraphNode<T> two = list.poll();
			if (one == null)
				break;
			if (two == null) {
				one.phNode.parent = null;
				list2.add(one);
				break;
			}
			PHNode<T> winner = merge(one.phNode, two.phNode);
			winner.parent = null;
			list2.add(winner.graphNode);
		}
		GraphNode<T> ret = list2.pop();
		while (list2.size() > 0) {
			ret = merge(ret.phNode, list2.pop().phNode).graphNode;
		}
		return ret.phNode;
	}

	/**
	 * Inserts e into the heap.
	 */
	public void insert(PHNode<T> e) {
		PHNode<T> expectedRoot;
		int[] expectedStamp = new int[1];
		while (true) {
			expectedRoot = root.get(expectedStamp);
			// Check if we can just insert this as a child of the root.
			if (e.distance >= expectedRoot.distance) {
				e.parent = expectedRoot.graphNode;
				expectedRoot.subHeaps.add(e.graphNode);
				break;
			}
			
			// Otherwise, make a new root.
			PHNode<T> newRoot = expectedRoot.clone();
			newRoot = merge(newRoot, e);
			if (root.compareAndSet(expectedRoot, newRoot, expectedStamp[0],
					expectedStamp[0] + 1)) {
				newRoot.graphNode.phNode = newRoot;
				break;
			}
			else if (newRoot == e)
				e.subHeaps.remove(expectedRoot.graphNode);
			else
				expectedRoot.subHeaps.remove(e.graphNode);
		}
		size.getAndIncrement();
	}

	// Not lock-free!
	public PHNode<T> deleteMin() {
		size.getAndDecrement();
		PHNode<T> ret = root.getReference();
		int stamp = root.getStamp();
		ret.graphNode.inHeap = false;
		if (ret.subHeaps.size() == 0)
			root.set(null, stamp + 1);
		else
			root.set(merger(ret.subHeaps), stamp + 1);
		//System.out.println("DeleteMin: " + ret);
		return ret;
	}

	public int size() {
		return size.get();
	}
	
	public PHNode<T> search(int id) {
		return search(id, root.getReference());
	}
	
	private PHNode<T> search(int id, PHNode<T> start) {
		if (start.graphNode.id == id)
			return start;
		for(GraphNode<T> node : start.subHeaps) {
			PHNode<T> res = search(id, node.phNode);
			if (res != null)
				return res;
		}
		return null;
	}
	
	public void decreaseKey(PHNode<T> key, int newValue) {
		if (!key.graphNode.inHeap || key.distance == newValue)
			return;
		PHNode<T> expectedRoot;
		int[] expectedStamp = new int[1];
		// Case 1: Decreasing the root
		while (key == root.getReference()) {
			expectedRoot = root.get(expectedStamp);
			if (key != expectedRoot)
				break;
			PHNode<T> newRoot = key.clone();
			newRoot.distance = newValue;
			if (root.compareAndSet(expectedRoot, newRoot, expectedStamp[0], expectedStamp[0] + 1)) {
				newRoot.graphNode.phNode = newRoot;
				return;
			}
		}
		
		// Update the weight.
		key.distance = newValue;
		
		if (key.parent == null)
			System.out.println(key + " " + key.hashCode());
		
		// Case 2: Target is still greater than its parent.
		// (No changes to the tree structure needed in this case.)
		if (key.parent.phNode.distance <= key.distance)
			return;
			
		// Delink the node from its parent.
		key.parent.phNode.subHeaps.remove(key.graphNode);
		
		while (true) {
			// Case 3: Target is greater than the current root.
			expectedRoot = root.get(expectedStamp);
			if (expectedRoot.distance <= key.distance) {
				key.parent = expectedRoot.graphNode;
				expectedRoot.subHeaps.add(key.graphNode);
				return;
			}
			
			// Case 4: Update the root.
			key.parent = null;
			PHNode<T> newRootClone = expectedRoot.clone();
			PHNode<T> newRoot = merge(newRootClone, key);
			if (root.compareAndSet(expectedRoot, newRoot, expectedStamp[0], expectedStamp[0] + 1)) {
				expectedRoot.graphNode.phNode = newRootClone;
				return;
			}
			else
				key.subHeaps.remove(expectedRoot.graphNode);
		}
	}
}