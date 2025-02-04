/*-
 * #%L
 * code for function fitting
 * %%
 * Copyright (C) 2015 - 2025 Developers
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
package fit.polynomial;

import java.util.ArrayList;
import java.util.Collection;

import fit.AbstractFunction;
import fit.InterpolatedFunction;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;

/**
 * @author Stephan Preibisch
 */

public class InterpolatedPolynomial<
		A extends AbstractFunction< A > & Polynomial< A, Point >,
		B extends AbstractFunction< B > & Polynomial< B, Point > >
	extends InterpolatedFunction< A, B, InterpolatedPolynomial< A, B > >
	implements Polynomial< InterpolatedPolynomial< A, B >, Point >
{
	private static final long serialVersionUID = 6929934343495578299L;

	public Polynomial< ?, Point > interpolatedFunction;

	public InterpolatedPolynomial( final A a, final B b, double lambda )
	{
		super( a, b, lambda );

		// use the higher-order polynom to fit a function to interpolated points
		if ( a.degree() > b.degree() )
			interpolatedFunction = a.copy();
		else
			interpolatedFunction = b.copy();
	}

	@Override
	protected void interpolate( final Collection< Point > points ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		final ArrayList< Point > interpolatedPoints = new ArrayList< Point >();

		for ( final Point p : points )
		{
			final double x = p.getW()[ 0 ];

			final double y1 = a.predict( x );
			final double y2 = b.predict( x );

			interpolatedPoints.add( new Point( new double[]{ x, l1 * y1 + lambda * y2 } ) );
		}

		interpolatedFunction.fitFunction( interpolatedPoints );
	}

	@Override
	public double predict( final double x ) { return interpolatedFunction.predict( x ); }

	@Override
	public double distanceTo( final Point point ) { return interpolatedFunction.distanceTo( point ); }

	@Override
	public int degree() { return interpolatedFunction.degree(); }

	@Override
	public double getCoefficient( final int j ) { return interpolatedFunction.getCoefficient( j ); }

	@Override
	public InterpolatedPolynomial< A, B > copy()
	{
		final InterpolatedPolynomial< A, B > copy = new InterpolatedPolynomial< A, B >( a.copy(), b.copy(), lambda );

		// it must be and AbstractFunction since it is A or B
		copy.interpolatedFunction = interpolatedFunction.copy();

		copy.setCost( getCost() );

		return copy;
	}

	public static void main( String[] args )
	{
		
	}
}
