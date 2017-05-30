package mt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

public class RansacFileChooser_ extends JPanel {
	
	
	
	/**
	 * TODO: This has to be the plugin, not the InteractiveRANSAC
	 */
	private static final long serialVersionUID = 1L;
	JPanel panelCont = new JPanel();
	JPanel panelIntro = new JPanel();
	JFileChooser chooserA;
	boolean wasDone = false;
	boolean isFinished = false;
	JButton Track;
	String choosertitleA;
	public RansacFileChooser_(){
		
     final JFrame frame = new JFrame("Welcome to the Ransac Part of MTV tracker");

		Track = new JButton("Choose file");
		
		panelCont.add(panelIntro, "1");
		/* Instantiation */
		final GridBagLayout layout = new GridBagLayout();
		final GridBagConstraints c = new GridBagConstraints();

		panelIntro.setLayout(layout);
		
		final Label LoadtrackText = new Label("Input the .txt file generated by the MTV tracker");
		
		LoadtrackText.setBackground(new Color(1, 0, 1));
		LoadtrackText.setForeground(new Color(255, 255, 255));
		
		
		/* Location */

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 1.5;
		

		++c.gridy;
		c.insets = new Insets(10, 10, 10, 0);
		panelIntro.add(LoadtrackText, c);
		

		++c.gridy;
		c.insets = new Insets(10, 10, 10, 0);
		panelIntro.add(Track, c);

		panelIntro.setVisible(true);
		Track.addActionListener(new OpenTrackListener(frame));
		frame.addWindowListener(new FrameListener(frame));
		frame.add(panelCont, BorderLayout.CENTER);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		
	}
	protected class FrameListener extends WindowAdapter {
		final Frame parent;

		public FrameListener(Frame parent) {
			super();
			this.parent = parent;
		}

		@Override
		public void windowClosing(WindowEvent e) {
			close(parent);
		}
	}
	protected final void close(final Frame parent) {
		if (parent != null)
			parent.dispose();

		isFinished = true;
	}
	
	protected class OpenTrackListener implements ActionListener {

		final Frame parent;

		public OpenTrackListener(Frame parent) {

			this.parent = parent;

		}

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			int result;

			chooserA = new JFileChooser();
			chooserA.setCurrentDirectory(new java.io.File("."));
			chooserA.setDialogTitle(choosertitleA);
			chooserA.setFileSelectionMode(JFileChooser.FILES_ONLY);
			//
			// disable the "All files" option.
			//
			chooserA.setAcceptAllFileFilterUsed(false);
			FileNameExtensionFilter filter = new FileNameExtensionFilter("Text Files", "txt");

			chooserA.setFileFilter(filter);
			//
			if (chooserA.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
				System.out.println("getCurrentDirectory(): " + chooserA.getCurrentDirectory());
				System.out.println("getSelectedFile() : " + chooserA.getSelectedFile());
			} else {
				System.out.println("No Selection ");
			}

			
			Done(parent);
		}

	}
	
	
	protected void Done(final Frame parent){
		
		wasDone = true;


		new InteractiveRANSAC( Tracking.loadMT( new File( chooserA.getSelectedFile().getPath() ) ) );
	    close(parent);
		
		
	}
	protected class DoneButtonListener implements ActionListener {
		final Frame parent;
		final boolean Done;

		public DoneButtonListener(Frame parent, final boolean Done) {
			this.parent = parent;
			this.Done = Done;
		}

		@Override
		public void actionPerformed(final ActionEvent arg0) {
			wasDone = Done;


			new InteractiveRANSAC( Tracking.loadMT( new File( chooserA.getSelectedFile().getPath() ) ) );
		    close(parent);
		}
	}
	public Dimension getPreferredSize() {
		return new Dimension(800, 300);
	}

}
