package fit.circular;

import ij.IJ;
import ij.ImageJ;
import mpicbg.models.AffineModel2D;
import mpicbg.models.Point;
import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;

public class EllipsePointDistance implements ShapePointDistance< Ellipse, EllipsePointDistance, EllipsePointDistanceFactory >
{
	final int iter;
	final Ellipse e;
	final EllipsePointDistanceFactory factory;

	/**
	 * a model that axis-aligns the ellipse
	 */
	AffineModel2D axisAlign;

	public EllipsePointDistance( final Ellipse e, final int iter, final EllipsePointDistanceFactory factory )
	{
		this.iter = iter;
		this.e = e;
		this.factory = factory;

		notifyParameterChange();
	}

	@Override
	public EllipsePointDistanceFactory factory() { return factory; }

	@Override
	public Ellipse getShape() { return e; }

	@Override
	public synchronized void notifyParameterChange()
	{
		// we need to re-compute the parameters
		this.axisAlign = Ellipse.axisAlignEllipse( e );
	}

	@Override
	public double distanceTo( final Point p )
	{
		// we transform the intersection point into the axis-aligned ellipse
		final double[] i = new double[]{ p.getW()[ 0 ], p.getW()[ 1 ] };
		this.axisAlign.applyInPlace( i );

		return closestPoint( e.rAxis0, e.rAxis1, i, iter );
	}

	private static final double nextT( final double t, final double px, final double py, final double a, final double b, final double a2, final double b2 )
	{
		final double cosT = Math.cos(t);
		final double sinT = Math.sin(t);

		final double x = a * cosT;
		final double y = b * sinT;

		final double ex = (a2 - b2) * cosT*cosT*cosT / a;
		final double ey = (b2 - a2) * sinT*sinT*sinT / b;

		final double rx = x - ex;
		final double ry = y - ey;

		final double qx = px - ex;
		final double qy = py - ey;

		final double r = Math.hypot(ry, rx);
		final double q = Math.hypot(qy, qx);

		final double delta_c = r * Math.asin((rx*qy - ry*qx)/(r*q));
		final double delta_t = delta_c / Math.sqrt(a2 + b2 - x*x - y*y);

		return Math.min(Math.PI/2, Math.max(0, t + delta_t ));
	}

	public static final double closestPoint( final double a, final double b, final double[] p, final int iter )
	{
		// taken from Carl Chatfield
		// https://github.com/0xfaded/ellipse_demo
		// license: MIT

		final double sigX = p[ 0 ] == 0 ? 1 : Math.signum( p[ 0 ] );
		final double sigY = p[ 1 ] == 0 ? 1 : Math.signum( p[ 1 ] );

		final double px = Math.abs( p[0] );
		final double py = Math.abs( p[1] );

		final double a2 = a*a;
		final double b2 = b*b;

		double t = Math.PI / 4;
		double x = 0, y = 0;

		for ( int j = 0; j < iter; ++j )
		{
			t = nextT( t, px, py, a, b, a2, b2 );
			/*
			final double cosT = Math.cos(t);
			final double sinT = Math.sin(t);

			x = a * cosT;
			y = b * sinT;

			final double ex = (a2 - b2) * cosT*cosT*cosT / a;
			final double ey = (b2 - a2) * sinT*sinT*sinT / b;

			final double rx = x - ex;
			final double ry = y - ey;

			final double qx = px - ex;
			final double qy = py - ey;

			final double r = Math.hypot(ry, rx);
			final double q = Math.hypot(qy, qx);

			final double delta_c = r * Math.asin((rx*qy - ry*qx)/(r*q));
			final double delta_t = delta_c / Math.sqrt(a2 + b2 - x*x - y*y);

			t += delta_t;
			t = Math.min(Math.PI/2, Math.max(0, t));*/
		}

		x = a * Math.cos(t);
		y = b * Math.sin(t);

		final double iX = x * sigX;
		final double iY = y * sigY;

		// distance from the point to the closest point on 
		final double dist = Math.hypot( iX-p[0], iY-p[1] );

		p[ 0 ] = iX;
		p[ 1 ] = iY;

		return dist;
	}

	public static void main( String[] args )
	{
		final int iter = 10;

		System.out.println( closestPoint( 240, 200, new double[] { 0, 200 }, iter ) );

		new ImageJ();

		final Img< FloatType > img = ArrayImgs.floats( 1024, 1024 );
		final Cursor< FloatType > c = img.localizingCursor();

		final double[] p = new double[ 2 ];

		int i = 0;
		long time = System.currentTimeMillis();
		while ( c.hasNext() )
		{
			c.fwd();

			p[ 0 ] = c.getDoublePosition( 0 ) - img.dimension( 0 )/2;
			p[ 1 ] = c.getDoublePosition( 1 ) - img.dimension( 1 )/2;

			c.get().set( (float)closestPoint( 240, 100, p, iter ) );
			
			if ( ++i % 1000 == 0 )
				IJ.showProgress( i, (int)img.size() );
		}

		IJ.log( "time = " + (System.currentTimeMillis() - time ) );
		IJ.showProgress( 1.0 );

		ImageJFunctions.show( img );
	}
	
	/*
 // from: https://stackoverflow.com/questions/22959698/distance-from-given-point-to-given-ellipse

	//Pseudocode for robustly computing the closest ellipse point and distance to a query point. It
	//is required that e0 >= e1 > 0, y0 >= 0, and y1 >= 0.
	//e0,e1 = ellipse dimension 0 and 1, where 0 is greater and both are positive.
	//y0,y1 = initial point on ellipse axis (center of ellipse is 0,0)
	//x0,x1 = intersection point

	public double getRoot( final double r0, final double z0, final double z1, double g )
	{
		double n0 = r0 * z0;
		double s0 = z1 - 1;
		double s1 = ( g < 0 ? 0 : Math.sqrt( n0 * n0 + z1 * z1 ) - 1 );
		double s = 0;
		for ( int i = 0; i < maxIter; ++i )
		{
			s = ( s0 + s1 ) / 2;
			if ( s == s0 || s == s1 )
				break;
			double ratio0 = n0 / ( s + r0 );
			double ratio1 = z1 / ( s + 1 );
			g = ratio0 * ratio0 + ratio1 * ratio1 - 1;
			if ( g > 0 )
				s0 = s;
			else if ( g < 0 )
				s1 = s;
			else
				break;
		}
		return s;
	}

	double DistancePointEllipse( double e0 , double e1 , double y0 , double y1 , out double x0 , out double x1)
    {
        double distance;
        if ( y1 > 0){
            if ( y0 > 0){
                double z0 = y0 / e0; 
                double z1 = y1 / e1; 
                double g = z0*z0+z1*z1 - 1;
                if ( g != 0){
                    double r0 = (e0/e1)*(e0/e1);
                    double sbar = GetRoot(r0 , z0 , z1 , g);
                    x0 = r0 * y0 /( sbar + r0 );
                    x1 = y1 /( sbar + 1 );
                    distance = Math.Sqrt( (x0-y0)*(x0-y0) + (x1-y1)*(x1-y1) );
                    }else{
                        x0 = y0; 
                        x1 = y1;
                        distance = 0;
                    }
                }
                else // y0 == 0
                    x0 = 0 ; x1 = e1 ; distance = Math.Abs( y1 - e1 );
        }else{ // y1 == 0
            double numer0 = e0*y0 , denom0 = e0*e0 - e1*e1;
            if ( numer0 < denom0 ){
                    double xde0 = numer0/denom0;
                    x0 = e0*xde0 ; x1 = e1*Math.Sqrt(1 - xde0*xde0 );
                    distance = Math.Sqrt( (x0-y0)*(x0-y0) + x1*x1 );
                }else{
                    x0 = e0; 
                    x1 = 0; 
                    distance = Math.Abs( y0 - e0 );
            }
        }
        return distance;
    }*/
}
