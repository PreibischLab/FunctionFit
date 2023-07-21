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

import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Label;
import java.awt.Scrollbar;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import mt.InteractiveRANSAC;

public class CatastrophyCheckBoxListener implements ItemListener
{
	final InteractiveRANSAC parent;
	final Checkbox checkbox;
	final Label label;
	final Scrollbar scrollbar;

	public CatastrophyCheckBoxListener(
			final InteractiveRANSAC parent,
			final Checkbox checkbox,
			final Label label,
			final Scrollbar scrollbar )
	{
		this.parent = parent;
		this.checkbox = checkbox;
		this.label = label;
		this.scrollbar = scrollbar;

		enableDisable( checkbox.getState() );
	}

	@Override
	public void itemStateChanged( final ItemEvent e )
	{
		boolean state = parent.detectCatastrophe;
		enableDisable( checkbox.getState() );

		if ( checkbox.getState() != state )
		{
			while ( parent.updateCount > 0 )
			{
				try { Thread.sleep( 10 ); } catch ( InterruptedException ex ) {}
			}

			parent.updateRANSAC();
		}
	}

	protected void enableDisable( final boolean state )
	{
		label.setEnabled( state );
		scrollbar.setEnabled( state );

		if ( state )
			label.setForeground( Color.black );
		else
			label.setForeground( Color.GRAY );
		parent.detectCatastrophe = state;
	}
}
