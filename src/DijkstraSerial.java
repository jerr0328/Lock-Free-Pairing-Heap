import java.util.*;
import java.util.Map.*;
import java.io.*;

public class DijkstraSerial<T> {
	private Graph<T> graph;
	private HashMap<GraphNode<T>, Integer> distances;
	
	public static void main(String[] args) throws IOException {
		//System.out.println("Running with " + Integer.parseInt(args[0]) + " threads.");
		DijkstraSerial<Integer> d = new DijkstraSerial<Integer>(new RandomGraph(300, .10, 0));
		long time = System.nanoTime();
		d.run();
		System.out.println((System.nanoTime() - time)/1000000000.0);
		//for(Entry<GraphNode<Integer>, Integer> entry : d.distances.entrySet())
		//	System.out.println(entry.getKey().id + " -> " + entry.getValue());
		for(int i = 0; i < 300; i++) {
			boolean found = false;
			for(Entry<GraphNode<Integer>, Integer> entry : d.distances.entrySet()) {
				if (entry.getKey().id == i) {
					found = true;
					System.out.println(entry.getKey().toString() + " -> " + entry.getValue());
				}
			}
			if (!found)
				System.out.println("*** " + i + " missing! ***");
		}
	}
	
	public DijkstraSerial(Graph<T> graph) {
		this.graph = graph;
		this.distances = new HashMap<GraphNode<T>, Integer>();
	}
	
	public void run() throws IOException {
		PriorityQueue<GraphNode<T>> pq = new PriorityQueue<GraphNode<T>>();
		for(Object nodeO : this.graph.getNodes())
			pq.add((GraphNode<T>)nodeO);
		while (!pq.isEmpty()) {
			GraphNode<T> min = pq.poll();
			//System.out.println(min);
			int distToMin = min.distance;
			distances.put(min, distToMin);
			for(Object edgeO : min.edgesArray) {
				GraphEdge<T> edge = (GraphEdge<T>)edgeO;
				if (!edge.rhs.inHeap)
					continue;
				int newDistance =  distToMin + edge.weight;
				if (newDistance < edge.rhs.distance) {
					pq.remove(edge.rhs);
					edge.rhs.distance = newDistance;
					pq.add(edge.rhs);
				}
			}
		}
	}
}
