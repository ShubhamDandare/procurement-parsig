package com.kpmg.rcm.sourcing.russia.paralegal.parse;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kpmg.rcm.sourcing.common.config.properties.FilePathProperties;
import com.kpmg.rcm.sourcing.common.json.dto.Citation;
import com.kpmg.rcm.sourcing.common.json.dto.Country;
import com.kpmg.rcm.sourcing.common.json.dto.Dates;
import com.kpmg.rcm.sourcing.common.json.dto.Granule;
import com.kpmg.rcm.sourcing.common.json.dto.Language;
import com.kpmg.rcm.sourcing.common.json.dto.Linkage;
import com.kpmg.rcm.sourcing.common.json.dto.Linkage__1;
import com.kpmg.rcm.sourcing.common.json.dto.Note;
import com.kpmg.rcm.sourcing.common.json.dto.Source;
import com.kpmg.rcm.sourcing.common.json.dto.SubGranule;
import com.kpmg.rcm.sourcing.common.json.dto.Subcitation;
import com.kpmg.rcm.sourcing.common.json.dto.System;
import com.kpmg.rcm.sourcing.common.util.SecurityUtils;
import com.kpmg.rcm.sourcing.russia.constants.RussiaSourceConstants;
import com.kpmg.rcm.sourcing.russia.paralegal.service.ParaLegalService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CommonParser {

	@Autowired
	protected FilePathProperties filePathProperties;

	private static SimpleDateFormat granuleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss:SSS'Z'");

	private static SimpleDateFormat filePathDateFormat = new SimpleDateFormat("yyyy-MM-dd");

	public void getGranule(File file, String sourceUrl, String[] indexPageContents, String docType, String dirPath,
			Integer sourceId, Boolean gsApplicable) throws Exception {

		List<SubGranule> subGranuleList = new ArrayList<SubGranule>();
		List<Granule> granuleList = new ArrayList<Granule>();
		String[] filePart = file.getName().split("_");
		sourceUrl = sourceUrl + "&" + filePart[2].substring(0, 3) + "="
				+ filePart[2].substring(3, filePart[2].lastIndexOf("."));
		String version = filePart[1].substring(1, 11);
		String key = RussiaSourceConstants.paralegalCommonKeyPrefix + version + "/" + indexPageContents[2];

		System system = System.builder().id(key).version(version).jurisdiction("RUSSIA").sourceName("PARALEGAL")
				.commonId(RussiaSourceConstants.paralegalCommonKeyPrefix + indexPageContents[2]).sourceUrl(sourceUrl)
				.docType(indexPageContents[1]).build();

		String formattedTime = granuleDateFormat.format(new Date().getTime());

		Map docDetails = validator(file, subGranuleList, system, dirPath);
		if (docDetails == null) {
			ParaLegalService.storeUnparseableDocPaths(file.getAbsolutePath() + "_" + docType.split("\\.")[0] + "|"
					+ indexPageContents[1] + " от " + indexPageContents[4] + " № " + indexPageContents[0], dirPath);
			return;
		}
		if (gsApplicable != null && gsApplicable.equals(true))
			dirPath = dirPath + "GSApplicableJsons";

		String directoryName = dirPath + File.separator + docType.split("_")[0] + "JsonFiles" + File.separator
				+ (docDetails.get("parserType") != null ? docDetails.get("parserType").toString() : "default")
				+ "JsonFiles";

		Citation citation = ((Citation) docDetails.get("citation"));
		List<String> aliasesList = (citation != null && citation.getAliases() != null) ? citation.getAliases()
				: new ArrayList<>();
		if (citation == null)
			citation = Citation.builder().build();
		citation.setPrimary(indexPageContents[2] != null ? indexPageContents[2] : RussiaSourceConstants.emptyString);
		if (aliasesList != null) {
			aliasesList.add(indexPageContents[0]);
			citation.setAliases(aliasesList);
		}
		List<Note> notes = new ArrayList<>();
		Note docNote = ((Note) docDetails.get("Notes"));
		notes.add(docNote);
		Dates dates = ((Dates) docDetails.get("date"));
		
		dates.setPublished(indexPageContents[3]);
		Source source = Source.builder()
				.heading(docDetails.get("heading") != null ? docDetails.get("heading").toString() : null)
				.citation(citation).notes(notes).dates(dates)
				._abstract(docDetails.get("abstract") != null ? docDetails.get("abstract").toString() : null)
				.language(Language.builder().code("RU").name("RUSSIAN").build())
				.country(Country.builder().code("RU").name("RUSSIA").build()).inForce(true)
				.linkages((List<Linkage>) docDetails.get("granulelinkageList")).build();

		Granule granule = Granule.builder().id(SecurityUtils.SHA256Hash(key)).key(key).created(formattedTime)
				.lastModified(formattedTime).system(system).source(source)
				.subGranules((List<SubGranule>) docDetails.get("subGranuleList")).build();

		granuleList.add(granule);
		writeCMMJsonFile(granuleList,
				file.getName().substring(0, file.getName().lastIndexOf(".")) + "_" + docType.split("\\.")[0],
				directoryName, dirPath);
	}

	public static String getRegexValue(String regex, String val) {
		String regexValue = RussiaSourceConstants.emptyString;
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(val);
		if (matcher.find()) {
			regexValue = matcher.group(0);
		}
		return regexValue;
	}

	public static void writeCMMJsonFile(List<Granule> granules, String fileName, String directoryPath, String dirPath) {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			String cmmJson = objectMapper.writeValueAsString(granules).replace("[\\s\\n\\t]",
					RussiaSourceConstants.emptyString);
			if(cmmJson.contains("null"))
				directoryPath=directoryPath+"/TobeRectifiedJsons/";
			createFolderIfNotExists(directoryPath.toLowerCase(Locale.ROOT));
			String filePath = directoryPath + File.separator + fileName + ".json";
			FileWriter fileWriter = new FileWriter(new File(filePath.toLowerCase(Locale.ROOT)));
			fileWriter.write(cmmJson);
			fileWriter.close();
			log.error("Successfully write the file :: " + fileName);
			// storeParseableDocPaths(fileName, dirPath);
		} catch (IOException e) {
			log.error("Error while creating the CMM json file", e);
		}

	}

	public static void createFolderIfNotExists(String directoryPath) {
		File folder = new File(directoryPath);
		if (!folder.exists())
			folder.mkdirs();
	}

	private String convertDatesEffectiveToVersion(String datesEffective) {
		DateFormat format1 = new SimpleDateFormat("dd MMMMM yyyy", Locale.FRENCH);
		Date date;
		String dateString = RussiaSourceConstants.emptyString;
		try {
			date = format1.parse(datesEffective);
			DateFormat format2 = new SimpleDateFormat("yyyy-MM-dd");
			dateString = format2.format(date);
		} catch (ParseException e) {
			log.error("Error while creating convertDates ", e);
		}
		return dateString;
	}

	public Map validator(File file, List<SubGranule> subGranuleList, System system, String dirPath) {
		Map docDetails = null;
		String typeTag = RussiaSourceConstants.emptyString;
		Document doc = getJsoupDocumentFromFile(file, dirPath);
//		if (file.getName().equals("102072376_v92 - от 29.12.2020_rdk92.html")) {
//			log.error("For this temp file only");
//		}
		int classT = 0, classC = 0, classI = 0, classED = 0, classW4 = 0, classM = 0, classH = 0;
		for (Element e : doc.select("p")) {
			if (e.hasClass("T")) {
				classT++;
			} else if (e.hasClass("C")) {
				classC++;
			} else if (e.hasClass("I")) {
				classI++;
			} else if (e.hasClass("ed")) {
				classED++;
			} else if (e.children().hasClass("W4")) {
				classW4++;
			} else if (e.hasClass("M")) {
				classM++;
			} else if (e.hasClass("H")) {
				classH++;
			}
		}
		if (classH > 0) {
			typeTag = "pTagClassH";
//			docDetails = pTagClassHParser(doc, subGranuleList, system);
			docDetails = commonParser(doc, subGranuleList, system);
		} else if (classC == 1 && classT == 2 && classI == 2 && classW4 > 0) {
			typeTag = "pTag";
//			docDetails = parser1(doc, subGranuleList, system, file);
			docDetails = commonParser(doc, subGranuleList, system);
		} else if (classC == 1 && classT == 2 && classI == 6 && classW4 == 0) {
			typeTag = "pTag";
//			docDetails = parser3(doc, subGranuleList, system);
			docDetails = commonParser(doc, subGranuleList, system);
		} else

		if (typeTag.equals(RussiaSourceConstants.emptyString)) {
			typeTag = "spanTag";
			Elements spans = doc.select("span");

			if (spans.size() > 1) {
//				docDetails = parser2(doc, subGranuleList, system);
				docDetails = commonParser(doc, subGranuleList, system);
			}
		}
		return docDetails;
	}

	public Map parser1(Document doc, List<SubGranule> subGranuleList, System system, File file) {

		Map docDetails = new HashMap();
		docDetails.put("subGranuleList", subGranuleList);
		List<Linkage> granulelinkageList = null;
		List<Linkage__1> subgranulelinkageList = null;
		String heading = RussiaSourceConstants.emptyString;
		String note = RussiaSourceConstants.emptyString;
		String dates = RussiaSourceConstants.emptyString;
		String _abstract = RussiaSourceConstants.emptyString;
		String subgranual = RussiaSourceConstants.emptyString;
		String subgranualMetaData = RussiaSourceConstants.emptyString;
		boolean isArticleContinued = false;
		ArrayList<String> aliase = new ArrayList<>();
		int size = doc.select("p").size();
		int counter = 0;
		for (Element e : doc.select("p")) {

			isArticleContinued = identifyAndProduceSubgranules(e, docDetails, system);

			if (e.hasClass("T")) {
				heading += e.text() + RussiaSourceConstants.newLineCharacter;
				granulelinkageList = getLinkagesParser(e, granulelinkageList);
			} else if (e.hasClass("C")) {
				heading += e.text() + RussiaSourceConstants.newLineCharacter;
				granulelinkageList = getLinkagesParser(e, granulelinkageList);
			} else if (counter == size - 5) {
				isArticleContinued = false;
				note += e.text();
				granulelinkageList = getLinkagesParser(e, granulelinkageList);
			} else if (counter == size - 2) {
				dates = e.text();
				granulelinkageList = getLinkagesParser(e, granulelinkageList);
			} else if (counter == size - 1) {
				aliase.add(e.text().replace(RussiaSourceConstants.newLineCharacter, RussiaSourceConstants.emptyString)
						.replace(RussiaSourceConstants.backSlashQuote, RussiaSourceConstants.emptyString));
				granulelinkageList = getLinkagesParser(e, granulelinkageList);
			} else if (e.hasClass("I")) {// || counter == size - 8
				isArticleContinued = false;
				_abstract = e.text() + RussiaSourceConstants.newLineCharacter;
				granulelinkageList = getLinkagesParser(e, granulelinkageList);
			}
			counter++;
		}
		saveSubGranule(docDetails, system);
		Citation citation = Citation.builder().aliases(aliase).build();
		Dates date = Dates.builder()
				.updated(dates.replace(RussiaSourceConstants.newLineCharacter, RussiaSourceConstants.emptyString)
						.replace(RussiaSourceConstants.backSlashQuote, RussiaSourceConstants.emptyString))
				.build();
		Note notes = Note.builder()
				.content(note.replace(RussiaSourceConstants.newLineCharacter, RussiaSourceConstants.emptyString)
						.replace(RussiaSourceConstants.backSlashQuote, RussiaSourceConstants.emptyString))
				.build();
		docDetails.put("heading",
				heading.replace(RussiaSourceConstants.newLineCharacter, RussiaSourceConstants.emptyString)
						.replace(RussiaSourceConstants.backSlashQuote, RussiaSourceConstants.emptyString));
		docDetails.put("abstract",
				_abstract.replace(RussiaSourceConstants.newLineCharacter, RussiaSourceConstants.emptyString)
						.replace(RussiaSourceConstants.backSlashQuote, RussiaSourceConstants.emptyString));
		docDetails.put("Notes", notes);
		docDetails.put("date", date);
		docDetails.put("citation", citation);
		docDetails.put("granulelinkageList", granulelinkageList);
		if (heading != null && !heading.equals(RussiaSourceConstants.emptyString) && dates != null
				&& !dates.equals(RussiaSourceConstants.emptyString) && note != null
				&& !note.equals(RussiaSourceConstants.emptyString) && _abstract != null
				&& !_abstract.equals(RussiaSourceConstants.emptyString))
			docDetails.put("parserType", "parser1");
		return docDetails;
	}

	public Map parser2(Document doc, List<SubGranule> subGranuleList, System system) {
		Map<String, Object> docDetails = new LinkedHashMap();
		List<Linkage> granulelinkageList = null;
		Elements spans = doc.select("span");
		boolean isArticleContinued = false;
		boolean isSubArticleContinued = false;
		Pattern pattern1 = Pattern.compile("Статья\\s\\d\\.(.*)", Pattern.DOTALL | Pattern.UNICODE_CASE);
		Pattern pattern2 = Pattern.compile("\\d\\.\\s(.*)", Pattern.DOTALL | Pattern.UNICODE_CASE);
		String article = RussiaSourceConstants.emptyString;
		String subArticle = RussiaSourceConstants.emptyString;
		String articleMetadata = RussiaSourceConstants.emptyString;
		String subArticleMetadata = RussiaSourceConstants.emptyString;
		String note = RussiaSourceConstants.emptyString;
		String dates = RussiaSourceConstants.emptyString;
		ArrayList<String> aliase = new ArrayList<>();

		int size = doc.select("span").size();
		int counter = 0;
		String heading = RussiaSourceConstants.emptyString;
		boolean headingComplete = false;
		for (Element e : spans) {
			Matcher matcher1 = pattern1.matcher(e.text());
			Matcher matcher2 = pattern2.matcher(e.text());
			if (matcher1.find()) {
				headingComplete = true;
				article = matcher1.group(1) != null
						? matcher1.group(1)
								.replace(RussiaSourceConstants.newLineCharacter, RussiaSourceConstants.emptyString)
								.replace(RussiaSourceConstants.backSlashQuote, RussiaSourceConstants.emptyString)
						: RussiaSourceConstants.emptyString;
				articleMetadata = matcher1.group(0).split("\\.")[0]
						.replace(RussiaSourceConstants.newLineCharacter, RussiaSourceConstants.emptyString)
						.replace(RussiaSourceConstants.backSlashQuote, RussiaSourceConstants.emptyString);
				isArticleContinued = true;
				docDetails.put(articleMetadata, article);
				List<Linkage__1> subgranulelinkageList = getLinkagesParser(e);
				docDetails.put(articleMetadata + "~Linkages", subgranulelinkageList);
			} else if (matcher2.find()) {
				headingComplete = true;
				if (isSubArticleContinued) {
					String childId = system.getCommonId() + "/" + articleMetadata + "_" + subArticleMetadata;
					if (docDetails.containsKey(articleMetadata + "_childIdList")) {
						List<String> childIdList = (List<String>) docDetails.get(articleMetadata + "_childIdList");
						if (!childIdList.contains(childId))
							childIdList.add(childId);
						docDetails.put(articleMetadata + "_childIdList", childIdList);
					} else {
						List<String> childIdList = new ArrayList<>();
						childIdList.add(childId);
						docDetails.put(articleMetadata + "_childIdList", childIdList);
					}
				}
				subArticleMetadata = matcher2.group(0).split("\\.")[0]
						.replace(RussiaSourceConstants.newLineCharacter, RussiaSourceConstants.emptyString)
						.replace(RussiaSourceConstants.backSlashQuote, RussiaSourceConstants.emptyString);
				subArticle = matcher2.group(1) != null
						? matcher2.group(1)
								.replace(RussiaSourceConstants.newLineCharacter, RussiaSourceConstants.emptyString)
								.replace(RussiaSourceConstants.backSlashQuote, RussiaSourceConstants.emptyString)
						: RussiaSourceConstants.emptyString;
				isSubArticleContinued = true;
				isArticleContinued = false;
				docDetails.put(articleMetadata + "#" + subArticleMetadata, subArticle);
				List<Linkage__1> subgranulelinkageList = getLinkagesParser(e);
				docDetails.put(articleMetadata + "#" + subArticleMetadata + "~Linkages", subgranulelinkageList);
			} else if (!isArticleContinued && !isSubArticleContinued && !headingComplete) {
				heading += e.text();
			} else if (counter == size - 4) {
				isSubArticleContinued = false;
				isArticleContinued = false;
				note += e.text().replace("[\\n\\t]", RussiaSourceConstants.emptyString)
						.replace(RussiaSourceConstants.newLineCharacter, RussiaSourceConstants.emptyString)
						.replace(RussiaSourceConstants.backSlashQuote, RussiaSourceConstants.emptyString) + " ";
			} else if (counter == size - 3) {
				note += e.text().replace("[\\n\\t]", RussiaSourceConstants.emptyString)
						.replace(RussiaSourceConstants.newLineCharacter, RussiaSourceConstants.emptyString)
						.replace(RussiaSourceConstants.backSlashQuote, RussiaSourceConstants.emptyString);
				granulelinkageList = getLinkagesParser(e, granulelinkageList);
			} else if (counter == size - 2) {
				dates = e.text().replace(RussiaSourceConstants.newLineCharacter, RussiaSourceConstants.emptyString)
						.replace(RussiaSourceConstants.backSlashQuote, RussiaSourceConstants.emptyString);
				granulelinkageList = getLinkagesParser(e, granulelinkageList);
			} else if (counter == size - 1) {
				aliase.add(e.text().replace(RussiaSourceConstants.newLineCharacter, RussiaSourceConstants.emptyString)
						.replace(RussiaSourceConstants.backSlashQuote, RussiaSourceConstants.emptyString));
				granulelinkageList = getLinkagesParser(e, granulelinkageList);
			} else if (isArticleContinued) {
				docDetails.put(articleMetadata,
						(docDetails.get(articleMetadata) != null ? docDetails.get(articleMetadata)
								: RussiaSourceConstants.emptyString)
								+ e.text()
										.replace(RussiaSourceConstants.newLineCharacter,
												RussiaSourceConstants.emptyString)
										.replace(RussiaSourceConstants.backSlashQuote,
												RussiaSourceConstants.emptyString));
				updateLinkagesInConnectingSubGranule(e, articleMetadata, docDetails);
			} else if (isSubArticleContinued) {

				docDetails.put(articleMetadata + "#" + subArticleMetadata,
						(docDetails.get(subArticleMetadata) != null ? docDetails.get(subArticleMetadata)
								: RussiaSourceConstants.emptyString)
								+ e.text()
										.replace(RussiaSourceConstants.newLineCharacter,
												RussiaSourceConstants.emptyString)
										.replace(RussiaSourceConstants.backSlashQuote,
												RussiaSourceConstants.emptyString));
				updateLinkagesInConnectingSubGranule(e, articleMetadata + "#" + subArticleMetadata, docDetails);
				subArticle = RussiaSourceConstants.emptyString;
				subArticleMetadata = RussiaSourceConstants.emptyString;
			}
			counter++;

		}
		Citation citation = Citation.builder().aliases(aliase).build();
		Dates date = Dates.builder().updated(dates).build();
		Note notes = Note.builder().content(note).build();

		for (Map.Entry parsedDocEntry : docDetails.entrySet()) {
			if (parsedDocEntry.getKey().toString().contains("Linkages")
					|| parsedDocEntry.getKey().toString().contains("childIdList"))
				continue;
			saveSubGranule(subGranuleList, parsedDocEntry.getValue().toString(), parsedDocEntry.getKey().toString(),
					system, docDetails);
		}
		docDetails.put("heading",
				heading.replace(RussiaSourceConstants.newLineCharacter, RussiaSourceConstants.emptyString));
		docDetails.put("Notes", notes);
		docDetails.put("date", date);
		docDetails.put("citation", citation);
		docDetails.put("subGranuleList", subGranuleList);

		if (heading != null && !heading.equals(RussiaSourceConstants.emptyString) && dates != null
				&& !dates.equals(RussiaSourceConstants.emptyString) && note != null
				&& !note.equals(RussiaSourceConstants.emptyString))
			docDetails.put("parserType", "parser2");

		return docDetails;
	}

	public Map parser3(Document doc, List<SubGranule> subGranuleList, System system) {
		Map<String, Object> docDetails = null;
		try {
			Elements paras = doc.select("p");
			int size = doc.select("p").size();
			docDetails = new HashMap();
			Boolean isArticleContinued = false;
			Boolean isSubArticleContinued = false;
			Boolean isHeadingStarted = false;
			Pattern pattern1 = Pattern.compile("^Статья\\s\\d\\.*(.*)", Pattern.DOTALL | Pattern.UNICODE_CASE);
			Pattern pattern2 = Pattern.compile("^\\d\\.*\\)*\\s(.*)", Pattern.DOTALL | Pattern.UNICODE_CASE);
			Pattern pattern3 = Pattern.compile("Президент Российской Федерации", Pattern.DOTALL | Pattern.UNICODE_CASE);
			String article = RussiaSourceConstants.emptyString;
			String subArticle = RussiaSourceConstants.emptyString;
			String articleMetadata = RussiaSourceConstants.emptyString;
			String subArticleMetadata = RussiaSourceConstants.emptyString;
			String heading = RussiaSourceConstants.emptyString;
			String _abstract = RussiaSourceConstants.emptyString;
			String note = RussiaSourceConstants.emptyString;
			String dates = RussiaSourceConstants.emptyString;
			ArrayList<String> aliase = new ArrayList<>();
			int counter = 0;
			for (Element e : paras) {
				Matcher matcher1 = pattern1.matcher(e.text());
				Matcher matcher2 = pattern2.matcher(e.text());
				Matcher matcher3 = pattern3.matcher(e.text());
				if (e.hasClass("T")) {
					isHeadingStarted = true;
					heading += e.text() + RussiaSourceConstants.newLineCharacter;
				} else if (e.hasClass("C")) {
					heading += e.text() + RussiaSourceConstants.newLineCharacter;
				} else if (counter == size - 6 || counter == size - 4) {
					note += e.text();
				} else if (matcher1.find()) {
					populateArticleData(isHeadingStarted, isArticleContinued, matcher1, articleMetadata, docDetails, e);
				} else if (matcher2.find()) {
					if (articleMetadata != null || !articleMetadata.equals(RussiaSourceConstants.emptyString))
						populateArticleData(isHeadingStarted, isArticleContinued, matcher1, articleMetadata, docDetails,
								e);
					if (isSubArticleContinued) {
						String childId = system.getCommonId() + "/" + articleMetadata + "_" + subArticleMetadata;
						putChildIdInParentGranule(docDetails, articleMetadata, childId);
					}
					subArticleMetadata = matcher2.group(0).split("\\.")[0];
					subArticle = matcher2.group(1);
					isSubArticleContinued = true;
					isArticleContinued = false;
					docDetails.put(articleMetadata + "#" + subArticleMetadata, subArticle);
					List<Linkage__1> subgranulelinkageList = getLinkagesParser(e);
					docDetails.put(articleMetadata + "#" + subArticleMetadata + "~Linkages", subgranulelinkageList);
				} else if (matcher3.find()) {
					break;
				} else if (isArticleContinued) {
					docDetails.put(articleMetadata,
							(docDetails.get(articleMetadata) != null ? docDetails.get(articleMetadata)
									: RussiaSourceConstants.emptyString) + e.text());
					updateLinkagesInConnectingSubGranule(e, articleMetadata, docDetails);
				} else if (isSubArticleContinued) {

					docDetails.put(articleMetadata + "#" + subArticleMetadata,
							(docDetails.get(articleMetadata + "#" + subArticleMetadata) != null
									? docDetails.get(articleMetadata + "#" + subArticleMetadata)
									: RussiaSourceConstants.emptyString) + e.text());
					isSubArticleContinued = false;
					subArticle = RussiaSourceConstants.emptyString;
					subArticleMetadata = RussiaSourceConstants.emptyString;
					updateLinkagesInConnectingSubGranule(e, articleMetadata + "#" + subArticleMetadata, docDetails);
				} else if (counter == size - 3) {
					dates = e.text();
				} else if (counter == size - 2) {
					aliase.add(e.text());
				} else if (isHeadingStarted) {
					heading += e.text() + RussiaSourceConstants.newLineCharacter;
				}
				counter++;
			}
			Citation citation = Citation.builder().aliases(aliase).build();
			Note notes = Note.builder().content(note).build();
			Dates date = Dates.builder().updated(dates).build();

			for (Map.Entry parsedDocEntry : docDetails.entrySet()) {
				if (parsedDocEntry.getKey().toString().contains("Linkages")
						|| parsedDocEntry.getKey().toString().contains("childIdList"))
					continue;
				saveSubGranule(subGranuleList, parsedDocEntry.getValue().toString(), parsedDocEntry.getKey().toString(),
						system, docDetails);
			}
			docDetails.put("heading", heading);
			docDetails.put("abstract", _abstract);
			docDetails.put("Notes", notes);
			docDetails.put("date", date);
			docDetails.put("citation", citation);
			docDetails.put("subGranuleList", subGranuleList);
			if (heading != null && !heading.equals(RussiaSourceConstants.emptyString) && note != null
					&& !note.equals(RussiaSourceConstants.emptyString))
				docDetails.put("parserType", "parser3");
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return docDetails;
	}

	public void updateLinkagesInConnectingSubGranule(Element e, String articleMetadata, Map docDetails) {
		List<Linkage__1> contSubgranulelinkageList = getLinkagesParser(e);
		List<Linkage__1> subgranulelinkageList = docDetails.get(articleMetadata + "~Linkages") != null
				? (List<Linkage__1>) docDetails.get(articleMetadata + "~Linkages")
				: null;
		if (subgranulelinkageList != null && contSubgranulelinkageList != null) {
			subgranulelinkageList.addAll(contSubgranulelinkageList);
		} else {
			subgranulelinkageList = contSubgranulelinkageList;
		}
		docDetails.put(articleMetadata + "~Linkages", subgranulelinkageList);
	}

	public void updateLinkagesInGranule(Element e, Map docDetails) {
		List<Linkage> granulelinkageList = docDetails.get("granulelinkageList") != null
				? (List<Linkage>) docDetails.get("granulelinkageList")
				: new ArrayList<>();
		granulelinkageList = getLinkagesParser(e, granulelinkageList);
		if (granulelinkageList != null && !granulelinkageList.isEmpty()) {
			docDetails.put("granulelinkageList", granulelinkageList);
		} else {
			docDetails.put("granulelinkageList", null);
		}
	}

	public void putChildIdInParentGranule(Map docDetails, String articleMetadata, String childId) {
		childId=childId.replace("#", "_");
		articleMetadata=articleMetadata.replace("#", "_");
		if (docDetails.containsKey(articleMetadata + "_childIdList")) {
			List<String> childIdList = (List<String>) docDetails.get(articleMetadata + "_childIdList");
			if (!childIdList.contains(childId))
				childIdList.add(childId);
			docDetails.put(articleMetadata + "_childIdList", childIdList);
		} else {
			List<String> childIdList = new ArrayList<>();
			childIdList.add(childId);
			docDetails.put(articleMetadata + "_childIdList", childIdList);
		}
	}

	public void populateArticleData(Boolean isHeadingStarted, Boolean isArticleContinued, Matcher matcher1,
			String articleMetadata, Map docDetails, Element e) {
		isHeadingStarted = false;
		articleMetadata = matcher1.group(0).split("\\.")[0];
		isArticleContinued = true;
		docDetails.put(articleMetadata, matcher1.group(1));
		List<Linkage__1> subgranulelinkageList = getLinkagesParser(e);
		docDetails.put(articleMetadata + "~Linkages", subgranulelinkageList);
	}

	Elements getLoopingHook(Document document) {
		if(document.select("p").hasClass("M")) {
			return documentMPreProcessor(document.getElementsByClass("M")).first().children();
		}
		else if (!document.select("p").isEmpty()) {
			return document.select("p");
		} else if (!document.select("span").isEmpty()) {
			return document.select("span");
		}
		return null;
	}
	Elements documentMPreProcessor(Elements elements){
		Pattern ChapterTokenPattern=Pattern.compile("^(?!Статья)([1-9A-Za-z]+\\s*[\\)\\.])",Pattern.DOTALL| Pattern.UNICODE_CASE);
		Pattern subTokenPattern=Pattern.compile("^(?!Статья)([1-9A-Za-z]+\\s*[\\)\\.])",Pattern.DOTALL| Pattern.UNICODE_CASE);
		Pattern articleTokenPattern=Pattern.compile("(Статья\\s+\\d+-*\\d*\\s*\\.)",Pattern.DOTALL| Pattern.UNICODE_CASE);
		for (TextNode textNode : elements.first().textNodes()) {
			Matcher matcher101=articleTokenPattern.matcher(textNode.text());
			List<String> textTemp1 = Arrays.asList(textNode.text().split("(Статья\\s+\\d+-*\\d*\\s*\\.)"));//[1-9A-Za-z]+\\s*[\\)\\.]
			List<String>  textTemp2= new ArrayList<String>();
			List<String>  textTempFinal=new ArrayList<String>();	
			
//			if(textNode.text().contains("Статья")) {
//				System.out.println(textNode);
//			}  article dsfisdbfiudf article sadjnsadjksadjksad
			if(textTemp1.size()>1) {
				textTemp2.add(textTemp1.get(0));
				for(int i=1;i<textTemp1.size();i++){
					if(matcher101.find()) {
						String match=matcher101.group(0);
						textTemp2.add(match+textTemp1.get(i));
					}
				}
			}
			else {
				textTemp2=textTemp1;
			}
			for(String s:textTemp2) {
				Matcher matcher102=subTokenPattern.matcher(s);
//				System.out.println(s);
				List<String>temp=Arrays.asList(s.split("^(?!Статья)[1-9A-Za-z]+\\s*[\\)\\.]"));
				textTempFinal.add(temp.get(0));
				if(temp.size()>1) {
					for(int i=1;i<temp.size();i++){
						if(matcher102.find()) {
							String match=matcher102.group(1);
							textTempFinal.add(match+temp.get(i));
						}
					}
				}
			}	
			if(textTempFinal.size()>1) {
				for(int i=0;i<textTempFinal.size();i++){
						textNode.after("<span>"+textTempFinal.get(i)+ "</span>");
					}
			}
			else {
				textNode.after("<span>"+textNode.text() + "</span>");
			}
			textNode.remove();
			}
		return elements;
	}
	public Map commonParser(Document doc, List<SubGranule> subGranuleList, System system) {
		Elements loopingHook = getLoopingHook(doc);
		List<Linkage> granulelinkageList = null;
		Map<String, Object> docDetails = new HashMap();
		String articleMetadata = "";
		String subArticleMetadata = "";
		String subSubArticleMetadata = "";
		String chapterMetadata = "";
		String sectionMetadata = "";
		Citation citation = null;
		String note = "";
		String _abstract = "";
		boolean isHeadingContniued = true, isArticleContinued = false, isSubArticleContinued = false, // года
				isNoteStarted = false, isSubSubArticleContinued = false, isChapterContinued = false,
				isAbstractContinued = false, headingAboutToEnd = false, isSectionContinued = false;
		Pattern sectionPattern = Pattern.compile("РАЗДЕЛ\\s+.*\\.(.*)", Pattern.DOTALL | Pattern.UNICODE_CASE);
		Pattern sectionMetaDataPattern = Pattern.compile("(РАЗДЕЛ\\s+.*?)\\..*", Pattern.DOTALL | Pattern.UNICODE_CASE);
		Pattern chapterPattern = Pattern.compile("^ГЛАВА|Глава\\s+.*\\.(.*)", Pattern.DOTALL | Pattern.UNICODE_CASE);
		Pattern chapterMetaDataPattern = Pattern.compile("(ГЛАВА|Глава\\s+.*?)\\..*",
				Pattern.DOTALL | Pattern.UNICODE_CASE);
		Pattern articlePattern = Pattern.compile("Статья\\s+\\d+-*\\d*\\s*\\.*\\d*\\.*(.*)", // Статья\\s+\\d+-*\\d*\\s*\\.*\\s*(.*)
				Pattern.DOTALL | Pattern.UNICODE_CASE);
		Pattern articleMetaDataPattern = Pattern.compile("(Статья\\s+\\d+-*\\d*\\s*\\.*\\d*).*", // (Статья\\s+\\d+-*\\d*+)\\s*\\.*
				Pattern.DOTALL | Pattern.UNICODE_CASE);
		Pattern notePattern = Pattern.compile("(Председатель Верховного Совета|Президент Российской Федерации.*)",
				Pattern.DOTALL | Pattern.UNICODE_CASE);
		Pattern citationPattern = Pattern.compile(".*(№\\s*\\d*-.*)", Pattern.DOTALL | Pattern.UNICODE_CASE);
		Pattern genericSubPattern = Pattern.compile("^[\\dЁёА-я]+\\s*[\\)\\.](.*)", Pattern.DOTALL | Pattern.UNICODE_CASE);
		Pattern genericSubMetaDataPattern = Pattern.compile("^([\\dЁёА-я]+)\\s*[\\)\\.].*",
				Pattern.DOTALL | Pattern.UNICODE_CASE);
		Pattern bracketPattern = Pattern.compile("^\\d+\\s*(\\)).*", Pattern.DOTALL | Pattern.UNICODE_CASE);
		Pattern dotPattern = Pattern.compile("^\\d+\\s*(\\.).*", Pattern.DOTALL | Pattern.UNICODE_CASE);
		Pattern datePattern = Pattern.compile("(\\d.*\\d{4}\\s*года).*", Pattern.DOTALL | Pattern.UNICODE_CASE);
		String currentSubMatcher = "";
		String dateUpdated="";
		for (Element element : loopingHook) {
			if(element.text().contains("Обязанности юридического лица по раскрытию информации о своих бенефициарных владельцах")) {
				log.error("Superscript text "+element.text());
			}
			Matcher matcher1 = articlePattern.matcher(element.text());
			Matcher matcher2 = notePattern.matcher(element.text());
			Matcher matcher3 = citationPattern.matcher(element.text());
			Matcher matcher4 = articleMetaDataPattern.matcher(element.text());
			Matcher matcher5 = genericSubMetaDataPattern.matcher(element.text());
			Matcher matcher6 = genericSubPattern.matcher(element.text());
			Matcher matcher7 = bracketPattern.matcher(element.text());
			Matcher matcher8 = dotPattern.matcher(element.text());
			Matcher matcher9 = chapterPattern.matcher(element.text());
			Matcher matcher10 = chapterMetaDataPattern.matcher(element.text());
			Matcher matcher11 = sectionPattern.matcher(element.text());
			Matcher matcher12 = sectionMetaDataPattern.matcher(element.text());
			if (element.text().contains("года")) {
				headingAboutToEnd = true;
			} else if (headingAboutToEnd && !element.text().contains("года")) {
				isHeadingContniued = false;
				isAbstractContinued = true;
			}
			if (matcher11.find() && !isNoteStarted) {
				isSectionContinued = true;
				isChapterContinued = false;
				isHeadingContniued = false;
				isArticleContinued = false;
				isSubArticleContinued = false;
				isSubSubArticleContinued = false;
				isAbstractContinued = false;
				headingAboutToEnd = false;
				if (matcher12.find()) {
					sectionMetadata = matcher12.group(1);
					chapterMetadata=superScriptDifferentatedStr(element, chapterMetadata);
					docDetails.put(sectionMetadata, matcher11.group(1));
					updateLinkagesInConnectingSubGranule(element, sectionMetadata, docDetails);
				}
			} else if (matcher9.find() && !isNoteStarted) {
				isChapterContinued = true;
				isHeadingContniued = false;
				isArticleContinued = false;
				isSubArticleContinued = false;
				isSubSubArticleContinued = false;
				isAbstractContinued = false;
				headingAboutToEnd = false;
				isSectionContinued = false;
				if (matcher10.find()) {
					chapterMetadata = matcher10.group(1);
					chapterMetadata=superScriptDifferentatedStr(element, chapterMetadata);
					if (sectionMetadata != null && !sectionMetadata.equals(RussiaSourceConstants.emptyString)) {
						String childId = system.getCommonId() + "/" + sectionMetadata + "/" + chapterMetadata;
						putChildIdInParentGranule(docDetails, sectionMetadata, childId);
					}
					chapterMetadata = (sectionMetadata != null
							&& !sectionMetadata.equals(RussiaSourceConstants.emptyString))
									? sectionMetadata + "#" + sectionMetadata
									: chapterMetadata;
					docDetails.put(chapterMetadata, matcher9.group(1));
					updateLinkagesInConnectingSubGranule(element, chapterMetadata, docDetails);
				}
			} else if (matcher1.find() && !isNoteStarted) {
				isArticleContinued = true;
				isHeadingContniued = false;
				isChapterContinued = false;
				isSubArticleContinued = false;
				isSubSubArticleContinued = false;
				isAbstractContinued = false;
				headingAboutToEnd = false;
				currentSubMatcher = "";
				if (matcher4.find()) {
					articleMetadata = matcher4.group(1);
					articleMetadata=superScriptDifferentatedStr(element, articleMetadata);
					if (chapterMetadata != null && !chapterMetadata.equals(RussiaSourceConstants.emptyString)) {
						String childId = system.getCommonId() + "/" + chapterMetadata + "_" + articleMetadata;
						putChildIdInParentGranule(docDetails, chapterMetadata, childId);
					}
					articleMetadata = (chapterMetadata != null
							&& !chapterMetadata.equals(RussiaSourceConstants.emptyString))
									? chapterMetadata + "#" + articleMetadata
									: articleMetadata;
					updateLinkagesInConnectingSubGranule(element, articleMetadata, docDetails);
					docDetails.put(articleMetadata, matcher1.group(1));
				}
			} else if (matcher2.find()) {
				isNoteStarted = true;
				isArticleContinued = false;
				isSubSubArticleContinued = false;
				if(matcher11.find()) {
					dateUpdated=matcher11.group(1);
				}
				else {
					note += element.text();
				}
				updateLinkagesInGranule(element, docDetails);
			} else if (matcher3.find() && isNoteStarted) {
				List<String> aliasList = new ArrayList<>();
				aliasList.add(matcher3.group(1));
				citation = Citation.builder().aliases(aliasList).build();
				docDetails.put("citation", citation);
				updateLinkagesInGranule(element, docDetails);
			} else if (matcher6.find() && matcher5.find() && !isNoteStarted) {
				String localCurrent = "";
				if (matcher7.find()) {
					localCurrent = matcher7.group(1);
				} else if (matcher8.find()) {
					localCurrent = matcher8.group(1);
				}
				if (isArticleContinued || currentSubMatcher.equals(localCurrent)) {
					subArticleMetadata = matcher5.group(1);
					subArticleMetadata=superScriptDifferentatedStr(element, subArticleMetadata);
					String subArticle = matcher6.group(1);
					isSubArticleContinued = true;
					isArticleContinued = false;
					isSubSubArticleContinued = false;
					currentSubMatcher = localCurrent;
					docDetails.put(articleMetadata + "#" + subArticleMetadata, subArticle);
					updateLinkagesInConnectingSubGranule(element, articleMetadata + "#" + subArticleMetadata,
							docDetails);
					String parentArticleMetaData = articleMetadata.contains("#")?articleMetadata.split("\\#")[(int) articleMetadata.chars().filter(ch -> ch == '#').count()]:articleMetadata;
					String childId = system.getCommonId() + "/" + articleMetadata + "_" + subArticleMetadata;
					putChildIdInParentGranule(docDetails, parentArticleMetaData, childId);
				} else {
					subSubArticleMetadata = matcher5.group(1);
					String subSubArticle = matcher6.group(1);
					isSubArticleContinued = false;
					isSubSubArticleContinued = true;
					docDetails.put(articleMetadata + "#" + subArticleMetadata + "#" + subSubArticleMetadata,
							subSubArticle);

					String childId = system.getCommonId() + "/" + articleMetadata + "_" + subArticleMetadata + "_"
							+ subSubArticleMetadata;
					putChildIdInParentGranule(docDetails, articleMetadata + "_" + subArticleMetadata, childId);

					updateLinkagesInConnectingSubGranule(element,
							articleMetadata + "#" + subArticleMetadata + "#" + subSubArticleMetadata, docDetails);
				}
			} else if (isNoteStarted) {
				isSubArticleContinued = false;
				isSubSubArticleContinued = false;
				if(matcher11.find()) {
					dateUpdated=matcher11.group(1);
				}
				else {
					note += element.text();
				}
			} else if (isSubArticleContinued) {
				String subArticleKey = articleMetadata + "#" + subArticleMetadata;
				docDetails.put(subArticleKey,
						(docDetails.get(subArticleKey) != null ? docDetails.get(subArticleKey) : "") + element.text());
				updateLinkagesInConnectingSubGranule(element, subArticleKey, docDetails);

				String childId = system.getCommonId() + "/" + articleMetadata + "_" + subArticleMetadata;
				putChildIdInParentGranule(docDetails, articleMetadata, childId);
			} else if (isSubSubArticleContinued) {
				String subSubArticleKey = articleMetadata + "#" + subArticleMetadata + "#" + subSubArticleMetadata;
				docDetails.put(subSubArticleKey,
						(docDetails.get(subSubArticleKey) != null ? docDetails.get(subSubArticleKey) : "")
								+ element.text());
				updateLinkagesInConnectingSubGranule(element, subSubArticleKey, docDetails);

				String childId = system.getCommonId() + "/" + articleMetadata + "_" + subArticleMetadata + "_"
						+ subSubArticleMetadata;
				putChildIdInParentGranule(docDetails, articleMetadata + "_" + subArticleMetadata, childId);
			} else if (isSectionContinued) {
				docDetails.put(sectionMetadata,
						(docDetails.get(sectionMetadata) != null ? docDetails.get(sectionMetadata) : "")
								+ element.text());
				updateLinkagesInConnectingSubGranule(element, chapterMetadata, docDetails);
			} else if (isChapterContinued) {
				docDetails.put(chapterMetadata,
						(docDetails.get(chapterMetadata) != null ? docDetails.get(chapterMetadata) : "")
								+ element.text());
				updateLinkagesInConnectingSubGranule(element, chapterMetadata, docDetails);
			} else if (isArticleContinued) {
				docDetails.put(articleMetadata,
						(docDetails.get(articleMetadata) != null ? docDetails.get(articleMetadata) : "")
								+ element.text());
				updateLinkagesInConnectingSubGranule(element, articleMetadata, docDetails);
			} else if (isHeadingContniued) {
				docDetails.put("heading",
						(docDetails.get("heading") != null ? docDetails.get("heading") : "") + element.text());
				updateLinkagesInGranule(element, docDetails);
			} else if (isAbstractContinued) {
				_abstract = _abstract + element.text() + RussiaSourceConstants.newLineCharacter;
				updateLinkagesInGranule(element, docDetails);
			}
		}
		Note notes = Note.builder().content(note).build();
//		docDetails.put("Note", notes);

		Dates date = Dates.builder().updated(dateUpdated).build();
		for (Map.Entry parsedDocEntry : docDetails.entrySet()) {

			if (parsedDocEntry.getKey().toString().contains("Linkages")
					|| parsedDocEntry.getKey().toString().contains("childIdList")
					|| parsedDocEntry.getKey().toString().contains("granulelinkageList")
					|| parsedDocEntry.getKey().toString().contains("heading")
					|| parsedDocEntry.getKey().toString().contains("citation"))
				continue;

			if (parsedDocEntry.getValue() == null)
				log.error("The key is " + parsedDocEntry.getKey() + " and its null value is "
						+ parsedDocEntry.getValue());
			saveSubGranule(subGranuleList, parsedDocEntry.getValue().toString(), parsedDocEntry.getKey().toString(),
					system, docDetails);
		}
		docDetails.put("Notes", notes);
		docDetails.put("date", date);
		docDetails.put("subGranuleList", subGranuleList);
		docDetails.put("parserType", "commonGenericParser");
		return docDetails;
	}
	public String superScriptDifferentatedStr(Element element, String subArticleMetadata) {
		String superScriptVal=null;
		Elements spanElements = element.select("span");
		if(spanElements!=null) {
			for(Element superScriptEle : spanElements) {
				if(superScriptEle.hasAttr("class") && superScriptEle.attr("class").equals("W9"))
					superScriptVal=superScriptEle.text();
			}
		}
		if(superScriptVal!=null && subArticleMetadata.contains(superScriptVal))	{
			int sslen=superScriptVal.length();
			if(subArticleMetadata.indexOf(".")==subArticleMetadata.length()-1)
				sslen=sslen+1;
			int metaDataSsStartIndex=subArticleMetadata.length()-sslen;
			
			subArticleMetadata=subArticleMetadata.substring(0,metaDataSsStartIndex)+"^"+superScriptVal;
		}
		return subArticleMetadata;
	}
	public Map pTagClassHParser(Document doc, List<SubGranule> subGranuleList, System system) {
		Elements para = doc.select("p");
		Map<String, Object> docDetails = new HashMap();
		String articleMetadata = "", note = "";
		String subArticleMetadata = "";
		String subSubArticleMetadata = "";
		Citation citation = null;
		boolean isHeadingContniued = true, isArticleContinued = false, isSubArticleContinued = false,
				isNoteStarted = false, isSubSubArticleContinued = false;
		Pattern articlePattern = Pattern.compile("Статья\\s+\\d+-*\\d*+\\s*\\.*\\s*(.*)",
				Pattern.DOTALL | Pattern.UNICODE_CASE);
		Pattern articleMetaDataPattern = Pattern.compile("(Статья\\s+\\d+-*\\d*+)\\s*\\.*",
				Pattern.DOTALL | Pattern.UNICODE_CASE);
		Pattern notePattern = Pattern.compile("(Президент Российской Федерации.*)",
				Pattern.DOTALL | Pattern.UNICODE_CASE);
		Pattern citationPattern = Pattern.compile(".*(№\\s*\\d*-ФЗ$)", Pattern.DOTALL | Pattern.UNICODE_CASE);
		Pattern subArticleMetaDataPattern = Pattern.compile("^(\\d+\\s*)\\..*", Pattern.DOTALL | Pattern.UNICODE_CASE);
		Pattern subArticlePattern = Pattern.compile("^\\d+\\s*\\.(.*)", Pattern.DOTALL | Pattern.UNICODE_CASE);
		Pattern subSubArticlePattern = Pattern.compile("^\\d+\\s*\\)(.*)", Pattern.DOTALL | Pattern.UNICODE_CASE);
		Pattern subSubArticleMetaDataPattern = Pattern.compile("(^\\d+\\s*)\\).*",
				Pattern.DOTALL | Pattern.UNICODE_CASE);
		for (Element span : para) {
			Matcher matcher1 = articlePattern.matcher(span.text());
			Matcher matcher2 = notePattern.matcher(span.text());
			Matcher matcher3 = citationPattern.matcher(span.text());
			Matcher matcher4 = articleMetaDataPattern.matcher(span.text());
			Matcher matcher5 = subArticleMetaDataPattern.matcher(span.text());
			Matcher matcher6 = subArticlePattern.matcher(span.text());
			Matcher matcher7 = subSubArticleMetaDataPattern.matcher(span.text());
			Matcher matcher8 = subSubArticlePattern.matcher(span.text());
			if (matcher1.find() && !isNoteStarted) {
				isHeadingContniued = false;
				isArticleContinued = true;

				if (matcher4.find()) {
					articleMetadata = matcher4.group(1);
					docDetails.put(articleMetadata, span.text());
					List<Linkage__1> subgranulelinkageList = getLinkagesParser(span);
					docDetails.put(articleMetadata + "~Linkages", subgranulelinkageList);
				}
			} else if (matcher2.find()) {
				// matcher3.find();
				isNoteStarted = true;
				isArticleContinued = false;
				isSubSubArticleContinued = false;
				note += span.text();
			} else if (matcher3.find()) {
				List<String> aliasList = new ArrayList<>();
				aliasList.add(matcher3.group(1));
				citation = Citation.builder().aliases(aliasList).build();
				docDetails.put("citation", citation);
			} else if (matcher6.find() && matcher5.find() && isArticleContinued) {
				subArticleMetadata = matcher5.group(1);
				String subArticle = matcher6.group(1);
				isSubArticleContinued = true;
				isArticleContinued = false;
				docDetails.put(articleMetadata + "#" + subArticleMetadata, subArticle);
				List<Linkage__1> subgranulelinkageList = getLinkagesParser(span);
				docDetails.put(articleMetadata + "#" + subArticleMetadata + "~Linkages", subgranulelinkageList);
			} else if (matcher8.find() && matcher7.find()) {
				subSubArticleMetadata = matcher7.group(1);
				String subSubArticle = matcher8.group(1);
				isSubArticleContinued = false;
				isSubSubArticleContinued = true;
				docDetails.put(articleMetadata + "#" + subArticleMetadata + "#" + subSubArticleMetadata, subSubArticle);
				List<Linkage__1> subgranulelinkageList = getLinkagesParser(span);
				docDetails.put(articleMetadata + "#" + subArticleMetadata + "#" + subSubArticleMetadata + "~Linkages",
						subgranulelinkageList);
			} else if (isSubArticleContinued) {
				String subArticleKey = articleMetadata + "#" + subArticleMetadata;
				docDetails.put(subArticleKey,
						(docDetails.get(subArticleKey) != null ? docDetails.get(subArticleKey) : "") + span.text());
				updateLinkagesInConnectingSubGranule(span, subArticleKey, docDetails);
			} else if (isSubSubArticleContinued) {
				String subSubArticleKey = articleMetadata + "#" + subArticleMetadata + "#" + subSubArticleMetadata;
				docDetails.put(subSubArticleKey,
						(docDetails.get(subSubArticleKey) != null ? docDetails.get(subSubArticleKey) : "")
								+ span.text());
				updateLinkagesInConnectingSubGranule(span, subSubArticleKey, docDetails);
			} else if (isArticleContinued) {
				docDetails.put(articleMetadata,
						(docDetails.get(articleMetadata) != null ? docDetails.get(articleMetadata) : "") + span.text());
				updateLinkagesInConnectingSubGranule(span, articleMetadata, docDetails);
			} else if (isHeadingContniued) {
				docDetails.put("heading",
						(docDetails.get("heading") != null ? docDetails.get("heading") : "") + span.text());
			} else if (isNoteStarted) {
				isSubArticleContinued = false;
				note += span.text();
			}
		}
		Note notes = Note.builder().content(note).build();
		docDetails.put("Note", notes);

		Dates date = Dates.builder().updated("").build();

		for (Map.Entry parsedDocEntry : docDetails.entrySet()) {
//			log.error("Key "+parsedDocEntry.getValue().toString()+"\nValue\t"+ parsedDocEntry.getKey().toString());
			if (parsedDocEntry.getKey().toString().contains("Linkages")
					|| parsedDocEntry.getKey().toString().contains("childIdList"))
				continue;

			saveSubGranule(subGranuleList, parsedDocEntry.getValue().toString(), parsedDocEntry.getKey().toString(),
					system, docDetails);
		}
		docDetails.put("Notes", notes);
		docDetails.put("date", date);
		docDetails.put("subGranuleList", subGranuleList);
		if (docDetails.containsKey("heading") && note != null && !note.equals(RussiaSourceConstants.emptyString))
			docDetails.put("parserType", "pTagClassHParser");
		return docDetails;
	}

	public List<Linkage__1> getLinkagesParser(Element docElement) {
		List<Linkage__1> linkageList = null;
		Elements select = docElement.select("a[href]");
		for (Element element : select) {
			if (linkageList == null)
				linkageList = new ArrayList<>();
			Linkage__1 link = Linkage__1.builder().citation(element.text())
					.externalUrl(RussiaSourceConstants.paraLegalDomain + element.attr("href")).type(element.tagName())
					.build();
			linkageList.add(link);
		}
		return linkageList;
	}

	public List<Linkage> getLinkagesParser(Element docElement, List<Linkage> linkageList) {
		Elements select = docElement.select("a[href]");
		for (Element element : select) {
			if (linkageList == null)
				linkageList = new ArrayList<>();
			Linkage link = Linkage.builder().citation(element.text())
					.externalUrl(RussiaSourceConstants.paraLegalDomain + element.attr("href")).type(element.tagName())
					.build();
			linkageList.add(link);
		}
		return linkageList;
	}

	public boolean identifyAndProduceSubgranules(Element ele, Map docDetails, System system) {
		String regexVal = RussiaSourceConstants.emptyString;
		boolean isSubgranualContinued = false;
		int regexNumber = 0;
		for (String regexStr : RussiaSourceConstants.regexParaLegalFederalLaw) {
			regexVal = getRegexValue(regexStr, ele.text());
			if (!regexVal.equals(RussiaSourceConstants.emptyString)) {
				regexNumber = RussiaSourceConstants.regexParaLegalFederalLaw.indexOf(regexStr);
				docDetails.put("hierarchyLevel", regexNumber);
				break;
			}
		}
		if (ele.hasClass("I") && ele.text().contains("Президент")) {
			return isSubgranualContinued;
		}
		if (!regexVal.equals(RussiaSourceConstants.emptyString)) {
			saveSubGranule(docDetails, system);
			List<Linkage__1> subgranulelinkageList = null;
			subgranulelinkageList = getLinkagesParser(ele);
			String subgranual = (docDetails.get("subgranual") != null ? (String) docDetails.get("subgranual")
					: RussiaSourceConstants.emptyString)
					+ ele.text().replace(RussiaSourceConstants.newLineCharacter, RussiaSourceConstants.emptyString)
							.replace(RussiaSourceConstants.backSlashQuote, RussiaSourceConstants.emptyString);
			String subgranualMetaData = RussiaSourceConstants.emptyString;
			if (ele.childrenSize() > 0)
				subgranualMetaData = ele.children().text().split("\\.")[0]
						.replace(RussiaSourceConstants.newLineCharacter, RussiaSourceConstants.emptyString)
						.replace(RussiaSourceConstants.backSlashQuote, RussiaSourceConstants.emptyString);
			else
				subgranualMetaData = ele.text().split("\\.")[0].toString()
						.replace(RussiaSourceConstants.newLineCharacter, RussiaSourceConstants.emptyString)
						.replace(RussiaSourceConstants.backSlashQuote, RussiaSourceConstants.emptyString);

			docDetails.put("subgranual", subgranual);
			isSubgranualContinued = true;
			docDetails.put("isSubgranualContinued", isSubgranualContinued);
			docDetails.put("subgranualMetaData", subgranualMetaData);
			docDetails.put("subgranulelinkageList", subgranulelinkageList);

		} else if (docDetails.get("isSubgranualContinued") != null
				&& docDetails.get("isSubgranualContinued").equals(true)) {
			docDetails.put("subgranual",
					(String) docDetails.get("subgranual") + ele.text()
							.replace(RussiaSourceConstants.newLineCharacter, RussiaSourceConstants.emptyString)
							.replace(RussiaSourceConstants.backSlashQuote, RussiaSourceConstants.emptyString));
		}

		return isSubgranualContinued;
	}

	public Map saveSubGranule(Map docDetails, System system) {
		if (docDetails.get("subgranual") != null
				&& !docDetails.get("subgranual").toString().equals(RussiaSourceConstants.emptyString)) {
			String subgranual = (String) docDetails.get("subgranual");
			if (subgranual.substring(0, subgranual.indexOf(".")).length() < 9) {
				subgranual = subgranual.substring(subgranual.indexOf(".") + 3);
			}

			String id = (docDetails.get("parentSubGranuleMetaData") != null
					? docDetails.get("parentSubGranuleMetaData").toString()
					: RussiaSourceConstants.emptyString)
					+ (docDetails.get("subgranualMetaData").toString().split("\\s").length > 1
							? (docDetails.get("subgranualMetaData").toString().split("\\s")[0] + "_"
									+ docDetails.get("subgranualMetaData").toString().split("\\s")[1])
							: "_" + docDetails.get("subgranualMetaData").toString());

			Subcitation subcitation = Subcitation.builder()
					.primary(system.getCommonId().substring(system.getCommonId().lastIndexOf("/") + 1) + " "
							+ docDetails.get("subgranualMetaData").toString())
					.build();
			SubGranule subGranule = SubGranule.builder().id(id).commonId(system.getCommonId() + "/" + id)
					.subcitation(subcitation).linkages((List<Linkage__1>) docDetails.get("subgranulelinkageList"))
					.content(subgranual.replace(RussiaSourceConstants.newLineCharacter,
							RussiaSourceConstants.emptyString))
					.heading(docDetails.get("subgranualMetaData").toString()).build();
			String childId = system.getCommonId() + "/" + id;
			List<SubGranule> subGranuleList = ((List<SubGranule>) docDetails.get("subGranuleList"));
			if (docDetails.get("hierarchyLevel") != null && ((Integer) docDetails.get("hierarchyLevel")) > 0) {

				if (docDetails.get("parentgranual") == null) {
					docDetails.put("parentgranual", subGranule);
					docDetails.put("parentSubGranuleMetaData", id);
					docDetails.put("parentSubGranuleId", childId);
				} else {
					if (docDetails.get("childIdList") != null) {
						List<String> childIdList = (List<String>) docDetails.get("childIdList");
						childIdList.add(childId);
						docDetails.put("childIdList", childIdList);
					} else {
						List<String> childIdList = new ArrayList<>();
						childIdList.add(childId);
						docDetails.put("childIdList", childIdList);
					}
					if (docDetails.containsKey("parentSubGranuleId"))
						subGranule.setParentId(docDetails.get("parentSubGranuleId").toString());
					subGranuleList.add(subGranule);
				}
			} else {
				if (docDetails.get("hierarchyLevel") != null && ((Integer) docDetails.get("hierarchyLevel")) == 0
						&& docDetails.get("parentgranual") != null) {
					subGranuleList.add(subGranule);
					if (docDetails.containsKey("parentSubGranuleId"))
						subGranule.setParentId(docDetails.get("parentSubGranuleId").toString());
					subGranule = (SubGranule) docDetails.get("parentgranual");
					docDetails.remove("parentgranual");
					docDetails.remove("parentSubGranuleMetaData");
					docDetails.remove("parentSubGranuleId");
					if (docDetails.get("childIdList") != null) {
						List<String> childIdList = (List<String>) docDetails.get("childIdList");
						childIdList.add(childId);
						docDetails.put("childIdList", childIdList);
					}
					subGranule.setChildId((List<String>) docDetails.get("childIdList"));
					docDetails.remove("childIdList");
				}
				subGranuleList.add(subGranule);
			}
			docDetails.put("subGranuleList", subGranuleList);
			docDetails.put("subgranual", RussiaSourceConstants.emptyString);
			docDetails.put("subgranualMetaData", RussiaSourceConstants.emptyString);
		}

		return docDetails;
	}

	public boolean saveSubGranule(List<SubGranule> subGranuleList, String subgranuleContent,
			String subgranuleMetaContent, System system, Map docDetails) {
		boolean isSubArticleSaved = true;
		try {
			Subcitation subcitation = Subcitation.builder()
					.primary(system.getCommonId().substring(system.getCommonId().lastIndexOf("/") + 1) + " "
							+ subgranuleMetaContent.replace("#", "_"))
					.build();
			String heading = subgranuleMetaContent;
			String parentId = RussiaSourceConstants.emptyString;
			if (subgranuleMetaContent.contains("#")) {
				String[] metaContentArray = subgranuleMetaContent.split("\\#");
				heading = metaContentArray[metaContentArray.length - 1];
				String parentIdPart = "";
				for (int i = 0; i < metaContentArray.length - 1; i++) {
					parentIdPart += metaContentArray[i];
					if (i < metaContentArray.length - 2)
						parentIdPart += "_";
				}
				parentId = system.getCommonId() + "/" + parentIdPart;
			}
			String id = subgranuleMetaContent.replace("#", "_");
			SubGranule subGranule = SubGranule.builder().id(id).commonId(system.getCommonId() + "/" + id)
					.subcitation(subcitation)
					.linkages(docDetails.get(subgranuleMetaContent + "~Linkages") != null
							? (List<Linkage__1>) docDetails.get(subgranuleMetaContent + "~Linkages")
							: null)
					.content(subgranuleContent
							.replace(RussiaSourceConstants.newLineCharacter, RussiaSourceConstants.emptyString))
					.parentId(parentId).heading(heading)
					.childId(docDetails.get(subgranuleMetaContent.replace("#", "_") + "_childIdList") != null
							? (List<String>) docDetails.get(subgranuleMetaContent.replace("#", "_") + "_childIdList")
							: null)
					.build();

			subGranuleList.add(subGranule);
		} catch (Exception e) {
			isSubArticleSaved = false;
		}

		return isSubArticleSaved;
	}

	public Document getJsoupDocumentFromFile(File file, String dirPath) {
		Document doc = null;
		try {
			doc = Jsoup.parse(file, null);
		} catch (IOException e) {
			log.error("The File Path :: " + file.getAbsolutePath() + " is not parsed", e);
			try {
				ParaLegalService.storeNonparsedJsoupDocPaths(file.getAbsolutePath(), dirPath);
			} catch (IOException e1) {
				log.error("Error saving file path for unparseable jsoup files :: " + file.getAbsolutePath(), e1);
			}
		}
		return doc;
	}

	public static void storeParseableDocPaths(String url, String dirPath) throws IOException {
		String dirName = dirPath + File.separator + "ParseableDocPaths";
		File theDir = new File(dirName);
		if (!theDir.exists()) {
			theDir.mkdirs();
		}

		String filepath = dirName + "\\docPaths.txt";
		File file = new File(filepath);
		if (file.exists()) {
			FileWriter fw = new FileWriter(filepath, true); // the true will append the new data
			fw.write(RussiaSourceConstants.newLineCharacter);// appends the string to the file
			fw.write(url);
			fw.close();
		} else {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			writer.write(url);
			writer.close();
		}
	}
}
