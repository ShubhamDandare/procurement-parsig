package com.kpmg.rcm.sourcing.russia.paralegal.service;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.kpmg.rcm.sourcing.common.dto.SourceFieldMappingResponse;
import com.kpmg.rcm.sourcing.russia.constants.RussiaSourceConstants;
import com.kpmg.rcm.sourcing.russia.paralegal.parse.CommonParser;
import com.kpmg.rcm.sourcing.russia.paralegal.vo.GSUrlDto;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ParaLegalService {

	@Value("${filepath.downloadedFileLocation}")
	private String dirPath;

	@Autowired
	private CommonParser commonParser;

	protected static List<String> nonReachableURLs;

	public void procure(String srcValue, SourceFieldMappingResponse sourceFieldMappingResponse, Boolean gsApplicable)
			throws IOException {
		Map procuredMap = getprocurementDetails(srcValue, sourceFieldMappingResponse);
		Integer totalCounter = Integer.parseInt(procuredMap.get("totalCounter").toString());
		ExecutorService executor = Executors.newFixedThreadPool(8);// (numberOfThreads);
		log.info("starting time" + LocalDateTime.now());
		int pageSize = 100;
		if (gsApplicable.equals(true)) {

			List<GSUrlDto> gsURLList = populateGSURLs();
			gsURLList.forEach(gsURL -> {
				try {
					procureIndexFileAndExtractDocOnThread(procuredMap, pageSize, srcValue, gsURLList.indexOf(gsURL), executor, gsApplicable, gsURL.getSearchWebLink(), gsURL.getDocWebLink());
				} catch (IOException e) {
					log.error("Error in GS Docs Procurement "+gsURL.getSearchWebLink() + " || "+gsURL.getDocWebLink());
				}
			});
		} else {
			for (int i = 0; i < totalCounter; i += pageSize) {
				procureIndexFileAndExtractDocOnThread(procuredMap, pageSize, srcValue, i, executor,null,null,null);
			}
		}
		executor.shutdown();
		log.info("ending time" + LocalDateTime.now());
	}
	
	private void procureIndexFileAndExtractDocOnThread(Map procuredMap, int pageSize, String srcValue, int i, ExecutorService executor, Boolean gsApplicable, String gsSearchWeblink, String gsDocLink) throws IOException {
		String baseUri = null;
		String directoryName = null;
		if(gsApplicable!=null && gsApplicable.equals(true)) {
			baseUri=gsSearchWeblink.replace("searchres=", "list_itself=");
			directoryName = dirPath + File.separator + getDirectoryNameFromSrcVal(srcValue) + "GSDocuments";
		}else {
			baseUri = UriComponentsBuilder.fromUriString(procuredMap.get("baseUrl").toString())
				.queryParam("a3", srcValue).queryParam("lstsize", pageSize).queryParam("start", i).build()
				.toUriString();
			directoryName = dirPath + File.separator + getDirectoryNameFromSrcVal(srcValue) + "Documents";
		}

		
		boolean runThread = true;
		File theDir = new File(directoryName);
		if (!theDir.exists()) {
			theDir.mkdirs();
		}
		try {
			downloadPageWithRetryLogic(baseUri, procuredMap.get("subSource").toString() + "_" + i,
					directoryName);
		} catch (IOException e) {
			runThread = false;
			log.error("Download failed for :: " + baseUri, e);
			storeUnreachableURLs(baseUri, dirPath);
		}
		ProcurementThread procurementThread = null;
		if (runThread) {
			if(gsApplicable!=null && gsApplicable.equals(true)) {
				procurementThread = ProcurementThread.builder().versionExtractionUrl(gsDocLink)
						.dirPath(directoryName).gsApplicable(gsApplicable).build();
				executor.execute(procurementThread);
			}else {
				procurementThread = ProcurementThread.builder().baseUrl(baseUri)
						.versionExtractionUrl(procuredMap.get("versionExtractionUrl").toString())
						.docExtractionUrl(procuredMap.get("docExtractionUrl").toString()).dirPath(directoryName)
						.build();
			}
			
			executor.execute(procurementThread);
		}
	}
	
	@SuppressWarnings("unlikely-arg-type")
	public Map getprocurementDetails(String srcValue, SourceFieldMappingResponse sourceFieldMappingResponse) {
		String baseUrl = sourceFieldMappingResponse.getSourceLocation();
		Map procureMap = new HashMap();
		if (srcValue.equals(RussiaSourceConstants.subSource.FederalLaw.getValue())) {
			procureMap.put("totalCounter", Integer.parseInt(getTotalDocs(srcValue)));
			procureMap.put("baseUrl", baseUrl + RussiaSourceConstants.paraLegalFederalLawBaseUrl);
			procureMap.put("versionExtractionUrl", baseUrl + RussiaSourceConstants.paraLegalFederalLawVerUrl);
			procureMap.put("docExtractionUrl", baseUrl + RussiaSourceConstants.paraLegalFederalLawDocUrl);
			procureMap.put("subSource", RussiaSourceConstants.subSource.FederalLaw);
		}

		return procureMap;
	}

	public String getTotalDocs(String lawType) {
		String tempURL = "http://pravo.gov.ru/proxy/ips/?searchlist=&bpas=cd00000&a3=" + lawType + "&a3type=1";
		System.out.println(tempURL);
		Document index = getJsoupDocument(tempURL);
		Element number = index.select(".large").first();

		return number.text();
	}

	@Retryable(value = IOException.class)
	public void downloadPageWithRetryLogic(String targetUrl, String uniqueNum, String directoryPath)
			throws IOException {
		URLConnection htmlUrl = new URL(targetUrl).openConnection();
		htmlUrl.setRequestProperty("User-Agent", "Mozilla 5.0 (Windows; U; " + "Windows NT 5.1; en-US; rv:1.8.0.11) ");
		InputStream inputStream = htmlUrl.getInputStream();
		if ((new File(directoryPath + "/" + uniqueNum + ".html")).exists())
			return;
		try (BufferedInputStream in = new BufferedInputStream(inputStream);
				FileOutputStream fileOutputStream = new FileOutputStream(directoryPath + "/" + uniqueNum + ".html")) {
			byte dataBuffer[] = new byte[1024];
			int bytesRead;
			while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
				fileOutputStream.write(dataBuffer, 0, bytesRead);
			}
			fileOutputStream.close();

		} catch (IOException ex) {
			log.error("Download failed for :: " + targetUrl, ex);
			storeUnreachableURLs(targetUrl, dirPath);
		}
	}

	@Recover
	void downloadPageRecovery(IOException e, String targetUrl, String uniqueNum, String directoryPath)
			throws IOException {
		log.error("Error downloading url :: " + targetUrl, e);
		storeUnreachableURLs(targetUrl, dirPath);
	}

	public Document getJsoupDocument(String url) {
		Document doc = null;
		try {
			doc = Jsoup.connect(url).get();
		} catch (IOException e) {
			log.error("The Web link :: " + url + " is not available", e);
		}
		return doc;
	}

	public Document getJsoupDocumentFromFile(File file) {
		Document doc = null;
		try {
			doc = Jsoup.parse(file, null);
		} catch (IOException e) {
			log.error("The File Path :: " + file.getAbsolutePath() + " is not parsed", e);
			try {
				storeNonparsedJsoupDocPaths(file.getAbsolutePath(), dirPath);
			} catch (IOException e1) {
				log.error("Error saving file path for unparseable jsoup files :: " + file.getAbsolutePath(), e1);
			}
		}
		return doc;
	}

	public void parse(String srcValue, SourceFieldMappingResponse sourceFieldMappingResponse, Boolean gsApplicable) throws IOException {
		String baseUrl = sourceFieldMappingResponse.getSourceLocation();
		ExecutorService executor = Executors.newFixedThreadPool(4);
		String filePath = null;
		if(gsApplicable!=null && gsApplicable.equals(true))
			filePath = dirPath + File.separator + getDirectoryNameFromSrcVal(srcValue) + "GSDocuments";
		else
			filePath = dirPath + File.separator + getDirectoryNameFromSrcVal(srcValue) + "Documents";
		
		File dir = new File(filePath);
		if (dir.exists()) {
			File[] indexfiles = dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.contains(RussiaSourceConstants.subSource.FederalLaw.toString());
				}
			});
			
			for (File indexfile : indexfiles) {
				int count=0;
				Document document = getJsoupDocumentFromFile(indexfile);
				log.error("The Picked IndexFile is :: " + indexfile.getAbsolutePath());
				Elements evenElements = document.getElementsByClass("list_elem even");
				for (Element element : evenElements) {
					if(gsApplicable!=null && gsApplicable.equals(true) && count<1)
						count++;
					else
						continue;
					try {
						String[] indexPageContents = getIndexPageMetaData(element);
						Elements input = element.select("input");
						String uniqueNum = input.attr("name").substring(6);
						File[] docFiles = dir.listFiles(new FilenameFilter() {
							@Override
							public boolean accept(File dir, String name) {
								return name.startsWith(uniqueNum);
							}
						});
						for (File docFile : docFiles) {
							ParserThread parserThread = ParserThread.builder().commonParser(commonParser).file(docFile)
									.indexPageContents(indexPageContents)
									.gsApplicable(gsApplicable)
									.sourceUrl(baseUrl + RussiaSourceConstants.paraLegalFederalLawVerUrl.toString()
											+ "&nd=" + uniqueNum)
									.docType(indexfile.getName()).dirPath(dirPath).build();
							executor.execute(parserThread);
						}
					} catch (Exception e) {
						log.error("Parsing failed for :: " + indexfile.getAbsolutePath(), e);
						storeUnparseableDocPaths(indexfile.getAbsolutePath(), dirPath);
					}
				}
				Elements oddElements = document.getElementsByClass("list_elem odd");
				for (Element element : oddElements) {
					if(gsApplicable!=null && gsApplicable.equals(true) && count<1)
						count++;
					else
						continue;
					try {
						String[] indexPageContents = getIndexPageMetaData(element);
						Elements input = element.select("input");
						String uniqueNum = input.attr("name").substring(6);
						File[] docFiles = dir.listFiles(new FilenameFilter() {
							@Override
							public boolean accept(File dir, String name) {
								return name.startsWith(uniqueNum);
							}
						});
						for (File docFile : docFiles) {
							ParserThread parserThread = ParserThread.builder().commonParser(commonParser).file(docFile)
									.indexPageContents(indexPageContents)
									.gsApplicable(gsApplicable)
									.sourceUrl(baseUrl + RussiaSourceConstants.paraLegalFederalLawVerUrl.toString()
											+ "&nd=" + uniqueNum)
									.docType(indexfile.getName()).dirPath(dirPath).build();
							executor.execute(parserThread);
						}
					} catch (Exception e) {
						log.error("Parsing failed for :: " + indexfile.getAbsolutePath(), e);
						storeUnparseableDocPaths(indexfile.getAbsolutePath(), dirPath);
					}
				}
			}
		}
		executor.shutdown();
		log.info("ending time" + LocalDateTime.now());
	}

	public static synchronized void storeUnreachableURLs(String url, String dirPath) throws IOException {
		String dirName = dirPath + File.separator + "UnreachableURLs";
		File theDir = new File(dirName);
		if (!theDir.exists()) {
			theDir.mkdirs();
		}

		String filepath = dirName + "\\URLs.txt";
		File file = new File(filepath);
		if (file.exists()) {
			FileWriter fw = new FileWriter(filepath, true); // the true will append the new data
			fw.write("\n");// appends the string to the file
			fw.write(url);
			fw.close();
		} else {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			writer.write(url);
			writer.close();
		}
	}

	public static synchronized void storeUnparseableDocPaths(String url, String dirPath) throws IOException {
		String dirName = dirPath + File.separator + "UnParseableDocPaths";
		File theDir = new File(dirName);
		if (!theDir.exists()) {
			theDir.mkdirs();
		}

		String filepath = dirName + "\\docPaths.txt";
		File file = new File(filepath);
		if (file.exists()) {
			FileWriter fw = new FileWriter(filepath, true); // the true will append the new data
			fw.write("\n");// appends the string to the file
			fw.write(url);
			fw.close();
		} else {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			writer.write(url);
			writer.close();
		}
	}

	public static synchronized void storeNonparsedJsoupDocPaths(String url, String dirPath) throws IOException {
		String dirName = dirPath + File.separator + "UnParseableDocPaths";
		File theDir = new File(dirName);
		if (!theDir.exists()) {
			theDir.mkdirs();
		}

		String filepath = dirName + "\\jsoupDocPaths.txt";
		File file = new File(filepath);
		if (file.exists()) {
			FileWriter fw = new FileWriter(filepath, true); // the true will append the new data
			fw.write("\n");// appends the string to the file
			fw.write(url);
			fw.close();
		} else {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			writer.write(url);
			writer.close();
		}
	}

	public String[] getIndexPageMetaData(Element table) {
		String[] indexPageContents = new String[5];
		Pattern linkDatePattern = Pattern.compile("от\\s*(\\d+\\.\\d+\\.\\d+)\\s*.*",
				Pattern.DOTALL | Pattern.UNICODE_CASE);
		Pattern sourceCitationAliasesPattern = Pattern.compile("№\\s*(\\d*-ФЗ).*",
				Pattern.DOTALL | Pattern.UNICODE_CASE);
		Pattern systemDocTypePattern = Pattern.compile("^(.*)от", Pattern.DOTALL | Pattern.UNICODE_CASE);
		Pattern datePublishedPattern = Pattern.compile("от\\s*(\\d*\\.\\d*\\.\\d*)\\s*г",
				Pattern.DOTALL | Pattern.UNICODE_CASE);
		Pattern sourceCitationPattern = Pattern.compile("ст.\\s*(\\d+).*", Pattern.DOTALL | Pattern.UNICODE_CASE);

		Matcher matcher1, matcher2, matcher3, matcher4, matcher5;
		String linkText = table.select("a") != null ? table.select("a").first().text() : null;
		Elements list = table.getElementsByClass("tiny");
		if (linkText != null) {
			matcher1 = linkDatePattern.matcher(linkText);
			matcher2 = sourceCitationAliasesPattern.matcher(linkText);
			matcher3 = systemDocTypePattern.matcher(linkText);
			if (matcher1.find()) {
				indexPageContents[4] = matcher1.group(1);
			}
			if (matcher2.find()) {
				indexPageContents[0] = matcher2.group(1);
			}
			if (matcher3.find()) {
				indexPageContents[1] = matcher3.group(1);
			}

		}
		Boolean dateFound = false;
		for (Element listItem : list) {
			matcher4 = datePublishedPattern.matcher(listItem.text());
			matcher5 = sourceCitationPattern.matcher(listItem.text());
			if (matcher4.find() && !dateFound) {
				indexPageContents[3] = matcher4.group(1);
				dateFound = true;
			}
			if (matcher5.find()) {
				indexPageContents[2] = matcher5.group(1);
			}
		}
		return indexPageContents;
	}

	private String getDirectoryNameFromSrcVal(String srcValue) {

		if (srcValue.equals(RussiaSourceConstants.subSource.FederalLaw.getValue().toString()))
			return RussiaSourceConstants.subSource.FederalLaw.toString();
		else if (srcValue.equals(RussiaSourceConstants.subSource.Order.getValue().toString()))
			return RussiaSourceConstants.subSource.Order.toString();
		else if (srcValue.equals(RussiaSourceConstants.subSource.Code.getValue().toString()))
			return RussiaSourceConstants.subSource.Code.toString();

		return null;
	}

	private List<GSUrlDto> populateGSURLs() {
		List<GSUrlDto> gsUrlList = new ArrayList<>();
		gsUrlList.add(new GSUrlDto("http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=39-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%F0%FB%ED%EA%E5+%F6%E5%ED%ED%FB%F5+%E1%F3%EC%E0%E3&sort=-1","http://pravo.gov.ru/proxy/ips/?docbody=&link_id=0&nd=102040905&firstDoc=1&fulltext=1"));
		gsUrlList.add(new GSUrlDto("http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=173-%D4%C7.+%CE+%E2%E0%EB%FE%F2%ED%EE%EC+%F0%E5%E3%F3%EB%E8%F0%EE%E2%E0%ED%E8%E8+%E8+%E2%E0%EB%FE%F2%ED%EE%EC+%EA%EE%ED%F2%F0%EE%EB%E5&sort=-1","http://pravo.gov.ru/proxy/ips/?docbody=&nd=102084553&rdk=0&firstDoc=1"));
		gsUrlList.add(new GSUrlDto("http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=115-%D4%C7.+&sort=-1","http://pravo.gov.ru/proxy/ips/?docbody=&link_id=0&nd=102072376&firstDoc=1&fulltext=1"));
		gsUrlList.add(new GSUrlDto("http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=129-%D4%C7.+%CE+%E3%EE%F1%F3%E4%E0%F0%F1%F2%E2%E5%ED%ED%EE%E9+%F0%E5%E3%E8%F1%F2%F0%E0%F6%E8%E8+%FE%F0%E8%E4%E8%F7%E5%F1%EA%E8%F5+%EB%E8%F6+%E8+%E8%ED%E4%E8%E2%E8%E4%F3%E0%EB%FC%ED%FB%F5+%EF%F0%E5%E4%EF%F0%E8%ED%E8%EC%E0%F2%E5%EB%E5%E9&sort=-1","http://pravo.gov.ru/proxy/ips/?docbody=&link_id=0&nd=102072405&firstDoc=1&fulltext=1"));
		gsUrlList.add(new GSUrlDto("http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1","http://pravo.gov.ru/proxy/ips/?docbody=&link_id=0&nd=102108261&firstDoc=1&fulltext=1"));
		gsUrlList.add(new GSUrlDto("http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=+46-%D4%C7&sort=-1","http://pravo.gov.ru/proxy/ips/?docbody=&link_id=2&nd=102058488&firstDoc=1&fulltext=1"));
		gsUrlList.add(new GSUrlDto("http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=224-%D4%C7+%CE+%EF%F0%EE%F2%E8%E2%EE%E4%E5%E9%F1%F2%E2%E8%E8+%ED%E5%EF%F0%E0%E2%EE%EC%E5%F0%ED%EE%EC%F3+%E8%F1%EF%EE%EB%FC%E7%EE%E2%E0%ED%E8%FE+%E8%ED%F1%E0%E9%E4%E5%F0%F1%EA%EE%E9+%E8%ED%F4%EE%F0%EC%E0%F6%E8%E8+%E8+%EC%E0%ED%E8%EF%F3%EB%E8%F0%EE%E2%E0%ED%E8%FE+%F0%FB%ED%EA%EE%EC&sort=-1","http://pravo.gov.ru/proxy/ips/?docbody=&link_id=0&nd=102140499&firstDoc=1&fulltext=1"));
		gsUrlList.add(new GSUrlDto("http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=127-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%ED%E5%F1%EE%F1%F2%EE%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8+%28%E1%E0%ED%EA%F0%EE%F2%F1%F2%E2%E5%29&sort=-1","http://pravo.gov.ru/proxy/ips/?docbody=&link_id=0&nd=102078527&firstDoc=1&fulltext=1"));
		gsUrlList.add(new GSUrlDto("http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=173-%D4%C7.%CE%E1+%EE%F1%EE%E1%E5%ED%ED%EE%F1%F2%FF%F5+%F4%E8%ED%E0%ED%F1%EE%E2%FB%F5+%EE%EF%E5%F0%E0%F6%E8%E9+%F1+%E8%ED%EE%F1%F2%F0%E0%ED%ED%FB%EC%E8+%F4%E8%E7%E8%F7%E5%F1%EA%E8%EC%E8+%E8+%FE%F0%E8%E4%E8%F7%E5%F1%EA%E8%EC%E8+%EB%E8%F6%E0%EC%E8&sort=-1","http://pravo.gov.ru/proxy/ips/?docbody=&link_id=3&nd=102354387&firstDoc=1&fulltext=1"));
		gsUrlList.add(new GSUrlDto("http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=+%E8%ED%F1%E0%E9%E4%E5%F0%EE%E2+%EE%F0%E3%E0%ED%E8%E7%E0%F2%EE%F0%E0%EC+%F2%EE%F0%E3%EE%E2%EB%E8+%F4%E8%ED%E0%ED%F1%EE%E2%FB%EC%E8+%E8%ED%F1%F2%F0%F3%EC%E5%ED%F2%E0%EC%E8%2C+%E8%ED%EE%F1%F2%F0%E0%ED%ED%EE%E9+%E2%E0%EB%FE%F2%EE%E9+%E8+%28%E8%EB%E8%29+%F2%EE%E2%E0%F0%E0%EC%E8&sort=-1","http://pravo.gov.ru/proxy/ips/?docbody=&link_id=0&nd=102168000&firstDoc=1&fulltext=1"));
		gsUrlList.add(new GSUrlDto("http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=+%E8%ED%F1%E0%E9%E4%E5%F0%EE%E2+%EE%F0%E3%E0%ED%E8%E7%E0%F2%EE%F0%E0%EC+%F2%EE%F0%E3%EE%E2%EB%E8+%F4%E8%ED%E0%ED%F1%EE%E2%FB%EC%E8+%E8%ED%F1%F2%F0%F3%EC%E5%ED%F2%E0%EC%E8%2C+%E8%ED%EE%F1%F2%F0%E0%ED%ED%EE%E9+%E2%E0%EB%FE%F2%EE%E9+%E8+%28%E8%EB%E8%29+%F2%EE%E2%E0%F0%E0%EC%E8&sort=-1","http://pravo.gov.ru/proxy/ips/?docbody=&link_id=5&nd=102158388&firstDoc=1&fulltext=1"));
		gsUrlList.add(new GSUrlDto("http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=86-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%D6%E5%ED%F2%F0%E0%EB%FC%ED%EE%EC+%E1%E0%ED%EA%E5+%D0%EE%F1%F1%E8%E9%F1%EA%EE%E9+%D4%E5%E4%E5%F0%E0%F6%E8%E8+%28%C1%E0%ED%EA%E5+%D0%EE%F1%F1%E8%E8%29&sort=-1","http://pravo.gov.ru/proxy/ips/?docbody=&link_id=0&nd=102077052&firstDoc=1&fulltext=1"));
		gsUrlList.add(new GSUrlDto("http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=395-1-%D4%C7.+%CE+%E1%E0%ED%EA%E0%F5+%E8+%E1%E0%ED%EA%EE%E2%F1%EA%EE%E9+%E4%E5%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8&sort=-1","http://pravo.gov.ru/proxy/ips/?docbody=&link_id=0&nd=102010268&firstDoc=1&fulltext=1"));
		gsUrlList.add(new GSUrlDto("http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=161-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%ED%E0%F6%E8%EE%ED%E0%EB%FC%ED%EE%E9+%EF%EB%E0%F2%E5%E6%ED%EE%E9+%F1%E8%F1%F2%E5%EC%E5&sort=-1","http://pravo.gov.ru/proxy/ips/?docbody=&link_id=0&nd=102148779&firstDoc=1&fulltext=1"));
		gsUrlList.add(new GSUrlDto("http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=195-%D4%C7.+%CA%EE%E4%E5%EA%F1+%D0%EE%F1%F1%E8%E9%F1%EA%EE%E9+%D4%E5%E4%E5%F0%E0%F6%E8%E8+%EE%E1+%E0%E4%EC%E8%ED%E8%F1%F2%F0%E0%F2%E8%E2%ED%FB%F5+%EF%F0%E0%E2%EE%ED%E0%F0%F3%F8%E5%ED%E8%FF%F5&sort=-1","http://pravo.gov.ru/proxy/ips/?docbody=&link_id=0&nd=102074277&fulltext=1&firstDoc=1"));
		gsUrlList.add(new GSUrlDto("http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=14-%D4%C7.+%CE%E1+%EE%E1%F9%E5%F1%F2%E2%E0%F5+%F1+%EE%E3%F0%E0%ED%E8%F7%E5%ED%ED%EE%E9+%EE%F2%E2%E5%F2%F1%F2%E2%E5%ED%ED%EE%F1%F2%FC%FE&sort=-1","http://pravo.gov.ru/proxy/ips/?docbody=&link_id=0&nd=102051516&fulltext=1&firstDoc=1"));
		gsUrlList.add(new GSUrlDto("http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=38-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%F0%E5%EA%EB%E0%EC%E5&sort=-1","http://pravo.gov.ru/proxy/ips/?docbody=&link_id=0&nd=102105292&fulltext=1&firstDoc=1"));
		gsUrlList.add(new GSUrlDto("http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=135-%D4%C7.+%CE%E1+%EE%F6%E5%ED%EE%F7%ED%EE%E9+%E4%E5%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8+%E2+%D0%EE%F1%F1%E8%E9%F1%EA%EE%E9+%D4%E5%E4%E5%F0%E0%F6%E8%E8&sort=-1","https://pravo.gov.ru/proxy/ips/?docbody=&link_id=0&nd=102054672&fulltext=1&firstDoc=1"));
		gsUrlList.add(new GSUrlDto("http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=51-%D4%C7.+%C3%F0%E0%E6%E4%E0%ED%F1%EA%E8%E9+%EA%EE%E4%E5%EA%F1+%D0%EE%F1%F1%E8%E9%F1%EA%EE%E9+%D4%E5%E4%E5%F0%E0%F6%E8%E8.+%D7%E0%F1%F2%FC+%EF%E5%F0%E2%E0%FF&sort=-1","http://pravo.gov.ru/proxy/ips/?docbody=&link_id=14&nd=102033239&fulltext=1&firstDoc=1"));
		gsUrlList.add(new GSUrlDto("http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=14-%D4%C7.+%C3%F0%E0%E6%E4%E0%ED%F1%EA%E8%E9+%EA%EE%E4%E5%EA%F1+%D0%EE%F1%F1%E8%E9%F1%EA%EE%E9+%D4%E5%E4%E5%F0%E0%F6%E8%E8.+%D7%E0%F1%F2%FC+%E2%F2%EE%F0%E0%FF&sort=-1","http://pravo.gov.ru/proxy/ips/?docbody=&link_id=2&nd=102039276&fulltext=1&firstDoc=1"));
		gsUrlList.add(new GSUrlDto("http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=++%CF%D0%C8%CA%C0%C7+%D4%C5%C4%C5%D0%C0%CB%DC%CD%CE%C9+%D1%CB%D3%C6%C1%DB+%CF%CE+%D4%C8%CD%C0%CD%D1%CE%C2%DB%CC+%D0%DB%CD%CA%C0%CC+++N+10-4%2F%EF%E7-%ED&sort=-1","http://pravo.gov.ru/proxy/ips/?docbody=&link_id=0&nd=102138655&fulltext=1&firstDoc=1"));
		gsUrlList.add(new GSUrlDto("http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=149-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%AB%CE%E1+%E8%ED%F4%EE%F0%EC%E0%F6%E8%E8%2C+%E8%ED%F4%EE%F0%EC%E0%F6%E8%EE%ED%ED%FB%F5+%F2%E5%F5%ED%EE%EB%EE%E3%E8%FF%F5+%E8+%E7%E0%F9%E8%F2%E5+%E8%ED%F4%EE%F0%EC%E0%F6%E8%E8&sort=-1","http://pravo.gov.ru/proxy/ips/?docbody=&link_id=8&nd=102108264&fulltext=1&firstDoc=1"));
		gsUrlList.add(new GSUrlDto("http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=98-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EA%EE%EC%EC%E5%F0%F7%E5%F1%EA%EE%E9+%F2%E0%E9%ED%E5&sort=-1","http://pravo.gov.ru/proxy/ips/?docbody=&link_id=0&nd=102088094&fulltext=1&firstDoc=1"));
		gsUrlList.add(new GSUrlDto("http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=135-+%D4%C7&sort=-1","https://pravo.gov.ru/proxy/ips/?docbody=&nd=102108256&firstDoc=1"));

		return gsUrlList;

	}
}
