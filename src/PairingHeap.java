import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;


public class PairingHeap<T> {

	AtomicReference<PairingHeapNode<T>> root;
	
	public PairingHeap(Weighted<T> v)
	{
		PairingHeapNode<T> add = new PairingHeapNode<T>(this,new AtomicReference<PairingHeapNode<T>>(null),new AtomicReference<PairingHeapNode<T>>(null),new AtomicReference<PairingHeapNode<T>>(null),v);
		root = new AtomicReference<PairingHeapNode<T>>(add);
	}
	
	public Weighted<T> returnMin()
	{
		return root.get().getValue();
	}
	
	/**
	 * Link two Pairing Heaps together. 
	 * 
	 * TODO: Update parent reference of added root
	 * 
	 * @param other
	 */
	public void link(PairingHeap<T> other)
	{
		//Lower tree becomes the right most child
		while(true)
		{
			if(other.root.get().getValue().getWeight() > root.get().getValue().getWeight())
			{
				//add this other pairing heap as a child of our root
				//find the rightmost child node to append to
				AtomicReference<PairingHeapNode<T>> sw = root.get().getLeftChild();
				if(sw.get() != null)
				{
					while(sw.get().getRightSibling().get() != null)
					{
						sw = sw.get().getRightSibling();
					}
					
					PairingHeapNode<T> add = other.root.get();
					if(!sw.get().getRightSibling().compareAndSet(null, add)) continue;
					else break;
				}
				else{// no children, add first child
					if(!sw.compareAndSet(null, other.root.get())) continue;
					else break;
				}
			}
			else{ //link the other way
				other.link(this);
				root = other.root; // grab the new heap root (should be atomic?)
				break;
			}
		}
	}
	
	/**
	 * Search for the atomic reference matching the provided Weighted parameter. 
	 * 
	 * TODO: Pruning - Stop searching once all remaining children are higher value than val
	 * 
	 * @param val
	 * @param start Should be the root of the heap if searching the complete heap
	 * @return
	 */
	private AtomicReference<PairingHeapNode<T>> find(Weighted<T> val, AtomicReference<PairingHeapNode<T>> start)
	{		
		//Queue of nodes to search through
		//For each node in the queue, check the node, all its right siblings, and add all left children to the queue
		Vector<AtomicReference<PairingHeapNode<T>>> checks = new Vector<AtomicReference<PairingHeapNode<T>>>();
		checks.add(start);
		while(checks.size() > 0)
		{			
			AtomicReference<PairingHeapNode<T>> iter = checks.remove(0);
			
			if(iter.get().getValue().compareTo(val))
				return iter;
			
			if(iter.get().getLeftChild().get() != null)
				checks.add(iter.get().getLeftChild());
			
			for(AtomicReference<PairingHeapNode<T>> iter2 = iter.get().getRightSibling(); iter2.get() != null; iter2 = iter2.get().getRightSibling())
			{
				if(iter2.get().getValue().compareTo(val))
					return iter2;
				
				if(iter2.get().getLeftChild().get() != null)
					checks.add(iter2.get().getLeftChild());
			}
		}
		
		return null;
	}
	
	private AtomicReference<PairingHeapNode<T>> findAtomic(Weighted<T> val)
	{
		return find(val,root);
	}
	
	/**
	 * Check that this value is in the heap
	 * @param val
	 * @return
	 */
	public boolean find(Weighted<T> val)
	{
		if(find(val, root) == null) return false;
		return true;
	}
	
	/**
	 * Insert node into heap.
	 * 
	 * TODO: Make sure the top level heap is returned to caller
	 * @param value
	 */
	public void insert(Weighted<T> value)
	{
		//Create a new heap with this value as root
		PairingHeap<T> temp = new PairingHeap<T>(value);
		
		//Link our heap with this new heap
		link(temp);
	}
	
	public void remove(Weighted<T> value)
	{
		
	}
	
	/**
	 * Decrease the key value of the value stored in the heap
	 * If no change happens, the matching node has not been found and cannot be updated
	 * 
	 * TODO: Update the ordering of nodes if the decrease key changes the value of the node to less than its parent (link with new heap)
	 * 
	 * @param val
	 * @param newWeight
	 */
	public void decreaseKey(Weighted<T> val, int newWeight)
	{
		AtomicReference<PairingHeapNode<T>> change = findAtomic(val);
		
		//continue until no value to change exists
		while(change != null)
		{
			PairingHeapNode<T> old = change.get();
			Weighted<T> nv = new Weighted<T>(val.get(), newWeight);
			PairingHeapNode<T> t = new PairingHeapNode<T>(this, change.get().getParent(), change.get().getRightSibling(),change.get().getLeftChild(),nv);
			
			//something changed, try again
			if(!change.compareAndSet(old, t))
			{
				change = findAtomic(val);
				continue;
			}
			else
				return;
		}
	}
	
	public void deleteMin()
	{
		
	}
	
}
