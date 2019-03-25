package embryo.gui;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import embryo.gui.LoadedEmbryo.Status;
import embryo.gui.TextFileAccess.CSV_TYPE;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.io.Opener;

public class EmbryoVerification
{
	final File file;

	final ArrayList< LoadedEmbryo > embryoList; // never sort this list
	final HashMap< String, ArrayList< Integer > > fnLookup;

	final EmbryoGUI gui;

	LoadedEmbryo currentEmbryo;
	int embryoIndex = -1;

	final File previewDirectory;
	
	ImagePlus dapiImp = null;
	Overlay dapiImpOverlay = null;

	public EmbryoVerification( final File file )
	{
		this.file = file;
		this.embryoList = LoadedEmbryo.loadCSV( file );

		this.fnLookup = new HashMap< String, ArrayList< Integer > >();

		for ( int i = 0; i < embryoList.size(); ++i )
		{
			final LoadedEmbryo e = embryoList.get( i );

			if ( this.fnLookup.containsKey( e.filename ) )
			{
				final ArrayList< Integer > indices = fnLookup.get( e.filename );
				indices.add( i );
			}
			else
			{
				final ArrayList< Integer > indices = new ArrayList< Integer >();
				indices.add( i );
				fnLookup.put( e.filename, indices );
			}
		}

		this.previewDirectory = new File( file.getParentFile() + "/preview" );

		this.gui = new EmbryoGUI();

		assignCurrentEmbryo( 0 );

		gui.frame.addWindowListener( new WindowListener()
		{
			public void windowClosing(WindowEvent arg0){ save(); }
			public void windowOpened(WindowEvent arg0) {}
			public void windowClosed(WindowEvent arg0) {}
			public void windowIconified(WindowEvent arg0) {}
			public void windowDeiconified(WindowEvent arg0) {}
			public void windowActivated(WindowEvent arg0) {}
			public void windowDeactivated(WindowEvent arg0) {}
		} );

		gui.forward.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				assignCurrentEmbryo( embryoIndex + 1 );
			}
		} );

		gui.save.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				save();
			}
		} );

		gui.addnew.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				addnew();
			}
		} );

		gui.back.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				assignCurrentEmbryo( embryoIndex - 1 );
			}
		} );

		gui.good.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				updateStatus( Status.GOOD );
			}
		} );

		gui.incomplete.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				updateStatus( Status.INCOMPLETE );
			}
		} );

		gui.bad.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				updateStatus( Status.BAD );
			}
		} );

		final KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		manager.addKeyEventDispatcher( new KeyEventDispatcher()
		{
			
			@Override
			public boolean dispatchKeyEvent( KeyEvent e )
			{
				if (e.getID() == KeyEvent.KEY_PRESSED)
				{
					if ( e.getKeyChar() == '7' )
					{
						if ( gui.good.isEnabled() )
							gui.good.doClick();
					}
					else if ( e.getKeyChar() == '8' )
					{
						if ( gui.incomplete.isEnabled() )
							gui.incomplete.doClick();
					}
					else if ( e.getKeyChar() == '9' )
					{
						if ( gui.bad.isEnabled() )
							gui.bad.doClick();
					}
					else if ( e.getKeyChar() == '.' )
						gui.forward.doClick();
					else if ( e.getKeyChar() == ',' )
						gui.back.doClick();
				}
				return false;
			}
		});
	}

	protected void updateStatus( final Status status )
	{
		currentEmbryo.updateStatus( status, gui );
		drawEllipses();
	}

	protected void assignCurrentEmbryo( final int newIndex )
	{
		final int lastEmbryoIndex = this.embryoIndex;

		this.embryoIndex = newIndex;
		this.currentEmbryo = this.embryoList.get( this.embryoIndex );

		if ( this.embryoIndex == embryoList.size() - 1 )
			gui.forward.setEnabled( false );
		else
			gui.forward.setEnabled( true );

		if ( this.embryoIndex == 0 )
			gui.back.setEnabled( false );
		else
			gui.back.setEnabled( true );

		gui.frame.setTitle( gui.frameTitle + " (" + (embryoIndex + 1) + "/" + embryoList.size() + ")" );
	
		if ( lastEmbryoIndex < 0 || !currentEmbryo.filename.equals( embryoList.get( lastEmbryoIndex ).filename ) )
		{
			double mag = 1.0;
	
			if ( dapiImp != null && dapiImp.getWindow() != null )
			{
				final ImageWindow window = dapiImp.getWindow();
				ImageWindow.setNextLocation( window.getLocationOnScreen() );

				final ImageCanvas canvas = dapiImp.getCanvas();
				mag = canvas.getMagnification();

				dapiImp.close();
				dapiImp = null;
			}

			dapiImp = new Opener().openImage( new File( previewDirectory, this.currentEmbryo.filename + EmbryoGUI.dapiExt ).getAbsolutePath() );
			dapiImp.show();

			if ( mag != 1.0 )
			{
				dapiImp.getCanvas().setMagnification( mag );
				dapiImp.getCanvas().zoomIn( 0, 0 );
				dapiImp.getCanvas().zoomOut( 0, 0 );
			}
		}

		currentEmbryo.updateGUI( this.gui );
		drawEllipses();
	}

	protected void drawEllipses()
	{
		dapiImp.setOverlay( new Overlay() );

		dapiImpOverlay = new Overlay();

		final ArrayList< Integer > foundEmbryos = fnLookup.get( currentEmbryo.filename );

		for ( final int i : foundEmbryos )
			embryoList.get( i ).drawEllipse( dapiImpOverlay, i == embryoIndex );

		dapiImp.setOverlay( dapiImpOverlay );
		dapiImp.updateAndDraw();
	}

	public void addnew()
	{
		final Roi roi = dapiImp.getRoi();

		if ( roi == null || !PolygonRoi.class.isInstance( roi ) )
		{
			IJ.log( "You need to draw a polygon or freehand roi." );
			return;
		}

		// translate the ROI to the first quarter
		final PolygonRoi fr = (PolygonRoi)roi;

		final Rectangle bounds = fr.getBounds();

		int x = (int)Math.round( bounds.getMinX() );
		int y = (int)Math.round( bounds.getMinY() );
		final int sx = (int)Math.round( bounds.getWidth() );
		final int sy = (int)Math.round( bounds.getHeight() );

		final boolean shiftX, shiftY;

		if ( x + sx > dapiImp.getWidth() / 2 )
			shiftX = true;
		else
			shiftX = false;

		if ( y + sy > dapiImp.getHeight() / 2 )
			shiftY = true;
		else
			shiftY = false;

		if ( shiftX && (x - dapiImp.getWidth() / 2) < 0 || shiftY && (y - dapiImp.getHeight() / 2) < 0 )
			IJ.log( "WARNING: You drew an invalid ROI, which is out of bounds after shifting it to the DAPI channel image (top left one)." );

		// bounding box of roi needs to be added to those coordinates
		final int[] xpTmp = fr.getXCoordinates();
		final int[] ypTmp = fr.getYCoordinates();

		final int[] xp = new int[ fr.getNCoordinates() ];
		final int[] yp = new int[ fr.getNCoordinates() ];

		for ( int i = 0; i < xp.length; ++i )
		{
			xp[ i ] = Math.max( 0, xpTmp[ i ] + x - (shiftX ? dapiImp.getWidth() / 2 : 0 ) );
			yp[ i ] = Math.max( 0, ypTmp[ i ] + y - (shiftY ? dapiImp.getHeight() / 2 : 0 ) );
		}

		final PolygonRoi newRoi = new PolygonRoi( xp, yp, xp.length, Roi.FREEROI );
		//dapiImp.setRoi( new PolygonRoi( xp, yp, xp.length, Roi.FREEROI ) );

		roi.setStrokeColor( EmbryoGUI.incompleteColor );
		dapiImpOverlay.add( newRoi );
		dapiImp.setOverlay( dapiImpOverlay );
		dapiImp.updateAndDraw();
	}

	public void save()
	{
		final GenericDialog gd = new GenericDialog( "Saving ..." );
		gd.addCheckbox( "Save", true );
		gd.addCheckbox( "Mark unassigned as bad up to current embryo", false );
		gd.addCheckbox( "Mark ALL unassigned as bad", false );

		gd.showDialog();

		if ( gd.wasCanceled() )
			return;

		final boolean save = gd.getNextBoolean();
		final boolean replaceBefore = gd.getNextBoolean();
		final boolean replaceAll = gd.getNextBoolean();

		if ( replaceBefore )
			for ( int i = 0; i <= embryoIndex; ++i )
				if ( embryoList.get( i ).status == Status.NOT_ASSIGNED )
					embryoList.get( i ).status = Status.BAD;

		if ( replaceAll )
			for ( final LoadedEmbryo e : embryoList )
				if ( e.status == Status.NOT_ASSIGNED )
					e.status = Status.BAD;

		if ( save )
		{
			LoadedEmbryo.saveCSV( embryoList, file );
			IJ.log( "Saved " + file.getAbsolutePath() );
		}
	}

	public static void main( String[] args )
	{
		new ImageJ();

		/*
		ImagePlus m = IJ.createImage( "dgd", 512, 512, 1, 32 );
		m.show();

		Overlay l = new Overlay();

		Roi line = new Line( 10, 10, 500, 10 );
		line.setStrokeColor( Color.RED );
		l.add( line );

		line = new Line( 10, 200, 500, 200 );
		line.setStrokeColor( Color.GREEN );
		l.add( line );

		line = new Line( 10, 400, 500, 400 );
		line.setStrokeColor( Color.BLUE );
		l.add( line );

		m.setOverlay( l );
		m.updateAndDraw();
		 */

		// This one overwrites the original annotation file, making backups
		final File csvFile = TextFileAccess.loadPath( CSV_TYPE.ANNOTATED );

		if ( csvFile == null )
		{
			System.out.println( "CSV file not defined in path.txt" );
			System.out.println( "csvFileOut (ANNOTATED)= " + csvFile );
			System.exit( 0 );
		}

		new EmbryoVerification( csvFile );
	}
}
