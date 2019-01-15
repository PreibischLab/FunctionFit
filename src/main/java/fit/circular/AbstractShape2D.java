package fit.circular;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import fit.AbstractFunction;
import fit.PointFunctionMatch;
import mpicbg.models.AbstractModel;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.PointMatch;

/**
 * Adds a RANSAC with a specifc max gap between points in 2d on the x-axis
 * 
 * @author Stephan Saalfeld and Stephan Preibisch
 *
 * @param <M> the model
 */
public abstract class AbstractShape2D< M extends AbstractShape2D< M > > extends AbstractFunction< M > implements ClosedContinousShape2D
{
	private static final long serialVersionUID = 9102425001575237374L;

	/**
	 * Find the {@link AbstractModel} of a set of {@link PointMatch} candidates
	 * containing a high number of outliers using
	 * {@link #ransac(List, Collection, int, double, double, int) RANSAC}
	 * \citet[{FischlerB81}.
	 *
	 * @param candidates candidate data points inluding (many) outliers
	 * @param inliers remaining candidates after RANSAC
	 * @param iterations number of iterations
	 * @param epsilon maximal allowed transfer error
	 * @param minInlierRatio minimal number of inliers to number of
	 *   candidates
	 * @param minNumInliers minimally required absolute number of inliers
	 * @param maxArea max area of a closed shape (circle, ellipse)
	 * @param <P> some PointFunctionMatch
	 * 
	 * @throws NotEnoughDataPointsException if there are not enough points
	 * 
	 * @return true if {@link AbstractModel} could be estimated and inliers is not
	 *   empty, false otherwise.  If false, {@link AbstractModel} remains unchanged.
	 */
	@SuppressWarnings("deprecation")
	final public < P extends PointFunctionMatch >boolean ransac(
			final List< P > candidates,
			final Collection< P > inliers,
			final int iterations,
			final double epsilon,
			final double minInlierRatio,
			final int minNumInliers,
			final double minArea,
			final double maxArea,
			final double minRatio, // ratio = large axis / small axis
			final double maxRatio )
		throws NotEnoughDataPointsException
	{
		if ( candidates.size() < getMinNumMatches() )
			throw new NotEnoughDataPointsException( candidates.size() + " data points are not enough to solve the Model, at least " + getMinNumMatches() + " data points required." );

		cost = Double.MAX_VALUE;

		final M copy = copy();
		final M m = copy();

		inliers.clear();

		int i = 0;
		final HashSet< P > minMatches = new HashSet< P >();

A:		while ( i < iterations )
		{
			// choose model.MIN_SET_SIZE disjunctive matches randomly
			minMatches.clear();
			for ( int j = 0; j < getMinNumMatches(); ++j )
			{
				P p;
				do
				{
					p = candidates.get( ( int )( rnd.nextDouble() * candidates.size() ) );
				}
				while ( minMatches.contains( p ) );
				minMatches.add( p );
			}
			try { m.fit( minMatches ); }
			catch ( final IllDefinedDataPointsException e )
			{
				++i;
				continue;
			}

			final ArrayList< P > tempInliers = new ArrayList< P >();

			int numInliers = 0;
			boolean isGood = m.test( candidates, tempInliers, epsilon, minInlierRatio, minNumInliers, minArea, maxArea, minRatio, maxRatio );
			while ( isGood && numInliers < tempInliers.size() )
			{
				numInliers = tempInliers.size();
				try { m.fit( tempInliers ); }
				catch ( final IllDefinedDataPointsException e )
				{
					++i;
					continue A;
				}
				isGood = m.test( candidates, tempInliers, epsilon, minInlierRatio, minNumInliers, minArea, maxArea, minRatio, maxRatio  );
			}
			if (
					isGood &&
					m.betterThan( copy ) &&
					tempInliers.size() >= minNumInliers )
			{
				copy.set( m );
				inliers.clear();
				inliers.addAll( tempInliers );
			}
			++i;
		}
		if ( inliers.size() == 0 )
			return false;

		set( copy );
		return true;
	}

	/**
	 * Test the {@link AbstractModel} for a set of {@link PointMatch} candidates.
	 * Return true if the number of inliers / number of candidates is larger
	 * than or equal to min_inlier_ratio, otherwise false.
	 *
	 * Clears inliers and fills it with the fitting subset of candidates.
	 *
	 * Sets {@link #getCost() cost} = 1.0 - |inliers| / |candidates|.
	 *
	 * @param candidates set of point correspondence candidates
	 * @param inliers set of point correspondences that fit the model
	 * @param epsilon maximal allowed transfer error
	 * @param minInlierRatio minimal ratio |inliers| / |candidates| (0.0 is 0%, 1.0 is 100%)
	 * @param minNumInliers minimally required absolute number of inliers
	 * @param maxArea max area of a closed shape (circle, ellipse)
	 * @param <P> some PointFunctionMatch
	 * @return if successful
	 */
	public < P extends PointFunctionMatch > boolean test(
			final Collection< P > candidates,
			final List< P > inliers,
			final double epsilon,
			final double minInlierRatio,
			final int minNumInliers,
			final double minArea,
			final double maxArea,
			final double minRatio, // ratio = large axis / small axis
			final double maxRatio )
	{
		final double a = this.area();
		final double r = this.ratio();

		if ( a >= minArea && a <= maxArea && r >= minRatio && r <= maxRatio && this.test( candidates, inliers, epsilon, minInlierRatio, minNumInliers ) )
			return true;
		else
			return false;
	}
}
