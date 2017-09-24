package utility;

/**
 * @author abhishek
 * Class to redirect the output of the thread to a specific path.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ThreadInputStream extends Thread {
    InputStream is;

    // reads everything from is until empty. 
    public ThreadInputStream(InputStream is) {
        this.is = is;
    }

    public void run(boolean sys, String filename) {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            FileOneByOneLineWriter file=null;
            if(!sys)
            	file = new FileOneByOneLineWriter(filename);
            String line=null;
            while ((line = br.readLine()) != null){
            	if(sys)
            		System.out.println(line);
            	else
            		file.writeLine(line);	
            }
            isr.close();
            br.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();  
        }
    }
    public void run(String filename){
    	try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line=null;
            FileOneByOneLineWriter file= new FileOneByOneLineWriter(filename);
            while ( (line = br.readLine()) != null){
            	file.writeLine(line);
            	//System.out.println(line);
            }
            isr.close();
            br.close();
            file.close();
    	}
    	catch (IOException ioe) {
           ioe.printStackTrace();  
        }
    }
}
