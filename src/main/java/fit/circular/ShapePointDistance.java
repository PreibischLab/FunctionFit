package fit.circular;

import mpicbg.models.Point;

public interface ShapePointDistance< S extends ClosedContinousShape2D, D extends ShapePointDistance< S, D, F >, F extends ShapePointDistanceFactory< S, D, F > >
{
	/**
	 * computes the distance of a point to the closest point on the ellipse
	 * 
	 * @param point
	 * @return distance
	 */
	public double distanceTo( final Point point );

	/**
	 * called when the ellipse parameters change (fitting or setting)
	 */
	public void notifyParameterChange();

	public F factory();
	public S getShape();
}
