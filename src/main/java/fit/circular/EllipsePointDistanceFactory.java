package fit.circular;

public class EllipsePointDistanceFactory implements
	ShapePointDistanceFactory < Ellipse, EllipsePointDistance, EllipsePointDistanceFactory >
{
	public static int defaultIter = 10;
	final int iter;

	public EllipsePointDistanceFactory( final int iter ){ this.iter = iter; }
	public EllipsePointDistanceFactory() { this( defaultIter ); }

	@Override
	public EllipsePointDistance create( final Ellipse shape )
	{
		return new EllipsePointDistance( shape, iter, this );
	}
}
