package fit.circular;

import mpicbg.models.Point;

public interface ShapePointDistance< S extends ClosedContinousShape2D >
{
	public S getShape();
	public double distanceTo( final Point point );
}
