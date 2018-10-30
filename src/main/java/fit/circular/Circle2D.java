package fit.circular;

import java.util.ArrayList;
import java.util.Collection;

import fit.AbstractFunction2D;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;

public class Circle2D extends AbstractFunction2D< Circle2D >
{
	private static final long serialVersionUID = 583246361064913748L;

	final static int minNumPoints = 3;

	double x, y, r; // m*x + n

	public Circle2D() { this( 0, 0, 0 ); }
	public Circle2D( final double x, final double y, final double r )
	{
		this.x = x;
		this.y = y;
		this.r = r;
	}

	/**
	 * @return - the center of the circle in x
	 */
	public double getX() { return x; }

	/**
	 * @return - the center of the circle in y
	 */
	public double getY() { return y; }

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
		this.x = (vv * (uuu + uvv) - uv * (uuv + vvv)) * f;
		this.y = (-uv * (uuu + uvv) + uu * (uuv + vvv)) * f;
		this.r = Math.sqrt( this.x * this.x + this.y * this.y + (uu + vv) / total);

		this.x += x;
		this.y += y;
	}

	@Override
	public double distanceTo( final Point point )
	{
		final double x1 = point.getW()[ 0 ] - getX(); 
		final double y1 = point.getW()[ 1 ] - getY();

		return Math.abs( Math.sqrt( x1*x1 + y1*y1 ) - getR() );
	}

	@Override
	public void set( final Circle2D m )
	{
		this.x = m.getX();
		this.y = m.getY();
		this.r = m.getR();
		this.setCost( m.getCost() );
	}

	@Override
	public Circle2D copy()
	{
		Circle2D c = new Circle2D();

		c.x = getX();
		c.y = getY();
		c.r = getR();
		c.setCost( getCost() );

		return c;
	}

	@Override
	public String toString() { return "(x − " + getX() + " )^2 + (y − " + getY() + " )^2=" + getR(); }

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
