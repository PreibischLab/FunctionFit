package embryo.gui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class EmbryoGUI
{
	public static Color goodColor = Color.GREEN;
	public static Color incompleteColor = Color.YELLOW;
	public static Color badColor = Color.RED;
	public static Color notAssignedColor = Color.WHITE;

	public static Color goodColorFG = goodColor.darker().darker().darker();
	public static Color incompleteColorFG = incompleteColor.darker().darker().darker();
	public static Color badColorFG = badColor.darker().darker().darker();

	public static Color goodColorBG = goodColor.darker().darker();
	public static Color incompleteColorBG = incompleteColor.darker().darker();
	public static Color badColorBG = badColor.darker().darker();
	public static Color notAssignedColorBG = Color.GRAY;

	public static String dapiExt = ".dapimax.jpg";
	public static String gfpExt = ".gfpmax.jpg";
	public static String cy5Ext = ".cy5mid.jpg";

	JButton good, incomplete, bad, forward, back, save;
	JLabel text0, text1, text2;
	JFrame frame;

	String frameTitle = "Verify Embryo Shapes";
	Color orginalBackground, originalForeground;

	public EmbryoGUI()
	{
		displayGUI();
	}

	public void displayGUI()
	{
		frame = new JFrame( frameTitle );
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		/* Instantiation */
		final GridBagLayout layout = new GridBagLayout();
		final GridBagConstraints c = new GridBagConstraints();

		this.good = new JButton( "Good [7]" );
		this.incomplete = new JButton( "Needs correction [8]" );
		this.bad = new JButton( "Bad [9]" );

		this.forward = new JButton( ">> [.]" );
		this.save = new JButton( "Save" );
		this.back = new JButton( "<< [,]" );

		this.text0 = new JLabel( "Threshaergjskdljglsdfghsdlfkhgjlsdkfhlkdjsfhjklold = " );
		this.text1 = new JLabel( "Threshaergjskdljglsdfghsdlfkhgjlsdkfhlkdjsfhjklold = " );
		this.text2 = new JLabel( "Threshaergjskdljglsdfghsdlfkhgjlsdkfhlkdjsfhjklold = " );

		/* Location */
		frame.setLayout( layout );
		
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.insets = new Insets(20,0,20,0 );
		frame.add ( good, c );
		
		++c.gridx;
		frame.add( incomplete, c );

		++c.gridx;
		frame.add( bad, c );

		c.gridx = 0;
		++c.gridy;
		c.gridwidth = 3;
		c.insets = new Insets(0,0,0,0 );
		frame.add ( text0, c );

		++c.gridy;
		frame.add ( text1, c );

		c.insets = new Insets(0,0,20,0 );
		++c.gridy;
		frame.add ( text2, c );

		c.gridx = 0;
		++c.gridy;
		c.gridwidth = 1;
		frame.add ( back, c );

		++c.gridx;
		frame.add( save, c );

		++c.gridx;
		frame.add( forward, c );

		this.good.setOpaque(true);
		this.good.setBorderPainted( true );

		this.incomplete.setOpaque(true);
		this.incomplete.setBorderPainted( true );

		this.bad.setOpaque(true);
		this.bad.setBorderPainted( true );

		this.orginalBackground = good.getBackground();
		this.originalForeground = good.getForeground();

		frame.pack();
		frame.setSize( 550, frame.getHeight() );
		frame.setVisible( true );
	}

}
