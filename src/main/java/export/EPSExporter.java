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
