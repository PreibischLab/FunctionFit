package embryo.gui;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import embryo.FindEmbryos;
import embryo.Util;
import embryo.gui.LoadedEmbryo.Status;
import fit.PointFunctionMatch;
import fit.circular.Ellipse;
import fit.circular.EllipsePointDistanceFactory;
import fit.circular.ShapePointDistanceFactory;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.io.FileSaver;
import ij.io.Opener;
import ij.plugin.StackCombiner;
import ij.plugin.ZProjector;
import mpicbg.models.Point;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Pair;

public class FindAllEmbryos
{
	public static ArrayList< LoadedEmbryo > processEmbryoimage( final LoadedEmbryo e, final File csv, final boolean showImages )
	{
		final File file = new File( csv.getParentFile().getParentFile() + "/masks", e.filename + ".tif" );

		IJ.log( "Processing mask: " + file.getAbsolutePath() );

		final Img< FloatType > img = Util.openAs32Bit( file );
		final Img< FloatType > edgeImg = img.factory().create( img, img.firstElement() );

		final ArrayList< Point > mts = FindEmbryos.edges4( img, edgeImg );

		System.out.println( "Found " + mts.size() + " edge pixels." );

		ImagePlus edgeImp = null;
		if ( showImages )
		{
			edgeImp = ImageJFunctions.show( edgeImg );
			edgeImp.setTitle( e.filename );	
		}

		final double minArea = 35000; // minimal size in square-pixels of the ellipse
		final double maxArea = 100000; // maximal size in square-pixels of the ellipse
		final double minRatio = 0.999; // ratio = large axis / small axis
		final double maxRatio = 3.0;
		final double maxError = 10.0; // maximal distance of an edge pixel to the ellipse and still belong to it
		final int minNuminliers = 800; // minimal amount of edge pixels that belong to the ellipse
		final int numIterations = 500; // how often RANSAC tries

		final ShapePointDistanceFactory< Ellipse, ?, ? > factory = new EllipsePointDistanceFactory();//BruteForceShapePointDistanceFactory< Ellipse >();

		final ArrayList< Pair< Ellipse, ArrayList< PointFunctionMatch > > > functions =
				Util.findAllFunctions( mts, new Ellipse( factory ), maxError, minNuminliers, minArea, maxArea, minRatio, maxRatio, numIterations );

		final Overlay o = new Overlay();

		final ArrayList< LoadedEmbryo > embryos = new ArrayList< LoadedEmbryo >();

		if ( functions.size() == 0 )
		{
			final LoadedEmbryo newEmbryo = e.clone();
			newEmbryo.eor = null;
			newEmbryo.status = Status.NO_ELLIPSE_FOUND;

			embryos.add( newEmbryo );
		}
		else
		{
			for ( final Pair< Ellipse, ArrayList< PointFunctionMatch > > function : functions )
			{
				final Ellipse ellipse = function.getA();
	
				System.out.println( "isEllipse: " + ellipse.isEllipse() );
				System.out.println( "area: " + ellipse.area() );
	
				ellipse.drawCenter( o, Color.YELLOW );
				ellipse.drawAxes( o, Color.YELLOW );
				ellipse.draw( o, 0.01, Color.YELLOW );

				final LoadedEmbryo newEmbryo = e.clone();
				newEmbryo.eor = new EllipseOrROI( ellipse );
				newEmbryo.status = Status.NOT_ASSIGNED;

				embryos.add( newEmbryo );

			}
		}

		if ( showImages )
		{
			edgeImp.setOverlay( o );
			edgeImp.updateAndDraw();			
		}

		return embryos;
	}

	public static void prepareImages( final LoadedEmbryo e, final File csv, final boolean onlyDAPI )
	{
		final File dir = new File( csv.getParentFile().getParentFile() + "/preview" );

		if ( !dir.exists() )
			dir.mkdir();

		final File dapiFile = new File( csv.getParentFile().getParentFile() + "/preview", e.filename + EmbryoGUI.dapiExt );

		if ( dapiFile.exists() )
			return;

		final File image = new File( csv.getParentFile().getParentFile() + "/tifs", e.filename + ".tif" );

		if ( !image.exists())
			throw new RuntimeException( "Couldn't find image file: " + image.getAbsolutePath() );

		final ImagePlus imp = new Opener().openImage( image.getAbsolutePath() );

		if ( imp == null )
			throw new RuntimeException( "Couldn't open image: " + image.getAbsolutePath() );

		final int size = imp.getNSlices();

		final ImagePlus cy5;

		if ( onlyDAPI )
			cy5 = null;
		else
			cy5 = new ImagePlus( "cy5", imp.getStack().getProcessor( imp.getStackIndex( e.getChannelFor( "cy5" ) + 1, size / 2 + 1, 1 ) ) );

		final ImagePlus proj = ZProjector.run(imp,"max");
		imp.close();

		final ImagePlus dapiMax = new ImagePlus( "dapi", proj.getStack().getProcessor( imp.getStackIndex( e.getChannelFor( "dapi" ) + 1, 1, 1 ) ) );
		final ImagePlus gfpMax;

		if ( onlyDAPI )
		{
			gfpMax = null;
		}
		else
		{
			final int gfpChannel = e.getChannelFor( "gfp" );

			if ( gfpChannel >= 0 )
				gfpMax = new ImagePlus( "gfp", proj.getStack().getProcessor( imp.getStackIndex( e.getChannelFor( "gfp" ) + 1, 1, 1 ) ) );
			else
				gfpMax = IJ.createImage( "gfp", dapiMax.getWidth(), dapiMax.getHeight(), 1, 16 );
		}

		dapiMax.resetDisplayRange();
		if ( !onlyDAPI )
		{
			gfpMax.resetDisplayRange();
			cy5.resetDisplayRange();
		}

		if ( onlyDAPI )
		{
			new FileSaver( dapiMax ).saveAsJpeg( dapiFile.getAbsolutePath() );
			dapiMax.close();
		}
		else
		{
			// assemble montage
			IJ.run(dapiMax, "8-bit", "");
			IJ.run(gfpMax, "8-bit", "");
			IJ.run(cy5, "8-bit", "");

			IJ.run(gfpMax, "Canvas Size...", "width=" + gfpMax.getWidth()*2 + " height=" + gfpMax.getHeight()  + " position=Top-Left zero");
			ImagePlus combined = new ImagePlus( "combined", new StackCombiner().combineHorizontally( dapiMax.getStack(), cy5.getStack() ) );
			ImagePlus finalImg = new ImagePlus( "final", new StackCombiner().combineVertically( combined.getStack(), gfpMax.getStack() ) );

			new FileSaver( finalImg ).saveAsJpeg( dapiFile.getAbsolutePath() );
	
			finalImg.close();
			combined.close();
			dapiMax.close();
			cy5.close();
			gfpMax.close();
		}
	}

	// Laura: you need a "public static void main([] args )" method to start
	public static void main( String[] args ) throws IOException
	{
		new ImageJ();

		/*
			path.txt:
			/Users/spreibi/Documents/BIMSB/Projects/Dosage Compensation/stephan_ellipsoid2/test-embryo.csv

			path.txt (OLD):
			ORIGINAL		/Users/spreibi/Documents/BIMSB/Projects/Dosage Compensation/stephan_ellipsoid/stephan_embryo_table.csv
			ANNOTATED	/Users/spreibi/Documents/BIMSB/Projects/Dosage Compensation/stephan_ellipsoid/stephan_embryo_table_annotated.csv
			CROPPED	/Users/spreibi/Documents/BIMSB/Projects/Dosage Compensation/stephan_ellipsoid/stephan_embryo_table_cropped.csv
		 */
		final File csvFile = TextFileAccess.loadPath();

		if ( csvFile == null || !csvFile.exists() )
		{
			System.out.println( "CSV file not defined in path.txt" );
			System.out.println( "csvFile= " + csvFile );
			System.exit( 0 );
		}

		// TODO
		// if annotated.exists() // >> this is an additional run
		// annotated.load()
		// process only those e.filename from original that are NOT present in annotated
		// add all to annotated
		// save annotated

		final ArrayList< LoadedEmbryo > embryos = LoadedEmbryo.readCSV( csvFile );
		final ArrayList< LoadedEmbryo > annotatedembryos = new ArrayList< LoadedEmbryo >();

		int i = 1;

		for ( final LoadedEmbryo e : embryos )
		{
			System.out.println( "Processing: '" + e.filename + "' (" + i++ + "/" + embryos.size() + "), STATUS=" + e.status );

			if ( e.gfpChannelIndex == -1 || e.dapiChannelIndex == -1 )
			{
				System.out.println( "Warning: No GFP and/or DAPI signal available, removing this image." );
				continue;
			}
			//System.out.println( LoadedEmbryo.toString( e ) );

			//if ( e.filename.equals( "SEA-12_300" ))
			if ( e.status == Status.NOT_RUN_YET || e.status == Status.NO_ELLIPSE_FOUND )
				annotatedembryos.addAll( processEmbryoimage( e, csvFile, false ) );

			//if ( e.filename.equals( "MK4_1" ))
			if ( e.status == Status.NOT_RUN_YET )
				prepareImages( e, csvFile, false );
		}

		//annotatedembryos.addAll( embryos );

		System.out.println( "saving csv to '" + csvFile.getAbsolutePath() + "'" );
		LoadedEmbryo.saveCSV( annotatedembryos, csvFile );

		System.out.println( "done" );
		//IJ.log( "done" );

		/*
		final ArrayList< LoadedEmbryo > embryosLoaded = LoadedEmbryo.loadCSV( new File( "/Users/spreibi/Documents/BIMSB/Projects/Dosage Compensation/stephan_ellipsoid/stephan_embryo_table3_test.csv") );
	
		ImagePlus imp = IJ.createImage( "test", 1000, 1000, 1, 32 );
		final Overlay o = new Overlay();

		embryosLoaded.get( 0 ).ellipse.drawCenter( o );
		embryosLoaded.get( 0 ).ellipse.drawAxes( o );
		embryosLoaded.get( 0 ).ellipse.draw( o, 0.01 );

		imp.setOverlay( o );
		imp.updateAndDraw();
		imp.show();
		*/

	}
}
