import java.util.*;

public class RandomGraph {
	public final TreeSet<GraphNode<Integer>> nodes;
	
	public RandomGraph(int size, double density) {
		nodes = new TreeSet<GraphNode<Integer>>();
		while(nodes.size() < size) {
			GraphNode<Integer> newNode = new GraphNode<Integer>((int)(Integer.MAX_VALUE*Rnd.dbl()));
			for(GraphNode<Integer> node : nodes) {
				if (Rnd.dbl() < density)
					node.addConnection(node, (int)(100*Rnd.dbl()));
			}
			nodes.add(newNode);
		}
	}
}
