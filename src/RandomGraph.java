import java.util.*;

public class RandomGraph implements Graph<Integer> {
	private LFPairingHeap<Integer> heap;
	private GraphNode<Integer> source;
	private Object[] nodeList;
	private final int maxWeight = 100;
	
	public RandomGraph(int size, double density, int seed) {
		Rnd.r.setSeed(seed);
		heap = null;
		ArrayList<GraphNode<Integer>> nodes = new ArrayList<GraphNode<Integer>>();
		while (nodes.size() < size) {
			GraphNode<Integer> newNode = new GraphNode<Integer>((int)(Integer.MAX_VALUE*Rnd.dbl()), nodes.size(), (int)(size*density));
			for(GraphNode<Integer> node : nodes) {
				if (Rnd.dbl() < density)
					newNode.addConnection(node, 1 + (int)(maxWeight*Rnd.dbl()));
			}
			
			if (nodes.size() == 0) {
				newNode.distance = 0;
				source = newNode;
			}
			else
				newNode.distance = Integer.MAX_VALUE - (maxWeight + 2);
			
			nodes.add(newNode);
		}
		for(GraphNode<Integer> node : nodes)
			node.finalize();
		nodeList = nodes.toArray();
	}
	
	public GraphNode<Integer> getSource() {
		return source;
	}
	
	public Object[] getNodes() {
		return nodeList;
	}
}
