package com.kpmg.rcm.sourcing.common.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.kpmg.rcm.sourcing.common.azure.AzureOperations;
import com.kpmg.rcm.sourcing.common.config.properties.AzureProperties;
import com.kpmg.rcm.sourcing.common.config.properties.FilePathProperties;
import com.kpmg.rcm.sourcing.common.config.properties.ServiceBusProperties;
import com.kpmg.rcm.sourcing.common.util.CommonConstants;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AzureOperationService {

	@Autowired
	private AzureOperations azureOperations;

	@Autowired
	private AzureProperties azureProperties;

	@Autowired
	private ServiceBusProperties serviceBusProperties;

	@Autowired
	private FilePathProperties filePathProperties;

	@Autowired
	private ProcurementDetailsService procurementDetailsService;

	@Value("${spring.profiles.active:default}")
	private String activeProfile;

	/**
	 * Common method for all source to upload file to azure storage, send message to
	 * service bus and store modified file details in procurement details table
	 *
	 * @param xmlPath
	 *            xml file path for Azure upload
	 * @param jsonPath
	 *            json file path for Azure upload
	 * @param outPutFileName
	 *            output file name
	 * @param modifiedDate
	 *            file modified date
	 * @param sourceId
	 *            source id
	 */
	public void uploadFilesAndSendMessageIfEnabled(String xmlPath, String jsonPath, String outPutFileName,
			String logFileName, String modifiedDate, Integer sourceId) {

		xmlPath = xmlPath.replace("\\", "/").toLowerCase();
		jsonPath = jsonPath.replace("\\", "/").toLowerCase();
		String downloadedFileLocation = filePathProperties.getDownloadedFileLocation();
		downloadedFileLocation = downloadedFileLocation.replace("\\", "/").toLowerCase();

		Boolean isAzureOperationEnabled = azureProperties.getIsAzureFileUploadEnable();

		String xmlBlobName = xmlPath.replace(downloadedFileLocation, "");
		String jsonBlobName = jsonPath.replace(downloadedFileLocation, "");

		if (isAzureOperationEnabled && !activeProfile.equals(CommonConstants.LOCAL_PROFILE)) {
			log.info("Azure uploaded enabled : " + isAzureOperationEnabled);
			// upload xml file
			azureOperations.uploadData(xmlPath, downloadedFileLocation);
			// upload json file
			azureOperations.uploadData(jsonPath, downloadedFileLocation);

			if (logFileName != null && !logFileName.isEmpty()) {
				// upload json file
				azureOperations.uploadData(logFileName, downloadedFileLocation);
			}
			Boolean isServiceBusOperationEnabled = serviceBusProperties.getIsServiceBusEnable();
			log.info("Service Bus Operation enabled : " + isServiceBusOperationEnabled);
			if (isServiceBusOperationEnabled) {
				azureOperations.sendMessageToServiceBus(jsonBlobName);
			}
			// Details only added if uploaded to Azure storage
			procurementDetailsService.addProcurementDetails(xmlBlobName, modifiedDate, jsonBlobName, sourceId,
					outPutFileName);
		} else {
			log.info("Profile enabled [{}]", activeProfile);
			procurementDetailsService.addProcurementDetails(xmlPath, modifiedDate, jsonPath, sourceId, outPutFileName);
		}
	}
}
