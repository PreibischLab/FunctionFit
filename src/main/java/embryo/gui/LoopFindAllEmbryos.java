package embryo.gui;

import java.io.IOException;

import ij.ImageJ;
import embryo.gui.FindAllEmbryos.EllipseFindingProperties;

public class LoopFindAllEmbryos {

    public static void main(String args[]) 
    { 
    	//System.out.println("We're starting");
        // Defined like ranges in python - {start,stop,step}
        final double[] maxAreaRange = {100000, 135000, 2000};
        final double[] maxErrorRange = {10, 20, 1};
        
        //final double[] maxAreaRange = {100000, 100000, 1000};
        //final double[] maxErrorRange = {10, 10, 1};  
        
        new ImageJ();
        
        // Exit when x becomes greater than 4 
        for (int i=(int) maxAreaRange[0]; i<=maxAreaRange[1]; i+=maxAreaRange[2]) 
        {
        	// System.out.println(i + j);
        	// change all parameters
        	EllipseFindingProperties.maxArea = i;
        	EllipseFindingProperties.maxError = maxErrorRange[0];

        	try {
				FindAllEmbryos.main(new String[] {});
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


        	System.out.println("Value of maxArea:" + i);
        	System.out.println("Value of maxError:" + EllipseFindingProperties.maxError);
        }
	    
	    for (int j=(int) maxErrorRange[0]; j<=maxErrorRange[1]; j+=maxErrorRange[2]) 
        {
        	// System.out.println(i + j);
        	// change all parameters
        	EllipseFindingProperties.maxArea = maxAreaRange[0];
        	EllipseFindingProperties.maxError = j;

        	try {
				FindAllEmbryos.main(new String[] {});
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


        	System.out.println("Value of maxArea:" + EllipseFindingProperties.maxArea);
        	System.out.println("Value of maxError:" + j);
	    	
        }  
    } 
}
