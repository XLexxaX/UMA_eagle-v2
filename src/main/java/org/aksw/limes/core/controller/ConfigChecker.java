package org.aksw.limes.core.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;

public class ConfigChecker {

	public ConfigChecker() {
		// TODO Auto-generated constructor stub
	}

	static boolean validateAgainstXSD(InputStream xml, InputStream xsd) {
		try {
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = factory.newSchema(new StreamSource(xsd));
			Validator validator = schema.newValidator();
			validator.validate(new StreamSource(xml));
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	static RunConfiguration getRunConfiguration(File xml, File xsd) {

		RunConfiguration rc = null;
		try {
			
			ConfigChecker.validateAgainstXSD(new FileInputStream(xml), new FileInputStream(xsd));
			
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(xml);
			
			rc = new RunConfiguration(0, "", "");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rc;
	}

}
