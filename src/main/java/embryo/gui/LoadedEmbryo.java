package embryo.gui;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import fit.circular.Ellipse;
import fit.circular.EllipsePointDistanceFactory;
import ij.IJ;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import net.imglib2.util.Pair;

public class LoadedEmbryo
{
	public static int numBackups = 5;

	final static String[] requiredColumns = new String[]{
			"", "#channels", "c0", "c1", "c2", "c3", "c4",
			"c0_lambda", "c1_lambda", "c2_lambda", "c3_lambda", "c4_lambda", 
			"original filename", "DAPI channel", "mask maker", "GFP channel",
			"c0_type", "c1_type", "c2_type", "comments", "stage", "integrity",
			"signal", "filename" };

	final static String[] optionalColumns = new String[]{
			"status", "ellipse", "valid", "cropped_mask_file", "cropped_image_file", "crop_offset_x", "crop_offset_y" };

	public enum Status { NOT_ASSIGNED, GOOD, INCOMPLETE, BAD };

	Status status;
	EllipseOrROI eor;

	int id, numChannels, dapiChannelIndex, gfpChannelIndex;
	String c0, c1, c2, c3, c4, c0_lambda, c1_lambda, c2_lambda, c3_lambda, c4_lambda;
	String originalFN, manualMaskMaker;
	String c0_type, c1_type, c2_type;
	String comments, stage, filename;
	int signal, integrity;

	int cropOffsetX = -1, cropOffsetY = -1;
	String croppedImgFile = null, croppedMaskFile = null;

	public LoadedEmbryo()
	{
		this.status = Status.NOT_ASSIGNED;
	}

	public LoadedEmbryo( final Status status )
	{
		this.status = status;
	}

	public void updateStatus( final Status newStatus, final EmbryoGUI gui )
	{
		this.status = newStatus;

		if ( gui != null )
			updateGUI( gui );
	}


	public void updateGUI( final EmbryoGUI gui )
	{
		gui.good.setBackground( gui.orginalBackground );
		gui.good.setForeground( gui.originalForeground );

		gui.incomplete.setBackground( gui.orginalBackground );
		gui.incomplete.setForeground( gui.originalForeground );

		gui.bad.setBackground( gui.orginalBackground );
		gui.bad.setForeground( gui.originalForeground );

		if ( eor == null )
		{
			gui.good.setEnabled( false );
			gui.incomplete.setEnabled( false );
			gui.bad.setEnabled( false );
		}
		else
		{
			gui.good.setEnabled( true );
			gui.incomplete.setEnabled( true );
			gui.bad.setEnabled( true );
		}

		if ( status == Status.GOOD )
		{
			gui.good.setBackground( EmbryoGUI.goodColor );
			gui.good.setForeground( EmbryoGUI.goodColorFG );
		}
		else if ( status == Status.INCOMPLETE )
		{
			gui.incomplete.setBackground( EmbryoGUI.incompleteColor  );
			gui.incomplete.setForeground( EmbryoGUI.incompleteColorFG );
		}
		else if ( status == Status.BAD )
		{
			gui.bad.setBackground( EmbryoGUI.badColor );
			gui.bad.setForeground( EmbryoGUI.badColorFG );
		}

		gui.text0.setText( " Filename: " + this.filename );
		gui.text1.setText( " cy5: " + getChannelFor( "cy5" ) + ", dapi: " + getChannelFor( "dapi" ) + ", gfp: " + getChannelFor( "gfp" ) );
		gui.text2.setText( " c0: " + this.c0 + " (" + this.c0_type + ")" + ", c1: " + this.c1 + " (" + this.c1_type + ")" + ", c2: " + this.c2 + " (" + this.c2_type + ")"  );
	}

	public void drawEllipseOrROI( final Overlay o, final boolean active )
	{
		if ( eor == null )
			return;

		Color c;

		if ( active )
		{
			if ( status == Status.GOOD )
				c = EmbryoGUI.goodColor;
			else if ( status == Status.INCOMPLETE )
				c = EmbryoGUI.incompleteColor;
			else if ( status == Status.BAD )
				c = EmbryoGUI.badColor;
			else
				c = EmbryoGUI.notAssignedColor;
		}
		else
		{
			if ( status == Status.GOOD )
				c = EmbryoGUI.goodColorBG;
			else if ( status == Status.INCOMPLETE )
				c = EmbryoGUI.incompleteColorBG;
			else if ( status == Status.BAD )
				c = EmbryoGUI.badColorBG;
			else
				c = EmbryoGUI.notAssignedColorBG;
		}

		if ( eor.isEllipse() )
		{
			eor.getEllipse().drawCenter( o, c );
			eor.getEllipse().drawAxes( o, c );
			eor.getEllipse().draw( o, 0.01, c );
		}
		else
		{
			eor.getROI().setStrokeColor( c );
			o.add( eor.getROI() );
		}
	}

	@Override
	public LoadedEmbryo clone()
	{
		final LoadedEmbryo newEmbryo = new LoadedEmbryo();

		newEmbryo.status = this.status;

		if ( this.eor != null )
			newEmbryo.eor = this.eor.copy();
		else
			newEmbryo.eor = null;

		newEmbryo.id = id;
		newEmbryo.numChannels = numChannels;
		newEmbryo.dapiChannelIndex = dapiChannelIndex;
		newEmbryo.gfpChannelIndex = gfpChannelIndex;
		newEmbryo.c0 = c0;
		newEmbryo.c1 = c1;
		newEmbryo.c2 = c2;
		newEmbryo.c3 = c3;
		newEmbryo.c4 = c4;
		newEmbryo.c0_lambda = c0_lambda;
		newEmbryo.c1_lambda = c1_lambda;
		newEmbryo.c2_lambda = c2_lambda;
		newEmbryo.c3_lambda = c3_lambda;
		newEmbryo.c4_lambda = c4_lambda;
		newEmbryo.originalFN = originalFN;
		newEmbryo.manualMaskMaker = manualMaskMaker;
		newEmbryo.c0_type = c0_type;
		newEmbryo.c1_type = c1_type;
		newEmbryo.c2_type = c2_type;
		newEmbryo.comments = comments;
		newEmbryo.stage = stage;
		newEmbryo.filename = filename;
		newEmbryo.signal = signal;
		newEmbryo.integrity = integrity;

		newEmbryo.cropOffsetX = cropOffsetX;
		newEmbryo.cropOffsetY = cropOffsetY;
		newEmbryo.croppedMaskFile = croppedMaskFile;
		newEmbryo.croppedImgFile = croppedImgFile;

		return newEmbryo;
	}

	public int getChannelFor( final String label )
	{
		if ( c0.equalsIgnoreCase( label ) )
			return 0;
		else  if ( c1.equalsIgnoreCase( label ) )
			return 1;
		else  if ( c2.equalsIgnoreCase( label ) )
			return 2;
		else  if ( c3.equalsIgnoreCase( label ) )
			return 3;
		else  if ( c4.equalsIgnoreCase( label ) )
			return 4;
		else
			return -1;
	}

	public static String ellipseOrROIToString( final EllipseOrROI eor )
	{
		if ( eor == null )
		{
			return "null";
		}
		else if ( eor.isEllipse())
		{
			final Ellipse e = eor.getEllipse();
			return "E_" + e.getA() + "_" + e.getB() + "_" + e.getC() + "_" + e.getD() + "_" + e.getE() + "_" + e.getF();
		}
		else
		{
			final Pair< int[], int[] > roiCoord = EllipseOrROI.getROIPoints( eor.getROI() );

			String out = "R";

			for ( int i = 0; i < roiCoord.getA().length; ++i )
				out += "_" + roiCoord.getA()[ i ] + "x" + roiCoord.getB()[ i ];

			return out;
		}
	}

	public static EllipseOrROI stringToEllipseOrROI( final String s )
	{
		final String[] entries = s.trim().split( "_" );

		if ( entries[ 0 ].equals( "R" ) )
		{
			final int[] xp = new int[ entries.length - 1 ];
			final int[] yp = new int[ entries.length - 1 ];

			for ( int i = 1; i < entries.length; ++i )
			{
				final String[] coord = entries[ i ].split( "x" );
				xp[ i - 1 ] = Integer.parseInt( coord[ 0 ] );
				yp[ i - 1 ] = Integer.parseInt( coord[ 1 ] );
			}

			return new EllipseOrROI( new PolygonRoi( xp, yp, xp.length, Roi.FREEROI ) );
		}
		else if ( entries[ 0 ].equals( "E" ) ) 
		{
			if ( entries.length != 7 )
			{
				return null;
			}
			else
			{
				final double a = Double.parseDouble( entries[ 1 ] );
				final double b = Double.parseDouble( entries[ 2 ] );
				final double c = Double.parseDouble( entries[ 3 ] );
				final double d = Double.parseDouble( entries[ 4 ] );
				final double e = Double.parseDouble( entries[ 5 ] );
				final double f = Double.parseDouble( entries[ 6 ] );
	
				return new EllipseOrROI( new Ellipse( a, b, c, d, e, f, new EllipsePointDistanceFactory() ) );
			}
		}
		else // old files do not start with "E" yet, this can be removed at some point
		{
			if ( entries.length != 6 )
			{
				return null;
			}
			else
			{
				final double a = Double.parseDouble( entries[ 0 ] );
				final double b = Double.parseDouble( entries[ 1 ] );
				final double c = Double.parseDouble( entries[ 2 ] );
				final double d = Double.parseDouble( entries[ 3 ] );
				final double e = Double.parseDouble( entries[ 4 ] );
				final double f = Double.parseDouble( entries[ 5 ] );
	
				return new EllipseOrROI( new Ellipse( a, b, c, d, e, f, new EllipsePointDistanceFactory() ) );
			}
		}
	}

	public static int count = 0;
	/*
	 * make an Embryo instance from a line of parameters read from a CSV file
	 */
	public static LoadedEmbryo fromString( final String[] line, final int[] lookup ) throws NumberFormatException
	{
		//System.out.println( ++count );
		
		/*
		if ( count ==  1543 )
		{
			System.out.println();
			
			for ( int i = 0; i <= 23; ++i )
			{
				System.out.println( i + ": " + " " + lookup[i] + ": " + line[ lookup[i] ] );
			}
		//	System.exit( 0);
		}
		*/

		for ( int i = 0; i < line.length; ++i )
			line[ i ] = line[ i ].trim();

		final LoadedEmbryo e = new LoadedEmbryo();

		e.id = (int)Math.round( Double.parseDouble( line[ lookup[0] ] ) );
		e.numChannels = (int)Math.round( Double.parseDouble( line[ lookup[1] ] ) );

		e.c0 = line[ lookup[2] ];
		e.c1 = line[ lookup[3] ];
		e.c2 = line[ lookup[4] ];
		e.c3 = line[ lookup[5] ];
		e.c4 = line[ lookup[6] ];

		e.c0_lambda = line[ lookup[7] ];
		e.c1_lambda = line[ lookup[8] ];
		e.c2_lambda = line[ lookup[9] ];
		e.c3_lambda = line[ lookup[10] ];
		e.c4_lambda = line[ lookup[11] ];

		e.originalFN = line[ lookup[12] ];
		if ( line[ lookup[13] ].length() == 0 )
			return null;
		e.dapiChannelIndex = (int)Math.round( Double.parseDouble( line[ lookup[13] ] ) );
		e.manualMaskMaker = line[ lookup[14] ];

		if ( line[ lookup[15] ].length() > 0 )
			e.gfpChannelIndex = (int)Math.round( Double.parseDouble( line[ lookup[15] ] ) );
		else
			e.gfpChannelIndex = -1;

		e.c0_type = line[ lookup[16] ];
		e.c1_type = line[ lookup[17] ];
		e.c2_type = line[ lookup[18] ];

		e.comments = line[ lookup[19] ];
		e.stage = line[ lookup[20] ];

		
		if ( line[ lookup[21] ].length() > 0 )
			e.integrity = (int)Math.round( Double.parseDouble( line[ lookup[21] ] ) );
		else
			e.integrity = -1;

		if ( line[ lookup[22] ].length() > 0 )
			e.signal = (int)Math.round( Double.parseDouble( line[ lookup[22] ] ) );
		else
			e.signal = -1;

		e.filename = line[ lookup[23] ];
		
		if ( lookup[24] > 0 )
			e.status = Status.values()[ (int)Math.round( Double.parseDouble( line[ lookup[24] ] ) ) ];
		else
			e.status = Status.NOT_ASSIGNED;

		if ( lookup[25] > 0 )
			e.eor = stringToEllipseOrROI( line[ lookup[25] ] );
		else
			e.eor = null;

		if ( lookup[26] > 0 && line[ lookup[26] ].equalsIgnoreCase( "x" ) )
			return null;

		if ( lookup[27] > 0 )
		{
			if ( line[ lookup[ 27 ] ].equals( "null" ) )
				e.croppedMaskFile = null;
			else
				e.croppedMaskFile = line[ lookup[ 27 ] ];
		}

		if ( lookup[28] > 0 )
		{
			if ( line[ lookup[ 28 ] ].equals( "null" ) )
				e.croppedImgFile = null;
			else
				e.croppedImgFile = line[ lookup[ 28 ] ];
		}

		if ( lookup[29] > 0 )
			e.cropOffsetX = Integer.parseInt( line[ lookup[ 29 ] ] );

		if ( lookup[30] > 0 )
			e.cropOffsetX = Integer.parseInt( line[ lookup[ 30 ] ] );

		return e;
	}

	/*
	 * make a CSV line for saving
	 */
	public static String toString( final LoadedEmbryo e )
	{
		String s = Integer.toString( e.id ) + ",";
		s += Integer.toString( e.numChannels ) + ",";

		s += e.c0 + ",";
		s += e.c1 + ",";
		s += e.c2 + ",";
		s += e.c3 + ",";
		s += e.c4 + ",";

		s += e.c0_lambda + ",";
		s += e.c1_lambda + ",";
		s += e.c2_lambda + ",";
		s += e.c3_lambda + ",";
		s += e.c4_lambda + ",";

		s += e.originalFN + ",";
		s += Integer.toString( e.dapiChannelIndex ) + ",";
		s += e.manualMaskMaker + ",";

		if ( e.gfpChannelIndex >= 0 )
			s += Integer.toString( e.gfpChannelIndex ) + ",";
		else
			s += ",";

		s += e.c0_type + ",";
		s += e.c1_type + ",";
		s += e.c2_type + ",";

		s += e.comments + ",";
		s += e.stage + ",";

		if ( e.integrity >= 0 )
			s += Integer.toString( e.integrity ) + ",";
		else
			s += ",";

		if ( e.signal >= 0 )
			s += Integer.toString( e.signal ) + ",";
		else
			s += ",";

		s += e.filename + ",";

		s += Integer.toString( e.status.ordinal() ) + ",";

		if ( e.eor != null )
			s += ellipseOrROIToString( e.eor ) + ",";
		else
			s += "null" + ",";

		s += "V,";

		if ( e.croppedMaskFile != null )
			s += e.croppedMaskFile + ",";
		else
			s += "null" + ",";

		if ( e.croppedImgFile != null )
			s += e.croppedImgFile + ",";
		else
			s += "null" + ",";

		s += e.cropOffsetX + ",";
		s += e.cropOffsetY;

		return s;
	}

	public static String createHeader()
	{
		String header = "";

		for ( int i = 0; i < requiredColumns.length - 1; ++i )
			header += requiredColumns[ i ] + ",";

		header += requiredColumns[ requiredColumns.length - 1 ];

		if ( optionalColumns.length > 0 )
			header += ",";

		for ( int i = 0; i < optionalColumns.length - 1; ++i )
			header += optionalColumns[ i ] + ",";

		header += optionalColumns[ optionalColumns.length - 1 ];

		return header;
	}

	public static int[] createHeaderLookup( final String[] splitHeader )
	{
		final int[] lookUp = new int[ requiredColumns.length + optionalColumns.length ];

		for ( int c = 0; c < requiredColumns.length; ++c )
		{
			lookUp[ c ] = -1;

			for ( int h = 0; h < splitHeader.length; ++h )
				if ( splitHeader[ h ].toLowerCase().equals( requiredColumns[ c ].toLowerCase() ) )
					lookUp[ c ] = h;

			if ( lookUp[ c ] == -1 )
			{
				IJ.log( "Column '" + requiredColumns[ c ] + "' missing, stopping." );
				return null;
			}
			else
			{
				IJ.log( "Column '" + requiredColumns[ c ] + "' found in column #" + lookUp[ c ] );
			}
		}

		for ( int c = 0; c < optionalColumns.length; ++c )
		{
			lookUp[ c + requiredColumns.length ] = -1;

			for ( int h = 0; h < splitHeader.length; ++h )
				if ( splitHeader[ h ].toLowerCase().equals( optionalColumns[ c ].toLowerCase() ) )
					lookUp[ c + requiredColumns.length ] = h;

			if ( lookUp[ c + requiredColumns.length ] == -1 )
			{
				IJ.log( "Column '" + optionalColumns[ c ] + "' missing, ignoring." );
			}
			else
			{
				IJ.log( "Column '" + optionalColumns[ c ] + "' found in column #" + lookUp[ c + requiredColumns.length ] );
			}
		}

		return lookUp;
	}

	public static boolean saveCSV( final ArrayList< LoadedEmbryo > embryos, final File file )
	{
		final String fn = file.getAbsolutePath();

		// fist make a copy of the file and save it to not loose it
		if ( file.exists() )
		{
			int maxExistingBackup = 0;
			for ( int i = 1; i < numBackups; ++i )
				if ( new File( fn + "~" + i ).exists() )
					maxExistingBackup = i;
				else
					break;
	
			// copy the backups
			try
			{
				for ( int i = maxExistingBackup; i >= 1; --i )
					TextFileAccess.copyFile( new File( fn + "~" + i ), new File( fn + "~" + (i + 1) ) );
	
				TextFileAccess.copyFile( new File( fn ), new File( fn + "~1" ) );
			}
			catch ( final IOException e )
			{
				IJ.log( "Could not save backup of annotation file: " + e );
				e.printStackTrace();
			}
		}

		final PrintWriter out = TextFileAccess.openFileWrite( file );

		if ( out == null )
			return false;

		out.println( createHeader() );

		for ( final LoadedEmbryo e : embryos )
			out.println( toString( e ) );

		out.close();

		return true;
	}

	public static ArrayList< LoadedEmbryo > simulateCSV()
	{
		final ArrayList< LoadedEmbryo > embryoList = new ArrayList< LoadedEmbryo >();

		final LoadedEmbryo embyro0 = new LoadedEmbryo( Status.GOOD );
		final LoadedEmbryo embyro1 = new LoadedEmbryo( Status.BAD );
		final LoadedEmbryo embyro2 = new LoadedEmbryo( Status.INCOMPLETE );
		final LoadedEmbryo embyro3 = new LoadedEmbryo( Status.NOT_ASSIGNED );

		embryoList.add( embyro0 );
		embryoList.add( embyro1 );
		embryoList.add( embyro2 );
		embryoList.add( embyro3 );

		return embryoList;
	}

	public static ArrayList< LoadedEmbryo > loadCSV( final File file )
	{
		final ArrayList< LoadedEmbryo > embryos = new ArrayList< LoadedEmbryo >();
		int lineNo = 1;
		String currentLine = null;

		try
		{
			final BufferedReader in = TextFileAccess.openFileRead( file );

			// header
			final String header = in.readLine();
			final String[] splitHeader = header.split( "," );

			for ( int i = 0; i < splitHeader.length; ++i )
				splitHeader[ i ] = splitHeader[ i ].trim();

			final int[] lookUp = createHeaderLookup( splitHeader );

			if ( lookUp == null )
				return null;

			while ( in.ready() )
			{
				lineNo++;
				currentLine = in.readLine().trim();
				String[] line = currentLine.split( "," );

				// add empty entries at the end if necessary
				if ( line.length < splitHeader.length )
				{
					final String[] lineNew = new String[ splitHeader.length ];

					for ( int i = 0; i < lineNew.length; ++i )
						lineNew[ i ] = "";

					for ( int i = 0; i < line.length; ++i )
						lineNew[ i ] = line[ i ];

					line = lineNew;
				}
				
				final LoadedEmbryo e = fromString( line, lookUp );

				if ( e == null )
					System.out.println( lineNo + " has no DAPI channel." );
				else
					embryos.add( e );
			}
		}
		catch (NumberFormatException e)
		{
			e.printStackTrace();
			IJ.log( "Could not parse file '': Numberformat exception in line " + lineNo );
			IJ.log( currentLine );
			return null;
		}
		catch ( IOException e )
		{
			e.printStackTrace();
			IJ.log( "Could not parse file '': IOException exception, line " + lineNo );
			return null;
		}

		return embryos;
	}
}
