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

import ij.gui.Overlay;

public interface ClosedContinousShape2D
{
	/*
	 * Computes the closest intersection point of a vector with the shape
	 * @param p - point
	 * @param i - intersection point
	 */
	public void intersectsAt( final double[] p, final double[] i );

	public double getPointXAt( final double t );

	public double getPointYAt( final double t );

	public double area();

	public void drawCenter( final Overlay overlay );

	public void draw( final Overlay overlay, final double step );

	/*
	 * computes the radius of the ellipse at a certain position in polar coordinates
	 * 
	 * @param t - polar angle (0 <= t < 2*PI)
	 * @return - the radius
	 */
	public double getRadiusAt( final double t );

	/*
	 * Computes the value of the underlying function at x,y.
	 * This is not an actual distance to the shape!
	 * 
	 * @param x - x coordinate
	 * @param y - y coordinate
	 * @return - function value
	 */
	public double eval( final double x, final double y );
}
