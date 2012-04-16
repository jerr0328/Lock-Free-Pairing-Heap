import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Benchmark {
	
	public static void main(String[] args) throws IOException{
		
		if(args.length < 2)
		{
			System.out.println("USAGE: Benchmark <SnapGraph File> <List of Threads>");
			System.out.println("EXAMPLE: Benchmarck ../graphs/soc-Epinions1.txt 1,2,4");
			System.exit(1);
		}
		
		String file = args[0];
		String threads = args[1];
		Scanner threadScanner = new Scanner(threads);
		threadScanner.useDelimiter(",");
		ArrayList<Integer> thread = new ArrayList<Integer>();
		while(threadScanner.hasNext())
			thread.add(threadScanner.nextInt());
		
		for(int t : thread)
		{
			System.out.println("Loading Graph");
			long time = System.nanoTime();
			SnapGraph sg = new SnapGraph(file);
			double load_time = (System.nanoTime() - time)/1000000000.0;
			System.out.println("Graph Loaded (" + load_time + ")");
			
			Dijkstra<Integer> d = new Dijkstra<Integer>(sg,t,false);
			time = System.nanoTime();
			d.run();
			double runtime = (System.nanoTime() - time)/1000000000.0;
			System.out.println(sg+"\nThreads: " + t + "\n"+runtime);
		}

	}

}
