import java.util.*;
import java.util.concurrent.*;

/**
 * A composition of a Graph Node and a Pairing Heap node.
 * 
 * @author Charles Newton
 *
 */
public class Node<T> {
	public int distance;
	public Node<T> parent;
	public T value;
	public final ConcurrentLinkedQueue<Node<T>> subHeaps;
	public ArrayList<GraphEdge<T>> edges;
	public Object[] edgesArray;
	public int id;
	public boolean inHeap;

	public Node(T value, int id, int expectedSize) {
		this.id = id;
		subHeaps = new ConcurrentLinkedQueue<Node<T>>();
		edges = new ArrayList<GraphEdge<T>>();
		this.value = value;
		this.inHeap = true;
	}

	private Node(Node<T> node) {
		this.subHeaps = node.subHeaps;
		this.edges = node.edges;
		this.edgesArray = node.edgesArray;
		this.distance = node.distance;
		this.parent = node.parent;
		this.value = node.value;
		this.id = node.id;
		this.inHeap = node.inHeap;
	}
	
	public Node<T> clone() {
		return new Node<T>(this);
	}
	
	public void addConnection(Node<T> rhs, int weight) {
		edges.add(new GraphEdge<T>(rhs, weight));
		rhs.edges.add(new GraphEdge<T>(this, weight));
	}
	
	public void finalize() {
		edgesArray = edges.toArray();
		edges = null;
	}
	
	public String toString() {
		return "Node(" + id + ")";
	}
}
