package com.kpmg.rcm.sourcing.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RecordChangeUtil {

    //Generate Checksum
    public static String getChecksum(String text) throws NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("SHA-256");

        md.update(text.getBytes(StandardCharsets.UTF_8)); // Change this to "UTF-16" if needed
        byte[] digest = md.digest();
        return DatatypeConverter.printHexBinary(digest).toUpperCase();
    }

}
