public class GraphEdge<T> {
	public Node<T> rhs;
	public int weight;
	
	public GraphEdge(Node<T> node, int weight) {
		this.rhs = node;
		this.weight = weight;
	}
}
