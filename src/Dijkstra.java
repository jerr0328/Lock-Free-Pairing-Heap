import java.util.*;
import java.util.concurrent.*;

public class Dijkstra<T> {
	private PriorityQueue<Weighted<GraphNode<T>>> p;
	private ConcurrentHashMap<GraphNode<T>, Integer> distance;
	
	private GraphNode<T> source;
	public Dijkstra(Graph<T> graph, GraphNode<T> source) {
		p = new PriorityQueue<Weighted<GraphNode<T>>>();
		this.source = source;
		for (GraphNode<T> node : graph.getNodes()) {
			if (node.equals(source)) {
				p.add(new Weighted<GraphNode<T>>(node, 0));
				distance.put(node, 0);
			}
			else {
				p.add(new Weighted<GraphNode<T>>(node, Integer.MAX_VALUE));
				distance.put(node, Integer.MAX_VALUE);
			}
		}
	}
	
	public void run() {
		while (!p.isEmpty()) {
			Weighted<GraphNode<T>> min = p.poll();
			for (GraphEdge<T> edge : min.get().getAdjacent()) {
				int newDistance = distance.get(min) + edge.weight;
				if (newDistance < distance.get(edge.rhs)) {
					distance.remove(edge);
				}
			}
		}
	}
}
