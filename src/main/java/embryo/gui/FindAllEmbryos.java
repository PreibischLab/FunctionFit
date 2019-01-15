package embryo.gui;

import java.io.File;
import java.util.ArrayList;

import embryo.FindEmbryos;
import embryo.Util;
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
import ij.plugin.ZProjector;
import mpicbg.models.Point;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Pair;

public class FindAllEmbryos
{
	public static ArrayList< LoadedEmbryo > processEmbryoimage( final LoadedEmbryo e, final File csv )
	{
		final File file = new File( csv.getParentFile() + "/masks", e.filename + ".tif" );

		IJ.log( "Processing mask: " + file.getAbsolutePath() );

		final Img< FloatType > img = Util.openAs32Bit( file );
		final Img< FloatType > edgeImg = img.factory().create( img, img.firstElement() );

		final ArrayList< Point > mts = FindEmbryos.edges4( img, edgeImg );

		System.out.println( "Found " + mts.size() + " edge pixels." );

		ImagePlus edgeImp = ImageJFunctions.show( edgeImg );
		edgeImp.setTitle( e.filename );

		final double minArea = 35000; // minimal size in square-pixels of the ellipse
		final double maxArea = 100000; // maximal size in square-pixels of the ellipse
		final double minRatio = 0.999; // ratio = large axis / small axis
		final double maxRatio = 3.0;
		final double maxError = 10.0; // maximal distance of an edge pixel to the ellipse and still belong to it
		final int minNuminliers = 800; // minimal amount of edge pixels that belong to the ellipse

		final ShapePointDistanceFactory< Ellipse, ?, ? > factory = new EllipsePointDistanceFactory();//BruteForceShapePointDistanceFactory< Ellipse >();

		final ArrayList< Pair< Ellipse, ArrayList< PointFunctionMatch > > > functions =
				Util.findAllFunctions( mts, new Ellipse( factory ), maxError, minNuminliers, minArea, maxArea, minRatio, maxRatio );

		final Overlay o = new Overlay();

		final ArrayList< LoadedEmbryo > embryos = new ArrayList< LoadedEmbryo >();

		if ( functions.size() == 0 )
		{
			final LoadedEmbryo newEmbryo = e.clone();
			newEmbryo.ellipse = null;

			embryos.add( newEmbryo );
		}
		else
		{
			for ( final Pair< Ellipse, ArrayList< PointFunctionMatch > > function : functions )
			{
				final Ellipse ellipse = function.getA();
	
				System.out.println( "isEllipse: " + ellipse.isEllipse() );
				System.out.println( "area: " + ellipse.area() );
	
				ellipse.drawCenter( o );
				ellipse.drawAxes( o );
				ellipse.draw( o, 0.01 );

				final LoadedEmbryo newEmbryo = e.clone();
				newEmbryo.ellipse = ellipse;

				embryos.add( newEmbryo );

			}
		}

		edgeImp.setOverlay( o );
		edgeImp.updateAndDraw();

		return embryos;
	}

	public static void prepareImages( final LoadedEmbryo e, final File csv )
	{
		final File dir = new File( csv.getParentFile() + "/preview" );

		if ( !dir.exists() )
			dir.mkdir();

		final File gfpFile = new File( csv.getParentFile() + "/preview", e.filename + ".gfpmax.jpg" );
		final File dapiFile = new File( csv.getParentFile() + "/preview", e.filename + ".dapimax.jpg" );
		final File cy5File = new File( csv.getParentFile() + "/preview", e.filename + ".cy5mid.jpg" );

		if ( gfpFile.exists() && dapiFile.exists() && cy5File.exists() )
			return;

		final File image = new File( csv.getParentFile() + "/tifs", e.filename + ".tif" );

		if ( !image.exists())
			throw new RuntimeException( "Couldn't find image file: " + image.getAbsolutePath() );

		final ImagePlus imp = new Opener().openImage( image.getAbsolutePath() );

		if ( imp == null )
			throw new RuntimeException( "Couldn't open image: " + image.getAbsolutePath() );

		final int size = imp.getNSlices();

		final ImagePlus cy5 = new ImagePlus( "cy5", imp.getStack().getProcessor( imp.getStackIndex( e.getChannelFor( "cy5" ) + 1, size / 2 + 1, 1 ) ) );
		final ImagePlus proj = ZProjector.run(imp,"max");
		imp.close();

		final ImagePlus dapiMax = new ImagePlus( "dapi", proj.getStack().getProcessor( imp.getStackIndex( e.getChannelFor( "dapi" ) + 1, 1, 1 ) ) );

		final int gfpChannel = e.getChannelFor( "gfp" );

		final ImagePlus gfpMax;
		if ( gfpChannel >= 0 )
			gfpMax = new ImagePlus( "gfp", proj.getStack().getProcessor( imp.getStackIndex( e.getChannelFor( "gfp" ) + 1, 1, 1 ) ) );
		else
			gfpMax = IJ.createImage( "gfp", dapiMax.getWidth(), dapiMax.getHeight(), 1, 16 );

		dapiMax.resetDisplayRange();
		gfpMax.resetDisplayRange();
		cy5.resetDisplayRange();

		new FileSaver( dapiMax ).saveAsJpeg( dapiFile.getAbsolutePath() );
		new FileSaver( gfpMax ).saveAsJpeg( gfpFile.getAbsolutePath() );
		new FileSaver( cy5 ).saveAsJpeg( cy5File.getAbsolutePath() );
		//cy5.show();
		//dapiMax.show();
		//gfpMax.show();

		cy5.close();
		dapiMax.close();
		gfpMax.close();
	}

	public static void main( String[] args )
	{
		new ImageJ();

		final File csvFile = new File( "/Users/spreibi/Documents/BIMSB/Projects/Dosage Compensation/stephan_ellipsoid/stephan_embryo_table3.csv");

		final ArrayList< LoadedEmbryo > embryos = LoadedEmbryo.loadCSV( csvFile );
		final ArrayList< LoadedEmbryo > annotatedembryos = new ArrayList< LoadedEmbryo >();

		for ( final LoadedEmbryo e : embryos )
		{
			//if ( e.filename.equals( "SEA-12_300" ))
			annotatedembryos.addAll( processEmbryoimage( e, csvFile ) );
			
			//if ( e.filename.equals( "MK4_1" ))
			prepareImages( e, csvFile );
		}

		LoadedEmbryo.saveCSV( annotatedembryos, new File( "/Users/spreibi/Documents/BIMSB/Projects/Dosage Compensation/stephan_ellipsoid/stephan_embryo_table_annotated.csv") );

		IJ.log( "done" );

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
