public class GraphEdge<T> {
	public GraphNode<T> rhs;
	public int weight;
	
	public GraphEdge(GraphNode<T> node, int weight) {
		this.rhs = node;
		this.weight = weight;
	}
}
