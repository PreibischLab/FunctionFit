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

import java.util.Random;

/**
 * Newton Raphson routine to get the shortest distance of a point from a
 * curve.
 * 
 * @author Varun Kapoor, Stephan Preibisch
 *
 */
public class NewtonRaphson
{
	public static int MAX_ITER = 1000000;
	public static double MIN_CHANGE = 1.0E-3;

	public double xc, xcNew, polyfunc, polyfuncdiff, delpolyfuncdiff, dmin, dMinDiff, secdelpolyfuncdiff, dminsecdiff;
	final int degree;
	final double[] powCache;
	double damp = 1;
	final Random rndx;

	public NewtonRaphson( final Random rndx, final int degree )
	{
		// Initial guesses for Newton Raphson
		this.rndx = rndx;
		this.xc = rndx.nextFloat();
		this.xcNew = rndx.nextFloat() * rndx.nextFloat();
		this.degree = degree;
		this.powCache = new double[ degree + 4 ];
	}

	public double run( final double x, final double y, final double[] coeff )
	{
		updatePowCache( xc );
		computeFunctions( coeff );

		int iteration = 0;

		do
		{
			xc = xcNew;
			dmin = (polyfunc - y) * polyfuncdiff + (xc - x);
			dMinDiff = polyfuncdiff * polyfuncdiff +  (polyfunc - y)* delpolyfuncdiff + 1;
			dminsecdiff = (polyfunc - y)*secdelpolyfuncdiff + delpolyfuncdiff * polyfuncdiff + 2 * polyfuncdiff * delpolyfuncdiff ;

			// Compute the first iteration of the new point

			++iteration;

			if ( iteration % 1000 == 0 )
			{
				damp = rndx.nextDouble();
				iterate();
				damp = 1;
			}
			else
			{
				iterate();
			}

			if ( Double.isNaN( xcNew ) )
				xcNew = xc;

			// Compute the functions and the required derivates at the new point
			delpolyfuncdiff = 0;
			polyfunc = 0;
			polyfuncdiff = 0;
			secdelpolyfuncdiff = 0;

			// precompute the powers
			updatePowCache( xcNew );
			computeFunctions( coeff );

			if ( iteration >= MAX_ITER )
				break;
		}
		while ( Math.abs( ( xcNew - xc ) ) > MIN_CHANGE );

		// After the solution is found compute the y co-oordinate of the point
		// on the curve
		polyfunc = 0;
		for (int j = degree; j >= 0; j--)
			polyfunc += coeff[j] * Math.pow(xc, j);

		// Get the distance of (x1, y1) point from the curve and return the
		// value	
		return distance( x, y, xc, polyfunc );
	}

	protected void updatePowCache( final double xc )
	{
		for ( int j = degree; j >= -3; j-- )
			if ( j >= 0 )
				powCache[ j + 3 ] = pow( xc, j );
			else
				powCache[ j + 3 ] = Math.pow( xc, j );
	}

	protected void computeFunctions( final double[] coeff )
	{
		for ( int j = degree; j >= 0; j-- )
		{
			double c = coeff[ j ];
			polyfunc += c * powCache[ j + 3 ];

			c *= j;
			polyfuncdiff += c * powCache[ j + 2 ];

			c *= ( j - 1 );
			delpolyfuncdiff += c * powCache[ j + 1 ];

			c *= ( j - 2 );
			secdelpolyfuncdiff += c * powCache[ j ];
		}
	}

	protected void iterate()
	{
		this.xcNew = iterate( xc, dmin, dMinDiff, dminsecdiff );
	}

	public double iterate( final double oldpoint, final double function, final double functionderiv, final double functionsecderiv )
	{
		return oldpoint -  (function / functionderiv) * (1 + damp * 0.5 * function * functionsecderiv / (functionderiv * functionderiv) );
	}

	public static double distance( final double minX, final double minY, final double maxX, final double maxY )
	{
		double tmp;
		double distance = 0;

		tmp = maxX - minX;
		distance += tmp*tmp;

		tmp = maxY - minY;
		distance += tmp*tmp;

		return Math.sqrt( distance );
	}

	public static double pow( final double a, final int b )
	{
		if ( b == 0 )
			return 1;
		else if ( b == 1 )
			return a;
		else
		{
			double result = a;

			for ( int i = 1; i < b; i++ )
				result *= a;

			return result;
		}
	}

}
