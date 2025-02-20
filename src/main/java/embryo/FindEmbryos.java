/*-
 * #%L
 * code for function fitting
 * %%
 * Copyright (C) 2015 - 2025 Developers
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
import fit.circular.BruteForceShapePointDistanceFactory;
import fit.circular.Ellipse;
import fit.circular.EllipsePointDistanceFactory;
import fit.circular.ShapePointDistanceFactory;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.io.FileSaver;
import mpicbg.models.Point;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Pair;
import net.imglib2.view.Views;

public class FindEmbryos
{
	public static < T extends RealType< T > > ArrayList< Point > edges4( final RandomAccessibleInterval< T > img, final RandomAccessibleInterval< T > edgeImg )
	{
		final int n = img.numDimensions();
		final RandomAccess< T > raEdge;

		if ( edgeImg != null )
		{
			for ( int d = 0; d < n; ++d )
				if ( img.min( d ) > edgeImg.min( d ) || img.max( d ) < edgeImg.max( d ) )
					throw new RuntimeException( "edge image is too small" );
	
			raEdge = edgeImg.randomAccess();
		}
		else
		{
			raEdge = null;
		}

		final Cursor< T > cursor = Views.iterable( img ).localizingCursor();
		final RandomAccess< T > ra = Views.extendZero( img ).randomAccess();
		final ArrayList< Point > mts = new ArrayList< Point >();

		while ( cursor.hasNext() )
		{
			final T type = cursor.next();

			// could be an edge pixel
			if ( type.getRealDouble() > 0 )
			{
				ra.setPosition( cursor );

				boolean edge = false;

				for ( int d = 0; d < n && !edge; ++d )
				{
					ra.bck( d );

					if ( ra.get().getRealDouble() == 0.0 )
					{
						// we are done
						edge = true;
					}
					else
					{
						ra.fwd( d );
						ra.fwd( d );

						if ( ra.get().getRealDouble() == 0.0 )
						{
							// we are done
							edge = true;
						}
						else
						{
							// move back to the center for the next dimension
							ra.bck( d );
						}
					}
				}

				if ( edge )
				{
					final double[] point = new double[ n ];
					cursor.localize( point );
					mts.add( new Point( point ) );
				}

				if ( raEdge != null )
				{
					raEdge.setPosition( cursor );
	
					if ( edge )
						raEdge.get().setOne();
					else
						raEdge.get().setZero();
				}
			}
		}

		return mts;
	}

	public static void main( String[] args )
	{
		final String[] names = new String[] { "MK4_8.tif", "MK4_69.tif", "N2_1015.tif", "N2_1022.tif", "N2_1061.tif" };
		final String saveDir = "/Users/spreibi/Documents/BIMSB/Projects/Dosage Compensation/for_ellipsoid_fit/";

		for ( final String name : names )
		{
			final String fileName = "/Users/spreibi/Documents/BIMSB/Projects/Dosage Compensation/for_ellipsoid_fit/labels/" + name;
			final String fileNameImg = "/Users/spreibi/Documents/BIMSB/Projects/Dosage Compensation/for_ellipsoid_fit/images/" + name;
	
			System.out.println( fileNameImg );

			final Img< FloatType > img = Util.openAs32Bit( new File( fileName ) );
			final Img< FloatType > origImg = Util.openAs32Bit( new File( fileNameImg ) );
			final Img< FloatType > edgeImg = img.factory().create( img, img.firstElement() );
	
			final ArrayList< Point > mts = edges4( img, edgeImg );
	
			System.out.println( "Found " + mts.size() + " edge pixels." );
			new ImageJ();
	
			//ImagePlus imp = ImageJFunctions.show( img );
			ImagePlus edgeImp = ImageJFunctions.show( edgeImg );
			ImagePlus origImp = ImageJFunctions.show( origImg );
	
			final double minArea = 40000;
			final double maxArea = 80000;

			final ShapePointDistanceFactory< Ellipse, ?, ? > factory = new EllipsePointDistanceFactory();//BruteForceShapePointDistanceFactory< Ellipse >();

			final ArrayList< Pair< Ellipse, ArrayList< PointFunctionMatch > > > functions =
					Util.findAllFunctions( mts, new Ellipse( factory ), 15, 300, minArea, maxArea );
	
			final Overlay o = new Overlay();
	
			for ( final Pair< Ellipse, ArrayList< PointFunctionMatch > > function : functions )
			{
				final Ellipse e = function.getA();
	
				System.out.println( "isEllipse: " + e.isEllipse() );
				System.out.println( "area: " + e.area() );
	
				e.drawCenter( o );
				e.drawAxes( o );
				e.draw( o, 0.01 );
	
			}
	
			/*
			final Pair< Ellipse, ArrayList< PointFunctionMatch > > function = Util.findFunction( mts, new Ellipse(), 10, 200, minArea, maxArea );
			final Ellipse e = function.getA();
	
			//0.6987618818406458*x^2 + 2*0.06942248837148446*x*y + 0.7017506002275764*y^2 + 2*-465.9306611834606*x + 2*-431.2670745786572*y + 493684.33508327027 = 0
			final Ellipse e = new Ellipse(
					0.6987618818406458,
					0.06942248837148446,
					0.7017506002275764,
					-465.9306611834606,
					-431.2670745786572,
					493684.33508327027 );
	
			System.out.println( "isEllipse: " + e.isEllipse() );
			System.out.println( "area: " + e.area() );
	
			e.drawCenter( o );
			e.drawAxes( o );
			e.draw( o, 0.01 ); */
	
			//imp.setOverlay( o );
			//imp.updateAndDraw();
	
			edgeImp.setOverlay( o );
			edgeImp.updateAndDraw();
	
			origImp.setOverlay( o );
			origImp.updateAndDraw();

			new FileSaver( edgeImp ).saveAsTiff( saveDir + name + ".edge.tif" );
			new FileSaver( origImp ).saveAsTiff( saveDir + name + ".edge.tif" );
		}

		System.out.println( "DONE" );
	}
}
