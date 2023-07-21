/*-
 * #%L
 * code for function fitting
 * %%
 * Copyright (C) 2015 - 2023 Developers
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Preibisch Lab nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
