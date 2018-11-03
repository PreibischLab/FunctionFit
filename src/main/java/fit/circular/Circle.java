package fit.circular;

import java.util.ArrayList;
import java.util.Collection;

import fit.AbstractFunction;
import fit.util.TransformUtil;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import net.imglib2.img.display.imagej.ImageJFunctions;

public class Circle extends AbstractFunction< Circle > implements ClosedContinousShape2D
{
	private static final long serialVersionUID = 583246361064913748L;

	final static int minNumPoints = 3;

	double u, v, r; // ( x − u )^2 + ( y − v )^2= r^2

	public Circle() { this( 0, 0, 0 ); }
	public Circle( final double x, final double y, final double r )
	{
		this.u = x;
		this.v = y;
		this.r = r;
	}

	public static Circle unitcircle()
	{
		return new Circle( 0, 0, 1 );
	}

	public void drawAxes( final Overlay overlay )
	{
		overlay.add( new OvalRoi( u - 3, v - 3, 6, 6 ) );
		overlay.add( new Line( u, v, u + r, v ) );
		overlay.add( new Line( u, v, u, v + r ) );
		overlay.add( new Line( u, v, u - r, v ) );
		overlay.add( new Line( u, v, u, v - r ) );
	}

	@Override
	public double getRadiusAt( final double t )
	{
		return r;
	}

	@Override
	public double getPointXAt( final double t )
	{
		return u + r * Math.cos( t );
	}

	@Override
	public double getPointYAt( final double t )
	{
		return v + r * Math.sin( t );
	}

	@Override
	public void drawCenter( final Overlay overlay )
	{
		TransformUtil.drawDiamond( overlay, u, v );
	}

	@Override
	public void draw( final Overlay overlay, final double step )
	{
		TransformUtil.drawOutline( overlay, this, step );
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

	@Override
	public void intersectsAt( final double[] p, final double[] i )
	{
		// intersection point on the circle
		final double x0 = p[ 0 ] - u;
		final double y0 = p[ 1 ] - v;

		final double len = Math.sqrt( x0*x0 + y0*y0 );

		// intersection point on the circle
		i[ 0 ] = (x0 / len)*r + u;
		i[ 1 ] = (y0 / len)*r + v;
	}

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
	public double eval( final double x, final double y )
	{
		final double x0 = x - u;
		final double y0 = y - v;

		return r * r - x0 * x0 - y0 * y0;
	}

	@Override
	public double distanceTo( final Point point )
	{
		final double x1 = point.getW()[ 0 ] - getU(); 
		final double y1 = point.getW()[ 1 ] - getV();

		return Math.abs( Math.sqrt( x1*x1 + y1*y1 ) - getR() );
	}

	@Override
	public void set( final Circle m )
	{
		this.u = m.getU();
		this.v = m.getV();
		this.r = m.getR();
		this.setCost( m.getCost() );
	}

	@Override
	public Circle copy()
	{
		final Circle c = new Circle();

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

		points.add( new Point( new double[]{ -50.0 + 500, 0.0 + 400 } ) );
		points.add( new Point( new double[]{ 50 + 500, 0 + 400 } ) );
		points.add( new Point( new double[]{ 0 + 500, 50 + 400 } ) );
		points.add( new Point( new double[]{ 0 + 500, -50 + 400 } ) );

		final Circle circ = new Circle();
		circ.fitFunction( points );
		System.out.println( circ );
		System.out.println( "Distance = " + circ.distanceTo( new Point( new double[]{ 500, 100 } ) ) );

		new ImageJ();
		final ImagePlus imp = ImageJFunctions.show( TransformUtil.drawDistanceBruteForce( circ, 1024, 1024 ) );
		imp.setDisplayRange( 0, imp.getDisplayRangeMax() );

		Overlay o = imp.getOverlay();
		if ( o == null )
			o = new Overlay();

		circ.drawCenter( o );
		circ.draw( o, 0.01 );

		final double[] p = new double[] { 650, 430 };
		final double[] i = new double[ 2 ];
		circ.intersectsAt( p, i );

		TransformUtil.drawCross( o, p[ 0 ], p[ 1 ] );
		TransformUtil.drawCross( o, i[ 0 ], i[ 1 ] );

		final double[] dp = new double[ 2 ];
		final double dist = new BruteForceShapePointDistance< Circle >().minDistanceAt( new Point( p ), dp, circ );

		TransformUtil.drawCross( o, dp[ 0 ], dp[ 1 ] );
		System.out.println( "dist (brute force) = " + dist );
		System.out.println( "dist (circle) = " + circ.distanceTo( new Point( p ) ) );

		imp.setOverlay( o );
		imp.updateAndDraw();
	}
}
