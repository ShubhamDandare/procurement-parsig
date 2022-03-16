package com.kpmg.rcm.sourcing.common.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kpmg.rcm.sourcing.common.dto.RecordChange;
import com.kpmg.rcm.sourcing.common.dto.RecordChangeWrapper;
import com.kpmg.rcm.sourcing.common.json.dto.Granule;
import com.kpmg.rcm.sourcing.common.service.AzureOperationService;
import com.kpmg.rcm.sourcing.common.service.CommonJsonFormat;
import com.kpmg.rcm.sourcing.common.service.RecordChangeService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CommonActionUtil {

	@Autowired
	private AzureOperationService azureOperationService;

	@Autowired
	private RecordChangeService recordChangeService;

	@Autowired
	private ObjectMapper objMapper;

	@Autowired
	private CommonJsonFormat commonJsonFormat;

	public void performCommonActions(String version, String xmlFilePath, Integer sourceId, File jsonFile,
			List<Granule> granules) {

		for (Granule granule : granules) {
			commonJsonFormat.handleCommonJsonFormatting(granule);
		}

		jsonFile.getParentFile().mkdirs();

		String outputFileName = jsonFile.getName().replace(CommonConstants.JSON_EXTENSION, "");
		String jsonFilePath = jsonFile.getAbsolutePath();
		try {
			List<Granule> rcGranules = new ArrayList<>();
			List<RecordChange> recordChanges = new ArrayList<>();
			log.info("Start record change for " + jsonFilePath);
			for (Granule grn : granules) {
				String granuleString = objMapper.writeValueAsString(grn);
				RecordChangeWrapper recordChangeWrapper = recordChangeService.generatePojoAndChecksum(granuleString);
				if (recordChangeWrapper != null) {
					rcGranules.add(recordChangeWrapper.getGranule());
					recordChanges.addAll(recordChangeWrapper.getRecordChanges());
				}
			}
			recordChangeService.recordChangeV4(rcGranules, recordChanges, sourceId, outputFileName);
			log.info("Completed record change for " + jsonFilePath);
			FileWriter fw = new FileWriter(jsonFile);
			BufferedWriter writer = new BufferedWriter(fw);
			writer.write(objMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rcGranules));
			writer.close();

			log.info("JSON file [{}] created for xml file [{}]", jsonFile.getAbsolutePath(), xmlFilePath);
		} catch (Exception e) {
			log.error("Exception occurred", e);
		}
		azureOperationService.uploadFilesAndSendMessageIfEnabled(xmlFilePath, jsonFilePath, outputFileName, "", version,
				sourceId);
	}

}
