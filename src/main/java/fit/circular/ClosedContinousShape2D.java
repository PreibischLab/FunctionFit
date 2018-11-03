package fit.circular;

import ij.gui.Overlay;

public interface ClosedContinousShape2D
{
	/**
	 * Computes the closest intersection point of a vector with the shape
	 * @param p - point
	 * @param i - intersection point
	 */
	public void intersectsAt( final double[] p, final double[] i );
	public double getPointXAt( final double t );
	public double getPointYAt( final double t );
	public void drawCenter( final Overlay overlay );
	public void draw( final Overlay overlay, final double step );

	/**
	 * computes the radius of the ellipse at a certain position in polar coordinates
	 * 
	 * @param t - polar angle (0 <= t < 2*PI)
	 * @return - the radius
	 */
	public double getRadiusAt( final double t );

	/**
	 * Computes the value of the underlying function at x,y.
	 * This is not an actual distance to the shape!
	 * 
	 * @param x - x coordinate
	 * @param y - y coordinate
	 * @return - function value
	 */
	public double eval( final double x, final double y );
}
