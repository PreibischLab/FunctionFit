package embryo.gui;

public class LoadedEmbryo
{
	// -1 means not assigned
	// 0 means good
	// 1 means incomplete
	// 2 means bad
	int status;

	public LoadedEmbryo( final int status )
	{
		this.status = status;
	}

	public void updateGUI( final EmbryoGUI gui )
	{
		gui.good.setBackground( gui.orginalBackground );
		gui.good.setForeground( gui.originalForeground );

		gui.incomplete.setBackground( gui.orginalBackground );
		gui.incomplete.setForeground( gui.originalForeground );

		gui.bad.setBackground( gui.orginalBackground );
		gui.bad.setForeground( gui.originalForeground );

		if ( status == 0 )
		{
			gui.good.setBackground( EmbryoGUI.goodColor );
			gui.good.setForeground( EmbryoGUI.goodColorFG );
		}
		else if ( status == 1 )
		{
			gui.incomplete.setBackground( EmbryoGUI.incompleteColor  );
			gui.incomplete.setForeground( EmbryoGUI.incompleteColorFG );
		}
		else if ( status == 2 )
		{
			gui.bad.setBackground( EmbryoGUI.badColor );
			gui.bad.setForeground( EmbryoGUI.badColorFG );
		}
	}
}
