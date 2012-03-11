import java.util.Random;

public class Rnd  {
    // TODO: Make this store the PRNG in 
    // thread-local storage (Java uses locks to handle multiple
    // threads accessing the PRNG.
    public static Random r;

    static {
    	r = new Random();
    }

    public static double dbl() {
    	return r.nextDouble();
    }
}