package fit.util;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;

import fit.AbstractFunction;
import fit.AbstractFunction2D;
import fit.circular.ClosedContinousShape2D;
import ij.IJ;
import ij.gui.Line;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import mpicbg.models.AffineModel2D;
import mpicbg.models.Point;
import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.real.FloatType;

public class TransformUtil
{
	public static AffineModel2D getModel( final AffineTransform t )
	{
		final AffineModel2D m = new AffineModel2D();
		final double[] flatMatrix = new double[ 6 ];
		t.getMatrix( flatMatrix );
		m.set( flatMatrix[ 0 ], flatMatrix[ 1 ], flatMatrix[ 2 ], flatMatrix[ 3 ], flatMatrix[ 4 ], flatMatrix[ 5 ] );
		return m;
	}

	public static Img< FloatType > drawBruteForce( final ClosedContinousShape2D shape, final int sizeX, final int sizeY, final double dist )
	{
		final Img< FloatType > img = ArrayImgs.floats( sizeX, sizeY );

		final Cursor< FloatType > c = img.localizingCursor();

		while ( c.hasNext() )
		{
			c.fwd();
			final double distance = shape.eval( c.getDoublePosition( 0 ), c.getDoublePosition( 1 ) );

			if ( !Double.isFinite( dist ) )
				c.get().set( (float)distance );
			else if ( Math.abs( distance ) < 50 )
				c.get().setOne();
		}

		return img;
	}

	public static Img< FloatType > drawDistanceBruteForce( final AbstractFunction< ? > shape, final int sizeX, final int sizeY )
	{
		IJ.showProgress( 0.0 );

		final Img< FloatType > img = ArrayImgs.floats( sizeX, sizeY );
		final Cursor< FloatType > c = img.localizingCursor();

		final Point p = new Point( new double[ 2 ] );

		int i = 0;

		while ( c.hasNext() )
		{
			c.fwd();

			p.getL()[ 0 ] = p.getW()[ 0 ] = c.getDoublePosition( 0 );
			p.getL()[ 1 ] = p.getW()[ 1 ] = c.getDoublePosition( 1 );

			c.get().set( (float)shape.distanceTo( p ) );
			
			if ( ++i % 1000 == 0 )
				IJ.showProgress( i, (int)img.size() );
		}

		IJ.showProgress( 1.0 );

		return img;
	}

	public static double distance( final double x, final double y )
	{
		return Math.sqrt( x*x + y*y );
	}

	public static double squareDistance( final double x, final double y )
	{
		return x*x + y*y;
	}

	public static void drawOutline( final Overlay o, final ClosedContinousShape2D shape, final double step )
	{
		final ArrayList< Double > xPoints = new ArrayList< Double >();
		final ArrayList< Double > yPoints = new ArrayList< Double >();

		for ( double t = 0; t < 2*Math.PI; t += step )
		{
			xPoints.add( shape.getPointXAt( t ) );
			yPoints.add( shape.getPointYAt( t ) );
		}

		final float[] xP = new float[ xPoints.size() ];
		final float[] yP = new float[ yPoints.size() ];

		for ( int i = 0; i < xP.length; ++i )
		{
			xP[ i ] = xPoints.get( i ).floatValue();
			yP[ i ] = yPoints.get( i ).floatValue();
		}

		o.add( new PolygonRoi( xP, yP, Roi.POLYGON ) );
	}

	public static void drawDiamond( final Overlay o, final double x, final double y )
	{
		drawDiamond( o, x, y, 5, 5 );
	}

	public static void drawDiamond( final Overlay o, final double x, final double y, final double rx, final double ry )
	{
		o.add( new Line( x, y - ry, x, y + ry ) );
		o.add( new Line( x - rx, y, x + rx, y ) );

		o.add( new Line( x, y - ry, x + rx, y ) );
		o.add( new Line( x + rx, y, x, y + ry ) );
		o.add( new Line( x, y + ry, x - rx, y ) );
		o.add( new Line( x - rx, y, x, y - ry ) );
	}

	public static void drawCross( final Overlay o, final double x, final double y )
	{
		drawCross( o, x, y, 1, 1 );
	}

	public static void drawCross( final Overlay o, final double x, final double y, final double rx, final double ry )
	{
		o.add( new Line( x - rx, y, x + rx, y ) );
		o.add( new Line( x, y - ry, x, y + ry ) );
		
		o.add( new Line( x - rx, y, x + rx, y ) );
		o.add( new Line( x, y - ry, x, y + ry ) );
	}
}
