/**
 * A Graph has nodes and a source. Intended for input into Dijkstra's algorithm.
 *  
 * @author Charles Newton
 */
public interface Graph<T> {
	public LFPairingHeap<T> getNodes();
	public Node<T> getSource();
}
