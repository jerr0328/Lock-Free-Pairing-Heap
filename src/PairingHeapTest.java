import java.util.TreeSet;
//import static org.junit.Assert.*;


public class PairingHeapTest {

	private TreeSet<Integer> array;
	private PairingHeap<Integer> pairingHeap;
	private final int testSize = 10240;
	
	public void init() {
		array = new TreeSet<Integer>();
		pairingHeap = new PairingHeap<Integer>(new Weighted<Integer>(null,0));
		while(array.size() < testSize) {
			int num = randSign() * (int)(Integer.MAX_VALUE * Rnd.dbl());
			array.add(num);
			pairingHeap.insert(new Weighted<Integer>(null, num));
		}
	}
	
	private int randSign() {
		return (Rnd.dbl() < 0.5) ? -1 : 1;
	}
	
	public void testAdd() {
		testFind();
	}

	public void testRemove() {
		while (array.size() > 0) {
			for(int i : (TreeSet<Integer>)array.clone()) {
				if(randSign() == 1.0) {
					array.remove(i);
					pairingHeap.remove(new Weighted<Integer>(null, i));
				}
			}
			testFind();
		}
		testFind();
	}
	
	public boolean assertEquals(String title, boolean test, boolean result)
	{
		if(test != result)
			System.out.println(title+": failed!");
		return test == result;
	}

	public void testFind() {
		System.out.println("Test Inclusion");
		for(int i : array) 
		{
			assertEquals("Inclusion", true, pairingHeap.find(new Weighted<Integer>(null, i)));
		}
		
		System.out.println("Test Exclusion");
		for(int i = 0; i < testSize;) {
			int num = randSign() * (int)(Integer.MAX_VALUE * Rnd.dbl());
			if (!array.contains(num))
			{
				if(!assertEquals("Exclusion", false, pairingHeap.find(new Weighted<Integer>(null, num))))
					System.out.println(num);
			}
			else
				continue;
			i++;
		}
		
		System.out.println("Test Decrease Key");
		int current_vals[] = new int[testSize];
		int ind = 0;
		for(int i: array)
		{
			current_vals[ind] = i;
			ind++;
		}
		for(int i = 0; i < testSize; i++) {
			int num = current_vals[i];
			
			if (!assertEquals("Decrease Key Old", true, pairingHeap.find(new Weighted<Integer>(null, num))))
			{
				System.out.println(i);
				break;
			}
			
			int delta = randSign() * (int)((Integer.MAX_VALUE-Math.abs(num)) * Rnd.dbl());
			pairingHeap.decreaseKey(new Weighted<Integer>(null,num), num+delta);
			
			assertEquals("Decrease Key", true, pairingHeap.find(new Weighted<Integer>(null, num+delta)));
		}
	}
	
}