import static org.junit.Assert.*;
import org.junit.*;
import java.util.*;

public class SkiplistTest {
    // Change to a ArrayList once we fix skiplists not being able to
    // contain duplicate keys!
    private TreeSet<Integer> array;
    private Skiplist<Integer> skipList;
    private final int testSize = 100;

    @Before
    public void init() {
        array = new TreeSet<Integer>();
        skipList = new Skiplist<Integer>();
        while(array.size() < testSize) {
            int num = randSign() * (int)(Integer.MAX_VALUE * Rnd.dbl());
            array.add(num);
            skipList.add(new Weighted<Integer>(null, num));
        }
    }

    private int randSign() {
        return (Rnd.dbl() < 0.5) ? -1 : 1;
    }

    @Test
    public void testAdd() {
        testFind();
    }

    @Test
    public void testRemove() {
        while (array.size() > 0) {
            for(int i : (TreeSet<Integer>)array.clone()) {
                if(randSign() == 1.0) {
                    array.remove(i);
                    skipList.remove(new Weighted<Integer>(null, i));
                }
            }
            testFind();
        }
        testFind();
    }

    @Test
    public void testFind() {
        for(int i : array)
            assertEquals("Inclusion", true, skipList.find(new Weighted<Integer>(null, i), null, null));
        for(int i = 0; i < testSize;) {
            int num = randSign() * (int)(Integer.MAX_VALUE * Rnd.dbl());
            if (!array.contains(num))
                assertEquals("Exclusion", false, skipList.find(new Weighted<Integer>(null, i), null, null));
            else
                continue;
            i++;
        }
    }
    
    @Test
    public void testDecreaseKey() {
    	
    	for(int i : (TreeSet<Integer>)array.clone()) {
    		int newValue = i - (int)(100*Rnd.dbl());
    		if (!array.contains(newValue)) {
    			array.remove(i);
    			array.add(newValue);
    			skipList.decreaseKey(new Weighted<Integer>(null, i), newValue);
    		}
    	}
    	testFind();
    }

}
