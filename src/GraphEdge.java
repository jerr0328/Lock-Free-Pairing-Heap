public class GraphEdge<T> {
	public GraphNode<T> rhs;
	public int weight;
	
	public GraphEdge(GraphNode<T> rhs, int weight) {
		this.rhs = rhs;
		this.weight = weight;
	}
}
