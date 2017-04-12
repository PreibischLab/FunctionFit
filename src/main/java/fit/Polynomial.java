package fit;

import mpicbg.models.Point;

/**
 * 
 * @author Varun Kapoor and Stephan Preibisch
 */
public interface Polynomial < P extends Point > extends Function< P >
{
	public int degree();
	public double getCoefficient( final int j );
}
