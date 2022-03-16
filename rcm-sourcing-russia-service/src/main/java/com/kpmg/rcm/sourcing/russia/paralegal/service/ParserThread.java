package com.kpmg.rcm.sourcing.russia.paralegal.service;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;

import com.kpmg.rcm.sourcing.russia.paralegal.parse.CommonParser;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Builder
@Data
@Slf4j
public class ParserThread implements Runnable{
	
	private File file;
	private String sourceUrl;
	private String[] indexPageContents;
	private String docType;
	private CommonParser commonParser;
	private String dirPath;
	private Boolean gsApplicable;
	
	public void run() {
		try {
			commonParser.getGranule(file, sourceUrl, indexPageContents, docType, dirPath,null,gsApplicable);
		} catch (Exception e) {
			log.error("Error in forming granule for file :: "+file.getAbsolutePath(), e);
			try {
				ParaLegalService.storeUnparseableDocPaths(file.getAbsolutePath(),dirPath);
			} catch (IOException e1) {
				log.error("Error saving file path for unparseable files :: "+file.getAbsolutePath(), e1);
			}
		}
	}
}
