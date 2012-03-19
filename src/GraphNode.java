import java.util.*;

public class GraphNode<T> implements Comparable<GraphNode<T>> {
	private TreeSet<GraphEdge<T>> adjacencyList;
	public T contents;
	private final UUID id;
	
	public GraphNode(T contents) {
		adjacencyList = new TreeSet<GraphEdge<T>>();
		this.contents = contents;
		id = UUID.randomUUID();
	}
	
	public TreeSet<GraphEdge<T>> getAdjacent() {
		return adjacencyList;
	}
	
	public void addConnection(LFPairingHeap<GraphNode<T>> node, int weight) {
		addConnection(new GraphEdge<T>(node, weight));
	}
	
	public void addConnection(GraphEdge<T> weightedEdge) {
		adjacencyList.add(weightedEdge);
		weightedEdge.rhs.getElement().get().adjacencyList.add(new GraphEdge<T>(this, weightedEdge.weight));
	}
	
	public boolean equals(GraphNode<T> node) {
		return this.id.equals(node.id);
	}
	
	public int compareTo(GraphNode<T> rhs) {
		if (this.equals(rhs))
			return 0;
		return -1;
	}
}
