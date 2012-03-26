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
	public volatile PHNode<T> root;
	private final AtomicStampedReference<WriteDescriptor> descriptor;
	private final AtomicInteger size;
	
	private abstract class WriteDescriptor {
	  private volatile boolean pending;

	  private WriteDescriptor() {
	    pending = true;
	  }

	  public abstract void executeThis();
	  
	  public void execute() {
	    if (pending) {
	      executeThis();
	      pending = false;
	    }
	  }
	}
	
	private class WriteDescriptorUpdateRoot extends WriteDescriptor {
	  private final PHNode newRoot;
	 
	  public WriteDescriptorUpdateRoot(PHNode newRoot) {
	    this.newRoot = newRoot;
	  }

	  public void executeThis() {
	    newRoot.graphNode.phNode = newRoot;
	    root = newRoot;
	  }
	}

	private class WriteDescriptorNewRoot extends WriteDescriptor {
	  private final PHNode newRoot;
	  private final PHNode oldRoot;
	  private final PHNode newLocForOldRoot;
	  
	  public WriteDescriptorNewRoot(PHNode newRoot, PHNode oldRoot, PHNode newLocForOldRoot) {
	    this.newRoot = newRoot;
	    this.oldRoot = oldRoot;
	    this.newLocForOldRoot = newLocForOldRoot;
	  }

	  public void executeThis() {
	    root = newRoot;
	    oldRoot.graphNode.phNode = newLocForOldRoot;
	  }
	}

	private class EmptyDescriptor extends WriteDescriptor {
	  public EmptyDescriptor() {
	    super.pending = false;
	  }

	  public void executeThis() {
	    
	  }
	}
	
	/**
	 * Creates a new pairing heap.
	 */
	public LFPairingHeap(PHNode<T> root) {
		root.parent = null;
		this.root = root;
		this.size = new AtomicInteger(1);
		this.descriptor = new AtomicStampedReference(new EmptyDescriptor(), 0);
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
		  WriteDescriptor d = descriptor.get(expectedStamp);
		  d.execute();
		  expectedRoot = root;

		  // Check if we can just insert this as a child of the root.
		  if (e.distance >= expectedRoot.distance) {
		    e.parent = expectedRoot.graphNode;
		    expectedRoot.subHeaps.add(e.graphNode);
		    break;
		  }
			
		  // Otherwise, make a new root.
		  PHNode<T> expectedRootClone = expectedRoot.clone();
		  e = merge(expectedRootClone, e);
		  WriteDescriptor dNew = new WriteDescriptorNewRoot(e, expectedRoot, expectedRootClone);
		  if (descriptor.compareAndSet(d, dNew, expectedStamp[0],
					 expectedStamp[0] + 1)) {
		    break;
		  }
		  else
		    e.subHeaps.remove(expectedRoot.graphNode);
		}
		descriptor.getReference().execute();
		size.getAndIncrement();
	}
	
	// Not lock-free!
	public PHNode<T> deleteMin() {
		PHNode<T> ret = root;

		size.getAndDecrement();
		ret.graphNode.inHeap = false;

		if (ret.subHeaps.size() == 0)
		  root = null;
		else
		  root = merger(ret.subHeaps);

		return ret;
	}

	public int size() {
	  return size.get();
	}
	
	public PHNode<T> search(int id) {
	  return search(id, root);
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
	  while (key == root) {
	    expectedRoot = root;
	    if (key != expectedRoot)
	      break;
	    WriteDescriptor d = descriptor.get(expectedStamp);
	    d.execute();
	    PHNode<T> newRoot = key.clone();
	    newRoot.distance = newValue;
	    WriteDescriptor newD = new WriteDescriptorUpdateRoot(newRoot);
	    
	    if (descriptor.compareAndSet(d, newD, expectedStamp[0], expectedStamp[0] + 1)) {
	      d.execute();
	      return;
	    }
	    else
	      d.execute();
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
	    WriteDescriptor d = descriptor.get(expectedStamp);
	    d.execute();
	    expectedRoot = root;

	    // Case 3: Target is greater than the current root.
	    if (expectedRoot.distance <= key.distance) {
	      key.parent = expectedRoot.graphNode;
	      expectedRoot.subHeaps.add(key.graphNode);
	      return;
	    }
	    
	    // Case 4: Update the root.
	    key.parent = null;
	    PHNode<T> oldRootClone = expectedRoot.clone();
	    PHNode<T> newRoot = merge(oldRootClone, key);
	    WriteDescriptor newD = new WriteDescriptorNewRoot(newRoot, expectedRoot, oldRootClone);
	    if (descriptor.compareAndSet(d, newD, expectedStamp[0], expectedStamp[0] + 1)) {
	      d.execute();
	      return;
	    }
	    else {
	      d.execute();
	      key.subHeaps.remove(expectedRoot.graphNode);
	    }
	  }
	}
}