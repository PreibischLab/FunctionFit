/*-
 * #%L
 * code for function fitting
 * %%
 * Copyright (C) 2015 - 2023 Developers
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Preibisch Lab nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
