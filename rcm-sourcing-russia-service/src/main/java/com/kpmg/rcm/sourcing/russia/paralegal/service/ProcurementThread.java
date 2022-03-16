package com.kpmg.rcm.sourcing.russia.paralegal.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.util.UriComponentsBuilder;

import com.kpmg.rcm.sourcing.russia.constants.RussiaSourceConstants;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Builder
@Data
@Slf4j
public class ProcurementThread implements Runnable {

	private String baseUrl;
	private String versionExtractionUrl;
	private String docExtractionUrl;
	private String dirPath;
	private Boolean gsApplicable;
	private static final int baseLineYear = 2008;
	private static final String originalEdition = "Исходная редакция";

	@Override
	public void run() {

		if (gsApplicable != null && gsApplicable.equals(true)) {
			Pattern pattern = Pattern.compile("&nd=(.*?)&", Pattern.DOTALL);
			Matcher matcher = pattern.matcher(versionExtractionUrl);
			if (matcher.find()) {
				procurementLogic(matcher.group(1).trim());
			}
			return;
		}
		Document document = getJsoupDocument(baseUrl);
		if(document==null)
			return;
		for (Element input : document.select("input")) {
			String uniqueNum = input.attr("name").substring(6);
			procurementLogic(uniqueNum);
		}
	}

	@Retryable(value = IOException.class)
	public void downloadPageWithRetryLogic(String targetUrl, String uniqueNum, String directoryPath)
			throws IOException {
		URLConnection htmlUrl = new URL(targetUrl).openConnection();
		htmlUrl.setRequestProperty("User-Agent", "Mozilla 5.0 (Windows; U; " + "Windows NT 5.1; en-US; rv:1.8.0.11) ");
		InputStream inputStream = htmlUrl.getInputStream();
//		if ((new File(directoryPath + "/" + uniqueNum + ".html")).exists())
//			return;
		try (BufferedInputStream in = new BufferedInputStream(inputStream);
				FileOutputStream fileOutputStream = new FileOutputStream(directoryPath + "/" + uniqueNum + ".html")) {
			byte dataBuffer[] = new byte[1024];
			int bytesRead;
			while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
				fileOutputStream.write(dataBuffer, 0, bytesRead);
			}
			fileOutputStream.close();

		} catch (IOException ex) {
			log.error("Error downloading url :: " + targetUrl, ex);
			ParaLegalService.storeUnreachableURLs(targetUrl, dirPath);
		}
	}

	@Recover
	public void downloadPageRecovery(IOException e, String targetUrl, String uniqueNum, String directoryPath)
			throws IOException {
		log.error("Error downloading url :: " + targetUrl, e);
		ParaLegalService.storeUnreachableURLs(targetUrl, dirPath);
	}

	public Document getJsoupDocument(String url) {
		Document doc = null;
		try {
			doc = Jsoup.connect(url).get();
		} catch (Exception e) {
			log.error("The Web link :: " + url + " is not available", e);
			try {
				ParaLegalService.storeUnreachableURLs(url, dirPath);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		return doc;
	}

	private void procurementLogic(String uniqueNum) {
		String verUri = null;
		if (gsApplicable != null & gsApplicable.equals(true))
			verUri = versionExtractionUrl;
		else
			verUri = UriComponentsBuilder.fromUriString(versionExtractionUrl).queryParam("nd", uniqueNum).build()
					.toUriString();

		Document doc = getJsoupDocument(verUri);

		Elements options = doc.select("select[name=doc_editions] > option");
		
		Elements newOptions = new Elements();
		for(Element option: options) {
			if(option.hasAttr("disabled")) {
				System.out.println("The document which contain disabled attribute "+uniqueNum);
				continue;
			}
			newOptions.add(option);
		}
		int latestFileIndex=newOptions.size()-1;
		if (newOptions != null && newOptions.size() > 0) {
			newOptions.forEach(o -> {
				/*if(gsApplicable!=null && gsApplicable.equals(true) && options.indexOf(o)<latestFileIndex) {
					return;
				}*/
				String versionValue = "";
				if (originalEdition.equals(o.text().trim())) {
					versionValue = RussiaSourceConstants.orgEditionString;
				} else {
					versionValue = o.text().trim();
					Pattern pattern = Pattern.compile("от(.*?)№", Pattern.DOTALL);
					Matcher matcher = pattern.matcher(versionValue);
					if (matcher.find()) {
						versionValue = matcher.group(1).trim();
						System.out.println("VersionValue::: " + versionValue);
					}
					if (Integer.parseInt(versionValue.split("\\.")[2].substring(0, 4).trim()) < baseLineYear) {
						log.error(
								"Skipped file for version ::  " + versionValue.split("\\.")[2].substring(0, 4).trim());
						return;
					}

				}
				System.out.println("Outer VersionValue::: " + versionValue);
				String versionNo = o.attr("value").split(",")[0];
				String docURL = "";
				
				if(gsApplicable!=null && gsApplicable.equals(true))
					docURL=versionExtractionUrl.replace("docbody=", "doc_itself=");
				else
					docURL=docExtractionUrl;
				
				String targetUri = UriComponentsBuilder.fromUriString(docURL).queryParam("nd", uniqueNum)
						.queryParam("rdk", versionNo).build().toUriString();//
				try {
					downloadPageWithRetryLogic(targetUri, uniqueNum + "_v" + versionValue+"_rdk"+versionNo, dirPath);
				} catch (IOException e) {
					log.error("Download failed for :: " + targetUri, e);
					try {
						ParaLegalService.storeUnreachableURLs(targetUri, dirPath);
					} catch (IOException e1) {
						log.error("Not able to store URL ", e1);
					}
				}
			});
		}
	}
}
