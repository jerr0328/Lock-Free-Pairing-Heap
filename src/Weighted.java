/**
 * Just a wrapper class for a weighted object.
 * 
 * @author Charles Newton
 */
public class Weighted<T> {
	private int weight;
	private T obj;
	
	public Weighted(T obj, int weight) {
		this.obj = obj;
		this.weight = weight;
	}
	
	public T get() {
		return obj;
	}
	
	public int getWeight() {
		return weight;
	}
	
	public boolean compareTo(Weighted<T> o)
	{
		return (o.obj == obj && o.weight == weight);
	}
}
