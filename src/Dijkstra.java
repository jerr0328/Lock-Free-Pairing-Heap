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
		System.out.println("Running with " + Integer.parseInt(args[0]) + " threads.");
		while (true) {
			Dijkstra<Integer> d = new Dijkstra<Integer>(new RandomGraph(5000, 1.0, 0), Integer.parseInt(args[0]));
			//System.in.read();
			//System.out.println("Created graph.");
			long time = System.nanoTime();
			d.run();
			System.out.println((System.nanoTime() - time)/100000000.0);
			/*for(int i = 0; i < d.distances.size(); i++) {
				boolean found = false;
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
		this.graph = graph;
		this.numWorkers = numWorkers;
		this.distances = new ConcurrentHashMap<Node<T>, Integer>();
	}
	
	public void run() throws IOException {
		// Create worker threads.
		ArrayList<DijkstraWorker> workers = new ArrayList<DijkstraWorker>(numWorkers);
		CountDownLatch startLatch = new CountDownLatch(1);
		while(workers.size() < numWorkers) {
			DijkstraWorker worker = new DijkstraWorker(startLatch);
			worker.start();
			workers.add(worker);
		}
		
		while (graph.getNodes().size() > 0) {
			// Pop the min distance off and record its distance
			Node<T> min = graph.getNodes().deleteMin();
			distances.put(min, min.distance);
			
			CountDownLatch latch = new CountDownLatch(numWorkers);
			CountDownLatch nextLatch = new CountDownLatch(1);
			for(int i = 0; i < workers.size(); i++) {
				DijkstraWorker worker = workers.get(i);
				worker.modifyWork(i, numWorkers, min, latch, nextLatch);
			}
			startLatch.countDown();
			startLatch = nextLatch;
			try {
				latch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
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
		//private AtomicInteger finishedCounter;
		private CountDownLatch latch;
		private CountDownLatch startLatch;
		private CountDownLatch tmpLatch;
		
		public DijkstraWorker(CountDownLatch nextLatch) {
			this.killed = false;
			this.startLatch = nextLatch;
		}
		
		public void modifyWork(int offset, int numThreads, Node<T> min, CountDownLatch latch, CountDownLatch nextLatch) {
			this.offset = offset;
			this.numThreads = numThreads;
			this.min = min;
			this.edges = min.edgesArray;
			this.latch = latch;
			this.tmpLatch = nextLatch;
		}
		
		public void kill() {
			killed = true;
			synchronized (this) {
				this.notify();
			}
		}
		
		public void run() {
			while (!killed) {
				try {
					startLatch.await();
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
				int pos = this.offset;
				while (!killed) {
					if (pos >= edges.length)
						break;					
					GraphEdge<T> edge = (GraphEdge<T>)edges[pos]; 
					int newDistance = min.distance + edge.weight;
					if (newDistance < edge.rhs.distance)
						graph.getNodes().decreaseKey(edge.rhs, newDistance);
					pos += this.numThreads;	
				}
				startLatch = tmpLatch;
				latch.countDown();
			}
		}
	}
}
