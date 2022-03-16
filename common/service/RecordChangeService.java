package com.kpmg.rcm.sourcing.common.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kpmg.rcm.sourcing.common.dto.RecordChange;
import com.kpmg.rcm.sourcing.common.dto.RecordChangeWrapper;
import com.kpmg.rcm.sourcing.common.entity.RecordChangeModel;
import com.kpmg.rcm.sourcing.common.json.dto.Granule;
import com.kpmg.rcm.sourcing.common.json.dto.SubGranule;
import com.kpmg.rcm.sourcing.common.repository.RecordChangeRepository;
import com.kpmg.rcm.sourcing.common.util.MemoryUtil;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RecordChangeService {

	@Autowired
	private ObjectMapper objMapper;

	@Autowired
	private RecordChangeRepository recordChangeRepository;

	@Autowired
	private RecordChangeFieldService recordChangeFieldService;

	@Autowired
	private RecordChangeChecksumService recordChangeChecksumService;

	// INFO: Generate Pojo and Checksum
	public RecordChangeWrapper generatePojoAndChecksum(String granuleJsonStr) {

		// INFO: Convert to Pojo
		Granule granule;
		try {
			granule = objMapper.readValue(granuleJsonStr, Granule.class);
			
			List<RecordChange> recordChanges = new ArrayList<>();

			// INFO: Build checksum for granules
			String granuleChecksum = recordChangeChecksumService.buildChecksum(granule);
			if (granuleChecksum == null) {
				return null;
			}
			recordChanges.add(new RecordChange(granule.getSystem().getCommonId(), granuleChecksum));

			// INFO: Build checksum for sub granules
			List<RecordChange> recordChangesSG = recordChangeChecksumService
					.buildChecksumForSubGranules(granule.getSubGranules(), granule.getSystem().getCommonId());

			if (recordChangesSG != null) {
				recordChanges.addAll(recordChangesSG);
			}

			return new RecordChangeWrapper(granule, recordChanges);
		} catch (JsonProcessingException e) {
			log.error("Error Occurred: ", e);
		}

		return new RecordChangeWrapper();
	}

	// TODO - clear DataStructures after they are used.
	@Deprecated
	public void recordChangeV3BatchProcessing(List<Granule> granules, List<RecordChange> recordChanges,
			Integer sourceId, String fileName) {

		// ConcurrentMap<String, String> recordChangeMap = recordChanges.stream()
		// .collect(Collectors.toConcurrentMap(RecordChange::getGranuleId,
		// RecordChange::getCurrentChecksum));

		List<RecordChangeModel> recordChangeModels = recordChangeRepository.findBySourceIdAndFileName(sourceId,
				fileName);
		log.info("Record Change Model size: " + recordChangeModels.size());
		// INFO Uncomment below if current method is being used
		// convertAndSaveRecordChange(recordChangeModels, recordChanges, sourceId,
		// fileName, "");

		if (recordChangeModels.size() == 0) {
			log.info("No Record changed for filename: " + fileName + ", all checksum saved in DB.");
			return;
		}

		// INFO Check Checksum between DB Model and Current Granule's checksum,
		// INFO if any granule is changed - download the file and perform record change.
		// INFO Prepare Batch Update Checksum List (This needs to be updated in batch)
		Map<String, RecordChangeModel> recordChangeModelMap = recordChangeModels.stream().collect(Collectors.toMap(
				RecordChangeModel::getGranuleId, recordChangeModel -> recordChangeModel, (granuleId, granuleId2) -> {
					log.warn("Duplicate granuleId found: " + granuleId);
					return granuleId;
				}));

		boolean isCheckSumChangedForGranule = compareChecksumForGranules(granules, recordChangeModelMap);
		MemoryUtil.clear(recordChangeModelMap);

		if (isCheckSumChangedForGranule) {
			// INFO: Below Function will download the Old file and will match field by field
			// recordChangeFieldService.recordFieldChanges(granules, fileName, sourceId);
		} else {
			log.debug("Checksum for no record changed for filename: " + fileName + ", skipping field check");
		}

	}

	// TODO - clear DataStructures after they are used.
	@Deprecated
	private Boolean compareChecksumForGranules(List<Granule> granules,
			Map<String, RecordChangeModel> recordChangeModelMap) {

		log.info("Comparing current checksum of granules with DB checksum");
		boolean isChecksumChanged = false;
		List<RecordChangeModel> changedRecordsToUpdate = new ArrayList<>();

		for (Granule granule : granules) {

			String id = granule.getSystem().getCommonId();

			// TODO why do we need to iterate over the map? - it will be O(n) complexity and
			// nested for's is O(n^2)
			// TODO & Sequence of the entry set is not ordered
			//
			// TODO convert this to recordChangeModelMap.get(id) -- DONE, TODO Test this
			RecordChangeModel recordChangeModel = recordChangeModelMap.get(id);
			if (recordChangeModel != null) {

				// if common id matches and checksum does not match, set field changed as true
				if (id.equalsIgnoreCase(recordChangeModel.getGranuleId()) && !granule.getSource().getChecksum()
						.equalsIgnoreCase(recordChangeModel.getChecksumCurrent())) {

					isChecksumChanged = true;
					granule.getSource().setChanged(true);

					// TODO Check do we need to maintain new Model for updating ? -- not needed
					// RecordChangeModel recordChange = new RecordChangeModel();

					// TODO I have updated the same model - need to test -- working
					recordChangeModel.setChecksumPrevious(recordChangeModel.getChecksumCurrent());
					recordChangeModel.setChecksumCurrent(granule.getSource().getChecksum());

					changedRecordsToUpdate.add(recordChangeModel);
				} else if (id.equalsIgnoreCase(recordChangeModel.getGranuleId())) {

					// if common id and checksum both match, set field changed as false
					granule.getSource().setChanged(false);
				}
			}

			List<SubGranule> subGranules = granule.getSubGranules();
			if (subGranules != null && subGranules.size() > 0) {

				for (SubGranule subGranule : subGranules) {

					// INFO: This commonId is already concatenated with Parent Granule CommonId /
					// subGranuleId
					String commonId = subGranule.getCommonId();

					// TODO why do we need to iterate over the map? - it will be O(n) complexity and
					// nested for's is O(n^2) --- working fine
					// TODO & Sequence of the entry set is not ordered --- working fine
					//
					// TODO convert this to recordChangeModelMap.get(commonId) -- DONE, TODO Test
					// this --- working fine
					RecordChangeModel recordChangeModelOfSubGranule = recordChangeModelMap.get(commonId);
					if (recordChangeModelOfSubGranule != null) {

						if (commonId.equalsIgnoreCase(recordChangeModelOfSubGranule.getGranuleId()) && !subGranule
								.getChecksum().equalsIgnoreCase(recordChangeModelOfSubGranule.getChecksumCurrent())) {
							isChecksumChanged = true;
							subGranule.setChanged(true);

							// TODO Check - do we need to maintain new Model for updating ? --- working fine
							// RecordChangeModel recordChange = new RecordChangeModel(); // not needed

							// TODO I have updated the same model - need to test --- working fine, updating
							// only the ones which are changed
							recordChangeModelOfSubGranule
									.setChecksumPrevious(recordChangeModelOfSubGranule.getChecksumCurrent());
							recordChangeModelOfSubGranule.setChecksumCurrent(subGranule.getChecksum());
							changedRecordsToUpdate.add(recordChangeModelOfSubGranule);
						}
					}
				}
			}
			MemoryUtil.clear(subGranules);
		}

		if (changedRecordsToUpdate.size() > 0) {
			updateChecksumInDBForChangedModules(changedRecordsToUpdate);
		}
		return isChecksumChanged;

	}

	// INFO: cleared DataStructures after they are used.
	@Deprecated
	private void updateChecksumInDBForChangedModules(List<RecordChangeModel> changedRecords) {
		log.info("Updating Checksum in DB for " + changedRecords.size() + " records\n...");
		List<RecordChangeModel> batchRecords = new ArrayList<>();
		int size = changedRecords.size();
		int counter = 0;

		for (RecordChangeModel changeModel : changedRecords) {
			changeModel.setUpdatedDate(LocalDateTime.now());
			batchRecords.add(changeModel);
			if ((counter + 1) % 50 == 0 || (counter + 1) == size) {
				recordChangeRepository.saveAll(batchRecords);
				batchRecords.clear();
			}

			counter++;
		}

		MemoryUtil.clear(changedRecords);
	}

	private void convertAndSaveRecordChange(List<RecordChangeModel> recordChangeModels,
			List<RecordChange> recordChanges, Integer sourceId, String fileName, String rootNode) {
		if (recordChangeModels.size() == 0) {
			int size = recordChanges.size();
			int counter = 0;

			List<RecordChangeModel> recordChangeModelsBatchList = new ArrayList<>();

			for (RecordChange recordChange : recordChanges) {
				RecordChangeModel recordChangeModel = new RecordChangeModel();
				recordChangeModel.setChecksumCurrent(recordChange.getCurrentChecksum());
				recordChangeModel.setGranuleId(recordChange.getGranuleId());
				recordChangeModel.setSourceId(sourceId);
				recordChangeModel.setFileName(fileName);
				recordChangeModel.setRootNode(rootNode);
				recordChangeModel.setCreatedDate(LocalDateTime.now());

				recordChangeModelsBatchList.add(recordChangeModel);

				if ((counter + 1) % 50 == 0 || (counter + 1) == size) {
					recordChangeRepository.saveAll(recordChangeModelsBatchList);
					log.debug("first time insert done for {} records", counter);
					recordChangeModelsBatchList.clear();
				}

				counter++;
			}
		} else {
			// INFO - Implement this if this method is called from
			// recordChangeV3BatchProcessing (@Deprecated)
			// TODO Check which granule/record are not present in recordChangeModel and
			// insert those in DB.
		}
	}

	public void recordChangeV4(List<Granule> granules, List<RecordChange> recordChanges, Integer sourceId,
			String fileName) {
		fileName = fileName.toLowerCase();

		boolean isRecordChanged = detectSaveOrUpdateRCV2(granules, sourceId, fileName, recordChanges);

		if (isRecordChanged) {
			// INFO: Below Function will download the Old file and will match field by field
			recordChangeFieldService.recordFieldChanges(granules, fileName, sourceId);
		} else {
			log.debug("Checksum for no record changed for filename: " + fileName + ", skipping field check");
		}
	}

	private boolean detectSaveOrUpdateRCV2(List<Granule> granules, Integer sourceId, String fileName,
			List<RecordChange> recordChanges) {

		// INFO PreCheck Granule Size
		if (granules == null || granules.size() == 0) {
			log.error("detectSaveOrUpdateRCV2 --> Granules List is empty for sourceId {} and fileName {}", sourceId,
					fileName);
			return false;
		}

		// INFO - Get Common Id from the root
		String commonId = granules.get(0).getSystem().getCommonId();
		String[] commonIdSplit = commonId.split("/");

		// INFO - Check common id length
		if (commonIdSplit.length < 3) {
			log.error("commonIdSplit error size {}, commonId {}", commonIdSplit.length, commonId);
			return false;
		}

		// INFO Get Root Node
		String rootNode = commonIdSplit[0] + "/" + commonIdSplit[1] + "/" + commonIdSplit[2];
		log.debug("rootNode {} from commonId {} for sourceId {} and fileName {}", rootNode, commonId, sourceId,
				fileName);
		List<RecordChangeModel> recordChangeModels = recordChangeRepository.findByRootNode(rootNode);

		// INFO: Logic for: if this is first time insert
		if (recordChangeModels == null || recordChangeModels.size() == 0) {
			log.debug("Starting first time insert for sourceId: {}, rootNode: {}, size: {}\n...Waiting", sourceId,
					rootNode, granules.size());
			convertAndSaveRecordChange(new ArrayList<>(), recordChanges, sourceId, fileName, rootNode);
			return false;
		}

		List<RecordChangeModel> rcModelBatchUpdateList = new ArrayList<>();
		List<RecordChangeModel> rcModelBatchInsertList = new ArrayList<>();

		boolean isRecordChanged = false;

		Map<String, RecordChangeModel> granuleAndSubGranuleRCMap = recordChangeModels.stream()
				.collect(Collectors.toMap(RecordChangeModel::getGranuleId, (recordChangeModel -> recordChangeModel),
						(recordChangeModel, recordChangeModel1) -> {
							log.warn("Duplicate granuleId found: {}", recordChangeModel);
							return recordChangeModel;
						}));

		for (Granule granule : granules) {

			RecordChangeModel recordChangeModel = granuleAndSubGranuleRCMap.get(granule.getSystem().getCommonId());
			if (recordChangeModel != null) {
				// INFO: Granule found

				// INFO: Compare checksum
				if (recordChangeModel.getChecksumCurrent().equalsIgnoreCase(granule.getSource().getChecksum())) {
					// INFO: Checksum Matched - no Change
					granule.getSource().setChanged(false);

				} else {
					// INFO: Checksum NOT Matched
					isRecordChanged = true;
					granule.getSource().setChanged(true);

					// INFO: Update new checksum and shift current_checksum to previous_checksum
					recordChangeModel.setChecksumPrevious(recordChangeModel.getChecksumCurrent());
					recordChangeModel.setChecksumCurrent(granule.getSource().getChecksum());
					recordChangeModel.setUpdatedDate(LocalDateTime.now());
					rcModelBatchUpdateList.add(recordChangeModel);

				}
			} else {
				// INFO: Granule not found - save the checksum_current for this granule.
				RecordChangeModel rcmInsert = new RecordChangeModel();
				rcmInsert.setChecksumCurrent(granule.getSource().getChecksum());
				rcmInsert.setGranuleId(granule.getSystem().getCommonId());
				rcmInsert.setSourceId(sourceId);
				rcmInsert.setFileName(fileName);
				rcmInsert.setRootNode(rootNode);
				rcmInsert.setCreatedDate(LocalDateTime.now());
				rcModelBatchInsertList.add(rcmInsert);
			}

			List<SubGranule> subGranules = granule.getSubGranules();
			if (!CollectionUtils.isEmpty(subGranules)) {
				for (SubGranule subGranule : subGranules) {

					RecordChangeModel subGranuleModel = granuleAndSubGranuleRCMap.get(subGranule.getCommonId());

					if (subGranuleModel != null) {
						// INFO: SubGranule found

						// INFO: Compare checksum
						if (subGranuleModel.getChecksumCurrent().equalsIgnoreCase(subGranule.getChecksum())) {
							// INFO: Checksum Matched - no Change
							subGranule.setChanged(false);

						} else {
							// INFO: Checksum NOT Matched
							isRecordChanged = true;
							subGranule.setChanged(true);

							// INFO: Update new checksum and shift current_checksum to previous_checksum
							subGranuleModel.setChecksumPrevious(subGranuleModel.getChecksumCurrent());
							subGranuleModel.setChecksumCurrent(subGranule.getChecksum());
							subGranuleModel.setUpdatedDate(LocalDateTime.now());
							rcModelBatchUpdateList.add(subGranuleModel);

						}
					} else {
						// INFO: SubGranule not found - save the checksum_current for this SubGranule.
						RecordChangeModel rcmInsert = new RecordChangeModel();
						rcmInsert.setChecksumCurrent(subGranule.getChecksum());
						rcmInsert.setGranuleId(subGranule.getCommonId());
						rcmInsert.setSourceId(sourceId);
						rcmInsert.setFileName(fileName);
						rcmInsert.setRootNode(rootNode);
						rcmInsert.setCreatedDate(LocalDateTime.now());
						rcModelBatchInsertList.add(rcmInsert);
					}

				}
			}
		}

		// INFO: Batch Insert
		if (rcModelBatchInsertList.size() > 0) {
			log.info("Starting batchSaveOrUpdate insert for sourceId: {}, size {}, filename {}\n...Waiting", sourceId,
					rcModelBatchInsertList.size(), fileName);
			batchSaveOrUpdate(rcModelBatchInsertList);
		}
		MemoryUtil.clear(rcModelBatchInsertList);

		// INFO: Batch Update
		if (rcModelBatchUpdateList.size() > 0) {
			log.info("Starting batchSaveOrUpdate updating for sourceId: {}, size {}, filename {}\n...Waiting", sourceId,
					rcModelBatchUpdateList.size(), fileName);
			// findDuplicates(rcModelBatchUpdateList);
			batchSaveOrUpdate(rcModelBatchUpdateList);
		}
		MemoryUtil.clear(rcModelBatchUpdateList);

		return isRecordChanged;
	}

	private void findDuplicates(List<RecordChangeModel> rcModelBatchUpdateList) throws JsonProcessingException {
		String s = objMapper.writeValueAsString(rcModelBatchUpdateList);
		System.out.println(s);

		Map<String, Integer> map = new HashMap<>();
		for (int i = 0; i < rcModelBatchUpdateList.size(); i++) {
			RecordChangeModel recordChangeModel = rcModelBatchUpdateList.get(i);
			Integer count = map.get(recordChangeModel.getGranuleId());
			if (count == null) {
				map.put(recordChangeModel.getGranuleId(), 1);
			} else {
				map.put(recordChangeModel.getGranuleId(), count + 1);
			}
		}
		log.info("==========================");
		log.info("\n" + map);
	}

	@Deprecated
	private boolean detectSaveOrUpdateRC(List<Granule> granules, Integer sourceId, String fileName,
			List<RecordChange> recordChanges) {

		// INFO: Logic for: if this is first time insert
		boolean isExist = recordChangeRepository.existsBySourceId(sourceId);
		if (!isExist) {
			log.info("Starting first time insert for sourceId: {}\n...Waiting", sourceId);
			// INFO uncomment below if current method is being used
			// convertAndSaveRecordChange(new ArrayList<>(), recordChanges, sourceId,
			// fileName, "");
			return false;
		}

		List<RecordChangeModel> rcModelBatchUpdateList = new ArrayList<>();
		List<RecordChangeModel> rcModelBatchInsertList = new ArrayList<>();

		boolean isRecordChanged = false;

		for (Granule granule : granules) {

			RecordChangeModel rcModelByGranuleId = recordChangeRepository
					.findByGranuleId(granule.getSystem().getCommonId());

			if (rcModelByGranuleId != null) {
				// INFO: Granule found

				// INFO: Compare checksum
				if (rcModelByGranuleId.getChecksumCurrent().equalsIgnoreCase(granule.getSource().getChecksum())) {
					// INFO: Checksum Matched - no Change
					granule.getSource().setChanged(false);

				} else {
					// INFO: Checksum NOT Matched
					isRecordChanged = true;
					granule.getSource().setChanged(true);

					// INFO: Update new checksum and shift current_checksum to previous_checksum
					rcModelByGranuleId.setChecksumPrevious(rcModelByGranuleId.getChecksumCurrent());
					rcModelByGranuleId.setChecksumCurrent(granule.getSource().getChecksum());
					rcModelByGranuleId.setUpdatedDate(LocalDateTime.now());
					rcModelBatchUpdateList.add(rcModelByGranuleId);

				}
			} else {
				// INFO: Granule not found - save the checksum_current for this granule.
				RecordChangeModel rcmInsert = new RecordChangeModel();
				rcmInsert.setChecksumCurrent(granule.getSource().getChecksum());
				rcmInsert.setGranuleId(granule.getSystem().getCommonId());
				rcmInsert.setSourceId(sourceId);
				rcmInsert.setFileName(fileName);
				rcmInsert.setCreatedDate(LocalDateTime.now());
				rcModelBatchInsertList.add(rcmInsert);
			}

			List<SubGranule> subGranules = granule.getSubGranules();
			if (!CollectionUtils.isEmpty(subGranules)) {
				for (SubGranule subGranule : subGranules) {

					RecordChangeModel rcModelBySubGranuleId = recordChangeRepository
							.findByGranuleId(subGranule.getCommonId());

					if (rcModelBySubGranuleId != null) {
						// INFO: SubGranule found

						// INFO: Compare checksum
						if (rcModelBySubGranuleId.getChecksumCurrent().equalsIgnoreCase(subGranule.getChecksum())) {
							// INFO: Checksum Matched - no Change
							subGranule.setChanged(false);

						} else {
							// INFO: Checksum NOT Matched
							isRecordChanged = true;
							subGranule.setChanged(true);

							// INFO: Update new checksum and shift current_checksum to previous_checksum
							rcModelBySubGranuleId.setChecksumPrevious(rcModelBySubGranuleId.getChecksumCurrent());
							rcModelBySubGranuleId.setChecksumCurrent(subGranule.getChecksum());
							rcModelBySubGranuleId.setUpdatedDate(LocalDateTime.now());
							rcModelBatchUpdateList.add(rcModelBySubGranuleId);

						}
					} else {
						// INFO: SubGranule not found - save the checksum_current for this SubGranule.
						RecordChangeModel rcmInsert = new RecordChangeModel();
						rcmInsert.setChecksumCurrent(subGranule.getChecksum());
						rcmInsert.setGranuleId(subGranule.getCommonId());
						rcmInsert.setSourceId(sourceId);
						rcmInsert.setFileName(fileName);
						rcmInsert.setCreatedDate(LocalDateTime.now());
						rcModelBatchInsertList.add(rcmInsert);
					}

				}
			}
		}

		// INFO: Batch Insert
		if (rcModelBatchInsertList.size() > 0) {
			log.info("Starting batchSaveOrUpdate insert for sourceId: {}, size {}, filename {}\n...Waiting", sourceId,
					rcModelBatchInsertList.size(), fileName);
			// batchSaveOrUpdate(rcModelBatchInsertList);
		}
		MemoryUtil.clear(rcModelBatchInsertList);

		// INFO: Batch Update
		if (rcModelBatchUpdateList.size() > 0) {
			log.info("Starting batchSaveOrUpdate updating for sourceId: {}, size {}, filename {}\n...Waiting", sourceId,
					rcModelBatchUpdateList.size(), fileName);
			// batchSaveOrUpdate(rcModelBatchUpdateList);
		}
		MemoryUtil.clear(rcModelBatchUpdateList);

		return isRecordChanged;
	}

	private void batchSaveOrUpdate(List<RecordChangeModel> rcModelBatchList) {

		if (rcModelBatchList.size() == 0) {
			log.warn("batchSaveOrUpdate list size 0");
			return;
		}

		int size = rcModelBatchList.size();
		int counter = 0;

		List<RecordChangeModel> recordChangeModelsBatchList = new ArrayList<>();

		for (RecordChangeModel recordChangeModel : rcModelBatchList) {

			recordChangeModelsBatchList.add(recordChangeModel);

			if ((counter + 1) % 50 == 0 || (counter + 1) == size) {
				recordChangeRepository.saveAll(recordChangeModelsBatchList);
				log.debug("batchSaveOrUpdate done for {} records", counter);
				recordChangeModelsBatchList.clear();
			}

			counter++;
		}

		log.debug("batchSaveOrUpdate recordChangeModelsBatchList size after for loop {}",
				recordChangeModelsBatchList.size());

		MemoryUtil.clear(recordChangeModelsBatchList);
	}
}
