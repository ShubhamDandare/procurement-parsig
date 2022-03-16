package com.kpmg.rcm.sourcing.common.service;

import static com.kpmg.rcm.sourcing.common.util.CommonConstants.JSON_EXTENSION;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kpmg.rcm.sourcing.common.azure.AzureOperations;
import com.kpmg.rcm.sourcing.common.config.properties.FilePathProperties;
import com.kpmg.rcm.sourcing.common.json.dto.Geography;
import com.kpmg.rcm.sourcing.common.json.dto.Geography__1;
import com.kpmg.rcm.sourcing.common.json.dto.Granule;
import com.kpmg.rcm.sourcing.common.json.dto.Linkage;
import com.kpmg.rcm.sourcing.common.json.dto.Linkage__1;
import com.kpmg.rcm.sourcing.common.json.dto.Note;
import com.kpmg.rcm.sourcing.common.json.dto.Note__1;
import com.kpmg.rcm.sourcing.common.json.dto.Org;
import com.kpmg.rcm.sourcing.common.json.dto.Org__1;
import com.kpmg.rcm.sourcing.common.json.dto.Source;
import com.kpmg.rcm.sourcing.common.json.dto.SubGranule;
import com.kpmg.rcm.sourcing.common.util.MemoryUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RecordChangeFieldService {

	@Autowired
	private ObjectMapper objMapper;

	@Autowired
	private AzureOperations azureOperations;

	@Autowired
	private ProcurementDetailsService procurementDetailsService;

	@Autowired
	private FilePathProperties filePathProperties;

	private static final Pattern historicalDateP = Pattern.compile("\\d{4}-\\d+-\\d+");

	// INFO - cleared DataStructures after they are used
	public void recordFieldChanges(List<Granule> granules, String fileName, Integer sourceId) {

		String tempFilePath = filePathProperties.getTempStoragePathForRecordChange() + "Old" + fileName
				+ JSON_EXTENSION;
		File oldFile = new File(tempFilePath);
		oldFile.getParentFile().mkdirs();
		log.info("oldFile: " + oldFile);

		// INFO this replacement of date in the json file name is done for ECFR source,
		// other sources should follow same
		Matcher dateM = historicalDateP.matcher(fileName);
		String latestFileName = "";
		if (dateM.find()) {
			String modifiedDate = dateM.group(0);
			if (modifiedDate != null) {
				latestFileName = fileName.replaceAll("_" + modifiedDate, "");
			}
		} else {
			latestFileName = fileName;
		}

		String latestFileLocation = procurementDetailsService.getLatestFile(latestFileName + JSON_EXTENSION, sourceId);
		if (latestFileLocation == null) {
			log.error("latestFileName {} not found in DB for fileName: {} and source Id {}", latestFileName, fileName,
					sourceId);
			return;
		}

		log.info("Old blobName: {} to download", latestFileLocation);
		azureOperations.downloadData(tempFilePath, latestFileLocation);

		// create a reader
		BufferedReader reader = null;
		try {
			reader = Files.newBufferedReader(Paths.get(tempFilePath));

			String jsonString = reader.lines().collect(Collectors.joining());
			if (StringUtils.isEmpty(jsonString)) {
				return;
			}
			ArrayList<Granule> oldGranules = objMapper.readValue(jsonString, new TypeReference<ArrayList<Granule>>() {
			});
			Map<String, Granule> oldGranuleMap = oldGranules.stream().collect(Collectors
					.toMap(granule -> granule.getSystem().getCommonId(), granule -> granule, (granule1, granule2) -> {
						return granule1;
					}));

			for (Granule granule : granules) {
				String id = granule.getId();

				if (granule.getSource().getChanged()!= null? granule.getSource().getChanged() : false ) {
					fieldMatchingForGranule(granule, oldGranuleMap.get(granule.getSystem().getCommonId()));
				}

				if (granule.getSubGranules() != null && oldGranuleMap.size() > 0) {

					Granule oldGranule = oldGranuleMap.get(granule.getSystem().getCommonId());

					if (oldGranule == null || oldGranule.getSubGranules() == null) {
						log.debug("oldGranule is null for granule Id: {}", granule.getSystem().getCommonId());
						continue;
					}

					// TODO Need to resolve Duplicate Key Exception
					Map<String, SubGranule> oldSubGranuleMap = oldGranule.getSubGranules().stream()
							.collect(Collectors.toMap(oldSubGranule -> {
								oldSubGranule.setCommonId(
										oldGranule.getSystem().getCommonId() + "/" + oldSubGranule.getId());
								return oldSubGranule.getCommonId();
							}, oldSubGranule -> oldSubGranule, (subGranuleId, subGranuleId1) -> {
								log.warn("Duplicate sub granule Id found: " + subGranuleId);
								return subGranuleId;
							}));
					for (int j = 0; j < granule.getSubGranules().size(); j++) {
						SubGranule subGranule = granule.getSubGranules().get(j);
						if (subGranule.getChanged() != null ? subGranule.getChanged() : false) {
							fieldMatchingForSubGranule(subGranule, oldSubGranuleMap.get(subGranule.getCommonId()));
						}
					}
					// Detect deleted subGranules
//					detectDeletedSubGranules(granule, oldGranule);

					oldSubGranuleMap.clear();
				}
			}

			MemoryUtil.clear(oldGranuleMap);
			MemoryUtil.clear(oldGranules);

		} catch (Exception e) {
			log.error("Error Occurred ", e);
		} finally {
			// TODO Check this
			if(null != reader) {
				try {
					reader.close();
				} catch (IOException e) {
					log.error("Error in deleting reader for old file ", e);
				}
			}
			if (oldFile.exists()) {
				oldFile.delete();
			}
			// TODO MemoryUtil.clear(oldGranuleMap);
			// MemoryUtil.clear(oldGranules);

		}

	}

	private void detectDeletedSubGranules(Granule granule, Granule oldGranule) {
		Set<String> oldSgCommonIds = oldGranule.getSubGranules().stream().map(sg -> sg.getCommonId())
				.collect(Collectors.toSet());
		Set<String> newSgCommonIds = granule.getSubGranules().stream().map(sg -> sg.getCommonId())
				.collect(Collectors.toSet());
		oldSgCommonIds.removeAll(newSgCommonIds);
		if (!oldSgCommonIds.isEmpty()) {
			for (String deletedCommonId : oldSgCommonIds) {
				Optional<SubGranule> subGranuleOpt = oldGranule.getSubGranules().stream()
						.filter(sg -> sg.getCommonId().equals(deletedCommonId)).findFirst();
				if (subGranuleOpt.isPresent()) {
					SubGranule deletedSg = subGranuleOpt.get();
					deletedSg.setChanged(true);
					deletedSg.setChangedFields(Collections.singletonList("deleted-subgranule"));
					granule.getSubGranules().add(deletedSg);
				}
			}
		}
	}

	private Date dateMinusOne() {
		final Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -1);
		return cal.getTime();
	}

	public void fieldMatchingForSubGranule(SubGranule subGranule, SubGranule oldSubGranule) {
		Set<String> changedFields = new HashSet<>();
		if (oldSubGranule == null && subGranule != null) {
			// INFO Setting this to false as we are capturing change record at Granule level
			//  with "subgranules" field change
			subGranule.setChanged(false);
		} else {
			// heading
			if (StringUtils.isNoneBlank(subGranule.getHeading(), oldSubGranule.getHeading())
					&& !subGranule.getHeading().equals(oldSubGranule.getHeading())) {
				// INFO Removed all spaces, concatenated and matched
				String oldHeading = oldSubGranule.getHeading().replaceAll("\\s+", "");
				String thisHeading = subGranule.getHeading().replaceAll("\\s+", "");
				if (!oldHeading.equals(thisHeading))
					changedFields.add("heading");
			} else if (StringUtils.isEmpty(subGranule.getHeading()) && !StringUtils.isEmpty(oldSubGranule.getHeading())) {
				changedFields.add("heading");
			} else if (!StringUtils.isEmpty(subGranule.getHeading()) && StringUtils.isEmpty(oldSubGranule.getHeading())) {
				changedFields.add("heading");
			}

			// teaser
			if (StringUtils.isNoneBlank(subGranule.getTeaser(), oldSubGranule.getTeaser())
					&& !subGranule.getTeaser().equals(oldSubGranule.getTeaser())) {
				// INFO Removed all spaces, concatenated and matched
				String oldTeaser = oldSubGranule.getTeaser().replaceAll("\\s+", "");
				String thisTeaser = subGranule.getTeaser().replaceAll("\\s+", "");
				if (!oldTeaser.equals(thisTeaser))
					changedFields.add("teaser");
			} else if (StringUtils.isEmpty(subGranule.getTeaser()) && !StringUtils.isEmpty(oldSubGranule.getTeaser())) {
				changedFields.add("teaser");
			} else if (!StringUtils.isEmpty(subGranule.getTeaser()) && StringUtils.isEmpty(oldSubGranule.getTeaser())) {
				changedFields.add("teaser");
			}

			// abstract
			if (StringUtils.isNoneBlank(subGranule.get_abstract(), oldSubGranule.get_abstract())
					&& !subGranule.get_abstract().equals(oldSubGranule.get_abstract())) {
				// INFO Removed all spaces, concatenated and matched
				String oldAbstract = oldSubGranule.get_abstract().replaceAll("\\s+", "");
				String thisAbstract = subGranule.get_abstract().replaceAll("\\s+", "");
				if (!oldAbstract.equals(thisAbstract))
					changedFields.add("abstract");
			} else if (StringUtils.isEmpty(subGranule.get_abstract()) && !StringUtils.isEmpty(oldSubGranule.get_abstract())) {
				changedFields.add("abstract");
			} else if (!StringUtils.isEmpty(subGranule.get_abstract()) && StringUtils.isEmpty(oldSubGranule.get_abstract())) {
				changedFields.add("abstract");
			}

			// content
			if (StringUtils.isNoneBlank(subGranule.getContent(), oldSubGranule.getContent())
					&& !subGranule.getContent().equals(oldSubGranule.getContent())) {
				// INFO Removed all spaces, concatenated and matched
				String oldContent = oldSubGranule.getContent().replaceAll("\\s+", "");
				String thisContent = subGranule.getContent().replaceAll("\\s+", "");
				if (!oldContent.equals(thisContent))
					changedFields.add("content");
			} else if (StringUtils.isEmpty(subGranule.getContent()) && !StringUtils.isEmpty(oldSubGranule.getContent())) {
				changedFields.add("content");
			} else if (!StringUtils.isEmpty(subGranule.getContent()) && StringUtils.isEmpty(oldSubGranule.getContent())) {
				changedFields.add("content");
			}

			/*
			 * notes notes[n].type notes[n].heading notes[n].content
			 */
			List<Note__1> noteList = subGranule.getNotes();
			List<Note__1> oldNoteList = oldSubGranule.getNotes();
			if (!CollectionUtils.isEmpty(noteList)) {
				if (!CollectionUtils.isEmpty(oldNoteList)) {
					if (noteList.size() != oldNoteList.size()) {
						changedFields.add("notes");
					} else {
						for (int noteIndex = 0; noteIndex < noteList.size(); noteIndex++) {
							Note__1 note = noteList.get(noteIndex);
							Note__1 oldNote = oldNoteList.get(noteIndex);
							if (oldNote != null && note != null) {

								try {
									if ((null == note.getType() && null != oldNote.getType())
											|| (null != note.getType() && !note.getType().equals(oldNote.getType())))
										changedFields.add("notes.type");
									if ((null == note.getHeading() && null != oldNote.getHeading())
											|| (null != note.getHeading()
													&& !note.getHeading().equals(oldNote.getHeading())))
										changedFields.add("notes.heading");
									if ((null == note.getContent() && null != oldNote.getContent())
											|| (null != note.getContent()
													&& !note.getContent().equals(oldNote.getContent())))
										changedFields.add("notes.content");
								} catch (Exception exception) {
									// TODO handle NPE above
									// handled above. If one of the field of note is null and respective field for
									// other note is not null, then changed fields is added
								}

							}
						}
					}
				} else {
					changedFields.add("notes");
				}
			} else if (!CollectionUtils.isEmpty(oldNoteList)) {
				changedFields.add("notes");
			}

			// dates.effective
			if (subGranule.getDates() != null && oldSubGranule.getDates() != null) {
				if ((null != subGranule.getDates().getEffective() && null != oldSubGranule.getDates().getEffective())
						&& !subGranule.getDates().getEffective().equals(oldSubGranule.getDates().getEffective())) {
					changedFields.add("dates.effective");
				} else if (StringUtils.isEmpty(subGranule.getDates().getEffective()) && !StringUtils.isEmpty(subGranule.getDates().getEffective())) {
					changedFields.add("dates.effective");
				} else if (!StringUtils.isEmpty(subGranule.getDates().getEffective()) && StringUtils.isEmpty(subGranule.getDates().getEffective())) {
					changedFields.add("dates.effective");
				}

				// dates.expire
				if ((null != subGranule.getDates().getExpire() && null != oldSubGranule.getDates().getExpire())
						&& !subGranule.getDates().getExpire().equals(oldSubGranule.getDates().getExpire())) {
					changedFields.add("dates.expire");
				} else if (StringUtils.isEmpty(subGranule.getDates().getExpire()) && !StringUtils.isEmpty(subGranule.getDates().getExpire())) {
					changedFields.add("dates.expire");
				} else if (!StringUtils.isEmpty(subGranule.getDates().getExpire()) && StringUtils.isEmpty(subGranule.getDates().getExpire())) {
					changedFields.add("dates.expire");
				}

				// dates.published
				if ((null != subGranule.getDates().getPublished() && null != oldSubGranule.getDates().getPublished())
						&& !subGranule.getDates().getPublished().equals(oldSubGranule.getDates().getPublished())) {
					changedFields.add("dates.published");
				} else if (StringUtils.isEmpty(subGranule.getDates().getPublished()) && !StringUtils.isEmpty(subGranule.getDates().getPublished())) {
					changedFields.add("dates.published");
				} else if (!StringUtils.isEmpty(subGranule.getDates().getPublished()) && StringUtils.isEmpty(subGranule.getDates().getPublished())) {
					changedFields.add("dates.published");
				}

				// dates.updated
				if ((null != subGranule.getDates().getUpdated() && null != oldSubGranule.getDates().getUpdated())
						&& !subGranule.getDates().getUpdated().equals(oldSubGranule.getDates().getUpdated())) {
					changedFields.add("dates.updated");
				} else if (StringUtils.isEmpty(subGranule.getDates().getUpdated()) && !StringUtils.isEmpty(subGranule.getDates().getUpdated())) {
					changedFields.add("dates.updated");
				} else if (!StringUtils.isEmpty(subGranule.getDates().getUpdated()) && StringUtils.isEmpty(subGranule.getDates().getUpdated())) {
					changedFields.add("dates.updated");
				}

				// dates.compliance
				if ((null != subGranule.getDates().getCompliance() && null != oldSubGranule.getDates().getCompliance())
						&& !subGranule.getDates().getCompliance().equals(oldSubGranule.getDates().getCompliance())) {
					changedFields.add("dates.compliance");
				} else if (StringUtils.isEmpty(subGranule.getDates().getCompliance()) && !StringUtils.isEmpty(subGranule.getDates().getCompliance())) {
					changedFields.add("dates.compliance");
				} else if (!StringUtils.isEmpty(subGranule.getDates().getCompliance()) && StringUtils.isEmpty(subGranule.getDates().getCompliance())) {
					changedFields.add("dates.compliance");
				}
			} else if (subGranule.getDates() != null && oldSubGranule.getDates() == null) {
				changedFields.add("dates");
			} else if (subGranule.getDates() == null && oldSubGranule.getDates() != null) {
				changedFields.add("dates");
			}

			// linkages[n].text
			List<Linkage__1> linkages = subGranule.getLinkages();
			List<Linkage__1> oldLinkages = oldSubGranule.getLinkages();
			if (!CollectionUtils.isEmpty(linkages)) {
				if (!CollectionUtils.isEmpty(oldLinkages)) {
					if (linkages.size() != oldLinkages.size()) {
						changedFields.add("linkages.text");
					} else {
						for (int i = 0; i < linkages.size(); i++) {

							if ((null == linkages.get(i).getText() && null != oldLinkages.get(i).getText())
									|| (null != linkages.get(i).getText()
											&& !linkages.get(i).getText().equals(oldLinkages.get(i).getText())))
								changedFields.add("linkages.text");
						}
					}
				} else {
					changedFields.add("linkages.text");
				}
			} else if (!CollectionUtils.isEmpty(oldLinkages)) {
				changedFields.add("linkages.text");
			}

			/*
			 * orgs orgs[n].name orgs[n].aliases orgs[n].type
			 */
			List<Org__1> orgs = subGranule.getOrgs();
			List<Org__1> oldOrg = oldSubGranule.getOrgs();
			if (!CollectionUtils.isEmpty(orgs)) {
				if (!CollectionUtils.isEmpty(oldOrg)) {
					if (orgs.size() != oldOrg.size()) {
						changedFields.add("orgs");
					} else {

						for (int org = 0; org < orgs.size(); org++) {
							if ((null == orgs.get(org).getName() && null != oldOrg.get(org).getName())
									|| (null != orgs.get(org).getName()
											&& !orgs.get(org).getName().equals(oldOrg.get(org).getName())))
								changedFields.add("orgs.name");
							if ((null == orgs.get(org).getAliases() && null != oldOrg.get(org).getAliases())
									|| (null != orgs.get(org).getAliases()
											&& !orgs.get(org).getAliases().equals(oldOrg.get(org).getAliases())))
								changedFields.add("orgs.aliases");
							if ((null == orgs.get(org).getType() && null != oldOrg.get(org).getType())
									|| (null != orgs.get(org).getType()
											&& !orgs.get(org).getType().equals(oldOrg.get(org).getType())))
								changedFields.add("orgs.type");
						}
					}
				} else {
					changedFields.add("orgs");
				}
			} else if (!CollectionUtils.isEmpty(oldOrg)) {
				changedFields.add("orgs");
			}

			// topics
			List<String> topics = subGranule.getTopics();
			List<String> oldTopics = oldSubGranule.getTopics();
			if (!CollectionUtils.isEmpty(topics)) {
				if (!CollectionUtils.isEmpty(oldTopics)) {
					if (topics.size() != oldTopics.size()) {
						changedFields.add("topics");
					} else {
						for (int topic = 0; topic < topics.size(); topic++) {
							if ((null == topics.get(topic) && null != oldTopics.get(topic))
									|| (null != topics.get(topic) && !topics.get(topic).equals(oldTopics.get(topic))))
								changedFields.add("topics");
						}
					}
				} else {
					changedFields.add("topics");
				}
			} else if (!CollectionUtils.isEmpty(oldTopics)) {
				changedFields.add("topics");
			}

			/*
			 * geographies geographies[n].name geographies[n].aliases
			 */
			List<Geography__1> geographies = subGranule.getGeographies();
			List<Geography__1> oldGeographies = oldSubGranule.getGeographies();
			if (!CollectionUtils.isEmpty(geographies)) {
				if (!CollectionUtils.isEmpty(oldGeographies)) {
					if (geographies.size() != oldGeographies.size()) {
						changedFields.add("geographies");
					} else {
						for (int geography = 0; geography < orgs.size(); geography++) {
							if ((null == geographies.get(geography).getName()
									&& null != oldGeographies.get(geography).getName())
									|| (null != geographies.get(geography).getName() && !geographies.get(geography)
											.getName().equals(oldGeographies.get(geography).getName())))
								changedFields.add("geographies.name");
							if ((null == geographies.get(geography).getAliases()
									&& null != oldGeographies.get(geography).getAliases())
									|| (null != geographies.get(geography).getAliases() && !geographies.get(geography)
											.getAliases().equals(oldGeographies.get(geography).getAliases())))
								changedFields.add("geographies.aliases");
						}
					}
				} else {
					changedFields.add("geographies");
				}
			} else if (!CollectionUtils.isEmpty(oldGeographies)) {
				changedFields.add("geographies");
			}

			// TODO priority1content - check with Shubham, why this is List<String>
			/*
			 * if (StringUtils.isNoneBlank(subGranule.getPriority1content(),
			 * oldSubGranule.getPriority1content()) &&
			 * !subGranule.getPriority1content().equals(oldSubGranule.getPriority1content())
			 * ) changedFields.add("priority1content");
			 */

			// priority2Content
			if (StringUtils.isNoneBlank(subGranule.getPriority2Content(), oldSubGranule.getPriority2Content())
					&& !subGranule.getPriority2Content().equals(oldSubGranule.getPriority2Content())) {
				changedFields.add("priority2Content");
			} else if (StringUtils.isEmpty(subGranule.getPriority2Content()) && !StringUtils.isEmpty(oldSubGranule.getPriority2Content())) {
				changedFields.add("priority2Content");
			} else if (!StringUtils.isEmpty(subGranule.getPriority2Content()) && StringUtils.isEmpty(oldSubGranule.getPriority2Content())) {
				changedFields.add("priority2Content");
			}

			// priority3Content
			if (StringUtils.isNoneBlank(subGranule.getPriority3Content(), oldSubGranule.getPriority3Content())
					&& !subGranule.getPriority3Content().equals(oldSubGranule.getPriority3Content())) {
				changedFields.add("priority3Content");
			} else if (StringUtils.isEmpty(subGranule.getPriority3Content()) && !StringUtils.isEmpty(oldSubGranule.getPriority3Content())) {
				changedFields.add("priority3Content");
			} else if (!StringUtils.isEmpty(subGranule.getPriority3Content()) && StringUtils.isEmpty(oldSubGranule.getPriority3Content())) {
				changedFields.add("priority3Content");
			}

			// originalText
			if (StringUtils.isNoneBlank(subGranule.getOriginalText(), oldSubGranule.getOriginalText())
					&& !subGranule.getOriginalText().equals(oldSubGranule.getOriginalText())) {
				changedFields.add("originalText");
			} else if (StringUtils.isEmpty(subGranule.getOriginalText()) && !StringUtils.isEmpty(oldSubGranule.getOriginalText())) {
				changedFields.add("originalText");
			} else if (!StringUtils.isEmpty(subGranule.getOriginalText()) && StringUtils.isEmpty(oldSubGranule.getOriginalText())) {
				changedFields.add("originalText");
			}
			
			// INFO: Checking all content of the list due to Parsing issue
			// TODO if Parsing issue is fixed - then we can compare size of list
			if (subGranule.getChildId() != null && oldSubGranule.getChildId() != null) {
				Set<String> s1 = new HashSet<>(subGranule.getChildId());
				Set<String> s2 = new HashSet<>(oldSubGranule.getChildId());
				if (!s1.equals(s2)) {
					changedFields.add("subGranules");
				}
				MemoryUtil.clear(s1, s2);
			}
		}

		if (changedFields.size() > 0) {
			subGranule.setChangedFields(new ArrayList<>(changedFields));
		}

	}

	public void fieldMatchingForGranule(Granule granule, Granule oldGranule) {

		if (oldGranule == null) {
			log.debug("oldGranule is null for granule Id: {}", granule.getSystem().getCommonId());
			return;
		}

		Set<String> changedFields = new HashSet();
		Source source = granule.getSource();
		Source oldSource = oldGranule.getSource();

		// heading
		if (StringUtils.isNoneBlank(source.getHeading(), oldSource.getHeading())
				&& !source.getHeading().equals(oldSource.getHeading())) {

			// INFO Removed all spaces, concatenated and matched
			String oldHeading = oldSource.getHeading().replaceAll("\\s+", "");
			String thisHeading = source.getHeading().replaceAll("\\s+", "");
			if (!oldHeading.equals(thisHeading))
				changedFields.add("heading");

		} else if (StringUtils.isEmpty(source.getHeading()) && !StringUtils.isEmpty(oldSource.getHeading())) {
			changedFields.add("heading");
		} else if (!StringUtils.isEmpty(source.getHeading()) && StringUtils.isEmpty(oldSource.getHeading())) {
			changedFields.add("heading");
		}

		// teaser
		if (StringUtils.isNoneBlank(source.getTeaser(), oldSource.getTeaser())
				&& !source.getTeaser().equals(oldSource.getTeaser())) {
			// INFO Removed all spaces, concatenated and matched
			String oldTeaser = oldSource.getTeaser().replaceAll("\\s+", "");
			String thisTeaser = source.getTeaser().replaceAll("\\s+", "");
			if (!oldTeaser.equals(thisTeaser))
				changedFields.add("teaser");
		} else if (StringUtils.isEmpty(source.getTeaser()) && !StringUtils.isEmpty(oldSource.getTeaser())) {
			changedFields.add("teaser");
		} else if (!StringUtils.isEmpty(source.getTeaser()) && StringUtils.isEmpty(oldSource.getTeaser())) {
			changedFields.add("teaser");
		}

		// abstract
		if (StringUtils.isNoneBlank(source.get_abstract(), oldSource.get_abstract())
				&& !source.get_abstract().equals(oldSource.get_abstract())) {
			// INFO Removed all spaces, concatenated and matched
			String oldAbstract = oldSource.get_abstract().replaceAll("\\s+", "");
			String thisAbstract = source.get_abstract().replaceAll("\\s+", "");
			if (!oldAbstract.equals(thisAbstract))
				changedFields.add("abstract");
		} else if (StringUtils.isEmpty(source.get_abstract()) && !StringUtils.isEmpty(oldSource.get_abstract())) {
			changedFields.add("abstract");
		} else if (!StringUtils.isEmpty(source.get_abstract()) && StringUtils.isEmpty(oldSource.get_abstract())) {
			changedFields.add("abstract");
		}

		// content
		if (StringUtils.isNoneBlank(source.getContent(), oldSource.getContent())
				&& !source.getContent().equals(oldSource.getContent())) {
			// INFO Removed all spaces, concatenated and matched
			String oldContent = oldSource.getContent().replaceAll("\\s+", "");
			String thisContent = source.getContent().replaceAll("\\s+", "");
			if (!oldContent.equals(thisContent))
				changedFields.add("content");
		} else if (StringUtils.isEmpty(source.getContent()) && !StringUtils.isEmpty(oldSource.getContent())) {
			changedFields.add("content");
		} else if (!StringUtils.isEmpty(source.getContent()) && StringUtils.isEmpty(oldSource.getContent())) {
			changedFields.add("content");
		}

		/*
		 * notes notes[n].type notes[n].heading notes[n].content
		 */
		List<Note> noteList = source.getNotes();
		List<Note> oldNoteList = oldSource.getNotes();
		if (!CollectionUtils.isEmpty(noteList)) {
			if (!CollectionUtils.isEmpty(oldNoteList)) {
				if (noteList.size() != oldNoteList.size()) {
					changedFields.add("notes");
				} else {
					for (int noteIndex = 0; noteIndex < noteList.size(); noteIndex++) {
						Note note = noteList.get(noteIndex);
						Note oldNote = oldNoteList.get(noteIndex);
						if (oldNote != null && note != null) {
							try {
								if ((null == note.getType() && null != oldNote.getType())
										|| (null != note.getType() && !note.getType().equals(oldNote.getType())))
									changedFields.add("notes.type");
								if ((null == note.getHeading() && null != oldNote.getHeading())
										|| (null != note.getHeading()
												&& !note.getHeading().equals(oldNote.getHeading())))
									changedFields.add("notes.heading");
								if ((null == note.getContent() && null != oldNote.getContent())
										|| (null != note.getContent()
												&& !note.getContent().equals(oldNote.getContent())))
									changedFields.add("notes.content");
							} catch (Exception exception) {
								// TODO handle NPE above
								// handled above. If one of the field of note is null and respective field for
								// other note is not null, then changed fields is added
							}
						}
					}
				}
			} else {
				changedFields.add("notes");
			}
		} else if (!CollectionUtils.isEmpty(oldNoteList)) {
			changedFields.add("notes");
		}

		// dates.effective
		if (source.getDates() != null && oldSource.getDates() != null) {
			if ((null != source.getDates().getEffective() && null != oldSource.getDates().getEffective())
					&& !source.getDates().getEffective().equals(oldSource.getDates().getEffective())) {
				changedFields.add("dates.effective");
			} else if (StringUtils.isEmpty(source.getDates().getEffective()) && !StringUtils.isEmpty(oldSource.getDates().getEffective())) {
				changedFields.add("dates.effective");
			} else if (!StringUtils.isEmpty(source.getDates().getEffective()) && StringUtils.isEmpty(oldSource.getDates().getEffective())) {
				changedFields.add("dates.effective");
			}

			// dates.expire
			if ((null != source.getDates().getExpire() && null != oldSource.getDates().getExpire())
					&& !source.getDates().getExpire().equals(oldSource.getDates().getExpire())) {
				changedFields.add("dates.expire");
			} else if (StringUtils.isEmpty(source.getDates().getExpire()) && !StringUtils.isEmpty(oldSource.getDates().getExpire())) {
				changedFields.add("dates.expire");
			} else if (!StringUtils.isEmpty(source.getDates().getExpire()) && StringUtils.isEmpty(oldSource.getDates().getExpire())) {
				changedFields.add("dates.expire");
			}

			// dates.published
			if ((null != source.getDates().getPublished() && null != oldSource.getDates().getPublished())
					&& !source.getDates().getPublished().equals(oldSource.getDates().getPublished())) {
				changedFields.add("dates.published");
			} else if (StringUtils.isEmpty(source.getDates().getPublished()) && !StringUtils.isEmpty(oldSource.getDates().getPublished())) {
				changedFields.add("dates.published");
			} else if (!StringUtils.isEmpty(source.getDates().getPublished()) && StringUtils.isEmpty(oldSource.getDates().getPublished())) {
				changedFields.add("dates.published");
			}

			// dates.updated
			if ((null != source.getDates().getUpdated() && null != oldSource.getDates().getUpdated())
					&& !source.getDates().getUpdated().equals(oldSource.getDates().getUpdated())) {
				changedFields.add("dates.updated");
			} else if (StringUtils.isEmpty(source.getDates().getUpdated()) && !StringUtils.isEmpty(oldSource.getDates().getUpdated())) {
				changedFields.add("dates.updated");
			} else if (!StringUtils.isEmpty(source.getDates().getUpdated()) && StringUtils.isEmpty(oldSource.getDates().getUpdated())) {
				changedFields.add("dates.updated");
			}

			// dates.compliance
			if ((null != source.getDates().getCompliance() && null != oldSource.getDates().getCompliance())
					&& !source.getDates().getCompliance().equals(oldSource.getDates().getCompliance())) {
				changedFields.add("dates.compliance");
			} else if (StringUtils.isEmpty(source.getDates().getCompliance()) && !StringUtils.isEmpty(oldSource.getDates().getCompliance())) {
				changedFields.add("dates.compliance");
			} else if (!StringUtils.isEmpty(source.getDates().getCompliance()) && StringUtils.isEmpty(oldSource.getDates().getCompliance())) {
				changedFields.add("dates.compliance");
			}
		} else if (source.getDates() != null && oldSource.getDates() == null) {
			changedFields.add("dates");
		} else if (source.getDates() == null && oldSource.getDates() != null) {
			changedFields.add("dates");
		}

		// linkages[n].text
		List<Linkage> linkages = source.getLinkages();
		List<Linkage> oldLinkages = oldSource.getLinkages();
		if (!CollectionUtils.isEmpty(linkages)) {
			if (!CollectionUtils.isEmpty(oldLinkages)) {
				if (linkages.size() != oldLinkages.size()) {
					changedFields.add("linkages.text");
				} else {
					Iterator<Linkage> linkageIterator = linkages.iterator();
					Iterator<Linkage> oldLinkagesIterator = oldLinkages.iterator();

					while (linkageIterator.hasNext() && oldLinkagesIterator.hasNext()) {
						Linkage linkage = linkageIterator.next();
						Linkage oldLinkage = oldLinkagesIterator.next();
						if ((null == linkage.getText() && null != oldLinkage.getText())
								|| (null != linkage.getText() && !linkage.getText().equals(oldLinkage.getText())))
							changedFields.add("linkages.text");
					}
				}
			} else {
				changedFields.add("linkages.text");
			}
		} else if (!CollectionUtils.isEmpty(oldLinkages)) {
			changedFields.add("linkages.text");
		}

		/*
		 * orgs orgs[n].name orgs[n].aliases orgs[n].type
		 */
		List<Org> orgs = source.getOrgs();
		List<Org> oldOrg = oldSource.getOrgs();
		if (!CollectionUtils.isEmpty(orgs)) {
			if (!CollectionUtils.isEmpty(oldOrg)) {
				if (orgs.size() != oldOrg.size()) {
					changedFields.add("orgs");
				} else {
					for (int org = 0; org < orgs.size(); org++) {
						if ((null == orgs.get(org).getName() && null != oldOrg.get(org).getName())
								|| (null != orgs.get(org).getName()
										&& !orgs.get(org).getName().equals(oldOrg.get(org).getName())))
							changedFields.add("orgs.name");
						if ((null == orgs.get(org).getAliases() && null != oldOrg.get(org).getAliases())
								|| (null != orgs.get(org).getAliases()
										&& !orgs.get(org).getAliases().equals(oldOrg.get(org).getAliases())))
							changedFields.add("orgs.aliases");
						if ((null == orgs.get(org).getType() && null != oldOrg.get(org).getType())
								|| (null != orgs.get(org).getType()
										&& !orgs.get(org).getType().equals(oldOrg.get(org).getType())))
							changedFields.add("orgs.type");
					}
				}
			} else {
				changedFields.add("orgs");
			}
		} else if (!CollectionUtils.isEmpty(oldOrg)) {
			changedFields.add("orgs");
		}

		// topics
		List<String> topics = source.getTopics();
		List<String> oldTopics = oldSource.getTopics();
		if (!CollectionUtils.isEmpty(topics)) {
			if (!CollectionUtils.isEmpty(oldTopics)) {
				if (topics.size() != oldTopics.size()) {
					changedFields.add("topics");
				} else {
					for (int topic = 0; topic < topics.size(); topic++) {
						if ((null == topics.get(topic) && null != oldTopics.get(topic))
								|| (null != topics.get(topic) && !topics.get(topic).equals(oldTopics.get(topic))))
							changedFields.add("topics");
					}
				}
			} else {
				changedFields.add("topics");
			}
		} else if (!CollectionUtils.isEmpty(oldTopics)) {
			changedFields.add("topics");
		}

		/*
		 * geographies geographies[n].name geographies[n].aliases
		 */
		List<Geography> geographies = source.getGeographies();
		List<Geography> oldGeographies = oldSource.getGeographies();
		if (!CollectionUtils.isEmpty(geographies)) {
			if (!CollectionUtils.isEmpty(oldGeographies)) {
				if (geographies.size() != oldGeographies.size()) {
					changedFields.add("geographies");
				} else {
					for (int geography = 0; geography < orgs.size(); geography++) {
						if ((null == geographies.get(geography).getName()
								&& null != oldGeographies.get(geography).getName())
								|| (null != geographies.get(geography).getName() && !geographies.get(geography)
										.getName().equals(oldGeographies.get(geography).getName())))
							changedFields.add("geographies.name");
						if ((null == geographies.get(geography).getAliases()
								&& null != oldGeographies.get(geography).getAliases())
								|| (null != geographies.get(geography).getAliases() && !geographies.get(geography)
										.getAliases().equals(oldGeographies.get(geography).getAliases())))
							changedFields.add("geographies.aliases");
					}
				}
			} else {
				changedFields.add("geographies");
			}
		} else if (!CollectionUtils.isEmpty(oldGeographies)) {
			changedFields.add("geographies");
		}

		// priority1content
		if (StringUtils.isNoneBlank(source.getPriority1content(), oldSource.getPriority1content())
				&& !source.getPriority1content().equals(oldSource.getPriority1content())) {
			changedFields.add("priority1content");
		} else if (StringUtils.isEmpty(source.getPriority1content()) && !StringUtils.isEmpty(oldSource.getPriority1content())) {
			changedFields.add("priority1content");
		} else if (!StringUtils.isEmpty(source.getPriority1content()) && StringUtils.isEmpty(oldSource.getPriority1content())) {
			changedFields.add("priority1content");
		}

		// priority2Content
		if (StringUtils.isNoneBlank(source.getPriority2Content(), oldSource.getPriority2Content())
				&& !source.getPriority2Content().equals(oldSource.getPriority2Content())) {
			changedFields.add("priority2Content");
		} else if (StringUtils.isEmpty(source.getPriority2Content()) && !StringUtils.isEmpty(oldSource.getPriority2Content())) {
			changedFields.add("priority2Content");
		} else if (!StringUtils.isEmpty(source.getPriority2Content()) && StringUtils.isEmpty(oldSource.getPriority2Content())) {
			changedFields.add("priority2Content");
		}

		// priority3Content
		if (StringUtils.isNoneBlank(source.getPriority3Content(), oldSource.getPriority3Content())
				&& !source.getPriority3Content().equals(oldSource.getPriority3Content())) {
			changedFields.add("priority3Content");
		} else if (StringUtils.isEmpty(source.getPriority3Content()) && !StringUtils.isEmpty(oldSource.getPriority3Content())) {
			changedFields.add("priority3Content");
		} else if (!StringUtils.isEmpty(source.getPriority3Content()) && StringUtils.isEmpty(oldSource.getPriority3Content())) {
			changedFields.add("priority3Content");
		}

		// originalText
		if (StringUtils.isNoneBlank(source.getOriginalText(), oldSource.getOriginalText())
				&& !source.getOriginalText().equals(oldSource.getOriginalText())) {
			changedFields.add("originalText");
		} else if (StringUtils.isEmpty(source.getOriginalText()) && !StringUtils.isEmpty(oldSource.getOriginalText())) {
			changedFields.add("originalText");
		} else if (!StringUtils.isEmpty(source.getOriginalText()) && StringUtils.isEmpty(oldSource.getOriginalText())) {
			changedFields.add("originalText");
		}

		if (granule.getSubGranules() != null && oldGranule.getSubGranules() != null
				&& granule.getSubGranules().size() != oldGranule.getSubGranules().size()) {
			changedFields.add("subGranules");
		}

		if (changedFields.size() > 0)
			source.setChangedFields(new ArrayList<>(changedFields));
	}

}
