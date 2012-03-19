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

	public Node(T value, int id, int expectedSize) {
		this.id = id;
		subHeaps = new ConcurrentLinkedQueue<Node<T>>();
		edges = new ArrayList<GraphEdge<T>>();
		this.value = value;
	}

	private Node(ConcurrentLinkedQueue<Node<T>> subHeaps, ArrayList<GraphEdge<T>> edges) {
		this.subHeaps = subHeaps;
		this.edges = edges;
	}
	
	public Node<T> clone() {
		Node<T> ret = new Node<T>(this.subHeaps, this.edges);
		ret.distance = distance;
		ret.parent = parent;
		ret.value = value;
		ret.id = id;
		return ret;
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
