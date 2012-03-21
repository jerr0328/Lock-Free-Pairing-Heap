import java.util.*;
import java.util.concurrent.*;

public class PHNode<T> {
	public volatile GraphNode<T> parent;
	public final ConcurrentLinkedQueue<GraphNode<T>> subHeaps;
	public volatile GraphNode<T> graphNode;
	public volatile int distance;
	
	public PHNode() {
		subHeaps = new ConcurrentLinkedQueue<GraphNode<T>>();
	}

	private PHNode(PHNode<T> node) {
		this.subHeaps = node.subHeaps;
		this.parent = node.parent;
		this.graphNode = node.graphNode;
		this.distance = node.distance;
	}
	
	public PHNode<T> clone() {
		return new PHNode<T>(this);
	}
	
	public String toString() {
		if (graphNode != null)
			return "PHNode(" + graphNode.id + ")";
		return "PHNode(unknown ID)";
	}
}
