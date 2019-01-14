package embryo.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

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

		final LoadedEmbryo embyro0 = new LoadedEmbryo( 0 );
		final LoadedEmbryo embyro1 = new LoadedEmbryo( 2 );
		final LoadedEmbryo embyro2 = new LoadedEmbryo( 1 );
		final LoadedEmbryo embyro3 = new LoadedEmbryo( -1 );

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
