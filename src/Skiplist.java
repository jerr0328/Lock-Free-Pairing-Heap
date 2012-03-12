import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * An implementation of Pugh's Skip list.
 * @author Charles Newton
 * @param <T>
 */
public class Skiplist<T> {
    /**
     * A node in our skip list.
     * A node of value 5 and height 2 looks like:
     * <pre>
     *   *---*
     *   |   |
     *   *---*
     *   |   |
     *   *---*
     *   |   |
     *   *---*
     *     5
     * </pre>
     *
     */
    private class Node<S> { 
        // <S> -> Stupid hack to make it so we can make an array of an inner class of a generic class.
        public final Weighted<T> value; 
        // A node has height 0 if it only points
        // to the next node in the Skip list. A node
        // of height n points to n+1 nodes later in the list (or NULL.)
        public int height; 
                
        // next[i] contains the ``next pointer'' at depth i.
        public final AtomicMarkableReference<Node<T>>[] next;
                        
        private Node(Weighted<T> value, int height) {
            this.value = value;
            // TODO: Fix casting (Java doesn't support generic'd arrays!), low priority
            this.next = new AtomicMarkableReference[height + 1];
            for(int i = 0; i < height + 1; i++)
                this.next[i] = new AtomicMarkableReference<Node<T>>(null, false); // Next ptrs. are NULL by default.
        }
    }
        
    final int LARGEST_LEVEL = 32; // How deep should the skip list be (the max. depth is LARGEST_LEVEL + 1)
        
    // Skiplists are ordered, so assign the sentinel nodes the smallest / 
    // highest possible values.
    final Node<T> head = new Node<T>(new Weighted<T>(null, Integer.MIN_VALUE), LARGEST_LEVEL);
    final Node<T> tail = new Node<T>(new Weighted<T>(null, Integer.MAX_VALUE), LARGEST_LEVEL);

    public Skiplist() {
        for(int i = 0; i < LARGEST_LEVEL + 1; i++)
            head.next[i] = new AtomicMarkableReference<Node<T>>(tail, false);
    }

    private int randomLevel() {
        int level = 0;
        while (Rnd.r.nextDouble() < 0.5)
            level++;
        return level;
    }

    boolean decreaseKey(Weighted<T> x, int newWeight) {
        int bottom = 0;
        Node[] prev = new Node[LARGEST_LEVEL + 1];
        Node[] next = new Node[LARGEST_LEVEL + 1];
        Node succ;
        boolean[] marked = new boolean[1];
        while (true) {
            if (!find(x, prev, next))
                return false;

            // Remove the node
            Node<T> target = next[bottom];
            for (int i = target.height; i >= bottom + 1; i--) {
                marked[0] = false;
                succ = target.next[i].get(marked);
                while (!marked[0]) {
                    target.next[i].compareAndSet(next[i], next[i], false, true);
                    succ = target.next[i].get(marked);
                }
            }
            
            marked[0] = false;
            succ = target.next[bottom].get(marked);
            while (true) {
                boolean markedTarget = target.next[bottom].compareAndSet(succ, succ, false, true);
                //succ = next[bottom].next.get(marked);
                if (markedTarget) {
                    find(x, null, null);
                    Node<T> hint = prev[0];
                    for (int i = 1; i < prev.length; i++) {
                        if (prev[i].value.getWeight() < newWeight)
                            hint = prev[i];
                        else
                            break;
                    }
                    x.setWeight(newWeight);
                    add(x, hint);
                    // Next, we need to reinsert our node at the proper location.
                    // Use find to clean everything up.
                    return true;
                }
                else if (marked[0]) {
                    // Someone else deleted our original node. Should we reinsert it?
                    return false;
                }
            }
        }
    }

    boolean add(Weighted<T> x) {
        // Height of the new node
        int height = 0; //randomLevel();
        int bottom = 0;
                        
        Node[] prev = new Node[LARGEST_LEVEL + 1];
        Node[] next = new Node[LARGEST_LEVEL + 1];
                        
        while (true) {
            // TODO: Add support for redundant elements.
            if (find(x, prev, next))
                return false;
            Node<T> n = new Node<T>(x, height);
            for (int i = bottom; i <= height; i++)
                n.next[i].set(next[i], false);
            Node<T> pred = prev[bottom];
            Node<T> succ = next[bottom];
            n.next[bottom].set(succ, false);
                    
            if (!pred.next[bottom].compareAndSet(succ, n, false, false))
                continue;
        
            for (int i = bottom + 1; i <= height; i++) {
                while (true) {
                    pred = prev[i];
                    succ = next[i];
                    if (pred.next[i].compareAndSet(succ, n, false, false))
                        break;
                
                    // Oops, we need to get new prev / next arrays!
                    find(x, prev, next);
                }
            }                   
        }
    }
        
    boolean add(Weighted<T> x, Node<T> hint) {
        // Height of the new node
        int height = 0; //randomLevel();
        int bottom = 0;
                        
        Node[] prev = new Node[LARGEST_LEVEL + 1];
        Node[] next = new Node[LARGEST_LEVEL + 1];
                        
        while (true) {
            // TODO: Add support for redundant elements.
            if (find(x, prev, next, hint))
                return false;
            Node<T> n = new Node<T>(x, height);
            for (int i = bottom; i <= height; i++)
                n.next[i].set(next[i], false);
            Node<T> pred = prev[bottom];
            Node<T> succ = next[bottom];
            n.next[bottom].set(succ, false);
                    
            if (!pred.next[bottom].compareAndSet(succ, n, false, false))
                continue;
        
            for (int i = bottom + 1; i <= height; i++) {
                while (true) {
                    pred = prev[i];
                    succ = next[i];
                    if (pred.next[i].compareAndSet(succ, n, false, false))
                        break;
                
                    // Oops, we need to get new prev / next arrays!
                    find(x, prev, next);
                }
            }                   
        }
    }

    boolean remove(Weighted<T> x) {
        // TODO: Make this not assume that x.getWeight() == y.getWeight() implies
        // x is equivalent to y! (Same issue as in add / find)
        int bottom = 0;
        Node<T>[] prev = new Node[LARGEST_LEVEL + 1];
        Node<T>[] next = new Node[LARGEST_LEVEL + 1];
        
        Node<T> succ;
        
        boolean[] marked = new boolean[1];
        marked[0] = false;
        
        while (true) {
            if (!find(x, prev, next))
                return false;
            Node<T> target = next[bottom];
            for (int i = target.height; i >= bottom + 1; i--) {
                succ = target.next[i].get(marked);
                while (!marked[0]) {
                    target.next[i].compareAndSet(succ, succ, false, true);
                    succ = target.next[i].get(marked);
                }
            }
        
            marked[0] = false;
            succ = target.next[bottom].get(marked);
            while (true) {
                boolean markedTarget = target.next[bottom].compareAndSet(succ, succ, false, true);
                succ = next[bottom].next[bottom].get(marked);
                if (markedTarget) {
                    // Use find to clean everything up.
                    find(x, null, null);
                    return true;
                }
                else if (marked[0])
                    return false; // Oops, someone else deleted it!
            }
        }
    }

    boolean find(Weighted<T> x, Node<T>[] prev, Node<T>[] next, Node<T> hint) {
        int bottom = 0;
        boolean[] marked = {false}; // Used to fetch markings from a AtomicMarkableReference
        Node<T> pred = null;
        Node<T> curr = null;
        Node<T> succ = null;
        // TODO: goto + labels are harmful!
        redo:
        while (true) {
            if (hint == null)
                pred = head;
            else
                pred = hint;
            for (int i = LARGEST_LEVEL; i >= bottom; i--) {
                curr = pred.next[i].getReference();
                while (true) {
                    succ = curr.next[i].get(marked);
                    while (marked[0]) {
                        if (!pred.next[i].compareAndSet(curr, succ, false, false))
                            continue redo;
                        curr = pred.next[i].getReference();
                        succ = curr.next[i].get(marked);
                    }
                    if (curr.value.getWeight() < x.getWeight()) {
                        pred = curr;
                        curr = succ;
                    }
                    else
                        break;
                }
                                
                // Only keep track of prev / next information
                // if we care about it.
                if (prev != null)
                    prev[i] = pred;
                if (next != null)
                    next[i] = curr;
            }
            return (curr.value.getWeight() == x.getWeight());
        }
    }
    
    /**
     * Returns true if x.getWeight() is in the Skip list, and false otherwise.
     * 
     * @param x
     * @param prev
     * @param next
     * @return
     */
    boolean find(Weighted<T> x, Node<T>[] prev, Node<T>[] next) {
        int bottom = 0;
        boolean[] marked = {false}; // Used to fetch markings from a AtomicMarkableReference
        Node<T> pred = null;
        Node<T> curr = null;
        Node<T> succ = null;
        // TODO: goto + labels are harmful!
        redo:
        while (true) {
            pred = head;
            for (int i = LARGEST_LEVEL; i >= bottom; i--) {
                curr = pred.next[i].getReference();
                while (true) {
                    succ = curr.next[i].get(marked);
                    while (marked[0]) {
                        if (!pred.next[i].compareAndSet(curr, succ, false, false))
                            continue redo;
                        curr = pred.next[i].getReference();
                        succ = curr.next[i].get(marked);
                    }
                    if (curr.value.getWeight() < x.getWeight()) {
                        pred = curr;
                        curr = succ;
                    }
                    else
                        break;
                }
                                
                // Only keep track of prev / next information
                // if we care about it.
                if (prev != null)
                    prev[i] = pred;
                if (next != null)
                    next[i] = curr;
            }
            return (curr.value.getWeight() == x.getWeight());
        }
    }
}