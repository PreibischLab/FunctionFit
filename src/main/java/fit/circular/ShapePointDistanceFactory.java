package fit.circular;

public interface ShapePointDistanceFactory< S extends ClosedContinousShape2D, D extends ShapePointDistance< S, D, F >, F extends ShapePointDistanceFactory< S, D, F > >
{
	public D create( final S shape );
}
