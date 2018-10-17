package org.aksw.limes.core.controller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.aksw.limes.core.io.config.Configuration;
import org.apache.commons.cli.CommandLine;

public class MultiCaller {

	
	public void run(String[] args) {

		try {
			File f = new File("C:/Users/Alexander/Desktop/data_phones/results.txt");
			f.createNewFile();
			BufferedWriter br = new BufferedWriter(new FileWriter(f));
			br.write("");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		double aT= exec(args, (int) 170, 0.1, 5); 
		//exec(args, 75, 0.1, 1);
	}
	
	public double exec(String[] args, int iterations, double startVal, int testRounds) {
		
		double aT = startVal;

			TrainTestResults[] ttr = new TrainTestResults[5];
			for (int j = 0; j < testRounds; j++) {
				 
				TrainTestResults[] ttr2 = new TrainTestResults[50];
				for (int i = 0; i < 1; i++) {

					try {
						String outString = "\nTest #" + (i + 1) + " for (i	terations=" + iterations + ", accept-threshold=" + aT + "):\n";
						Files.write(Paths.get("C:/Users/Alexander/Desktop/data_phones/results.txt"),
								outString.getBytes(), StandardOpenOption.APPEND);
					} catch (IOException e) {
						e.printStackTrace();
						// exception handling left as an exercise for the reader
					}

					Controller ctr = new Controller();
					ttr2[i] = ctr.run(args, iterations, aT);
					
				}
				/*ttr[j] = ttr2[0];
				for (int i = 1; i < 1; i++) {
					ttr[j].testResult.put("f", new Double(ttr[j].testResult.get("f")+ttr2[i].testResult.get("f")));
				}
				ttr[j].testResult.put("f", new Double(ttr[j].testResult.get("f")/5.0));*/
				
				//aT /= 10;
			}
			/*int maxRun = 0;
			double maxF = ttr[0].testResult.get("f");
			for (int i = 1; i < testRounds; i++) {
				if (maxF < ttr[i].testResult.get("f")) {
					maxF = ttr[i].testResult.get("f");
					maxRun = i;
				}
			}*/
			/*String endResult = "\nFound best mean result for run "+maxRun+" with acceptance-threshold="+(startVal*10.0/(10.0*(maxRun+1.0)));
			try {
				Files.write(Paths.get("C:/Users/Alexander/Desktop/data_phones/results.txt"),
						endResult.getBytes(), StandardOpenOption.APPEND);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}*/
			
			
			return startVal;//(startVal*10.0/(10.0*(maxRun+1.0)));
			
			
	}
	
	public static void main(String[] args) {
		new MultiCaller().run(args);
	}

}
