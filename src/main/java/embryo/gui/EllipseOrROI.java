package embryo.gui;

import fit.circular.Ellipse;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

public class EllipseOrROI
{
	final Ellipse e;
	final PolygonRoi p;

	public EllipseOrROI( final Ellipse e )
	{
		this.e = e;
		this.p = null;
	}

	public EllipseOrROI( final PolygonRoi p )
	{
		this.p = p;
		this.e = null;
	}

	public boolean isEllipse() { return p == null; }
	public Ellipse getEllipse() { return e; }
	public PolygonRoi getROI() { return p; }

	public static Pair< int[], int[] > getROIPoints( final PolygonRoi roi )
	{
		final int[] xpTmp = roi.getXCoordinates();
		final int[] ypTmp = roi.getYCoordinates();

		final int[] xp = new int[ roi.getNCoordinates() ];
		final int[] yp = new int[ roi.getNCoordinates() ];

		final int x = (int)Math.round( roi.getBounds().getMinX() );
		final int y = (int)Math.round( roi.getBounds().getMinY() );

		for ( int i = 0; i < xp.length; ++i )
		{
			xp[ i ] = xpTmp[ i ] + x;
			yp[ i ] = ypTmp[ i ] + y;
		}

		return new ValuePair< int[], int[] >( xp, yp );
	}

	public EllipseOrROI copy()
	{
		if ( isEllipse() )
		{
			return new EllipseOrROI( e.copy() );
		}
		else
		{
			final Pair< int[], int[] > roiCoord = EllipseOrROI.getROIPoints( p );

			final PolygonRoi newRoi = new PolygonRoi( roiCoord.getA(), roiCoord.getB(), roiCoord.getA().length, Roi.FREEROI );

			return new EllipseOrROI( newRoi );
		}
	}
}
