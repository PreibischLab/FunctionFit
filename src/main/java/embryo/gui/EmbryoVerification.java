package embryo.gui;

import java.awt.Color;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import embryo.gui.LoadedEmbryo.Status;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Line;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.io.Opener;
import net.imglib2.img.imageplus.ImagePlusImgs;

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

		gui.forward.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				assignCurrentEmbryo( embryoIndex + 1 );
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
	
			if ( dapiImp != null && dapiImp.getWindow() != null && ( dapiImp.getWindow().running || dapiImp.getWindow().running2 ) )
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
