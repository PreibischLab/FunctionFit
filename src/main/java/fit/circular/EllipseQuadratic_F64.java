package fit.circular;

import java.awt.geom.AffineTransform;
import java.io.Serializable;
import java.util.ArrayList;

import ij.gui.Line;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import mpicbg.models.AffineModel2D;
import mpicbg.models.NoninvertibleModelException;
import net.imglib2.util.Util;

/**
 * <p>
 * In general quadratic form, an ellipse is described by 6-coefficients:<br>
 * {@code F(x,y) = a*x^2 + 2*b*x*y + c*y^2 + 2*d*x + 2*e*y + f = 0}<br>
 * {@code a*c - b*b > 0}<br>
 * where [a,b,c,d,e,f] are the coefficients and [x,y] is the coordinate of a
 * point on the ellipse.
 * </p>
 * 
 * <p>
 * NOTE: these parameters are unique only up to a scale factor.
 * </p>
 * 
 * @author Peter Abeles and Stephan Preibisch
 */
public class EllipseQuadratic_F64 implements Serializable
{
	/**
	 * coefficients
	 */
	public double a, b, c, d, e, f;

	/**
	 * general parameters
	 */
	public double xc, yc, g, axis0, axis1, rAxis0, rAxis1;

	public EllipseQuadratic_F64( double a, double b, double c, double d,
			double e, double f )
	{
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.e = e;
		this.f = f;
	}

	public double valueAt( final double x, final double y )
	{
		return a * x * x + 2 * b * x * y + c * y * y + 2 * d * x + 2 * e * y
				+ f;
	}

	public EllipseQuadratic_F64()
	{
	}

	public void getCenter()
	{
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
		final double yc2 = (-e-b*xc)/c;

		System.out.println( "xc: " + xc );
		System.out.println( "yc: " + yc + " && " + yc2 );

		// compute g
		// Ax2c+2Bxcyc+Cy2c−F=G
		this.g = a*xc*xc + 2*b*xc*yc + c*yc*yc - f;

		System.out.println( "g: " + g );

		// compute major axes
		this.axis0 = 0.5 * Math.atan( 2*b / (a - c ) ) + 0 * (Math.PI / 2.0);
		this.axis1 = 0.5 * Math.atan( 2*b / (a - c ) ) + 1 * (Math.PI / 2.0);

		System.out.println( "axis 0: " + axis0 + " " + Math.toDegrees( axis0 ) );
		System.out.println( "axis 1: " + axis1 + " " + Math.toDegrees( axis1 ) );

		// compute max radii
		this.rAxis0 = getRadiusAt( axis0 );
		this.rAxis1 = getRadiusAt( axis1 );

		System.out.println( "r(axis 0): " + rAxis0 );
		System.out.println( "r(axis 1): " + rAxis1 );
	}

	public double getRadiusAt( final double axis )
	{
		final double cos = Math.cos( axis );
		final double sin = Math.sin( axis );

		return Math.sqrt( g / ( a*cos*cos + 2*b*cos*sin + c*sin*sin ) );
	}

	public void distanceTo( final Overlay o, final double[] p )// final double xp, final double yp )
	{
		// we transform the ellipse to a unit circle together with the point
		// where we can easily compute the point on the ellipse that is closest

		double step = Math.PI / 4;

		// unit circle
		//final Circle2D circ = Circle2D.unitcircle();
		final Circle2D circ = new Circle2D( 500, 500, 100 );

		final AffineModel2D model = new AffineModel2D();
		AffineTransform at;

		// center at 0,0
		at = new AffineTransform();
		at.translate( -xc, -yc );
		model.preConcatenate( getModel( at ) );

		// rotate major axis onto the x axis
		at = new AffineTransform();
		at.rotate( -this.axis0 );
		model.preConcatenate( getModel( at ) );

		// scale in x and y to radius 1
		at = new AffineTransform();
		at.scale( 1.0 / rAxis0, 1.0 / rAxis1 );
		model.preConcatenate( getModel( at ) );

		// scale in x and y to radius 100
		at = new AffineTransform();
		at.scale( 100, 100 );
		model.preConcatenate( getModel( at ) );

		// move to 500,500
		at = new AffineTransform();
		at.translate( 500, 500 );
		model.preConcatenate( getModel( at ) );

		// transformed point in circle space to whom we compute the distance
		final double[] pt = model.apply( p );

		// intersection point on the circle
		final double[] it = new double[ 2 ];

		it[ 0 ] = pt[ 0 ] - 500;
		it[ 1 ] = pt[ 1 ] - 500;

		final double len = Math.sqrt( it[ 0 ]*it[ 0 ] + it[ 1 ]*it[ 1 ] );

		it[ 0 ] = (it[ 0 ] / len) * 100 + 500;
		it[ 1 ] = (it[ 1 ] / len) * 100 + 500;

		// intersection point on the ellipse
		double[] i = null;
		try
		{
			i = model.applyInverse( it );
		} catch ( NoninvertibleModelException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		o.add( new Line( p[0] - 1, p[1], p[0] + 1, p[1] ) );
		o.add( new Line( p[0], p[1] - 1, p[0], p[1] + 1 ) );
		
		o.add( new Line( pt[0] - 1, pt[1], pt[0] + 1, pt[1] ) );
		o.add( new Line( pt[0], pt[1] - 1, pt[0], pt[1] + 1 ) );

		o.add( new Line( it[ 0 ] - 1, it[ 1 ], it[ 0 ] + 1, it[ 1 ] ) );
		o.add( new Line( it[ 0 ], it[ 1 ] - 1, it[ 0 ], it[ 1 ] + 1 ) );

		o.add( new Line( i[ 0 ] - 1, i[ 1 ], i[ 0 ] + 1, i[ 1 ] ) );
		o.add( new Line( i[ 0 ], i[ 1 ] - 1, i[ 0 ], i[ 1 ] + 1 ) );

		System.out.println( Util.printCoordinates( p ) + " >> " + Util.printCoordinates( pt ) + " >> " + it[ 0 ] + "," + it[ 0 ] );

		final ArrayList< Double > xPoints = new ArrayList< Double >();
		final ArrayList< Double > yPoints = new ArrayList< Double >();

		for ( double t = 0; t < 2*Math.PI; t += 0.01 )
		{
			final double[] l = new double[] { getPointXAt( t ), getPointYAt( t ) };

			model.applyInPlace( l );

			xPoints.add( l[ 0 ] );
			yPoints.add( l[ 1 ] );
		}

		final float[] xP = new float[ xPoints.size() ];
		final float[] yP = new float[ yPoints.size() ];

		for ( int j = 0; j < xP.length; ++j )
		{
			xP[ j ] = xPoints.get( j ).floatValue();
			yP[ j ] = yPoints.get( j ).floatValue();
		}

		o.add( new PolygonRoi( xP, yP, Roi.POLYGON ) );

	}

	public static AffineModel2D getModel( final AffineTransform t )
	{
		final AffineModel2D m = new AffineModel2D();
		final double[] flatMatrix = new double[ 6 ];
		t.getMatrix( flatMatrix );
		m.set( flatMatrix[ 0 ], flatMatrix[ 1 ], flatMatrix[ 2 ], flatMatrix[ 3 ], flatMatrix[ 4 ], flatMatrix[ 5 ] );
		return m;
	}

	public void drawCenter( final Overlay overlay )
	{
		overlay.add( new Line( xc, yc - 5, xc, yc + 5 ) );
		overlay.add( new Line( xc - 5, yc, xc + 5, yc ) );

		overlay.add( new Line( xc, yc - 5, xc + 5, yc ) );
		overlay.add( new Line( xc + 5, yc, xc, yc + 5 ) );
		overlay.add( new Line( xc, yc + 5, xc - 5, yc ) );
		overlay.add( new Line( xc - 5, yc, xc, yc - 5 ) );
	}

	public void drawAxes( final Overlay overlay )
	{
		double x0, y0;

		x0 = xc + rAxis0 * Math.cos( axis0 );
		y0 = yc + rAxis0 * Math.sin( axis0 );
		overlay.add( new Line( x0, y0, xc, yc ) );

		x0 = xc - rAxis0 * Math.cos( axis0 );
		y0 = yc - rAxis0 * Math.sin( axis0 );
		overlay.add( new Line( x0, y0, xc, yc ) );

		x0 = xc + rAxis1 * Math.cos( axis1 );
		y0 = yc + rAxis1 * Math.sin( axis1 );
		overlay.add( new Line( x0, y0, xc, yc ) );

		x0 = xc - rAxis1 * Math.cos( axis1 );
		y0 = yc - rAxis1 * Math.sin( axis1 );
		overlay.add( new Line( x0, y0, xc, yc ) );
	}

	public double getPointXAt( final double t )
	{
		return xc + getRadiusAt( t ) * Math.cos( t );
	}

	public double getPointYAt( final double t )
	{
		return yc + getRadiusAt( t ) * Math.sin( t );
	}

	public void draw( final Overlay overlay, final double step )
	{
		final ArrayList< Double > xPoints = new ArrayList< Double >();
		final ArrayList< Double > yPoints = new ArrayList< Double >();

		for ( double t = 0; t < 2*Math.PI; t += step )
		{
			xPoints.add( getPointXAt( t ) );
			yPoints.add( getPointYAt( t ) );
		}

		final float[] xP = new float[ xPoints.size() ];
		final float[] yP = new float[ yPoints.size() ];

		for ( int i = 0; i < xP.length; ++i )
		{
			xP[ i ] = xPoints.get( i ).floatValue();
			yP[ i ] = yPoints.get( i ).floatValue();
		}

		overlay.add( new PolygonRoi( xP, yP, Roi.POLYGON ) );
	}

	public String toString()
	{
		return a + "*x^2 + 2*" + b + "*x*y + " + c + "*y^2 + 2*" + d + "*x + 2*" + e + "*y + " + f + " = 0";
	}

	/**
	 * Checks to see if the parameters define an ellipse using the
	 * {@code a*c - b*b > 0} constraint.
	 * 
	 * @return true if it's an ellipse or false if not
	 */
	public boolean isEllipse() { return a * c - b * b > 0; }
}