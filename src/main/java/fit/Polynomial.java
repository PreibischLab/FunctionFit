package fit;

import mpicbg.models.Point;

/**
 * 
 * @author Varun Kapoor and Stephan Preibisch
 */
public interface Polynomial < F extends Polynomial< F, P >, P extends Point > extends Function< F, P >
{
	public int degree();
	public double getCoefficient( final int j );
}
