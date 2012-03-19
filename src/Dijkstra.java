import java.io.IOException;
import java.lang.Thread.State;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Dijkstra<T> {
	private Graph<T> graph;
	private int numWorkers;
	private ConcurrentHashMap<Node<T>, Integer> distances;
	
	public static void main(String[] args) throws IOException {
		while (true) {
			Dijkstra<Integer> d = new Dijkstra<Integer>(new RandomGraph(10, 0.5, 0), 2);
			//System.in.read();
			long time = System.nanoTime();
			d.run();
			System.out.println((System.nanoTime() - time)/100000000.0);
			/*for(int i = 0; i < d.distances.size(); i++) {
				boolean found = false;}
				for(Entry<Node<Integer>, Integer> entry : d.distances.entrySet()) {
					if (entry.getKey().id == i) {
						found = true;
						//System.out.println(entry.getKey().toString() + " -> " + entry.getValue());
					}
				}
				if (!found)
					System.out.println("*** " + i + " missing! ***");
			}*/
		}
	}
	
	public Dijkstra(Graph<T> graph, int numWorkers) {
		//p = new PriorityQueue<Weighted<GraphNode<T>>>();
		this.graph = graph;
		this.numWorkers = numWorkers;
		this.distances = new ConcurrentHashMap<Node<T>, Integer>();
	}
	
	public void run() {
		// Create worker threads.
		ArrayList<DijkstraWorker> workers = new ArrayList<DijkstraWorker>(numWorkers);
		while(workers.size() < numWorkers) {
			DijkstraWorker worker = new DijkstraWorker();
			worker.start();
			workers.add(worker);
		}
		
		while (graph.getNodes().size() > 0) {
			// Pop the min distance off and record its distance
			Node<T> min = graph.getNodes().deleteMin();
			distances.put(min, min.distance);
			
			//AtomicInteger counter = new AtomicInteger(0);
			AtomicInteger finishedCounter = new AtomicInteger(0);
			for (DijkstraWorker worker : workers)
				while (worker.getState() != State.WAITING)
					Thread.yield();
			for(int i = 0; i < workers.size(); i++) {
				DijkstraWorker worker = workers.get(i);
				worker.modifyWork(i, numWorkers, finishedCounter, min);
				synchronized (worker) {
					worker.notify();
				}
			}
			
			while(finishedCounter.get() < numWorkers)
				Thread.yield();
		}
		
		for (DijkstraWorker worker : workers)
			worker.kill();
	}
	
	private class DijkstraWorker extends Thread {
		private int offset;
		private int numThreads;
		//private ArrayList<GraphEdge<T>> edges;
		private Object[] edges;
		private Node<T> min;
		private boolean killed;
		private AtomicInteger finishedCounter;
		
		public DijkstraWorker() {
			this.killed = false;
		}
		
		public void modifyWork(int offset, int numThreads, AtomicInteger finishedCounter, Node<T> min) {
			this.offset = offset;
			this.numThreads = numThreads;
			this.finishedCounter = finishedCounter;
			this.min = min;
			this.edges = min.edgesArray;
		}
		
		public void kill() {
			killed = true;
			synchronized (this) {
				this.notify();
			}
		}
		
		public void run() {
			while (!killed) {
				synchronized (this) {
					try {
						this.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				int pos = this.offset;
				while (!killed) {
					if (edges == null || pos >= edges.length)
						break;
					GraphEdge<T> edge = (GraphEdge<T>)edges[pos]; 
					int newDistance = min.distance + edge.weight;
					if (newDistance < edge.rhs.distance)
						graph.getNodes().decreaseKey(edge.rhs, newDistance);
					pos += this.numThreads;
				}
				finishedCounter.getAndIncrement();
			}
		}
	}
}
