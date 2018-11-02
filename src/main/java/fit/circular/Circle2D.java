package fit.circular;

import java.util.ArrayList;
import java.util.Collection;

import fit.AbstractFunction2D;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;

public class Circle2D extends AbstractFunction2D< Circle2D >
{
	private static final long serialVersionUID = 583246361064913748L;

	final static int minNumPoints = 3;

	double u, v, r; // ( x − u )^2 + ( y − v )^2= r

	public Circle2D() { this( 0, 0, 0 ); }
	public Circle2D( final double x, final double y, final double r )
	{
		this.u = x;
		this.v = y;
		this.r = r;
	}

	public static Circle2D unitcircle()
	{
		return new Circle2D( 0, 0, 1 );
	}

	public void drawAxes( final Overlay overlay )
	{
		overlay.add( new OvalRoi( u - 3, v - 3, 6, 6 ) );
		overlay.add( new Line( u, v, u + r, v ) );
		overlay.add( new Line( u, v, u, v + r ) );
		overlay.add( new Line( u, v, u - r, v ) );
		overlay.add( new Line( u, v, u, v - r ) );
	}

	public double getPointXAt( final double t )
	{
		return u + r * Math.cos( t );
	}

	public double getPointYAt( final double t )
	{
		return v + r * Math.sin( t );
	}

	public void drawCenter( final Overlay overlay )
	{
		overlay.add( new Line( u, v - 5, u, v + 5 ) );
		overlay.add( new Line( u - 5, v, u + 5, v ) );

		overlay.add( new Line( u, v - 5, u + 5, v ) );
		overlay.add( new Line( u + 5, v, u, v + 5 ) );
		overlay.add( new Line( u, v + 5, u - 5, v ) );
		overlay.add( new Line( u - 5, v, u, v - 5 ) );
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

	/**
	 * @return - the center of the circle in x
	 */
	public double getU() { return u; }

	/**
	 * @return - the center of the circle in y
	 */
	public double getV() { return v; }

	/**
	 * @return - the radius of the circle
	 */
	public double getR() { return r; }

	@Override
	public int getMinNumPoints() { return minNumPoints; }

	public void fitFunction( final Collection<Point> points ) throws NotEnoughDataPointsException
	{
		final int numPoints = points.size();

		if ( numPoints < minNumPoints )
			throw new NotEnoughDataPointsException( "Not enough points, at least " + minNumPoints + " are necessary and available are: " + numPoints );

		// circle fit slightly adapted from: https://github.com/fiji/Fiji_Plugins/blob/master/src/main/java/fiji/util/Circle_Fitter.java

		// calculate mean centroid
		double x, y, total;
		x = y = total = 0;

		for ( final Point p : points )
		{
			final double w = 1;
			x += p.getW()[ 0 ] * w;
			y += p.getW()[ 1 ] * w;
			total += w;
		}

		x /= total;
		y /= total;

		// calculate the rest
		double uu, uv, vv, uuu, uuv, uvv, vvv;
		uu = uv = vv = uuu = uuv = uvv = vvv = 0;
		for ( final Point p : points )
		{
			final double w = 1;
			final double u = p.getW()[ 0 ] - x;
			final double v = p.getW()[ 1 ] - y;
			uu += u * u * w;
			uv += u * v * w;
			vv += v * v * w;
			uuu += u * u * u * w;
			uuv += u * u * v * w;
			uvv += u * v * v * w;
			vvv += v * v * v * w;
		}

		// calculate center & radius
		final double f = 0.5 / (uu * vv - uv * uv);
		this.u = (vv * (uuu + uvv) - uv * (uuv + vvv)) * f;
		this.v = (-uv * (uuu + uvv) + uu * (uuv + vvv)) * f;
		this.r = Math.sqrt( this.u * this.u + this.v * this.v + (uu + vv) / total);

		this.u += x;
		this.v += y;
	}

	@Override
	public double distanceTo( final Point point )
	{
		final double x1 = point.getW()[ 0 ] - getU(); 
		final double y1 = point.getW()[ 1 ] - getV();

		return Math.abs( Math.sqrt( x1*x1 + y1*y1 ) - getR() );
	}

	@Override
	public void set( final Circle2D m )
	{
		this.u = m.getU();
		this.v = m.getV();
		this.r = m.getR();
		this.setCost( m.getCost() );
	}

	@Override
	public Circle2D copy()
	{
		Circle2D c = new Circle2D();

		c.u = getU();
		c.v = getV();
		c.r = getR();
		c.setCost( getCost() );

		return c;
	}

	@Override
	public String toString() { return "(x − " + getU() + " )^2 + (y − " + getV() + " )^2=" + getR(); }

	public static void main( String[] args ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		final ArrayList< Point > points = new ArrayList<Point>();

		points.add( new Point( new double[]{ -1.0 + 5, 0.0 + 10 } ) );
		points.add( new Point( new double[]{ 1 + 5, 0 + 10 } ) );
		points.add( new Point( new double[]{ 0 + 5, 1 + 10 } ) );
		points.add( new Point( new double[]{ 0 + 5, -1 + 10 } ) );

		Circle2D c = new Circle2D();
		c.fitFunction( points );
		System.out.println( c );
		System.out.println( "Distance = " + c.distanceTo( new Point( new double[]{ 5, 10.5 } ) ) );
	}
}