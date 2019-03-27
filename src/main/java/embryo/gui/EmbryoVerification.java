package embryo.gui;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
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
import net.imglib2.util.Pair;

public class EmbryoVerification
{
	final File file;

	final ArrayList< LoadedEmbryo > embryoList; // never sort this list
	HashMap< String, ArrayList< Integer > > fnLookup;

	EmbryoGUI gui;

	LoadedEmbryo currentEmbryo;
	int embryoIndex = -1;
	double lastMag = 1.0;
	Point lastLocation = null;
	boolean overrideMag = false;

	File previewDirectory;

	ImagePlus dapiImp = null;
	Overlay dapiImpOverlay = null;

	public EmbryoVerification( final File file )
	{
		this.file = file;
		this.embryoList = LoadedEmbryo.loadCSV( file );

		setUp( 0 );
	}

	public void destroy()
	{
		this.embryoIndex = -1;
		this.currentEmbryo = null;
		this.gui.frame.dispose();
		this.dapiImp.close();
		this.dapiImp = null;
		this.dapiImpOverlay = null;
		// this.lastMag = 1.0; -- keep whatever was there
		// this.overrideMag = false;
	}

	public void setUp( final int currentEmbryo )
	{
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

		assignCurrentEmbryo( currentEmbryo );

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
		drawEllipsesOrROIs();
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
			double mag = lastMag;
	
			if ( dapiImp != null && dapiImp.getWindow() != null )
			{
				final ImageWindow window = dapiImp.getWindow();
				ImageWindow.setNextLocation( window.getLocationOnScreen() );
				lastLocation = new Point( window.getLocationOnScreen() );

				final ImageCanvas canvas = dapiImp.getCanvas();
				mag = canvas.getMagnification();

				dapiImp.close();
				dapiImp = null;
			}

			dapiImp = new Opener().openImage( new File( previewDirectory, this.currentEmbryo.filename + EmbryoGUI.dapiExt ).getAbsolutePath() );
			dapiImp.show();

			if ( overrideMag )
			{
				mag = lastMag;
				if ( lastLocation != null )
					dapiImp.getWindow().setLocation( lastLocation );
				overrideMag = false;
			}

			lastMag = mag;

			if ( mag != 1.0 )
			{
				dapiImp.getCanvas().setMagnification( mag );
				dapiImp.getCanvas().zoomIn( 0, 0 );
				dapiImp.getCanvas().zoomOut( 0, 0 );
			}
		}

		currentEmbryo.updateGUI( this.gui );
		drawEllipsesOrROIs();
	}

	protected void drawEllipsesOrROIs()
	{
		dapiImp.setOverlay( new Overlay() );

		dapiImpOverlay = new Overlay();

		final ArrayList< Integer > foundEmbryos = fnLookup.get( currentEmbryo.filename );

		for ( final int i : foundEmbryos )
			embryoList.get( i ).drawEllipseOrROI( dapiImpOverlay, i == embryoIndex );

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

		final int x = (int)Math.round( bounds.getMinX() );
		final int y = (int)Math.round( bounds.getMinY() );
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
		final Pair< int[], int[] > roiCoord = EllipseOrROI.getROIPoints( fr );

		final int[] xp = roiCoord.getA();
		final int[] yp = roiCoord.getB();

		for ( int i = 0; i < xp.length; ++i )
		{
			xp[ i ] = Math.max( 0, xp[ i ] - (shiftX ? dapiImp.getWidth() / 2 : 0 ) );
			yp[ i ] = Math.max( 0, yp[ i ] - (shiftY ? dapiImp.getHeight() / 2 : 0 ) );
		}

		final int oldIndex;

		if ( currentEmbryo.eor == null )
		{
			final LoadedEmbryo newEmbryo = currentEmbryo;
			newEmbryo.status = Status.GOOD;
			newEmbryo.eor = new EllipseOrROI( new PolygonRoi( xp, yp, xp.length, Roi.FREEROI ) );
	
			this.embryoList.remove( embryoIndex );
			this.embryoList.add( embryoIndex, newEmbryo );
			oldIndex = embryoIndex;
		}
		else
		{
			final LoadedEmbryo newEmbryo = currentEmbryo.clone();
			newEmbryo.status = Status.GOOD;
			newEmbryo.eor = new EllipseOrROI( new PolygonRoi( xp, yp, xp.length, Roi.FREEROI ) );
	
			this.embryoList.add( embryoIndex + 1, newEmbryo );
			oldIndex = embryoIndex + 1;
		}

		this.overrideMag = true;
		this.destroy();
		this.setUp( oldIndex );
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
