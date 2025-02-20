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

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collection;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.interfaces.decomposition.EigenDecomposition;
import org.ejml.interfaces.linsol.LinearSolver;
import org.ejml.interfaces.linsol.LinearSolverDense;
import org.ejml.dense.row.CommonOps_DDRM;

import fit.util.TransformUtil;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.Overlay;
import mpicbg.models.AffineModel2D;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NoninvertibleModelException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import net.imglib2.img.display.imagej.ImageJFunctions;

/**
 * 
 * @author Stephan Preibisch, Peter Abeles, 
 *
 */
public class Ellipse extends AbstractShape2D< Ellipse >
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 5342301083570896246L;

	final static int minNumPoints = 4;

	/**
	 * coefficients
	 */
	protected double a, b, c, d, e, f;

	/**
	 * general parameters
	 */
	protected double xc, yc, g, axis0, axis1, rAxis0, rAxis1;

	/**
	 * maps the ellipse to a circle
	 */
	protected AffineModel2D ellipseToUnitCircle;

	/**
	 * the function that computes the distance between a point and an ellipse
	 */
	final protected ShapePointDistance< Ellipse, ?, ? > distF;

	public Ellipse( final ShapePointDistanceFactory< Ellipse, ?, ? > factory ) { this( 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, new AffineModel2D(), factory ); }

	public Ellipse( final double a, final double b, final double c, final double d, final double e, final double f, final ShapePointDistanceFactory< Ellipse, ?, ? > factory )
	{
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.e = e;
		this.f = f;

		computeEllipseParameters();

		// has to be initialized after ellipse parameters are computed
		this.distF = factory.create( this );
	}

	protected Ellipse(
			final double a,
			final double b,
			final double c,
			final double d,
			final double e,
			final double f,
			final double xc,
			final double yc,
			final double g,
			final double axis0,
			final double axis1,
			final double rAxis0,
			final double rAxis1,
			final AffineModel2D ellipseToCircle,
			final ShapePointDistanceFactory< Ellipse, ?, ? > factory )
	{
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.e = e;
		this.f = f;

		this.xc = xc;
		this.yc = yc;
		this.g = g;
		this.axis0 = axis0;
		this.axis1 = axis1;
		this.rAxis0 = rAxis0;
		this.rAxis1 = rAxis1;

		this.ellipseToUnitCircle = ellipseToCircle;

		// has to be initialized after ellipse parameters are computed
		this.distF = factory.create( this );
	}

	@Override
	public int getMinNumPoints() { return minNumPoints; }

	/*
	 * computes the radius of the ellipse at a certain position in polar coordinates
	 * 
	 * @param t - polar angle (0 <= t < 2*PI)
	 * @return - the radius
	 */
	@Override
	public double getRadiusAt( final double t )
	{
		// taken from: https://math.stackexchange.com/q/1638614
		final double cos = Math.cos( t );
		final double sin = Math.sin( t );

		return Math.sqrt( g / ( a*cos*cos + 2*b*cos*sin + c*sin*sin ) );
	}

	/*
	 * computes the point in x on the ellipse at a certain position in polar coordinates
	 * 
	 * @param t - polar angle (0 <= t < 2*PI)
	 * @return - the x coordinate
	 */
	@Override
	public double getPointXAt( final double t )
	{
		return xc + getRadiusAt( t ) * Math.cos( t );
	}

	/*
	 * computes the point in y on the ellipse at a certain position in polar coordinates
	 * 
	 * @param t - polar angle (0 <= t < 2*PI)
	 * @return - the y coordinate
	 */
	@Override
	public double getPointYAt( final double t )
	{
		return yc + getRadiusAt( t ) * Math.sin( t );
	}

	/**
	 * Checks to see if the parameters define an ellipse using the
	 * {@code a*c - b*b > 0} constraint.
	 * 
	 * @return true if it's an ellipse or false if not
	 */
	public boolean isEllipse()
	{
		// fitting code taken from: https://www.javatips.net/api/GeoRegression-master/main/src/georegression/fitting/ellipse/FitEllipseAlgebraic.java
		// @author Peter Abeles (released under http://www.apache.org/licenses/LICENSE-2.0)
		return a * c - b * b > 0;
	}

	@Override
	public void fitFunction( final Collection< Point > points )
			throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		final int numPoints = points.size();

		if ( numPoints < minNumPoints )
			throw new NotEnoughDataPointsException( "Not enough points, at least " + minNumPoints + " are necessary and available are: " + numPoints );

		// fitting code taken from: https://www.javatips.net/api/GeoRegression-master/main/src/georegression/fitting/ellipse/FitEllipseAlgebraic.java
		// @author Peter Abeles (released under http://www.apache.org/licenses/LICENSE-2.0)

		// qudratic part of design matrix
		final DMatrixRMaj D1 = new DMatrixRMaj( 3, 1 );
		// linear part of design matrix
		final DMatrixRMaj D2 = new DMatrixRMaj( 3, 1 );

		// quadratic part of scatter matrix
		final DMatrixRMaj S1 = new DMatrixRMaj( 3, 3 );
		// combined part of scatter matrix
		final DMatrixRMaj S2 = new DMatrixRMaj( 3, 3 );
		// linear part of scatter matrix
		final DMatrixRMaj S3 = new DMatrixRMaj( 3, 3 );
		// Reduced scatter matrix
		final DMatrixRMaj M = new DMatrixRMaj( 3, 3 );

		// storage for intermediate steps
		final DMatrixRMaj T = new DMatrixRMaj( 3, 3 );
		final DMatrixRMaj Ta1 = new DMatrixRMaj( 3, 1 );
		final DMatrixRMaj S2_tran = new DMatrixRMaj( 3, 3 );

		final LinearSolverDense<DMatrixRMaj> solver = LinearSolverFactory_DDRM.linear( 3 );
		final EigenDecomposition< DMatrixRMaj > eigen = DecompositionFactory_DDRM.eig( 3, true, false );

		final int N = points.size();

		// Construct the design matrices. linear and quadratic
		D1.reshape( N, 3 );
		D2.reshape( N, 3 );
		int index = 0;

		for ( final Point p : points )
		{
			final double x = p.getW()[ 0 ];
			final double y = p.getW()[ 1 ];

			// fill in each row one at a time
			D1.data[ index ] = x * x;
			D2.data[ index++ ] = x;
			D1.data[ index ] = x * y;
			D2.data[ index++ ] = y;
			D1.data[ index ] = y * y;
			D2.data[ index++ ] = 1;
		}

		// Compute scatter matrix
		CommonOps_DDRM.multTransA( D1, D1, S1 ); // S1 = D1'*D1
		CommonOps_DDRM.multTransA( D1, D2, S2 ); // S2 = D1'*D2
		CommonOps_DDRM.multTransA( D2, D2, S3 ); // S3 = D2'*D2

		// for getting a2 from a1
		// T = -inv(S3)*S2'
		if ( !solver.setA( S3 ) )
			throw new IllDefinedDataPointsException( "Could not fit ellipse, failed at T = -inv(S3)*S2'" );

		CommonOps_DDRM.transpose( S2, S2_tran );
		CommonOps_DDRM.changeSign( S2_tran );
		solver.solve( S2_tran, T );

		// Compute reduced scatter matrix
		// M = S1 + S2*T
		CommonOps_DDRM.mult( S2, T, M );
		CommonOps_DDRM.add( M, S1, M );

		// Premultiply by inv(C1). inverse of constraint matrix
		for (int col = 0; col < 3; col++)
		{
			final double m0 = M.unsafe_get( 0, col );
			final double m1 = M.unsafe_get( 1, col );
			final double m2 = M.unsafe_get( 2, col );

			M.unsafe_set( 0, col, m2 / 2 );
			M.unsafe_set( 1, col, -m1 );
			M.unsafe_set( 2, col, m0 / 2 );
		}

		if ( !eigen.decompose( M ) )
			throw new IllDefinedDataPointsException( "Could not fit ellipse, failed at eigen.decompose( M )" );

		DMatrixRMaj a1 = selectBestEigenVector( eigen );
		if ( a1 == null )
			throw new IllDefinedDataPointsException( "Could not fit ellipse, could not find best eigenvector." );

		// ellipse coefficients
		CommonOps_DDRM.mult( T, a1, Ta1 );

		this.a = a1.data[ 0 ];
		this.b = a1.data[ 1 ] / 2;
		this.c = a1.data[ 2 ];
		this.d = Ta1.data[ 0 ] / 2;
		this.e = Ta1.data[ 1 ] / 2;
		this.f = Ta1.data[ 2 ];

		computeEllipseParameters();
	}

	@Override
	public double eval( final double x, final double y )
	{
		return a*x*x + 2*b*x*y + c*y*y + 2*d*x + 2*e*y + f;
	}

	@Override
	public double distanceTo( final Point point )
	{
		return distF.distanceTo( point );
	}

	@Override
	public Ellipse copy()
	{
		final Ellipse ellipse = new Ellipse( a, b, c, d, e, f, xc, yc, g, axis0, axis1, rAxis0, rAxis1, ellipseToUnitCircle, distF.factory() );
		ellipse.setCost( getCost() );

		return ellipse;
	}

	@Override
	public void set( final Ellipse e )
	{
		this.a = e.a;
		this.b = e.b;
		this.c = e.c;
		this.d = e.d;
		this.e = e.e;
		this.f = e.f;

		this.xc = e.xc;
		this.yc = e.yc;
		this.g = e.g;
		this.axis0 = e.axis0;
		this.axis1 = e.axis1;
		this.rAxis0 = e.rAxis0;
		this.rAxis1 = e.rAxis1;
		this.ellipseToUnitCircle = e.ellipseToUnitCircle.copy();

		this.setCost( e.getCost() );

		this.distF.notifyParameterChange();
	}

	@Override
	public void intersectsAt( final double[] p, final double[] i )
	{
		// we transform the ellipse to a unit circle together with the point
		// where we can easily compute the point on the ellipse that the straight line intersects with

		// transformed point in circle space to whom we compute the distance
		i[ 0 ] = p[ 0 ];
		i[ 1 ] = p[ 1 ];

		ellipseToUnitCircle.applyInPlace( i );

		// intersection point on the circle
		final double len = Math.sqrt( i[ 0 ]*i[ 0 ] + i[ 1 ]*i[ 1 ] );

		i[ 0 ] /= len;
		i[ 1 ] /= len;

		// map the intersection point back to the ellipse
		try
		{
			ellipseToUnitCircle.applyInverseInPlace( i );
		}
		catch ( NoninvertibleModelException e )
		{
			i[ 0 ] = i[ 1 ] = Double.NaN;
			e.printStackTrace();
		}
	}
	
	protected DMatrixRMaj selectBestEigenVector( final EigenDecomposition< DMatrixRMaj > eigen )
	{
		int bestIndex = -1;
		double bestCond = Double.MAX_VALUE;

		for (int i = 0; i < eigen.getNumberOfEigenvalues(); i++)
		{
			final DMatrixRMaj v = eigen.getEigenVector( i );

			if ( v == null ) // TODO WTF?!?!
				continue;

			// evaluate a'*C*a = 1
			final double cond = 4 * v.get( 0 ) * v.get( 2 ) - v.get( 1 ) * v.get( 1 );
			final double condError = ( cond - 1 ) * ( cond - 1 );

			if ( cond > 0 && condError < bestCond )
			{
				bestCond = condError;
				bestIndex = i;
			}
		}

		if ( bestIndex == -1 )
			return null;

		return eigen.getEigenVector( bestIndex );
	}

	protected void computeEllipseParameters()
	{
		// taken from: https://math.stackexchange.com/q/1638614

		// cancel linear terms
		// a. Axc+Byc+D=0
		// b. Bxc+Cyc+E=0

		// 1. yc=(-E-Bxc)/C   [from b.]
		// 2. Axc+B((-E-Bxc)/C)=-D   [put 1. into a.]
		// 3. CAxc-BE-B^2xc=-CD
		// 4. CAxc-B^2xc=BE-CD
		// 5. xc(CA-B^2)=BE-CD
		// 6. xc = (BE-CD)/(CA-B^2)

		// 7. Byc=-D-Axc && Cyc=-E-Bxc
		// 8. yc=(-D-Axc)/B && yc=(-E-Bxc)/C

		this.xc = (b*e-c*d)/(c*a-b*b);
		this.yc = (-d-a*xc)/b;
		//final double yc2 = (-e-b*xc)/c;

		//System.out.println( "xc: " + xc );
		//System.out.println( "yc: " + yc + " && " + yc2 );

		// compute g
		// Ax2c+2Bxcyc+Cy2c−F=G
		this.g = a*xc*xc + 2*b*xc*yc + c*yc*yc - f;

		//System.out.println( "g: " + g );

		// compute major axes
		this.axis0 = 0.5 * Math.atan( 2*b / (a - c ) ) + 0 * (Math.PI / 2.0);
		this.axis1 = 0.5 * Math.atan( 2*b / (a - c ) ) + 1 * (Math.PI / 2.0);

		//System.out.println( "axis 0: " + axis0 + " " + Math.toDegrees( axis0 ) );
		//System.out.println( "axis 1: " + axis1 + " " + Math.toDegrees( axis1 ) );

		// compute max radii
		this.rAxis0 = getRadiusAt( axis0 );
		this.rAxis1 = getRadiusAt( axis1 );

		//System.out.println( "r(axis 0): " + rAxis0 );
		//System.out.println( "r(axis 1): " + rAxis1 );

		this.ellipseToUnitCircle = transformEllipseToCircle( this );

		// maybe the distance function needs to update itself (it is null during object initialization)
		if ( distF != null )
			this.distF.notifyParameterChange();
	}

	public static AffineModel2D axisAlignEllipse( final Ellipse e )
	{
		final AffineModel2D model = new AffineModel2D();
		AffineTransform at;

		// center at 0,0
		at = new AffineTransform();
		at.translate( -e.xc, -e.yc );
		model.preConcatenate( TransformUtil.getModel( at ) );

		// rotate major axis onto the x axis
		at = new AffineTransform();
		at.rotate( -e.axis0 );
		model.preConcatenate( TransformUtil.getModel( at ) );

		return model;
	}

	public static AffineModel2D transformEllipseToCircle( final Ellipse e )
	{
		final AffineModel2D model = axisAlignEllipse( e );

		// scale in x and y to radius 1
		AffineTransform at = new AffineTransform();
		at.scale( 1.0 / e.rAxis0, 1.0 / e.rAxis1 );
		model.preConcatenate( TransformUtil.getModel( at ) );

		return model;
	}

	@Override
	public void drawCenter( final Overlay overlay )
	{
		TransformUtil.drawDiamond( overlay, xc, yc );
	}

	@Override
	public void draw( final Overlay overlay, final double step )
	{
		TransformUtil.drawOutline( overlay, this, step );
	}

	public void drawAxes( final Overlay overlay )
	{
		double x0, y0;

		x0 = getPointXAt( axis0 );//rAxis0 * Math.cos( axis0 );
		y0 = getPointYAt( axis0 );//rAxis0 * Math.sin( axis0 );
		overlay.add( new Line( x0, y0, xc, yc ) );

		x0 = getPointXAt( axis0 + Math.PI );//rAxis0 * Math.cos( axis0 );
		y0 = getPointYAt( axis0 + Math.PI );//rAxis0 * Math.sin( axis0 );
		overlay.add( new Line( x0, y0, xc, yc ) );

		x0 = getPointXAt( axis1 );//rAxis1 * Math.cos( axis1 );
		y0 = getPointYAt( axis1 );//rAxis1 * Math.sin( axis1 );
		overlay.add( new Line( x0, y0, xc, yc ) );

		x0 = getPointXAt( axis1 + Math.PI );//rAxis1 * Math.cos( axis1 );
		y0 = getPointYAt( axis1 + Math.PI );//rAxis1 * Math.sin( axis1 );
		overlay.add( new Line( x0, y0, xc, yc ) );
	}

	@Override
	public double area()
	{
		return Math.PI * rAxis0 * rAxis1;
	}

	@Override
	public String toString()
	{
		return a + "*x^2 + 2*" + b + "*x*y + " + c + "*y^2 + 2*" + d + "*x + 2*" + e + "*y + " + f + " = 0";
	}

	public static ArrayList< Point > toPoints( final double[][] points )
	{
		final ArrayList< Point > pObjs = new ArrayList< Point >();

		for ( final double[] point : points )
			pObjs.add( new Point( point ) );

		return pObjs;
	}

	public static void main( String[] args ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
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
		ellipse.fitFunction( toPoints( points ) );

		System.out.println( ellipse + " " + ellipse.isEllipse() );

		new ImageJ();
		final ImagePlus imp = ImageJFunctions.show( TransformUtil.drawDistanceBruteForce( ellipse, 1024, 1024 ) );
		imp.setDisplayRange( 0, imp.getDisplayRangeMax() );

		Overlay o = imp.getOverlay();
		if ( o == null )
			o = new Overlay();

		ellipse.drawCenter( o );
		ellipse.drawAxes( o );
		ellipse.draw( o, 0.01 );

		final double[] p = new double[] { 650, 430 };
		final double[] i = new double[ 2 ];
		ellipse.intersectsAt( p, i );

		TransformUtil.drawCross( o, p[ 0 ], p[ 1 ] );
		TransformUtil.drawCross( o, i[ 0 ], i[ 1 ] );

		final double[] dp = new double[ 2 ];
		final double distBF = new BruteForceShapePointDistanceFactory< Ellipse >( 0.001 ).create( ellipse ).minDistanceAt( new Point( p ), dp );

		TransformUtil.drawCross( o, dp[ 0 ], dp[ 1 ] );
		System.out.println( "dist (brute force) = " + distBF );
		System.out.println( "dist (ellipse) = " + ellipse.distanceTo( new Point( p ) ) );

		imp.setOverlay( o );
		imp.updateAndDraw();
	}
}
