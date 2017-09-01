package export;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileOutputStream;

import org.jfree.chart.JFreeChart;
import org.sourceforge.jlibeps.epsgraphics.EpsGraphics2D;

public class EPSExporter
{
	public static boolean write( final JFreeChart chart, final File file, final String title, final int w, final int h )
	{
		try
		{
			FileOutputStream finalImage = new FileOutputStream( file );
			EpsGraphics2D g = new EpsGraphics2D( title, finalImage, 0, 0, w, h);
			Rectangle2D drawArea = new Rectangle2D.Double(0, 0, w, h );
			chart.draw(g, drawArea);
			g.flush();
			g.close();
			finalImage.close();

			return true;
		}
		catch ( Exception e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();

			return false;
		}
	}
}
