/*-
 * #%L
 * code for function fitting
 * %%
 * Copyright (C) 2015 - 2023 Developers
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Preibisch Lab nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package mt.listeners;

import java.awt.Label;
import java.awt.Scrollbar;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import mt.InteractiveRANSAC;

public class MaxSlopeListener implements AdjustmentListener
{
	final InteractiveRANSAC parent;
	final Label label;
	final Scrollbar maxSlopeSB;

	public MaxSlopeListener( final InteractiveRANSAC parent, final Scrollbar maxSlopeSB, final Label label )
	{
		this.parent = parent;
		this.label = label;
		this.maxSlopeSB = maxSlopeSB;
		maxSlopeSB.addMouseListener( new StandardMouseListener( parent ) );
	}
	
	@Override
	public void adjustmentValueChanged( final AdjustmentEvent event )
	{
		parent.maxSlope = InteractiveRANSAC.computeValueFromDoubleExpScrollbarPosition(
				event.getValue(),
				InteractiveRANSAC.MAX_SLIDER,
				InteractiveRANSAC.MAX_ABS_SLOPE );

		if ( parent.maxSlope < parent.minSlope )
		{
			parent.maxSlope = parent.minSlope;
			maxSlopeSB.setValue( InteractiveRANSAC.computeScrollbarPositionValueFromDoubleExp( InteractiveRANSAC.MAX_SLIDER, parent.maxSlope, InteractiveRANSAC.MAX_ABS_SLOPE ) );
		}

		label.setText( "Max. Segment Slope (px/tp) = " + parent.maxSlope );
	}
}
