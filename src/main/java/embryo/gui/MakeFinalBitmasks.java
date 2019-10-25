package embryo.gui;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import embryo.gui.LoadedEmbryo.Status;
import fit.circular.Ellipse;
import ij.CompositeImage;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.io.FileSaver;
import ij.io.Opener;
import ij.plugin.Duplicator;
import ij.process.ImageProcessor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;

public class MakeFinalBitmasks
{
	public static Interval findBoundingBox( final EllipseOrROI e, final ImagePlus imp, final int boundary )
	{
		return findBoundingBox( e, imp, boundary, boundary );
	}

	public static Interval findBoundingBox( final EllipseOrROI e, final ImagePlus imp, final int boundaryX, final int boundaryY )
	{
		if ( e.isEllipse() )
			return findBoundingBox( e.getEllipse(), imp, boundaryX, boundaryY );
		else
			return findBoundingBox( e.getROI(), imp, boundaryX, boundaryY );
	}

	public static Interval findBoundingBox( final PolygonRoi r, final ImagePlus imp, final int boundaryX, final int boundaryY )
	{
		final Rectangle rect = r.getBounds();

		long x0 = rect.x - boundaryX;
		long x1 = rect.x + rect.width - 1 + boundaryX;

		long y0 = rect.y - boundaryY;
		long y1 = rect.y + rect.height - 1 + boundaryY;

		return imageSizeAdjustedInterval(x0, x1, y0, y1, imp.getWidth(), imp.getHeight() );
	}

	public static Interval findBoundingBox( final Ellipse e, final ImagePlus imp, final int boundaryX, final int boundaryY )
	{
		double xMin = e.getPointXAt( 0 );
		double xMax = xMin;
		double yMin = e.getPointYAt( 0 );
		double yMax = yMin;

		final double step = 0.05;

		for ( double t = step; t < 2*Math.PI; t += step )
		{
			final double x = e.getPointXAt( t );
			final double y = e.getPointYAt( t );

			xMin = Math.min( x, xMin );
			yMin = Math.min( y, yMin );

			xMax = Math.max( x, xMax );
			yMax = Math.max( y, yMax );
		}

		long x0 = Math.round( Math.floor( xMin ) ) - boundaryX;
		long x1 = Math.round( Math.ceil( xMax ) ) + boundaryX;

		long y0 = Math.round( Math.floor( yMin ) ) - boundaryY;
		long y1 = Math.round( Math.ceil( yMax ) ) + boundaryY;

		return imageSizeAdjustedInterval(x0, x1, y0, y1, imp.getWidth(), imp.getHeight() );
	}

	public static FinalInterval imageSizeAdjustedInterval( final long x0, final long x1, final long y0, final long y1, final long imgWidth, final long imgHeight )
	{
		return new FinalInterval(
				new long[] {
					Math.max( 0, x0 ),
					Math.max( 0, y0 )
				},
				new long[] {
						Math.min( imgWidth - 1, x1 ),
						Math.min( imgHeight - 1, y1 )
				} );		
	}

	public static void computeEllipseMask( final Ellipse ellipse, final ImageProcessor mask, final Interval cropArea )
	{
		// is inside positive or negative?
		final double inv;
		if ( ellipse.eval( ellipse.getXC(), ellipse.getYC() ) < 0 )
			inv = -1;
		else
			inv = 1;

		// for every pixel do ...
		for ( int x = 0; x < mask.getWidth(); ++x )
			for ( int y = 0; y < mask.getHeight(); ++y )
			{
				final double value = ellipse.eval( x + (int)cropArea.min( 0 ), y + (int)cropArea.min( 1 ) ) * inv;
				if ( value >= 0 )
					mask.set( x, y, 255 );
				else
					mask.set( x,y, 0 );
			}
	}

	public static void computeROIMask( final PolygonRoi roi, final ImageProcessor mask, final Interval cropArea )
	{
		// for every pixel do ...
		for ( int x = 0; x < mask.getWidth(); ++x )
			for ( int y = 0; y < mask.getHeight(); ++y )
			{
				if ( roi.contains( x + (int)cropArea.min( 0 ), y + (int)cropArea.min( 1 ) ) )
					mask.set( x, y, 255 );
				else
					mask.set( x,y, 0 );
			}
	}

	public static void main( String[] args ) throws IOException
	{
		new ImageJ();

		/*
		final ImagePlus i = new Opener().openImage( "/Users/spreibi/Documents/BIMSB/Projects/Dosage Compensation/stephan_ellipsoid/tifs/MK4_1.tif" );

		i.show();

		Duplicator dup = new Duplicator();
		i.setRoi( new Rectangle( 10, 10, 500, 300 ) );
		ImagePlus cropped = dup.run( i, 1, i.getStackSize() );
		CompositeImage c = new CompositeImage( cropped );
		c.setDimensions( i.getNChannels(), i.getNSlices(), i.getNFrames() );
		c.show();
		

		i.setRoi( new Rectangle( 500, 500, 500, 500 ) );
		cropped = dup.run( i, 1, i.getStackSize() );
		c = new CompositeImage( cropped );
		c.setDimensions( i.getNChannels(), i.getNSlices(), i.getNFrames() );
		c.show();
		*/

		// boundary around the ellipse for cropping (can be smaller if image is not big enough to support that)
		final int boundary = 40;

		final File csvFile = TextFileAccess.loadPath();

		if ( csvFile == null )
		{
			System.out.println( "CSV files not defined in path.txt" );
			System.out.println( "csvFile= " + csvFile );
			System.exit( 0 );
		}

		final ArrayList< LoadedEmbryo > embryos = LoadedEmbryo.readCSV( csvFile );

		final String parentDir = "/finaldata";
		final String tifDir = parentDir + "/tifs";
		final String maskDir = parentDir + "/masks";

		File dir = new File( csvFile.getParentFile().getParentFile() + parentDir );
		if ( !dir.exists() )
			dir.mkdir();

		dir = new File( csvFile.getParentFile().getParentFile() + tifDir );
		if ( !dir.exists() )
			dir.mkdir();

		dir = new File( csvFile.getParentFile().getParentFile() + maskDir );
		if ( !dir.exists() )
			dir.mkdir();

		int i = 1;

		for ( final LoadedEmbryo e : embryos )
		{
			System.out.println( "Investigating: '" + e.filename + "' (" + i++ + "/" + embryos.size() + "), status=" + e.status );

			// it is marked as good and that the files are not mentioned in the csv, meaning it has not been processed yet
			if ( e.status == Status.GOOD && ( e.croppedImgFile.trim().length() == 0 || e.croppedMaskFile.trim().length() == 0 || !new File( e.croppedImgFile ).exists() ) || !new File( e.croppedMaskFile ).exists() ) 
			{
				System.out.println( "Computing final masks for : '" + e.filename + "'" );

				// save cropped image and mask
				File maskFile = null, cropFile = null;
				String fileNameMask, fileNameTIF;

				if ( e.croppedImgFile.trim().length() != 0 && e.croppedMaskFile.trim().length() != 0 )
				{
					// reuse name from CSV if it's there
					maskFile = new File( csvFile.getParentFile().getParentFile() + maskDir, e.croppedMaskFile );
					cropFile = new File( csvFile.getParentFile().getParentFile() + tifDir, e.croppedImgFile );

					fileNameMask = e.croppedMaskFile;
					fileNameTIF = e.croppedImgFile;
				}
				else
				{
					// make a new name, make sure it doesnt exist by accident from any previous cropping run
					do
					{
						if ( maskFile != null )
							++i;
	
						String newFileName = e.filename + "_cropped_" + i;
						String newFileNameMask = newFileName + ".mask.tif";
						String newFileNameTIF = newFileName + ".tif";
		
						maskFile = new File( csvFile.getParentFile().getParentFile() + maskDir, newFileNameMask );
						cropFile = new File( csvFile.getParentFile().getParentFile() + tifDir, newFileNameTIF );

						fileNameMask = newFileNameMask;
						fileNameTIF = newFileNameTIF;
					}
					while ( maskFile.exists() || cropFile.exists() );
				}

				final File image = new File( csvFile.getParentFile().getParentFile() + "/tifs", e.filename + ".tif" );

				if ( !image.exists())
					throw new RuntimeException( "Couldn't find image file: " + image.getAbsolutePath() );

				final ImagePlus imp = new Opener().openImage( image.getAbsolutePath() );

				if ( imp == null )
					throw new RuntimeException( "Couldn't open image: " + image.getAbsolutePath() );

				// find bounding box
				final Interval cropArea = findBoundingBox( e.eor, imp, boundary );

				// cropped imp (all channels, z-slices)
				final Duplicator dup = new Duplicator();
				imp.setRoi( new Rectangle( (int)cropArea.min( 0 ), (int)cropArea.min( 1 ), (int)cropArea.dimension( 0 ), (int)cropArea.dimension( 1 ) ) );
				final CompositeImage cropped = new CompositeImage( dup.run( imp, 1, imp.getStackSize() ) );
				cropped.setDimensions( imp.getNChannels(), imp.getNSlices(), imp.getNFrames() );

				// make mask
				final ImagePlus mask = IJ.createImage( "mask", (int)cropArea.dimension( 0 ), (int)cropArea.dimension( 1 ), 1, 8 );
				final ImageProcessor ip = mask.getProcessor();

				if ( e.eor.isEllipse() )
					computeEllipseMask( e.eor.getEllipse(), ip, cropArea );
				else
					computeROIMask( e.eor.getROI(), ip, cropArea );

				new FileSaver( mask ).saveAsTiff( maskFile.getAbsolutePath() );
				new FileSaver( cropped ).saveAsTiffStack( cropFile.getAbsolutePath() );

				/*
				// DEBUG: Show output images
				final Overlay o = new Overlay();
				e.ellipse.draw( o, 0.01, Color.yellow );
				mask.setOverlay( o );
				imp.setOverlay( o );
				imp.show();
				mask.show();
				cropped.show();
				while ( imp != null ) {}
				*/

				imp.close();
				cropped.close();
				mask.close();

				// update the Embryo information
				// we need to add:
				// - mask file
				// - cropped tiff file
				// - offset
				e.croppedMaskFile = fileNameMask;
				e.croppedImgFile = fileNameTIF;
				e.cropOffsetX = (int)cropArea.min( 0 );
				e.cropOffsetY = (int)cropArea.min( 1 );
			}
		}

		// save new csv to finaldata
		System.out.println( "saving csv to '" + csvFile.getAbsolutePath() + "'" );
		LoadedEmbryo.saveCSV( embryos, csvFile );

		System.out.println( "done" );
		IJ.log( "done" );

	}
}
