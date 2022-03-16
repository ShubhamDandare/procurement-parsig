package com.kpmg.rcm.sourcing.common.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.springframework.stereotype.Component;

@Component
public class XmlToObject {
	public Object convertXmlToObject(StringReader xmlData, Class cls) throws IOException {
		Object rule = null;
		try {
			// printFile(file);
			JAXBContext jaxbContext = JAXBContext.newInstance(cls);

			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			rule = jaxbUnmarshaller.unmarshal(xmlData);

		} catch (JAXBException e) {
			// log.error("error in converting xml to java",e);
		}
		return rule;
	}

	public Object convertXmlToObject(File file, Class cls) throws IOException {
		Object rule = null;
		try {
			// printFile(file);
			JAXBContext jaxbContext = JAXBContext.newInstance(cls);

			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			rule = jaxbUnmarshaller.unmarshal(file);

		} catch (JAXBException e) {
			// log.error("error in converting xml to java",e);
		}
		return rule;
	}

	public void printFile(File file) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		String st;
		while ((st = br.readLine()) != null)
			System.out.println(st);
	}
}
