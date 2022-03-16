package com.kpmg.rcm.sourcing.common.util;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SplitXmlUtil {

    public static final int BUFFER_SIZE = 8192;

    public static List<String> splitXML(String filePath, String splitTagName) {
//        String filePath = "C:\\Users\\goswami_h\\Pictures\\ecfr-split\\ecfr-title12.xml";
//        splitTagName = "ECFRBRWS";

        List<String> filePaths = new ArrayList<>();

        java.io.File inputFile = new java.io.File(filePath);
        javax.xml.namespace.QName elementToSplitOn = javax.xml.namespace.QName.valueOf(splitTagName);
        java.io.InputStream inputStream = null;

        try {
            log.info("Splitting '" + inputFile.toURI() + "' on element '" + elementToSplitOn + "'");

            inputStream = new java.io.BufferedInputStream(new java.io.FileInputStream(inputFile), BUFFER_SIZE);

            javax.xml.stream.XMLInputFactory inputFactory = javax.xml.stream.XMLInputFactory.newInstance();
            javax.xml.stream.XMLOutputFactory outputFactory = javax.xml.stream.XMLOutputFactory.newInstance();
            javax.xml.stream.XMLEventReader reader = inputFactory.createXMLEventReader(inputStream);
            javax.xml.stream.XMLEventWriter writer = null;

            int i = 1;
            while (reader.hasNext()) {
                javax.xml.stream.events.XMLEvent event = reader.nextEvent();

                switch(event.getEventType()) {
                    case javax.xml.stream.XMLStreamConstants.START_ELEMENT:
                        javax.xml.stream.events.StartElement startElement = (javax.xml.stream.events.StartElement)event;
                        if (startElement.getName().equals(elementToSplitOn)) {
                            String fileNameWithoutExtn = inputFile.getName()
                                    .substring(0, inputFile.getName().lastIndexOf('.'));
                            java.io.File outputFile = new java.io.File(inputFile.getParent(),
                                    String.format("%s_vol_%02d.xml", fileNameWithoutExtn, i++));
                            filePaths.add(outputFile.getAbsolutePath());

                            log.info(String.format("Element '%s' found, splitting to file: '%s'",
                                    elementToSplitOn, outputFile.toURI()));
                            writer = outputFactory.createXMLEventWriter(new java.io.BufferedOutputStream(
                                    new java.io.FileOutputStream(outputFile), BUFFER_SIZE));
                        }
                        if (writer != null)
                            writer.add(event);

                        break;

                    case javax.xml.stream.XMLStreamConstants.END_ELEMENT:
                        javax.xml.stream.events.EndElement endElement = (javax.xml.stream.events.EndElement)event;
                        if (endElement.getName().equals(elementToSplitOn)) {
                            writer.add(event);
                            writer.close();
                            writer = null;
                        } else {
                            if (writer != null) {
                                try {
                                    writer.add(event);
                                } catch (Exception e) {
//                                    e.printStackTrace();
                                }
                            }
                        }
                        break;

                    default:
                        if (writer != null)
                            writer.add(event);
                        break;
                }
            }
            reader.close();
            if (writer != null)
                writer.close();
        } catch(Throwable ex) {
            log.error("Error while splitting file " + filePath, ex);
        } finally {
            StreamUtil.closeStreams(inputStream);
        }
        return filePaths;
    }
}
