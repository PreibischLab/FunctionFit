package embryo.gui;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;

import embryo.gui.LoadedEmbryo.Status;
import ij.ImageJ;

public class EmbryoVerification
{
	final File file;

	final ArrayList< LoadedEmbryo > embryoList;
	final EmbryoGUI gui;

	LoadedEmbryo currentEmbryo;
	int embryoIndex;

	public EmbryoVerification( final File file )
	{
		this.file = file;
		this.embryoList = loadCSV( file );

		this.gui = new EmbryoGUI();

		assignCurrentEmbryo( 0 );

		gui.forward.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				assignCurrentEmbryo( ++embryoIndex );
			}
		} );

		gui.back.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				assignCurrentEmbryo( --embryoIndex );
			}
		} );

		gui.good.addActionListener(  new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				currentEmbryo.updateStatus( Status.GOOD, gui );
			}
		} );

		gui.incomplete.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				currentEmbryo.updateStatus( Status.INCOMPLETE, gui );
			}
		} );

		gui.bad.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				currentEmbryo.updateStatus( Status.BAD, gui );
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
					if ( e.getKeyChar() == '1' )
						gui.good.doClick();
					else if ( e.getKeyChar() == '2' )
						gui.incomplete.doClick();
					else if ( e.getKeyChar() == '3' )
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

	protected void assignCurrentEmbryo( final int newIndex )
	{
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
		updateGUI( currentEmbryo );
	}


	public void updateGUI( final LoadedEmbryo embryo )
	{
		embryo.updateGUI( this.gui );
	}

	protected ArrayList< LoadedEmbryo > loadCSV( final File file )
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
	
	public static void main( String[] args )
	{
		new ImageJ();
		new EmbryoVerification( null );
	}
}
