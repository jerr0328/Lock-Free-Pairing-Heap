import java.io.IOException;


public class SnapGraphTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException{
		
		int numWorkers[] = {1,2,4};
		
		for(int t : numWorkers)
		{
			SnapGraph sg = new SnapGraph("../graphs/soc-Epinions1.txt",75879,508837,75879.0/508837);
			Dijkstra<Integer> d = new Dijkstra<Integer>(sg,t);
			long time = System.nanoTime();
			d.run();
			System.err.println((System.nanoTime() - time)/1000000000.0);
		}

	}

}
