package fit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import Jama.Matrix;
import Jama.QRDecomposition;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;

/**
 * 
 * @author Varun Kapoor, Stephan Preibisch
 * 
 */
public class Polynomial extends AbstractFunction< Polynomial >
{
	private static final long serialVersionUID = 5010369758205651325L;

	// For initial guesses for Newton Raphson
	final Random rndx = new Random( 43583458 );

	final int minNumPoints;
	final int degree;
	
	private double SSE;
	private double SST;
	public final double[] coeff;

	public Polynomial(final int degree)
	{
		this.degree = degree;
		this.minNumPoints = degree + 1;
		this.coeff = new double[degree + 1];
	}

	/**
	 * @return - the coefficients of the polynomial in x
	 */
	public double getCoefficients(final int j) { return coeff[j]; }

	@Override
	public int getMinNumPoints() { return minNumPoints; }

	/*
	 * 
	 * This is a fit function for the polynomial of user chosen degree
	 * 
	 */
	public void fitFunction( final Collection< Point > points ) throws NotEnoughDataPointsException
	{
		final int nPoints = points.size();
		if (nPoints < minNumPoints)
			throw new NotEnoughDataPointsException("Not enough points, at least " + minNumPoints + " are necessary.");
		final double[] y = new double[nPoints];
		final double[] x = new double[nPoints];

		int count = 0;
		for (final Point p : points) {
			x[count] = p.getW()[0];
			y[count] = p.getW()[1];
			count++;
		}

		// Vandermonde matrix
		final double[][] vandermonde = new double[nPoints][degree + 1];
		for (int i = 0; i < nPoints; i++)
			for (int j = 0; j <= degree; j++)
				vandermonde[i][j] = Math.pow(x[i], j);

		final Matrix X = new Matrix(vandermonde);

		// create matrix from vector
		final Matrix Y = new Matrix(y, nPoints);

		// find least squares solution
		final QRDecomposition qr = new QRDecomposition(X);
		final Matrix coefficients = qr.solve(Y);

		// mean of y[] values
		double sum = 0.0;
		for (int i = 0; i < nPoints; i++)
			sum += y[i];
		final double mean = sum / nPoints;

		// total variation to be accounted for
		for (int i = 0; i < nPoints; i++) {
			final double dev = y[i] - mean;
			SST += dev * dev;
		}

		// variation not accounted for
		final Matrix residuals = X.times(coefficients).minus(Y);
		this.SSE = residuals.norm2() * residuals.norm2();

		for (int j = degree; j >= 0; j--)
			this.coeff[j] = coefficients.get(j, 0);
	}

	// Distance of a point from a polynomial
	@Override
	public double distanceTo( final Point point )
	{
		final double x1 = point.getW()[0];
		final double y1 = point.getW()[1];

		if ( degree == 1 )
			return Math.abs( y1 - coeff[1]*x1 - coeff[0]) /(Math.sqrt( 1 + coeff[1] * coeff[1]));
		
		if ( degree == 2 )
		{
			double a3, a2, a1, a0, Abar, Bbar, Phi, xc;
			double xc1 = 0, xc2 = 0, xc3 = 0, yc1 = 0, yc2 = 0, yc3 = 0 ;

			a3 = 2 * coeff[2] * coeff[2] ;
			a2 = 3 * coeff[1] * coeff[2]  / a3 ;
			a1 = (2 * coeff[0] * coeff[2] - 2 * coeff[2] * y1 + 1 + coeff[1] *coeff[1]) / a3;
			a0 = (coeff[0] *coeff[1] - y1 * coeff[1] - x1) / a3 ;

			final double p = (3 * a1 - a2 * a2) / 3;
			final double q = (-9 * a1 * a2  + 27 * a0  + 2 * a2 * a2 * a2) / 27 ;

			final double tmp = Math.sqrt( -p / 3 );

			if ((q * q / 4 + p * p * p / 27) > 0)
			{
				Abar = Math.pow(-q/2 + Math.sqrt( q * q / 4 + p * p * p / 27), 1/3);
				Bbar = Math.pow(-q/2 - Math.sqrt( q * q / 4 + p * p * p / 27), 1/3);

				xc = Abar + Bbar;
				xc1 = xc;
				xc2 = xc;
				xc3 = xc;
			}

			if ((q * q / 4 + p * p * p / 27) == 0)
			{
				if ( q > 0 )
				{
					xc1 = -2 * tmp;
					xc2 = tmp;
					xc3 = xc2;
				}
				else if (q < 0)
				{
					xc1 = 2 * tmp;
					xc2 = -tmp;
					xc3 = xc2;
				}
				else
				{
					xc1 = 0;
					xc2 = 0;
					xc3 = 0;
				}
			}

			if ((q * q / 4 + p * p * p / 27) < 0)
			{
				if ( q >= 0)
					Phi = Math.acos(-Math.sqrt( q * q * 0.25 / (-p * p * p / 27)));
				else
					Phi = Math.acos(Math.sqrt(  q * q * 0.25 / (-p * p * p / 27)));

				xc1 = 2 * tmp * Math.cos(Phi / 3) - a2 / 3;
				xc2 = 2 * tmp * Math.cos((Phi + 2 * Math.PI) / 3) - a2 / 3;
				xc3 = 2 * tmp * Math.cos((Phi + 4 * Math.PI) / 3) - a2 / 3;
			}

			for ( int j = degree; j >= 0; --j )
			{
				yc1 += coeff[j] * NewtonRaphson.pow( xc1, j );
				yc2 += coeff[j] * NewtonRaphson.pow( xc2, j );
				yc3 += coeff[j] * NewtonRaphson.pow( xc3, j );
			}

			final double returndistA = NewtonRaphson.distance( x1, y1, xc1, yc1 );
			final double returndistB = NewtonRaphson.distance( x1, y1, xc2, yc2 );
			final double returndistC = NewtonRaphson.distance( x1, y1, xc3, yc3 );

			return Math.min( returndistA, Math.min( returndistB, returndistC ) );
		}
		else
		{
			return new NewtonRaphson( rndx, degree ).run( x1, y1, coeff );
		}
	}

	@Override
	public void set( final Polynomial p )
	{
		for (int j = degree; j >= 0; j--)
			this.coeff[j] = p.getCoefficients(j);

		this.setCost(p.getCost());
	}

	@Override
	public Polynomial copy()
	{
		final Polynomial c = new Polynomial( degree );

		for (int j = degree; j >= 0; j--)
			c.coeff[j] = getCoefficients(j);

		c.setCost( getCost() );

		return c;
	}

	public int degree() {
		return degree;
	}

	public double r2() {
		return 1.0 - SSE / SST;
	}

	// Horner's method to get y values correspoing to x
	public double predict( final double x )
	{
		// horner's method
		double y = 0.0;
		for (int j = degree; j >= 0; j--)
			y = getCoefficients(j) + (x * y);
		return y;
	}

	public static void main(String[] args) throws NotEnoughDataPointsException, IllDefinedDataPointsException {
		final ArrayList<Point> points = new ArrayList<Point>();

		points.add(new Point(new double[] { 1f, -3.95132f }));
		points.add(new Point(new double[] { 2f, 6.51205f }));
		points.add(new Point(new double[] { 3f, 18.03612f }));
		points.add(new Point(new double[] { 4f, 28.65245f }));
		points.add(new Point(new double[] { 5f, 42.05581f }));
		points.add(new Point(new double[] { 6f, 54.01327f }));
		points.add(new Point(new double[] { 7f, 64.58747f }));
		points.add(new Point(new double[] { 8f, 76.48754f }));
		points.add(new Point(new double[] { 9f, 89.00033f }));

		final ArrayList<PointFunctionMatch> candidates = new ArrayList<PointFunctionMatch>();
		final ArrayList<PointFunctionMatch> inliersPoly = new ArrayList<PointFunctionMatch>();
		long startTime = System.nanoTime();
		for (final Point p : points)
			candidates.add(new PointFunctionMatch(p));

		final int degree = 2;
		// Using the polynomial model to do the fitting
		final Polynomial regression = new Polynomial(degree);

		regression.ransac( candidates, inliersPoly, 100, 0.1, 0.5 );

		System.out.println("inliers: " + inliersPoly.size());
		for ( final PointFunctionMatch p : inliersPoly )
			System.out.println( regression.distanceTo( p.getP1() ) );
		regression.fit(inliersPoly);
		System.out.println(" y = "  );
		for (int i = degree; i >= 0; --i)
			System.out.println(regression.getCoefficients(i) + "  " + "x" + " X to the power of "  + i );
		long totalTime = (System.nanoTime()- startTime)/1000;
		System.out.println("Time: " + totalTime);

	}

}