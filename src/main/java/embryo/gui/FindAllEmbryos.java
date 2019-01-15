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

		IJ.log( "done" );

		return embryos;
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
		}

		LoadedEmbryo.saveCSV( annotatedembryos, new File( "/Users/spreibi/Documents/BIMSB/Projects/Dosage Compensation/stephan_ellipsoid/stephan_embryo_table_annotated.csv") );

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
