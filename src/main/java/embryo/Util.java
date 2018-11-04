package embryo;

import java.io.File;
import java.util.ArrayList;

import fit.PointFunctionMatch;
import fit.circular.AbstractShape2D;
import ij.ImagePlus;
import ij.io.Opener;
import ij.process.ImageProcessor;
import mpicbg.models.Point;
import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

public class Util
{
	public static < P extends AbstractShape2D< P > > ArrayList< Pair< P, ArrayList< PointFunctionMatch > > > findAllFunctions(
			final ArrayList< Point > mts,
			final P function,
			final double maxError,
			final int minNumInliers,
			final double minArea,
			final double maxArea )
	{
		boolean fitted;

		final ArrayList< Point > remainingPoints = new ArrayList< Point >();
		remainingPoints.addAll( mts );

		final ArrayList< Pair< P, ArrayList< PointFunctionMatch > > > segments = new ArrayList< Pair<P,ArrayList<PointFunctionMatch>> >();

		do
		{
			fitted = false;

			final Pair< P, ArrayList< PointFunctionMatch > > f = findFunction( remainingPoints, function.copy(), maxError, minNumInliers, minArea, maxArea );

			if ( f != null && f.getB().size() > 0 )
			{
				fitted = true;
				segments.add( f );

				final ArrayList< Point > inlierPoints = new ArrayList< Point >();
				for ( final PointFunctionMatch p : f.getB() )
					inlierPoints.add( p.getP1() );

				remainingPoints.removeAll( inlierPoints );
			}
		}
		while ( fitted );

		return segments;
	}

	public static < P extends AbstractShape2D< P > > Pair< P, ArrayList< PointFunctionMatch > > findFunction(
			final ArrayList< Point > mts,
			final P function,
			final double maxError,
			final int minNumInliers,
			final double minArea,
			final double maxArea )
	{
		final ArrayList< PointFunctionMatch > candidates = new ArrayList<PointFunctionMatch>();
		final ArrayList< PointFunctionMatch > inliers = new ArrayList<PointFunctionMatch>();
		
		for ( final Point p : mts )
			candidates.add( new PointFunctionMatch( p ) );

		try
		{
			function.ransac( candidates, inliers, 500, maxError, 0.01, minNumInliers, minArea, maxArea );

			if ( inliers.size() >= function.getMinNumPoints() )
			{
				function.fit( inliers );
	
				System.out.println( inliers.size() + "/" + candidates.size() );
				System.out.println( function );
			}
			else
			{
				System.out.println( "0/" + candidates.size() );
				return null;
			}
		}
		catch ( Exception e )
		{
			System.out.println( "Couldn't fit function: " + e );
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return null;
		}

		return new ValuePair< P, ArrayList< PointFunctionMatch > >( function, inliers );
	}

	public static Img< FloatType > openAs32Bit( final File file )
	{
		return openAs32Bit( file, new ArrayImgFactory< FloatType >() );
	}

	@SuppressWarnings("unchecked")
	public static ArrayImg< FloatType, ? > openAs32BitArrayImg( final File file )
	{
		return (ArrayImg< FloatType, ? >)openAs32Bit( file, new ArrayImgFactory< FloatType >() );
	}

	public static Img< FloatType > openAs32Bit( final File file, final ImgFactory< FloatType > factory )
	{
		if ( !file.exists() )
			throw new RuntimeException( "File '" + file.getAbsolutePath() + "' does not exisit." );

		final ImagePlus imp = new Opener().openImage( file.getAbsolutePath() );

		if ( imp == null )
			throw new RuntimeException( "File '" + file.getAbsolutePath() + "' coult not be opened." );

		final Img< FloatType > img;

		if ( imp.getStack().getSize() == 1 )
		{
			// 2d
			img = factory.create( new int[]{ imp.getWidth(), imp.getHeight() }, new FloatType() );
			final ImageProcessor ip = imp.getProcessor();

			final Cursor< FloatType > c = img.localizingCursor();
			
			while ( c.hasNext() )
			{
				c.fwd();

				final int x = c.getIntPosition( 0 );
				final int y = c.getIntPosition( 1 );

				c.get().set( ip.getf( x, y ) );
			}

		}
		else
		{
			// >2d
			img = factory.create( new int[]{ imp.getWidth(), imp.getHeight(), imp.getStack().getSize() }, new FloatType() );

			final Cursor< FloatType > c = img.localizingCursor();

			// for efficiency reasons
			final ArrayList< ImageProcessor > ips = new ArrayList< ImageProcessor >();

			for ( int z = 0; z < imp.getStack().getSize(); ++z )
				ips.add( imp.getStack().getProcessor( z + 1 ) );

			while ( c.hasNext() )
			{
				c.fwd();

				final int x = c.getIntPosition( 0 );
				final int y = c.getIntPosition( 1 );
				final int z = c.getIntPosition( 2 );

				c.get().set( ips.get( z ).getf( x, y ) );
			}
		}

		return img;
	}

}
