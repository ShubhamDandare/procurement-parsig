package com.kpmg.rcm.sourcing.common.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kpmg.rcm.sourcing.common.azure.AzureOperations;
import com.kpmg.rcm.sourcing.common.config.properties.AzureProperties;
import com.kpmg.rcm.sourcing.common.config.properties.FilePathProperties;

import lombok.extern.slf4j.Slf4j;

@Component("azureFileUploadCfr")
@Slf4j
public class AzureFileUpload {

	@Autowired
	private FilePathProperties filePathProperties;

	@Autowired
	private AzureOperations azureOperations;

	@Autowired
	private AzureProperties azureProperties;

	public void uploadFile(String file) {
		Boolean isAzureOperationEnabled = azureProperties.getIsAzureFileUploadEnable();
		log.info("Azure prop : " + isAzureOperationEnabled);
		if (isAzureOperationEnabled) {
			azureOperations.uploadData(file, filePathProperties.getDownloadedFileLocation());
		}
	}
}
