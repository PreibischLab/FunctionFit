package fit.circular;

import fit.util.TransformUtil;
import mpicbg.models.Point;

public class BruteForceShapePointDistance< S extends ClosedContinousShape2D > implements ShapePointDistance< S, BruteForceShapePointDistance< S >, BruteForceShapePointDistanceFactory< S > >
{
	final double step;
	final S shape;
	final BruteForceShapePointDistanceFactory< S > factory;

	protected BruteForceShapePointDistance( final S shape, final double step, final BruteForceShapePointDistanceFactory< S > factory )
	{
		this.step = step;
		this.shape = shape;
		this.factory = factory;
	}

	@Override
	public BruteForceShapePointDistanceFactory< S > factory() { return factory; }

	@Override
	public S getShape() { return shape; }

	@Override
	public double distanceTo( final Point point )
	{
		final double x0 = point.getW()[ 0 ];
		final double y0 = point.getW()[ 1 ];

		double minSqDist = Double.MAX_VALUE;

		for ( double t = 0; t < 2*Math.PI; t += step )
		{
			final double x = shape.getPointXAt( t );
			final double y = shape.getPointYAt( t );

			final double sqDist = TransformUtil.squareDistance( x - x0, y - y0 );

			if ( sqDist < minSqDist )
				minSqDist = sqDist;
		}

		return Math.sqrt( minSqDist );
	}

	@Override
	public void notifyParameterChange()
	{
		// nothing to do in this case ...
	}

	public double minDistanceAt(
			final Point point,
			final double[] minDistPoint )
	{
		final double x0 = point.getW()[ 0 ];
		final double y0 = point.getW()[ 1 ];

		double minSqDist = Double.MAX_VALUE;

		for ( double t = 0; t < 2*Math.PI; t += step )
		{
			final double x = shape.getPointXAt( t );
			final double y = shape.getPointYAt( t );

			final double sqDist = TransformUtil.squareDistance( x - x0, y - y0 );

			if ( sqDist < minSqDist )
			{
				minSqDist = sqDist;
				minDistPoint[ 0 ] = x;
				minDistPoint[ 1 ] = y;
			}
		}

		return Math.sqrt( minSqDist );
	}
}
