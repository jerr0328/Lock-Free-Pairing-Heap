import java.io.IOException;
import java.util.*;
import java.util.Map.*;
import java.util.concurrent.*;

public class DijkstraSkiplist<T> {
	private Graph<T> graph;
	private int numWorkers;
	private ConcurrentHashMap<GraphNode<T>, Integer> distances;
	
	public static void main(String[] args) throws IOException {
		//System.out.println("Running with " + Integer.parseInt(args[0]) + " threads.");
		while (true) {
			DijkstraSkiplist<Integer> d = new DijkstraSkiplist<Integer>(new RandomGraph(300, .10, 0), 5);//Integer.parseInt(args[0]));
			//System.in.read();
			//System.out.println("Created graph.");
			long time = System.nanoTime();
			d.run();
			System.out.println((System.nanoTime() - time)/1000000000.0);
			/*for(Entry<GraphNode<Integer>, Integer> entry : d.distances.entrySet())
				System.out.println(entry.getKey().id + " -> " + entry.getValue());*/
			//break;
			for(int i = 0; i < 300; i++) {
				boolean found = false;
				for(Entry<GraphNode<Integer>, Integer> entry : d.distances.entrySet()) {
					if (entry.getKey().id == i) {
						found = true;
						//System.out.println(entry.getKey().toString() + " -> " + entry.getValue());
					}
				}
				if (!found)
					System.out.println("*** " + i + " missing! ***");
			}
			break;
		}
	}
	
	public DijkstraSkiplist(Graph<T> graph, int numWorkers) {
		this.graph = graph;
		this.numWorkers = numWorkers;
		this.distances = new ConcurrentHashMap<GraphNode<T>, Integer>();
	}
	
	public void run() throws IOException {
		ConcurrentSkipListSet<GraphNode<T>> pq = new ConcurrentSkipListSet<GraphNode<T>>();
		// Create worker threads.
		ArrayList<DijkstraWorker> workers = new ArrayList<DijkstraWorker>(numWorkers);
		CountDownLatch startLatch = new CountDownLatch(1);
		
		for(Object nodeO : graph.getNodes())
			pq.add((GraphNode<T>)nodeO);
		
		while(workers.size() < numWorkers) {
			DijkstraWorker worker = new DijkstraWorker(startLatch, pq);
			worker.start();
			workers.add(worker);
		}
		
		GraphNode<T> min;
		
		while ((min = pq.pollFirst()) != null) {
			// Pop the min distance off and record its distance
			distances.put(min, min.distance);
			min.inHeap = false;
			
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
		startLatch.countDown();
	}
	
	private class DijkstraWorker extends Thread {
		private int offset;
		private int numThreads;
		//private ArrayList<GraphEdge<T>> edges;
		private Object[] edges;
		private GraphNode<T> min;
		private boolean killed;
		//private AtomicInteger finishedCounter;
		private CountDownLatch latch;
		private CountDownLatch startLatch;
		private CountDownLatch tmpLatch;
		private ConcurrentSkipListSet<GraphNode<T>> heap;
		public DijkstraWorker(CountDownLatch nextLatch, ConcurrentSkipListSet<GraphNode<T>> heap) {
			this.killed = false;
			this.startLatch = nextLatch;
			this.heap = heap;
		}
		
		public void modifyWork(int offset, int numThreads, GraphNode<T> min, CountDownLatch latch, CountDownLatch nextLatch) {
			this.offset = offset;
			this.numThreads = numThreads;
			this.min = min;
			this.edges = min.edgesArray;
			this.latch = latch;
			this.tmpLatch = nextLatch;
		}
		
		public void kill() {
			killed = true;
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
					if (edge.rhs.inHeap) {
						int newDistance = min.distance + edge.weight;
						if (newDistance < edge.rhs.distance) {
							heap.remove(edge.rhs);
							edge.rhs.distance = newDistance;
							heap.add(edge.rhs);
						}
					}
					pos += this.numThreads;
				}
				startLatch = tmpLatch;
				latch.countDown();
			}
		}
	}
}
