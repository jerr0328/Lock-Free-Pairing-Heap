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
	//public volatile PHNode<T> root;
	private final AtomicStampedReference<WriteDescriptor> descriptor;
	private final AtomicInteger size;
	
	private abstract class WriteDescriptor {
	  public final PHNode<T> root;
	  private volatile boolean pending;

	  private WriteDescriptor(PHNode<T> root) {
		this.root = root;
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
	  private final PHNode<T> oldRoot;
	  
	  public WriteDescriptorUpdateRoot(PHNode<T> oldRoot, PHNode<T> newRoot) {
		super(newRoot);
		this.oldRoot = oldRoot;
	  }

	  public void executeThis() {
	    root.graphNode.phNode.compareAndSet(oldRoot, root);
	  }
	}

	private class WriteDescriptorNewRoot extends WriteDescriptor {
	  private final PHNode<T> newLocForOldRoot;
	  private final PHNode<T> oldRoot;
	  
	  public WriteDescriptorNewRoot(PHNode<T> newRoot, PHNode<T> oldRoot, PHNode<T> newLocForOldRoot) {
	    super(newRoot);
	    this.newLocForOldRoot = newLocForOldRoot;
	    this.oldRoot = oldRoot;
	  }

	  public void executeThis() {
	    newLocForOldRoot.graphNode.phNode.compareAndSet(oldRoot, newLocForOldRoot);
	  }
	}

	private class EmptyDescriptor extends WriteDescriptor {
	  public EmptyDescriptor(PHNode<T> root) {
		super(root);
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
		this.size = new AtomicInteger(1);
		this.descriptor = new AtomicStampedReference<WriteDescriptor>(new EmptyDescriptor(root), 0);
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
				one.phNode.get().parent = null;
				list2.add(one);
				break;
			}
			PHNode<T> winner = merge(one.phNode.get(), two.phNode.get());
			winner.parent = null;
			list2.add(winner.graphNode);
		}
		GraphNode<T> ret = list2.pop();
		while (list2.size() > 0) {
			ret = merge(ret.phNode.get(), list2.pop().phNode.get()).graphNode;
		}
		return ret.phNode.get();
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
		  expectedRoot = d.root;

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
		  else {
		    e.subHeaps.remove(expectedRoot.graphNode);
		    descriptor.getReference().execute();
		  }
		}
		descriptor.getReference().execute();
		size.getAndIncrement();
	}
	
	// Not lock-free!
	public PHNode<T> deleteMin() {
		int[] stamp = new int[1];
		WriteDescriptor d = descriptor.get(stamp);
		PHNode<T> ret = d.root;

		size.getAndDecrement();
		ret.graphNode.inHeap = false;
		PHNode<T> newRoot;
		if (ret.subHeaps.size() == 0)
		  newRoot = null;
		else
		  newRoot = merger(ret.subHeaps);
		descriptor.set(new EmptyDescriptor(newRoot), stamp[0] + 1);
		return ret;
	}

	public int size() {
	  return size.get();
	}
	
	public PHNode<T> search(int id) {
	  return search(id, descriptor.getReference().root);
	}
	
	private PHNode<T> search(int id, PHNode<T> start) {
		if (start.graphNode.id == id)
			return start;
		for(GraphNode<T> node : start.subHeaps) {
			PHNode<T> res = search(id, node.phNode.get());
			if (res != null)
				return res;
		}
		return null;
	}
	
	public void decreaseKey(GraphNode<T> key, int newValue) {
	  if (!key.inHeap || key.phNode.get().distance == newValue)
	    return;
	  
	  PHNode<T> keyLoc;
	  PHNode<T> expectedRoot;
	  int[] expectedStamp = new int[1];
	  
	  // Case 1: Decreasing the root
	  while (key.phNode.get() == descriptor.getReference().root) {
		WriteDescriptor d = descriptor.get(expectedStamp);
	    d.execute();
	    keyLoc = key.phNode.get();
	    if (d.root != keyLoc)
	    	break;
	    PHNode<T> newRoot = keyLoc.clone();
	    newRoot.distance = newValue;
	    WriteDescriptor newD = new WriteDescriptorUpdateRoot(d.root, newRoot);
	    
	    if (descriptor.compareAndSet(d, newD, expectedStamp[0], expectedStamp[0] + 1)) {
	      descriptor.getReference().execute();
	      return;
	    }
	    else
	      descriptor.getReference().execute();
	      
	  }
	  
	  descriptor.getReference().execute();
	  keyLoc = key.phNode.get();
	  
	  // Update the weight.
	  keyLoc.distance = newValue;
		
	  if (keyLoc.parent == null)
	    System.out.println(key + " " + key.hashCode());
		
	  // Case 2: Target is still greater than its parent.
	  // (No changes to the tree structure needed in this case.)
	  if (keyLoc.parent.phNode.get().distance <= keyLoc.distance)
	    return;
	  
	  // Delink the node from its parent.
	  keyLoc.parent.phNode.get().subHeaps.remove(key);
		
	  while (true) {
	    WriteDescriptor d = descriptor.get(expectedStamp);
	    d.execute();
	    
	    keyLoc = key.phNode.get();
	    expectedRoot = d.root;

	    // Case 3: Target is greater than the current root.
	    if (expectedRoot.distance <= keyLoc.distance) {
	      keyLoc.parent = expectedRoot.graphNode;
	      expectedRoot.subHeaps.add(key);
	      return;
	    }
	    
	    // Case 4: Update the root.
	    keyLoc.parent = null;
	    PHNode<T> oldRootClone = expectedRoot.clone();
	    PHNode<T> newRoot = merge(oldRootClone, keyLoc);
	    WriteDescriptor newD = new WriteDescriptorNewRoot(newRoot, expectedRoot, oldRootClone);
	    if (descriptor.compareAndSet(d, newD, expectedStamp[0], expectedStamp[0] + 1)) {
	      descriptor.getReference().execute();
	      return;
	    }
	    else {
	      descriptor.getReference().execute();
	      keyLoc.subHeaps.remove(expectedRoot.graphNode);
	    }
	  }
	}
}