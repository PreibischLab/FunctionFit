package mt.listeners;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import mt.InteractiveRANSAC;

/**
 * Updates when mouse is released
 * 
 * @author spreibi
 *
 */
public class StandardMouseListener implements MouseListener
{
	final InteractiveRANSAC parent;

	public StandardMouseListener( final InteractiveRANSAC parent )
	{
		this.parent = parent;
	}

	@Override
	public void mouseReleased( MouseEvent arg0 )
	{
		/*
		System.out.println( parent.maxError );
		System.out.println( parent.minSlope );
		System.out.println( parent.maxSlope );
		System.out.println( parent.maxDist );
		System.out.println( parent.minInliers );
		*/

		while ( parent.updateCount > 0 )
		{
			try { Thread.sleep( 10 ); } catch ( InterruptedException e ) {}
		}

		parent.updateRANSAC();
	}

	@Override
	public void mousePressed( MouseEvent arg0 ){}

	@Override
	public void mouseExited( MouseEvent arg0 ) {}

	@Override
	public void mouseEntered( MouseEvent arg0 ) {}

	@Override
	public void mouseClicked( MouseEvent arg0 ) {}
}
