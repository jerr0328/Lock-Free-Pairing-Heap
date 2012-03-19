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
	
	public int compareTo(Weighted<T> rhs) {
		return this.weight - rhs.weight;
	}
	
	public boolean equals(Weighted<T> o)
	{
		return (o.obj == obj && o.weight == weight);
	}

	public void setWeight(int i) {
		this.weight = i;
	}
	
	public Weighted<T> clone() {
		return new Weighted<T>(this.obj, this.weight);
	}

}
