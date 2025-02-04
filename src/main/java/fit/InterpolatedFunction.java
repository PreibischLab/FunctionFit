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
package fit;

import java.util.Collection;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;

/**
 * Interpolation of two AbstractFunctions, inspired by Interpolated Models by Stephan Saalfeld
 * 
 * @author Stephan Preibisch
 */
public abstract class InterpolatedFunction< A extends AbstractFunction< A >, B extends AbstractFunction< B >, M extends InterpolatedFunction< A, B, M > > extends AbstractFunction2D< M >
{
	private static final long serialVersionUID = -8524786898599474286L;

	final protected A a;
	final protected B b;
	protected double lambda;
	protected double l1;

	public InterpolatedFunction( final A a, final B b, final double lambda )
	{
		this.a = a;
		this.b = b;
		this.lambda = lambda;
		l1 = 1.0 - lambda;
	}

	public A getA() { return a; }
	public B getB() { return b; }
	public double getLambda() { return lambda; }

	public void setLambda( final double lambda )
	{
		this.lambda = lambda;
		this.l1 = 1.0f - lambda;
	}

	@Override
	public int getMinNumPoints() { return Math.max( a.getMinNumPoints(), b.getMinNumPoints() ); }

	@Override
	public void set( final M m )
	{
		a.set( m.a );
		b.set( m.b );
		lambda = m.lambda;
		l1 = m.l1;
		cost = m.cost;
	}

	@Override
	public void fitFunction( final Collection< Point > points )
			throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		a.fitFunction( points );
		b.fitFunction( points );

		interpolate( points );
	}

	protected abstract void interpolate( final Collection< Point > points )
			throws NotEnoughDataPointsException, IllDefinedDataPointsException;
}
