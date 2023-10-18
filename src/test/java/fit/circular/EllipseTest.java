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
