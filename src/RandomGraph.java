import java.util.*;

public class RandomGraph implements Graph<Integer> {
	private LFPairingHeap<Integer> heap;
	private Node<Integer> source;
	
	public RandomGraph(int size, double density, int seed) {
		Rnd.r.setSeed(seed);
		heap = null;
		ArrayList<Node<Integer>> nodes = new ArrayList<Node<Integer>>();
		while (nodes.size() < size) {
			Node<Integer> newNode = new Node<Integer>((int)(Integer.MAX_VALUE*Rnd.dbl()), nodes.size(), (int)(size*density));
			for(Node<Integer> node : nodes) {
				if (Rnd.dbl() < density)
					newNode.addConnection(node, 1 + (int)(100*Rnd.dbl()));
			}
			if (heap == null) {
				newNode.distance = 0;
				heap = new LFPairingHeap<Integer>(newNode);
			}
			else {
				newNode.distance = Integer.MAX_VALUE;
				heap.insert(newNode);
			}
			nodes.add(newNode);
		}
		for(Node<Integer> node : nodes)
			node.finalize();
	}
	
	public LFPairingHeap<Integer> getNodes() {
		return heap;
	}
	
	public Node<Integer> getSource() {
		return source;
	}
}
