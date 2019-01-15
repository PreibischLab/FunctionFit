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
	public static void processEmbryoimage( final LoadedEmbryo e, final File csv )
	{
		final File file = new File( csv.getParentFile() + "/masks", e.filename + ".tif" );

		IJ.log( "Processing mask: " + file.getAbsolutePath() );

		final Img< FloatType > img = Util.openAs32Bit( file );
		final Img< FloatType > edgeImg = img.factory().create( img, img.firstElement() );

		final ArrayList< Point > mts = FindEmbryos.edges4( img, edgeImg );

		System.out.println( "Found " + mts.size() + " edge pixels." );

		ImagePlus edgeImp = ImageJFunctions.show( edgeImg );

		final double minArea = 40000; // minimal size in square-pixels of the ellipse
		final double maxArea = 90000; // maximal size in square-pixels of the ellipse
		final double maxError = 20.0; // maximal distance of an edge pixel to the ellipse and still belong to it
		final int minNuminliers = 300; // minimal amount of edge pixels that belong to the ellipse

		final ShapePointDistanceFactory< Ellipse, ?, ? > factory = new EllipsePointDistanceFactory();//BruteForceShapePointDistanceFactory< Ellipse >();

		final ArrayList< Pair< Ellipse, ArrayList< PointFunctionMatch > > > functions =
				Util.findAllFunctions( mts, new Ellipse( factory ), maxError, minNuminliers, minArea, maxArea );

		final Overlay o = new Overlay();

		for ( final Pair< Ellipse, ArrayList< PointFunctionMatch > > function : functions )
		{
			final Ellipse ellipse = function.getA();

			System.out.println( "isEllipse: " + ellipse.isEllipse() );
			System.out.println( "area: " + ellipse.area() );

			ellipse.drawCenter( o );
			ellipse.drawAxes( o );
			ellipse.draw( o, 0.01 );

		}

		IJ.log( "done" );

		edgeImp.setOverlay( o );
		edgeImp.updateAndDraw();
	}

	public static void main( String[] args )
	{
		new ImageJ();

		final File csvFile = new File( "/Users/spreibi/Documents/BIMSB/Projects/Dosage Compensation/stephan_ellipsoid/stephan_embryo_table3.csv");

		final ArrayList< LoadedEmbryo > embryos = LoadedEmbryo.loadCSV( csvFile );

		//for ( final LoadedEmbryo e : embryos )
		{
			processEmbryoimage( embryos.get( 0 ), csvFile );
			
		}

		//saveCSV( embryos, file );

	}
}
