package org.aksw.limes.core.controller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.aksw.limes.core.io.config.Configuration;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileUtils;

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

		exec(args, 55);
	}

	public void exec(String[] args, int iterations) {

		String[] configFiles = new String[] {
				
				  
				  "B:/Development/limes3/limes/limes-core/target/classes/datasets/phones/phones-allprops-50pop-f-original.xml"//,
				//"B:/Development/limes3/limes/limes-core/target/classes/datasets/phones/phones-4props-50pop-f-original.xml",
				 //"B:/Development/limes3/limes/limes-core/target/classes/datasets/phones/phones-10props-50pop-f-original.xml"////,
				 /*

				"B:/Development/limes3/limes/limes-core/target/classes/datasets/phones/phones-4props-50pop-f-alternative.xml",
				"B:/Development/limes3/limes/limes-core/target/classes/datasets/phones/phones-10props-50pop-f-alternative.xml",
				"B:/Development/limes3/limes/limes-core/target/classes/datasets/phones/phones-allprops-50pop-f-alternative.xml",

				"B:/Development/limes3/limes/limes-core/target/classes/datasets/phones/phones-4props-50pop-simplef-alternative.xml",
				"B:/Development/limes3/limes/limes-core/target/classes/datasets/phones/phones-10props-50pop-simplef-alternative.xml",
				"B:/Development/limes3/limes/limes-core/target/classes/datasets/phones/phones-allprops-50pop-simplef-alternative.xml",
				"B:/Development/limes3/limes/limes-core/target/classes/datasets/phones/phones-allprops-10pop-simplef-alternative.xml" */};

		for (int i = 0; i < configFiles.length; i++) {

			for (int j = 0; j < 3; j++) {

				try {
					String outString = "\n\n\n --- ( " + configFiles[i].split("/")[configFiles[i].split("/").length - 1]
							+ " | " + (j+1) + " ) ---\n";
					Files.write(Paths.get("C:/Users/Alexander/Desktop/data_phones/results.txt"), outString.getBytes(),
							StandardOpenOption.APPEND);
				} catch (IOException e) {
					e.printStackTrace();
					// exception handling left as an exercise for the reader
				}

				Controller ctr = new Controller();
				ctr.run(new String[] { configFiles[i] }, iterations, 0.1);

				try {
					File cachefolder = new File("cache/");
					FileUtils.deleteDirectory(cachefolder);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
		
	}

	public static void main(String[] args) {
		new MultiCaller().run(args);
	}

}
