import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

public class Benchmark {
	
	public static void main(String[] args) throws IOException{
		
		if(args.length < 3)
		{
			System.out.println("USAGE: Benchmark <SnapGraph File> <Runs> <List of Threads>");
			System.out.println("EXAMPLE: Benchmarck ../graphs/soc-Epinions1.txt 5 1,2,4");
			System.exit(1);
		}
		
		String file = args[0];
		int runs = Integer.valueOf(args[1]);
		String threads = args[2];
		Scanner threadScanner = new Scanner(threads);
		threadScanner.useDelimiter(",");
		ArrayList<Integer> thread = new ArrayList<Integer>();
		while(threadScanner.hasNext())
			thread.add(threadScanner.nextInt());
		
		PrintWriter out = new PrintWriter(new FileWriter("output.txt",true));
		for(int i = 0; i < runs; i++){
			System.out.println("Run: "+(i+1));
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
				out.println(t+"\t"+runtime);
			}
		}
		System.out.println("Benchmark complete");
		out.close();

	}

}
