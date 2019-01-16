package embryo.gui;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import embryo.gui.LoadedEmbryo.Status;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Overlay;
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
						gui.good.doClick();
					else if ( e.getKeyChar() == '8' )
						gui.incomplete.doClick();
					else if ( e.getKeyChar() == '9' )
						gui.bad.doClick();
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
		new EmbryoVerification( new File( "/Users/spreibi/Documents/BIMSB/Projects/Dosage Compensation/stephan_ellipsoid/stephan_embryo_table_annotated.csv") );

	}
}
