import java.util.ArrayList;
import java.util.concurrent.atomic.*;


public class PairingHeapNode<T> {

	private AtomicReference<PairingHeapNode<T>> rightSibling, parent;
	private AtomicReference<PairingHeapNode<T>> leftChild;
	private PairingHeap<T> myHeap;
	private Weighted<T> value;
	
	public PairingHeapNode(PairingHeap<T> h, AtomicReference<PairingHeapNode<T>> p, AtomicReference<PairingHeapNode<T>> r, AtomicReference<PairingHeapNode<T>> l, Weighted<T> v)
	{
		myHeap = h;
		rightSibling = r;
		parent = p;
		leftChild = l;
		value = v;
	}
	
	public PairingHeapNode(PairingHeap<T> h, Weighted<T> v)
	{
		myHeap = h;
		rightSibling = new AtomicReference<PairingHeapNode<T>>(null);
		parent = new AtomicReference<PairingHeapNode<T>>(null);
		leftChild = new AtomicReference<PairingHeapNode<T>>(null);
		value = v;
	}
	
	/**
	 * Defunct constructor, each node should not create its own atomic reference
	 * @param h
	 * @param p
	 * @param r
	 * @param v
	 */
	/*
	public PairingHeapNode(PairingHeap<T> h, PairingHeapNode<T> p, PairingHeapNode<T> r, Weighted<T> v)
	{
		myHeap = h;
		rightSibling = new AtomicReference<PairingHeapNode<T>>(r);
		parent = new AtomicReference<PairingHeapNode<T>>(p);
		value = v;
		leftChild = new AtomicReference<PairingHeapNode<T>>(null);
	}
	
	public PairingHeapNode(PairingHeap<T> h, PairingHeapNode<T> p, PairingHeapNode<T> r, PairingHeapNode<T> l, Weighted<T> v)
	{
		myHeap = h;
		rightSibling = new AtomicReference<PairingHeapNode<T>>(r);
		parent = new AtomicReference<PairingHeapNode<T>>(p);
		leftChild = new AtomicReference<PairingHeapNode<T>>(l);
		value = v;
	}
	*/
	
	public void changeValue(Weighted<T> v)
	{
		value = v;
	}
	
	public Weighted<T> getValue()
	{
		return value;
	}
	
	public PairingHeap<T> getHeap()
	{
		return myHeap;
	}
	
	public AtomicReference<PairingHeapNode<T>> getParent()
	{
		return parent;
	}
	
	public AtomicReference<PairingHeapNode<T>> getRightSibling()
	{
		return rightSibling;
	}

	public AtomicReference<PairingHeapNode<T>> getLeftChild() {
		return leftChild;
	}
	
}
