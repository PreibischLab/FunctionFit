package fit.circular;

import fit.util.TransformUtil;
import mpicbg.models.Point;

public class BruteForceShapePointDistance< S extends ClosedContinousShape2D > implements ShapePointDistance< S >
{
	final double step;

	public BruteForceShapePointDistance()
	{
		this( 0.001 );
	}

	public BruteForceShapePointDistance( final double step )
	{
		this.step = step;
	}

	@Override
	public double distanceTo( final Point point, final S shape )
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

	public double minDistanceAt(
			final Point point,
			final double[] minDistPoint,
			final S shape )
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
