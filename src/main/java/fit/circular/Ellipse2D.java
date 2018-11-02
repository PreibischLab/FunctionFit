package fit.circular;

import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.DecompositionFactory;
import org.ejml.factory.LinearSolverFactory;
import org.ejml.interfaces.decomposition.EigenDecomposition;
import org.ejml.interfaces.linsol.LinearSolver;
import org.ejml.ops.CommonOps;

import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;

/**
 * 
 * 
 * @author Peter Abeles and Stephan Preibisch
 */
public class Ellipse2D // extends AbstractFunction2D< Ellipse2D > //implements
						// Polynomial< LinearFunction, Point >
{
	public static void main( String[] args )
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

		final Ellipse2D e = new Ellipse2D();
		e.process( points );
		System.out.println( e.getEllipse() + " " + e.getEllipse().isEllipse() );
		e.getEllipse().getCenter();

		final Img< FloatType > img = ArrayImgs.floats( 1024, 1024 );

		final Cursor< FloatType > c = img.localizingCursor();

		while ( c.hasNext() )
		{
			c.fwd();
			final double distance = e.getEllipse().valueAt(
					c.getDoublePosition( 0 ), c.getDoublePosition( 1 ) );

			// c.get().set( (float)distance );

			if ( Math.abs( distance ) < 50 )
				c.get().setOne();
		}

		new ImageJ();
		ImagePlus imp = ImageJFunctions.show( img );

		Overlay o = imp.getOverlay();
		if ( o == null )
			o = new Overlay();

		e.getEllipse().drawCenter( o );
		e.getEllipse().drawAxes( o );
		e.getEllipse().draw( o, 0.01 );

		// final Circle2D test = new Circle2D( 100, 100, 20 );
		// test.drawAxes( o );
		// test.draw( o, 0.01 );

		imp.setOverlay( o );
		imp.updateAndDraw();

		final double[] p = new double[] { 650, 430 };

		// e.getEllipse().distanceTo( o, p );
	}

	// qudratic part of design matrix
	private DenseMatrix64F D1 = new DenseMatrix64F( 3, 1 );
	// linear part of design matrix
	private DenseMatrix64F D2 = new DenseMatrix64F( 3, 1 );

	// quadratic part of scatter matrix
	private DenseMatrix64F S1 = new DenseMatrix64F( 3, 3 );
	// combined part of scatter matrix
	private DenseMatrix64F S2 = new DenseMatrix64F( 3, 3 );
	// linear part of scatter matrix
	private DenseMatrix64F S3 = new DenseMatrix64F( 3, 3 );
	// Reduced scatter matrix
	private DenseMatrix64F M = new DenseMatrix64F( 3, 3 );

	// storage for intermediate steps
	private DenseMatrix64F T = new DenseMatrix64F( 3, 3 );
	private DenseMatrix64F Ta1 = new DenseMatrix64F( 3, 1 );
	private DenseMatrix64F S2_tran = new DenseMatrix64F( 3, 3 );

	private LinearSolver< DenseMatrix64F > solver = LinearSolverFactory.linear( 3 );
	private EigenDecomposition< DenseMatrix64F > eigen = DecompositionFactory.eig( 3, true, false );

	private EllipseQuadratic_F64 ellipse = new EllipseQuadratic_F64();

	public boolean process( double[][] points )
	{
		int N = points.length;

		// Construct the design matrices. linear and quadratic
		D1.reshape( N, 3 );
		D2.reshape( N, 3 );
		int index = 0;
		for (int i = 0; i < N; i++)
		{
			final double x = points[ i ][ 0 ];
			final double y = points[ i ][ 1 ];

			// fill in each row one at a time
			D1.data[ index ] = x * x;
			D2.data[ index++ ] = x;
			D1.data[ index ] = x * y;
			D2.data[ index++ ] = y;
			D1.data[ index ] = y * y;
			D2.data[ index++ ] = 1;
		}

		// Compute scatter matrix
		CommonOps.multTransA( D1, D1, S1 ); // S1 = D1'*D1
		CommonOps.multTransA( D1, D2, S2 ); // S2 = D1'*D2
		CommonOps.multTransA( D2, D2, S3 ); // S3 = D2'*D2

		// for getting a2 from a1
		// T = -inv(S3)*S2'
		if ( !solver.setA( S3 ) )
			return false;

		CommonOps.transpose( S2, S2_tran );
		CommonOps.changeSign( S2_tran );
		solver.solve( S2_tran, T );

		// Compute reduced scatter matrix
		// M = S1 + S2*T
		CommonOps.mult( S2, T, M );
		CommonOps.add( M, S1, M );

		// Premultiply by inv(C1). inverse of constraint matrix
		for (int col = 0; col < 3; col++)
		{
			double m0 = M.unsafe_get( 0, col );
			double m1 = M.unsafe_get( 1, col );
			double m2 = M.unsafe_get( 2, col );

			M.unsafe_set( 0, col, m2 / 2 );
			M.unsafe_set( 1, col, -m1 );
			M.unsafe_set( 2, col, m0 / 2 );
		}

		if ( !eigen.decompose( M ) )
			return false;

		DenseMatrix64F a1 = selectBestEigenVector();
		if ( a1 == null )
			return false;

		// ellipse coefficients
		CommonOps.mult( T, a1, Ta1 );

		ellipse.a = a1.data[ 0 ];
		ellipse.b = a1.data[ 1 ] / 2;
		ellipse.c = a1.data[ 2 ];
		ellipse.d = Ta1.data[ 0 ] / 2;
		ellipse.e = Ta1.data[ 1 ] / 2;
		ellipse.f = Ta1.data[ 2 ];

		return true;
	}

	private DenseMatrix64F selectBestEigenVector()
	{

		int bestIndex = -1;
		double bestCond = Double.MAX_VALUE;

		for (int i = 0; i < eigen.getNumberOfEigenvalues(); i++)
		{
			DenseMatrix64F v = eigen.getEigenVector( i );

			if ( v == null ) // TODO WTF?!?!
				continue;

			// evaluate a'*C*a = 1
			double cond = 4 * v.get( 0 ) * v.get( 2 ) - v.get( 1 ) * v.get( 1 );
			double condError = ( cond - 1 ) * ( cond - 1 );

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

	public EllipseQuadratic_F64 getEllipse()
	{
		return ellipse;
	}
}