package org.aksw.limes.core.controller;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class MultiCaller {

	public void run(String[] args) {

		//String gold_file = "B:/Development/limes3/limes/limes-core/src/main/resources/datasets/headphones/alignment.ttl";

		String[] configFiles = new String[] {
				"B:/Development/limes3/limes/limes-core/src/main/resources/datasets/headphones/headphones-4props.xml",
				"B:/Development/limes3/limes/limes-core/src/main/resources/datasets/headphones/headphones-10props.xml",
				"B:/Development/limes3/limes/limes-core/src/main/resources/datasets/headphones/headphones-allprops.xml",
				"B:/Development/limes3/limes/limes-core/src/main/resources/datasets/tvs/tvs-4props.xml",
				"B:/Development/limes3/limes/limes-core/src/main/resources/datasets/tvs/tvs-10props.xml",
				"B:/Development/limes3/limes/limes-core/src/main/resources/datasets/tvs/tvs-40props.xml",
				"B:/Development/limes3/limes/limes-core/src/main/resources/datasets/tvs/tvs-allprops.xml",

				// "C:/Users/Alexander/Desktop/yelp_zomato_config.xml"//,
				// "B:/Development/limes3/limes/limes-core/target/classes/datasets/phones/phones-4props-50pop-f-original.xml",
				// "B:/Development/limes3/limes/limes-core/target/classes/datasets/phones/phones-10props-50pop-f-original.xml"////,
				/*
				 * 
				 * "B:/Development/limes3/limes/limes-core/target/classes/datasets/phones/phones-4props-50pop-f-alternative.xml",
				 * "B:/Development/limes3/limes/limes-core/target/classes/datasets/phones/phones-10props-50pop-f-alternative.xml",
				 * "B:/Development/limes3/limes/limes-core/target/classes/datasets/phones/phones-allprops-50pop-f-alternative.xml",
				 * 
				 * "B:/Development/limes3/limes/limes-core/target/classes/datasets/phones/phones-4props-50pop-simplef-alternative.xml",
				 * "B:/Development/limes3/limes/limes-core/target/classes/datasets/phones/phones-10props-50pop-simplef-alternative.xml",
				 * "B:/Development/limes3/limes/limes-core/target/classes/datasets/phones/phones-allprops-50pop-simplef-alternative.xml",
				 * "B:/Development/limes3/limes/limes-core/target/classes/datasets/phones/phones-allprops-10pop-simplef-alternative.xml"
				 */ };

		for (int i = 0; i < configFiles.length; i++) {

			try {
				File cachefolder = new File("cache/");
				FileUtils.deleteDirectory(cachefolder);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			Controller ctr = new Controller();
			ctr.run(new String[] { configFiles[i] });

		}

	}

	public static void main(String[] args) {
		new MultiCaller().run(args);
	}

}
