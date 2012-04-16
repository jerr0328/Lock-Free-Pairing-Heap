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
	private String name;
	private int connections,size;
	
	public SnapGraph(String filename)
	{
		int expected_nodes = 0;
		int expected_edges = 0;
		double density = 0.0;
		connections = 0;
		
		int firstNode = -1;
		Scanner s = null;
		ArrayList<GraphNode<Integer>> nodes = null;
		TreeMap<Integer,GraphNode<Integer>> nodeMap = new TreeMap<Integer,GraphNode<Integer>>();
		try
		{
			s = new Scanner(new BufferedReader(new FileReader(filename)));
			String graphtype = s.nextLine();
			String graphName = s.nextLine();
			String graphProperties = s.nextLine();
			String graphFormat = s.nextLine();
			
			name = graphName;
			
			try{
				expected_nodes = Integer.parseInt(graphProperties.substring(graphProperties.indexOf(':')+2,graphProperties.indexOf('E')-1));
				graphProperties = graphProperties.substring(graphProperties.indexOf(':') + 1);
				expected_edges = Integer.parseInt(graphProperties.substring(graphProperties.indexOf(':')+2));
				density = (double)(expected_edges)/expected_nodes;
			}
			catch(Exception E)
			{
				System.out.println("Error: " + expected_nodes + " " + expected_edges + " " + density);
				E.printStackTrace();
			}
			
			//nodes = new ArrayList<GraphNode<Integer>>(expected_nodes);
			
			int a = 0,b = 0;
			while(s.hasNext())
			{
				a = s.nextInt();
				b = s.nextInt();
				
				if(firstNode == -1)
					firstNode = a;
				
				connections++;
				
				GraphNode<Integer> nodeA, nodeB;
				
				if(connections % (int)(expected_edges*.1) == 0)
					System.out.println(connections + " (" + connections*100/expected_edges + "%) "+ a + " " + b);
				
				/*if(a > nodes.size() || b > nodes.size())
				{
					for(int i = nodes.size(); i <= Math.max(a, b); i++)
					{
						if(i >= nodes.size())
							nodes.add(i,null);
					}
					
				}*/
				
				if(!nodeMap.containsKey(a))
				//if(nodes.get(a)==null)
				{
					//nodeA = new GraphNode<Integer>(a,a,(int)(expected_nodes*density));
					nodeA = new GraphNode<Integer>(a,a,0);
					nodeA.distance = Integer.MAX_VALUE - (maxWeight + 2);
					nodeMap.put(a, nodeA);
					//nodes.add(a,nodeA);
				}
				else
				{
					//nodeA = nodes.get(a);
					nodeA = nodeMap.get(a);
				}
				
				if(!nodeMap.containsKey(b))
				//if(nodes.get(b)==null)
				{
					//nodeB = new GraphNode<Integer>(b,b,(int)(expected_nodes*density));
					nodeB = new GraphNode<Integer>(b,b,0);
					nodeB.distance = Integer.MAX_VALUE - (maxWeight + 2);
					//nodes.add(b,nodeB);
					nodeMap.put(b, nodeB);
				}
				else
				{
					//nodeB = nodes.get(b);
					nodeB = nodeMap.get(b);
				}
				
				nodeA.addConnection(nodeB, 1);

			}
		}
		catch(Exception E)
		{
			System.out.println("Error at:" + s.nextLine());
			E.printStackTrace();
		}
		
		//source = nodes.get(0);
		source = nodeMap.get(firstNode);
		source.distance = 0;
		
		//clean up empty nodes
		//while(nodes.contains(null))
		//	nodes.remove(null);
		
		for(GraphNode<Integer> node : nodeMap.values())
			if(node != null)
				node.finalize();
		
		nodeList = nodeMap.values().toArray();
		size = nodeList.length;
		
	}
	
	public SnapGraph(String filename, int expected_nodes, int expected_edges, double density)
	{
		ArrayList<GraphNode<Integer>> nodes = new ArrayList<GraphNode<Integer>>(expected_nodes);
		
		Scanner s = null;
		try
		{
			s = new Scanner(new BufferedReader(new FileReader(filename)));
			
			String header = s.nextLine();
			while(header.contains("#")) //still on header lines
				header = s.nextLine();
			//error: skips first line of input
			
			int a,b = 0;
			while(s.hasNext())
			{
				a = s.nextInt();
				b = s.nextInt();
				connections++;
				
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
		
		//clean up empty nodes
		while(nodes.contains(null))
			nodes.remove(null);
		
		for(GraphNode<Integer> node : nodes)
			if(node != null)
				node.finalize();
		
		nodeList = nodes.toArray();
		size = nodeList.length;
		
	}

	public Object[] getNodes() {
		return nodeList;
	}

	public GraphNode<Integer> getSource() {
		return source;
	}
	
	public String toString()
	{
		return name + "\nNodes: " + size + " Edges: " + connections;
	}
	
	public void reset()
	{
		for (Object nodeO : nodeList) {
			GraphNode<Integer> node = (GraphNode<Integer>)nodeO;
			node.distance = Integer.MAX_VALUE - (maxWeight + 2);			
		}
		source.distance = 0;
	}

}
