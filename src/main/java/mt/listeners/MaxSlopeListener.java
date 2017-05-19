package mt.listeners;

import java.awt.Label;
import java.awt.Scrollbar;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

public class MaxSlopeListener implements AdjustmentListener
{
	final InteractiveRANSAC_ parent;
	final Label label;
	final Scrollbar maxSlopeSB;

	public MaxSlopeListener( final InteractiveRANSAC_ parent, final Scrollbar maxSlopeSB, final Label label )
	{
		this.parent = parent;
		this.label = label;
		this.maxSlopeSB = maxSlopeSB;
		maxSlopeSB.addMouseListener( new StandardMouseListener( parent ) );
	}
	
	@Override
	public void adjustmentValueChanged( final AdjustmentEvent event )
	{
		parent.maxSlope = InteractiveRANSAC_.computeValueFromDoubleExpScrollbarPosition(
				event.getValue(),
				InteractiveRANSAC_.MAX_SLIDER,
				InteractiveRANSAC_.MAX_ABS_SLOPE );

		if ( parent.maxSlope < parent.minSlope )
		{
			parent.maxSlope = parent.minSlope;
			maxSlopeSB.setValue( InteractiveRANSAC_.computeScrollbarPositionValueFromDoubleExp( InteractiveRANSAC_.MAX_SLIDER, parent.maxSlope, InteractiveRANSAC_.MAX_ABS_SLOPE ) );
		}

		label.setText( "Max. Segment Slope (px/tp) = " + parent.maxSlope );
	}
}
