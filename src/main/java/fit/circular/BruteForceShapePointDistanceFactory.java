package fit.circular;

public class BruteForceShapePointDistanceFactory< S extends ClosedContinousShape2D > implements
	ShapePointDistanceFactory < S , BruteForceShapePointDistance< S >, BruteForceShapePointDistanceFactory< S > >
{
	public static double defaultStep = 0.01;
	final double step;

	public BruteForceShapePointDistanceFactory( final double step ){ this.step = step; }
	public BruteForceShapePointDistanceFactory() { this( defaultStep ); }

	@Override
	public BruteForceShapePointDistance< S > create( final S shape )
	{
		return new BruteForceShapePointDistance< S >( shape, step, this );
	}
}
