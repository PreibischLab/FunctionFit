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
package fit.util;

import Jama.Matrix;
import Jama.SingularValueDecomposition;
import mpicbg.models.NoninvertibleModelException;

/**
 * @author Stephan Preibisch (stephan.preibisch@gmx.de) and Timothee Lionnet
 */
public class MatrixFunctions
{
	/**
	 * Cannot instantiate this class
	 */
	private MatrixFunctions() {}
	
	final public static void invert2x2( final float[] matrix )
	{
		final float a = matrix[ 0 ];
		final float b = matrix[ 1 ];
		final float c = matrix[ 2 ];
		final float d = matrix[ 3 ];
		
		final float det = 1.0f / ( a*d - b*c );
		
		matrix[ 0 ] = d * det;
		matrix[ 1 ] = -b * det;
		matrix[ 2 ] = -c * det;
		matrix[ 3 ] = a * det;
	}

	final public static void invert2x2( final double[] matrix )
	{
		final double a = matrix[ 0 ];
		final double b = matrix[ 1 ];
		final double c = matrix[ 2 ];
		final double d = matrix[ 3 ];
		
		final double det = 1.0 / ( a*d - b*c );
		
		matrix[ 0 ] = d * det;
		matrix[ 1 ] = -b * det;
		matrix[ 2 ] = -c * det;
		matrix[ 3 ] = a * det;
	}

	final static public void invert3x3( final double[] m ) throws NoninvertibleModelException
	{
		assert m.length == 9 : "Matrix3x3 supports 3x3 double[][] only.";
		
		final double det = det3x3( m );
		if ( det == 0 ) throw new NoninvertibleModelException( "Matrix not invertible." );
		
		final double i00 = ( m[ 4 ] * m[ 8 ] - m[ 5 ] * m[ 7 ] ) / det;
		final double i01 = ( m[ 2 ] * m[ 7 ] - m[ 1 ] * m[ 8 ] ) / det;
		final double i02 = ( m[ 1 ] * m[ 5 ] - m[ 2 ] * m[ 4 ] ) / det;
		
		final double i10 = ( m[ 5 ] * m[ 6 ] - m[ 3 ] * m[ 8 ] ) / det;
		final double i11 = ( m[ 0 ] * m[ 8 ] - m[ 2 ] * m[ 6 ] ) / det;
		final double i12 = ( m[ 2 ] * m[ 3 ] - m[ 0 ] * m[ 5 ] ) / det;
		
		final double i20 = ( m[ 3 ] * m[ 7 ] - m[ 4 ] * m[ 6 ] ) / det;
		final double i21 = ( m[ 1 ] * m[ 6 ] - m[ 0 ] * m[ 7 ] ) / det;
		final double i22 = ( m[ 0 ] * m[ 4 ] - m[ 1 ] * m[ 3 ] ) / det;
		
		m[ 0 ] = i00;
		m[ 1 ] = i01;
		m[ 2 ] = i02;

		m[ 3 ] = i10;
		m[ 4 ] = i11;
		m[ 5 ] = i12;

		m[ 6 ] = i20;
		m[ 7 ] = i21;
		m[ 8 ] = i22;
	}

	
	/**
	 * Calculate the determinant of a matrix given as a float[] (row after row).
	 * 
	 * @author Stephan Saalfeld
	 * 
	 * @param a matrix given row by row
	 * 
	 * @return determinant
	 */
	final static public double det3x3( final double[] a )
	{
		assert a.length == 9 : "Matrix3x3 supports 3x3 float[][] only.";
		
		return
			a[ 0 ] * a[ 4 ] * a[ 8 ] +
			a[ 3 ] * a[ 7 ] * a[ 2 ] +
			a[ 6 ] * a[ 1 ] * a[ 5 ] -
			a[ 2 ] * a[ 4 ] * a[ 6 ] -
			a[ 5 ] * a[ 7 ] * a[ 0 ] -
			a[ 8 ] * a[ 1 ] * a[ 3 ];
	}

	/**
	 * Computes the pseudo-inverse of a matrix using Singular Value Decomposition
	 * 
	 * @param M - the input {@link Matrix}
	 * @param threshold - the threshold for inverting diagonal elements (suggested 0.001)
	 * @return the inverted {@link Matrix} or an approximation with lowest possible squared error
	 */
	final public static Matrix computePseudoInverseMatrix( final Matrix M, final double threshold )
	{
		final SingularValueDecomposition svd = new SingularValueDecomposition( M );

		Matrix U = svd.getU(); // U Left Matrix
		final Matrix S = svd.getS(); // W
		final Matrix V = svd.getV(); // VT Right Matrix

		double temp;

		// invert S
		for ( int j = 0; j < S.getRowDimension(); ++j )
		{
			temp = S.get( j, j );

			if ( temp < threshold ) // this is an inaccurate inverting of the matrix 
				temp = 1.0 / threshold;
			else 
				temp = 1.0 / temp;
			
			S.set( j, j, temp );
		}

		// transponse U
		U = U.transpose();

		//
		// compute result
		//
		return ((V.times(S)).times(U));
	}
}
