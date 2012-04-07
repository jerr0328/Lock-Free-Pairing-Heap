import java.util.*;
import java.util.regex.Pattern;
import java.io.*;

/**
 * Class for loading graphs from SNAP (http://snap.stanford.edu)
 * 
 * @author ptonner
 *
 */
public class SnapGraph implements Graph<Integer> {
	
	private GraphNode<Integer> source;
	private final int maxWeight = 100;
	private Object nodeList[];
	
	public SnapGraph(String filename, int expected_nodes, int expected_edges, double density)
	{
		ArrayList<GraphNode<Integer>> nodes = new ArrayList<GraphNode<Integer>>(expected_nodes);
		//TreeMap<Integer,GraphNode<Integer>> nodes = new TreeMap<Integer,GraphNode<Integer>>();
		
		Scanner s = null;
		try
		{
			s = new Scanner(new BufferedReader(new FileReader(filename)));
			
			String header = s.nextLine();
			while(header.contains("#")) //still on header lines
				header = s.nextLine();
			//error: skips first line of input
			
			int a,b;
			while(s.hasNext())
			{
				a = s.nextInt();
				b = s.nextInt();
				
				if(nodes.size() % 10000 == 0)
					System.out.println(nodes.size() + "\t" + a +"\t" + b);
				
				GraphNode<Integer> nodeA, nodeB;
				
				if(a > nodes.size() || b > nodes.size())
				{
					for(int i = Math.min(a, b); i <= Math.max(a, b); i++)
					{
						if(i >= nodes.size())
							nodes.add(i,null);
					}
				}
				
				if(nodes.get(a)==null)
				{
					nodeA = new GraphNode<Integer>(a,a,(int)(expected_nodes*density));
					nodeA.distance = Integer.MAX_VALUE - (maxWeight + 2);
					nodes.add(a,nodeA);
				}
				else
				{
					nodeA = nodes.get(a);
				}
				
				if(nodes.get(b)==null)
				{
					nodeB = new GraphNode<Integer>(b,b,(int)(expected_nodes*density));
					nodeB.distance = Integer.MAX_VALUE - (maxWeight + 2);
					nodes.add(b,nodeB);
				}
				else
				{
					nodeB = nodes.get(b);
				}
				
				nodeA.addConnection(nodeB, 1);

			}
		}
		catch(Exception E)
		{
			System.out.println("Error at:" + s.nextLine());
			E.printStackTrace();
		}
		
		source = nodes.get(0);
		source.distance = 0;
		
		for(GraphNode<Integer> node : nodes)
			node.finalize();
		
		nodeList = nodes.toArray();
		
	}

	public Object[] getNodes() {
		return nodeList;
	}

	public GraphNode<Integer> getSource() {
		return source;
	}

}
