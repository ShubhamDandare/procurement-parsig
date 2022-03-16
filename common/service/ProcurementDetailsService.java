package com.kpmg.rcm.sourcing.common.service;

import static com.kpmg.rcm.sourcing.common.util.CommonConstants.JSON_EXTENSION;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kpmg.rcm.sourcing.common.entity.ProcurementDetails;
import com.kpmg.rcm.sourcing.common.repository.ProcurementDetailsRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProcurementDetailsService {

	@Autowired
	private ProcurementDetailsRepository procurementDetailsRepository;

	public void addProcurementDetails(String downloadedFileLocation, String modifiedDate, String jsonFileLocation,
			Integer sourceId, String jsonFileName) {

		// modifiedDate = modifiedDate.replace(modifiedDate.substring(3, 4),
		// modifiedDate.substring(3, 4).toUpperCase());

		// INFO this replacement of date in the json file name is done for ECFR source,
		// other sources should follow same
		if (modifiedDate != null) {
			jsonFileName = jsonFileName.replaceAll("_" + modifiedDate, "");
		}

		// 06-nov-2021-01-47

		// DateTimeFormatter formatter =
		// DateTimeFormatter.ofPattern("dd-MMM-yyyy-HH-mm");
		// LocalDateTime dateTime = LocalDateTime.parse(modifiedDate, formatter);

		// LocalDateTime dateFromRepo =
		// detailsRepository.findModifiedDateByFileName(jsonFileLocation);

		ProcurementDetails details = ProcurementDetails.builder().cmmFileLocation(jsonFileLocation)
				.createdDate(LocalDateTime.now()).downloadedFileLocation(downloadedFileLocation)
				.sourceDetailsId(sourceId).updatedDate(LocalDateTime.now()).versionNumber(1).modifiedDate(modifiedDate)
				.fileName(jsonFileName + JSON_EXTENSION).build();
		procurementDetailsRepository.save(details);
		log.info("Saved into procurement details table for " + jsonFileName);
	}

	public String getLatestFile(String fileName, Integer sourceId) {
		// ProcurementDetails procurementDetailsModel =
		// procurementDetailsRepository.findLatestFileByFileNameAndSourceId(fileName,
		// sourceId);
		ProcurementDetails procurementDetailsModel = procurementDetailsRepository
				.findFirstByFileNameAndSourceDetailsIdOrderByIdDesc(fileName, sourceId);
		if (procurementDetailsModel != null) {
			return procurementDetailsModel.getCmmFileLocation().replace("\\", "/");
		} else
			return null;
	}

	public String getLastProcuredVersion(Integer sourceId) {
		ProcurementDetails procurementDetails = procurementDetailsRepository
				.findFirstBySourceDetailsIdOrderByIdDesc(sourceId);
		if (procurementDetails != null && procurementDetails.getModifiedDate() != null) {
			return procurementDetails.getModifiedDate();
		}
		return "2000-01-01";
	}

	/*public List<ProcurementDetails> getLastVersionProcuredFiles(String modifiedDate, Integer sourceId) {
		List<ProcurementDetails> procurementDetails = procurementDetailsRepository
				.findByModifiedDateAndSourceDetailsIdOrderByIdDesc(modifiedDate, sourceId);
		return procurementDetails;
	}*/
}
