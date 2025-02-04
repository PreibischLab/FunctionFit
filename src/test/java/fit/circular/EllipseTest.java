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
package fit.circular;

import org.junit.Test;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;

import static org.junit.Assert.assertTrue;


public class EllipseTest {

	@Test
	public void test() throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		final double[][] points = new double[ 8 ][ 2 ];

		points[ 0 ][ 0 ] = 320;
		points[ 0 ][ 1 ] = 443;

		points[ 0 ][ 0 ] = 0; // non-axis-aligned ellipse
		points[ 0 ][ 1 ] = 17;

		points[ 1 ][ 0 ] = 377;
		points[ 1 ][ 1 ] = 377;

		points[ 2 ][ 0 ] = 507;
		points[ 2 ][ 1 ] = 350;

		points[ 3 ][ 0 ] = 640;
		points[ 3 ][ 1 ] = 378;

		points[ 4 ][ 0 ] = 694;
		points[ 4 ][ 1 ] = 444;

		points[ 5 ][ 0 ] = 639;
		points[ 5 ][ 1 ] = 511;

		points[ 6 ][ 0 ] = 508;
		points[ 6 ][ 1 ] = 538;

		points[ 7 ][ 0 ] = 376;
		points[ 7 ][ 1 ] = 511;

		//final ShapePointDistanceFactory< Ellipse, ?, ? > factory = new BruteForceShapePointDistanceFactory< Ellipse >( 0.001 );
		final ShapePointDistanceFactory< Ellipse, ?, ? > factory = new EllipsePointDistanceFactory( 10 );

		final Ellipse ellipse = new Ellipse( factory );
		ellipse.fitFunction( Ellipse.toPoints( points ) );

		assertTrue(ellipse.isEllipse());
	}
}
