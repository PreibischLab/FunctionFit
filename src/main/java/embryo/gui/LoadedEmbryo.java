package embryo.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import fit.circular.Ellipse;
import ij.IJ;
import ij.ImageJ;

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
			"status", "ellipse" };

	public enum Status { NOT_ASSIGNED, GOOD, INCOMPLETE, BAD };

	Status status;
	Ellipse ellipse;

	int id, numChannels, dapiChannelIndex, gfpChannelIndex;
	String c0, c1, c2, c3, c4, c0_lambda, c1_lambda, c2_lambda, c3_lambda, c4_lambda;
	String originalFN, manualMaskMaker;
	String c0_type, c1_type, c2_type;
	String comments, stage, filename;
	int signal, integrity;

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
	}

	/*
	 * make an Embryo instance from a line of parameters read from a CSV file
	 */
	public static LoadedEmbryo fromString( final String[] line, final int[] lookup ) throws NumberFormatException
	{
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
			e.ellipse = null; //TODO: import ellipse (can be "null" or "f(x)=")
		else
			e.ellipse = null;

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

		if ( e.ellipse != null )
			s += "f(x)=ellipse equation"; //TODO: export ellipse
		else
			s += "null";

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
				final String[] line = currentLine.split( "," );

				embryos.add( fromString( line, lookUp ) );
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

	public static void main( String[] args )
	{
		String line = "a,b,,,d";
		String[] split = line.split( "," );
		
		for ( final String s : split )
			System.out.println( "'" + s + "'" );

		String a = "";
		String b = "";

		System.out.println( a.equals( b ) );

		new ImageJ();

		final File file = new File( "/Users/spreibi/Documents/BIMSB/Projects/Dosage Compensation/stephan_ellipsoid/stephan_embryo_table3.csv");

		final ArrayList< LoadedEmbryo > embryos = loadCSV( file );

		saveCSV( embryos, file );

		IJ.log( "done" );
	}
}