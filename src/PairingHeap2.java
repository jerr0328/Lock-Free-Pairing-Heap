import java.util.*;

/**
 * A pairing heap of weightings on a type T.
 *
 * @author Charles Newton
 */
public class PairingHeap2<T extends Weighted<S>, S> {
  private T ele;
  private PairingHeap<T, S> parent;
  private Stack<PairingHeap<T, S>> subHeaps;

  public class NewHeapAndValue {
    public T value;
    public PairingHeap<T, S> heap;
    
    private NewHeapAndValue(PairingHeap<T, S> heap, T value) {
      this.heap = heap;
      this.value = value;
    }
  }

  /**
   * Creates a new (empty) pairing heap.
   */
  public PairingHeap() {
    subHeaps = new Stack<PairingHeap<T, S>>();
    ele = null;
  }

  /**
   * Creates a trivial heap containing
   * only one element.
   */
  private PairingHeap(T ele) {
    this();
    this.ele = ele;
  }

  public boolean isEmpty() {
    return ele == null;
  }

  /**
   * Merges two heaps together. The larger of rhs of lhs will
   * become the child of the smaller. Makes a stateful change
   * to which ever the smaller of lhs and rhs are.
   *
   * @return the smallest heap (which now contains both heaps.)
   */
  private PairingHeap<T, S> merge(PairingHeap<T, S> lhs, PairingHeap<T, S> rhs) {
    // Trivial cases
    if (lhs.isEmpty())
      return rhs;
    if (rhs.isEmpty())
      return lhs;

    PairingHeap<T, S> small;
    PairingHeap<T, S> large;
    if (lhs.ele.compareTo(rhs.ele) <= 0) {
      small = lhs;
      large = rhs;
    }
    else {
      small = rhs;
      large = lhs;
    }
    
    small.subHeaps.push(large); // See note in deleteMin about ordering.
    large.parent = small;
    return small;
  }

  private PairingHeap<T, S> merger(Stack<PairingHeap<T, S>> list) {
    if (list.size() == 0)
      return new PairingHeap<T, S>();
    if (list.size() == 1)
      return list.pop();

    return merge(merge(list.pop(), list.pop()), merger(list));
  }

  /**
   * Inserts e into the heap and returns
   * the new heap.
   */
  public PairingHeap<T, S> insert(T e) {
    return merge(new PairingHeap<T, S>(e), this);
  }

  public NewHeapAndValue deleteMin() {
    return new NewHeapAndValue(merger(subHeaps), ele);
  }
  
  public PairingHeap<T, S> decreaseKey(PairingHeap<T, S> root, int delta) {
    ele.setWeight(ele.getWeight() - delta);
    return merge(root, this);
  }
}