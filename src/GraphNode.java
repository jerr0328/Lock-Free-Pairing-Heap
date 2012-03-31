import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class GraphNode<T> implements Comparable<GraphNode<T>> {
	public ArrayList<GraphEdge<T>> edges;
	public Object[] edgesArray;
	public final int id;
	public T value;
	public volatile int distance; // LFPairingHeap should /not/ change this field!
	public final AtomicReference<PHNode<T>> phNode;
	public volatile boolean inHeap;
	
	public GraphNode(T value, int id, int expectedSize) {
		this.id = id;
		this.value = value;
		edges = new ArrayList<GraphEdge<T>>(expectedSize);
		inHeap = true;
		phNode = new AtomicReference<PHNode<T>>();
	}

	public void addConnection(GraphNode<T> rhs, int weight) {
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
	
	public int compareTo(GraphNode<T> rhs) {
		if (this.distance == rhs.distance)
			return -1;
		return this.distance - rhs.distance;
	}
}
