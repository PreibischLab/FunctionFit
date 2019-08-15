package embryo.gui;

import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

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

	final static String[] columns = new String[]{
			"c0", "c1", "c2", "c3", "c4",
			"c0_lambda", "c1_lambda", "c2_lambda", "c3_lambda", "c4_lambda",
			"c0_type", "c1_type", "c2_type",
			"#c0_smfish", "#c1_smfish", "#c2_smfish",
			"#nuclei", "#nucs_predicted",
			"DAPI channel", "GFP channel", "#channels",
			"original filename", "signal", "filename",
			"status", "ellipse",
			"cropped_image_file", "cropped_mask_file", "crop_offset_x", "crop_offset_y",
			"is_dapi_stack", "is_valid_final", "unique_id", 
			"#c0_smfish_adj", "#c1_smfish_adj", "#c2_smfish_adj",
			"is_male_batch", "is_male", "is_z_cropped", "is_too_bleached",
			"num_z_planes", "tx", "tx_desc" };

	public enum Status { NOT_ASSIGNED, GOOD, INCOMPLETE, BAD, NOT_RUN_YET, NO_ELLIPSE_FOUND };

	// reading everything as String that is not required for processing
	String c0, c1, c2, c3, c4;
	String c0_lambda, c1_lambda, c2_lambda, c3_lambda, c4_lambda;
	String c0_type, c1_type, c2_type;
	String c0_smfish, c1_smfish, c2_smfish;
	String numNuclei, nucsPredicted;
	int dapiChannelIndex, gfpChannelIndex, numChannels;
	String originalFN, signal, filename;
	Status status;
	EllipseOrROI eor;
	String croppedImgFile, croppedMaskFile;
	int cropOffsetX, cropOffsetY;
	String is_dapi_stack, is_valid_final;
	int uniqueId;
	String c0_smfish_adj, c1_smfish_adj, c2_smfish_adj;
	String is_male_batch, is_male, is_z_cropped, is_too_bleached;
	String num_z_planes, tx, tx_desc;

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

		newEmbryo.c0_type = c0_type;
		newEmbryo.c1_type = c1_type;
		newEmbryo.c2_type = c2_type;

		newEmbryo.c0_smfish = c0_smfish;
		newEmbryo.c1_smfish = c1_smfish;
		newEmbryo.c2_smfish = c2_smfish;

		newEmbryo.numNuclei = numNuclei;
		newEmbryo.nucsPredicted = nucsPredicted;

		newEmbryo.dapiChannelIndex = dapiChannelIndex;
		newEmbryo.gfpChannelIndex = gfpChannelIndex;
		newEmbryo.numChannels = numChannels;

		newEmbryo.originalFN = originalFN;
		newEmbryo.signal = signal;
		newEmbryo.filename = filename;

		newEmbryo.status = this.status;

		if ( this.eor != null )
			newEmbryo.eor = this.eor.copy();
		else
			newEmbryo.eor = null;

		newEmbryo.croppedImgFile = croppedImgFile;
		newEmbryo.croppedMaskFile = croppedMaskFile;

		newEmbryo.cropOffsetX = cropOffsetX;
		newEmbryo.cropOffsetY = cropOffsetY;

		newEmbryo.is_dapi_stack = is_dapi_stack;
		newEmbryo.is_valid_final = is_valid_final;

		newEmbryo.uniqueId = uniqueId;

		newEmbryo.c0_smfish_adj = c0_smfish_adj;
		newEmbryo.c1_smfish_adj = c1_smfish_adj;
		newEmbryo.c2_smfish_adj = c2_smfish_adj;

		newEmbryo.is_male_batch = is_male_batch;
		newEmbryo.is_male = is_male;
		newEmbryo.is_z_cropped = is_z_cropped;
		newEmbryo.is_too_bleached = is_too_bleached;

		newEmbryo.num_z_planes = num_z_planes;
		newEmbryo.tx = tx;
		newEmbryo.tx_desc = tx_desc;

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

	public static ArrayList< LoadedEmbryo > readCSV( final File file ) throws IOException
	{
		final ArrayList< LoadedEmbryo > embryos = new ArrayList< LoadedEmbryo >();

		final Reader in = new FileReader( file );
		final Iterable< CSVRecord > records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse( in );

		int lineNo = 0;

		for ( final CSVRecord record : records )
		{
			lineNo++;

			final LoadedEmbryo e = fromCSVRecord( record );
	
			// TODO: check that we now keep embrypos that have no dapi channel
			if ( e.dapiChannelIndex == -1 )
				System.out.println( "WARNING: line " + lineNo + " has no DAPI channel." );

			embryos.add( e );
		}

		System.out.println( "Loaded " + lineNo + " embryos." );

		in.close();

		return embryos;
	}

	/*
	 * make an Embryo instance from a line of parameters read from a CSV file
	 */
	public static LoadedEmbryo fromCSVRecord( final CSVRecord record ) throws NumberFormatException
	{
		final LoadedEmbryo e = new LoadedEmbryo();

		e.c0 = record.get( columns[ 0 ] ); // "c0" );
		e.c1 = record.get( columns[ 1 ] ); // "c1" );
		e.c2 = record.get( columns[ 2 ] ); // "c2" );
		e.c3 = record.get( columns[ 3 ] ); // "c3" );
		e.c4 = record.get( columns[ 4 ] ); // "c4" );

		e.c0_lambda = record.get( columns[ 5 ] ); // "c0_lambda" );
		e.c1_lambda = record.get( columns[ 6 ] ); // "c1_lambda" );
		e.c2_lambda = record.get( columns[ 7 ] ); // "c2_lambda" );
		e.c3_lambda = record.get( columns[ 8 ] ); // "c3_lambda" );
		e.c4_lambda = record.get( columns[ 9 ] ); // "c4_lambda" );

		e.c0_type = record.get( columns[ 10 ] ); // "c0_type" );
		e.c1_type = record.get( columns[ 11 ] ); // "c1_type" );
		e.c2_type = record.get( columns[ 12 ] ); // "c2_type" );

		e.c0_smfish = record.get( columns[ 13 ] ); // "#c0_smfish" );
		e.c1_smfish = record.get( columns[ 14 ] ); // "#c1_smfish" );
		e.c2_smfish = record.get( columns[ 15 ] ); // "#c2_smfish" );

		e.numNuclei = record.get( columns[ 16 ] ); // "#nuclei" );
		e.nucsPredicted = record.get( columns[ 17 ] ); // "#nucs_predicted" );

		e.dapiChannelIndex = (int)Math.round( Double.parseDouble( record.get( columns[ 18 ] ) ) ); // "dapiChannelIndex" ) ) );
		e.gfpChannelIndex = (int)Math.round( Double.parseDouble( record.get( columns[ 19 ] ) ) ); // "gfpChannelIndex" ) ) );
		e.numChannels = (int)Math.round( Double.parseDouble( record.get( columns[ 20 ] ) ) ); // "#channels" ) ) );

		e.originalFN = record.get( columns[ 21 ] ); // "original filename" );
		e.signal = record.get( columns[ 22 ] ); // "signal" );
		e.filename = record.get( columns[ 23 ] ); // "filename" );

		final int status = (int)Math.round( Double.parseDouble( record.get( columns[ 24 ] ) ) ); // "status" ) ) );

		if ( status >= 0 )
			e.status = Status.values()[ status ];
		else if ( status == -1 )
			e.status = Status.NOT_RUN_YET;
		else // status == -2
			e.status = Status.NO_ELLIPSE_FOUND;

		final String eor = record.get( columns[ 25 ] ).trim(); // "ellipse" );

		if ( eor.length() > 0 )
			e.eor = stringToEllipseOrROI( eor );
		else
			e.eor = null;

		e.croppedImgFile = record.get( columns[ 26 ] ); // "cropped_image_file" );
		e.croppedMaskFile = record.get( columns[ 27 ] ); // "cropped_mask_file" );

		e.cropOffsetX = (int)Math.round( Double.parseDouble( record.get( columns[ 28 ] ) ) ); // "crop_offset_x" ) ) );
		e.cropOffsetY = (int)Math.round( Double.parseDouble( record.get( columns[ 29 ] ) ) ); // "crop_offset_y" ) ) );

		e.is_dapi_stack = record.get( columns[ 30 ] ); // "is_dapi_stack" );
		e.is_valid_final = record.get( columns[ 31 ] ); // "is_valid_final" );

		e.uniqueId = (int)Math.round( Double.parseDouble( record.get( columns[ 32 ] ) ) ); // "unique_id" ) ) );

		e.c0_smfish_adj = record.get( columns[ 33 ] ); // "#c0_smfish_adj" );
		e.c1_smfish_adj = record.get( columns[ 34 ] ); // "#c1_smfish_adj" );
		e.c2_smfish_adj = record.get( columns[ 35 ] ); // "#c2_smfish_adj" );

		e.is_male_batch = record.get( columns[ 36 ] ); // "is_male_batch" );
		e.is_male = record.get( columns[ 37 ] ); // "is_male" );
		e.is_z_cropped = record.get( columns[ 38 ] ); // "is_z_cropped" );
		e.is_too_bleached = record.get( columns[ 39 ] ); // "is_too_bleached" );

		e.num_z_planes = record.get( columns[ 40 ] ); // "num_z_planes" );
		e.tx = record.get( columns[ 41 ] ); // "tx" );
		e.tx_desc = record.get( columns[ 42 ] ); // "tx_desc" );

		return e;
	}

	public static String createHeader()
	{
		String header = "";

		for ( int i = 0; i < columns.length - 1; ++i )
			header += columns[ i ] + ",";

		header += columns[ columns.length - 1 ];

		return header;
	}

	/*
	 * make a CSV line for saving
	 */
	public static String toString( final LoadedEmbryo e )
	{
		String s = "";

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

		s += e.c0_type + ",";
		s += e.c1_type + ",";
		s += e.c2_type + ",";

		s += e.c0_smfish + ",";
		s += e.c1_smfish + ",";
		s += e.c2_smfish + ",";

		s += e.numNuclei + ",";
		s += e.nucsPredicted + ",";
		
		s += e.dapiChannelIndex + ",";
		s += e.gfpChannelIndex + ",";
		s += e.numChannels + ",";

		s += e.originalFN + ",";
		s += e.signal + ",";
		s += e.filename + ",";

		if ( e.status.ordinal() <= 4 )
			s += e.status.ordinal() + ",";
		else if ( e.status == Status.NOT_RUN_YET)
			s += "-1" + ",";
		else
			s += "-2" + ",";

		if ( e.eor != null )
			s += ellipseOrROIToString( e.eor ) + ",";
		else
			s += "" + ",";

		s += e.croppedImgFile + ",";
		s += e.croppedMaskFile + ",";

		s += e.cropOffsetX + ",";
		s += e.cropOffsetY + ",";

		s += e.is_dapi_stack + ",";
		s += e.is_valid_final + ",";

		s += e.uniqueId + ",";

		s += e.c0_smfish_adj + ",";
		s += e.c1_smfish_adj + ",";
		s += e.c2_smfish_adj + ",";

		s += e.is_male_batch + ",";
		s += e.is_male + ",";
		s += e.is_z_cropped + ",";
		s += e.is_too_bleached + ",";

		s += e.num_z_planes + ",";
		s += e.tx + ",";
		s += e.tx_desc + ",";

		return s;
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

		try
		{
			final PrintWriter out = TextFileAccess.openFileWriteEx( file );

			out.println( createHeader() );
	
			for ( final LoadedEmbryo e : embryos )
				out.println( toString( e ) );

			out.close();

			return true;
		}
		catch ( Exception e1 )
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
			IJ.error( "Couldn't save file '" + file.getAbsolutePath() + "': " + e1 );

			final File fileNew = new File( "backup.csv" );
			IJ.log( "Trying to save it to the directory where the java code lies '" + fileNew.getAbsolutePath() + "': " + e1 );
			final PrintWriter out1 = TextFileAccess.openFileWrite( file );

			out1.println( createHeader() );
	
			for ( final LoadedEmbryo e : embryos )
				out1.println( toString( e ) );

			out1.close();
			return false;
		}
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
}
