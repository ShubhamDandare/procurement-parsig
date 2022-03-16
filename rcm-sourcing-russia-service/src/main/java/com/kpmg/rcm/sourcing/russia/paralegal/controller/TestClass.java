package com.kpmg.rcm.sourcing.russia.paralegal.controller;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

import com.kpmg.rcm.sourcing.russia.paralegal.vo.TestDto;

public class TestClass {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		TestClass testClass = new TestClass();
		testClass.newfunction();

	}
	void testparser() {
		Document document;
		try {
			document = Jsoup.parse(new File("D:\\var\\tmp\\102000505IndexPagess\\602682821_vИсходная редакция.html"),null);
			Elements paras=document.select("p");
			int size = document.select("p").size();
			Map docDetails = new HashMap();
			boolean isArticleContinued=false;
			boolean isSubArticleContinued=false;
			boolean isHeadingStarted=false;
			Pattern pattern1 = Pattern.compile("^Статья\\s\\d\\.*(.*)", Pattern.DOTALL | Pattern.UNICODE_CASE);
			Pattern pattern2 = Pattern.compile("^\\d\\.*\\)*\\s(.*)", Pattern.DOTALL | Pattern.UNICODE_CASE);
			Pattern pattern3=Pattern.compile("Президент Российской Федерации",Pattern.DOTALL | Pattern.UNICODE_CASE);
			String article="";
			String subArticle="";
			String articleMetadata="";
			String subArticleMetadata="";
			String heading = "";
			String _abstract = "";
			String note = "";
			String dates = "";
			ArrayList<String> aliase = new ArrayList<>();
			int counter=0;
			for (Element e : paras) {
				Matcher matcher1 = pattern1.matcher(e.text());
				Matcher matcher2 = pattern2.matcher(e.text());
				Matcher matcher3=pattern3.matcher(e.text());
				if (e.hasClass("T")) {
					isHeadingStarted=true;
					heading += e.text() + "\n";
				} else if (e.hasClass("C")) {
					heading += e.text() + "\n";
				} else if (counter == size - 6 || counter == size - 4) {
					note += e.text();
				} 
				  else if(matcher1.find()) {
					isHeadingStarted=false;  
					article=matcher1.group(1);
					articleMetadata=matcher1.group(0).split("\\.")[0];
					isArticleContinued=true;
//					docDetails.put(articleMetadata,article);
				  }
				else if(matcher2.find()) {
					subArticleMetadata=matcher2.group(0).split("\\.")[0];
					subArticle=matcher2.group(1);
					isSubArticleContinued=true;
					isArticleContinued=false;
					docDetails.put(articleMetadata+"#"+subArticleMetadata, subArticle);				
				}
				else if(matcher3.find()) {
					break;
				}
				else if(isArticleContinued) {
					docDetails.put(articleMetadata,(docDetails.get(articleMetadata)!=null?docDetails.get(articleMetadata):"")+e.text());
//					docDetails.put(articleMetadata,docDetails.get(articleMetadata)+e.text());
				}
				else if(isSubArticleContinued) {	
					
					docDetails.put(articleMetadata+"#"+subArticleMetadata,docDetails.get(subArticleMetadata)+e.text());
					isSubArticleContinued=false;
					subArticle="";
					subArticleMetadata="";
				}
				else if (counter == size - 3) {
						dates = e.text();
				} else if (counter == size - 2) {
						aliase.add(e.text());
				} else if (isHeadingStarted) {
						heading += e.text() + "\n";
				}
				counter++;
				}
			docDetails.put("heading", heading);
			docDetails.put("abstract", _abstract);
			docDetails.put("Notes", note);
			docDetails.put("date", dates);   
			docDetails.put("aliases",aliase);
			docDetails.put("citation",aliase);
			System.out.println(docDetails);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
	}
	void newfunction() {

		List<TestDto> dtoList = populateDto();
		ExecutorService executorService = Executors.newCachedThreadPool();
		for (TestDto testDto : dtoList) {
			String url = testDto.getUrl().replace("?searchres=", "?list_itself").concat("&start=0&lstsize=99999999");
			Document doc = getJsoupDocument(url);
			List<String> urlList = new ArrayList<>();
			Elements tables = doc.select("table");
			Pattern pattern1 = Pattern.compile("от\\s*(\\d*\\.\\d*\\.\\d*)\\s*г",
					Pattern.DOTALL | Pattern.UNICODE_CASE);
			
			
			
			Runnable runnable = new Thread() {
				public void run(){
					runTask(doc, testDto);
				};
			};
			
			executorService.execute(runnable);
		}
		executorService.shutdown();
	}

	public void storeUnreachableURLs(String url, String dirPath) throws IOException {
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

	public List<TestDto> populateDto() {
		List<TestDto> dtoList = new ArrayList<TestDto>();
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
		try {
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=39-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%F0%FB%ED%EA%E5+%F6%E5%ED%ED%FB%F5+%E1%F3%EC%E0%E3&sort=-1",
					sdf.parse("04/22/1996")));

			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=39-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%F0%FB%ED%EA%E5+%F6%E5%ED%ED%FB%F5+%E1%F3%EC%E0%E3&sort=-1",
					sdf.parse("04/22/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=39-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%F0%FB%ED%EA%E5+%F6%E5%ED%ED%FB%F5+%E1%F3%EC%E0%E3&sort=-1",
					sdf.parse("04/22/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=39-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%F0%FB%ED%EA%E5+%F6%E5%ED%ED%FB%F5+%E1%F3%EC%E0%E3&sort=-1",
					sdf.parse("04/22/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=39-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%F0%FB%ED%EA%E5+%F6%E5%ED%ED%FB%F5+%E1%F3%EC%E0%E3&sort=-1",
					sdf.parse("04/22/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=39-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%F0%FB%ED%EA%E5+%F6%E5%ED%ED%FB%F5+%E1%F3%EC%E0%E3&sort=-1",
					sdf.parse("04/22/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=39-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%F0%FB%ED%EA%E5+%F6%E5%ED%ED%FB%F5+%E1%F3%EC%E0%E3&sort=-1",
					sdf.parse("04/22/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=173-%D4%C7.+%CE+%E2%E0%EB%FE%F2%ED%EE%EC+%F0%E5%E3%F3%EB%E8%F0%EE%E2%E0%ED%E8%E8+%E8+%E2%E0%EB%FE%F2%ED%EE%EC+%EA%EE%ED%F2%F0%EE%EB%E5&sort=-1",
					sdf.parse("06/10/2004")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=173-%D4%C7.+%CE+%E2%E0%EB%FE%F2%ED%EE%EC+%F0%E5%E3%F3%EB%E8%F0%EE%E2%E0%ED%E8%E8+%E8+%E2%E0%EB%FE%F2%ED%EE%EC+%EA%EE%ED%F2%F0%EE%EB%E5&sort=-1",
					sdf.parse("06/10/2004")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=173-%D4%C7.+%CE+%E2%E0%EB%FE%F2%ED%EE%EC+%F0%E5%E3%F3%EB%E8%F0%EE%E2%E0%ED%E8%E8+%E8+%E2%E0%EB%FE%F2%ED%EE%EC+%EA%EE%ED%F2%F0%EE%EB%E5&sort=-1",
					sdf.parse("06/10/2004")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=173-%D4%C7.+%CE+%E2%E0%EB%FE%F2%ED%EE%EC+%F0%E5%E3%F3%EB%E8%F0%EE%E2%E0%ED%E8%E8+%E8+%E2%E0%EB%FE%F2%ED%EE%EC+%EA%EE%ED%F2%F0%EE%EB%E5&sort=-1",
					sdf.parse("06/10/2004")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=173-%D4%C7.+%CE+%E2%E0%EB%FE%F2%ED%EE%EC+%F0%E5%E3%F3%EB%E8%F0%EE%E2%E0%ED%E8%E8+%E8+%E2%E0%EB%FE%F2%ED%EE%EC+%EA%EE%ED%F2%F0%EE%EB%E5&sort=-1",
					sdf.parse("06/10/2004")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=173-%D4%C7.+%CE+%E2%E0%EB%FE%F2%ED%EE%EC+%F0%E5%E3%F3%EB%E8%F0%EE%E2%E0%ED%E8%E8+%E8+%E2%E0%EB%FE%F2%ED%EE%EC+%EA%EE%ED%F2%F0%EE%EB%E5&sort=-1",
					sdf.parse("06/10/2004")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=115-%D4%C7.+&sort=-1",
					sdf.parse("02/01/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=115-%D4%C7.+&sort=-1",
					sdf.parse("02/01/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=115-%D4%C7.+&sort=-1",
					sdf.parse("02/01/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=115-%D4%C7.+&sort=-1",
					sdf.parse("02/01/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=115-%D4%C7.+&sort=-1",
					sdf.parse("02/01/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=115-%D4%C7.+&sort=-1",
					sdf.parse("02/01/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=129-%D4%C7.+%CE+%E3%EE%F1%F3%E4%E0%F0%F1%F2%E2%E5%ED%ED%EE%E9+%F0%E5%E3%E8%F1%F2%F0%E0%F6%E8%E8+%FE%F0%E8%E4%E8%F7%E5%F1%EA%E8%F5+%EB%E8%F6+%E8+%E8%ED%E4%E8%E2%E8%E4%F3%E0%EB%FC%ED%FB%F5+%EF%F0%E5%E4%EF%F0%E8%ED%E8%EC%E0%F2%E5%EB%E5%E9&sort=-1",
					sdf.parse("07/01/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=129-%D4%C7.+%CE+%E3%EE%F1%F3%E4%E0%F0%F1%F2%E2%E5%ED%ED%EE%E9+%F0%E5%E3%E8%F1%F2%F0%E0%F6%E8%E8+%FE%F0%E8%E4%E8%F7%E5%F1%EA%E8%F5+%EB%E8%F6+%E8+%E8%ED%E4%E8%E2%E8%E4%F3%E0%EB%FC%ED%FB%F5+%EF%F0%E5%E4%EF%F0%E8%ED%E8%EC%E0%F2%E5%EB%E5%E9&sort=-1",
					sdf.parse("07/01/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=129-%D4%C7.+%CE+%E3%EE%F1%F3%E4%E0%F0%F1%F2%E2%E5%ED%ED%EE%E9+%F0%E5%E3%E8%F1%F2%F0%E0%F6%E8%E8+%FE%F0%E8%E4%E8%F7%E5%F1%EA%E8%F5+%EB%E8%F6+%E8+%E8%ED%E4%E8%E2%E8%E4%F3%E0%EB%FC%ED%FB%F5+%EF%F0%E5%E4%EF%F0%E8%ED%E8%EC%E0%F2%E5%EB%E5%E9&sort=-1",
					sdf.parse("07/01/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=129-%D4%C7.+%CE+%E3%EE%F1%F3%E4%E0%F0%F1%F2%E2%E5%ED%ED%EE%E9+%F0%E5%E3%E8%F1%F2%F0%E0%F6%E8%E8+%FE%F0%E8%E4%E8%F7%E5%F1%EA%E8%F5+%EB%E8%F6+%E8+%E8%ED%E4%E8%E2%E8%E4%F3%E0%EB%FC%ED%FB%F5+%EF%F0%E5%E4%EF%F0%E8%ED%E8%EC%E0%F2%E5%EB%E5%E9&sort=-1",
					sdf.parse("07/01/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=129-%D4%C7.+%CE+%E3%EE%F1%F3%E4%E0%F0%F1%F2%E2%E5%ED%ED%EE%E9+%F0%E5%E3%E8%F1%F2%F0%E0%F6%E8%E8+%FE%F0%E8%E4%E8%F7%E5%F1%EA%E8%F5+%EB%E8%F6+%E8+%E8%ED%E4%E8%E2%E8%E4%F3%E0%EB%FC%ED%FB%F5+%EF%F0%E5%E4%EF%F0%E8%ED%E8%EC%E0%F2%E5%EB%E5%E9&sort=-1",
					sdf.parse("07/01/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=129-%D4%C7.+%CE+%E3%EE%F1%F3%E4%E0%F0%F1%F2%E2%E5%ED%ED%EE%E9+%F0%E5%E3%E8%F1%F2%F0%E0%F6%E8%E8+%FE%F0%E8%E4%E8%F7%E5%F1%EA%E8%F5+%EB%E8%F6+%E8+%E8%ED%E4%E8%E2%E8%E4%F3%E0%EB%FC%ED%FB%F5+%EF%F0%E5%E4%EF%F0%E8%ED%E8%EC%E0%F2%E5%EB%E5%E9&sort=-1",
					sdf.parse("07/01/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=129-%D4%C7.+%CE+%E3%EE%F1%F3%E4%E0%F0%F1%F2%E2%E5%ED%ED%EE%E9+%F0%E5%E3%E8%F1%F2%F0%E0%F6%E8%E8+%FE%F0%E8%E4%E8%F7%E5%F1%EA%E8%F5+%EB%E8%F6+%E8+%E8%ED%E4%E8%E2%E8%E4%F3%E0%EB%FC%ED%FB%F5+%EF%F0%E5%E4%EF%F0%E8%ED%E8%EC%E0%F2%E5%EB%E5%E9&sort=-1",
					sdf.parse("07/01/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=129-%D4%C7.+%CE+%E3%EE%F1%F3%E4%E0%F0%F1%F2%E2%E5%ED%ED%EE%E9+%F0%E5%E3%E8%F1%F2%F0%E0%F6%E8%E8+%FE%F0%E8%E4%E8%F7%E5%F1%EA%E8%F5+%EB%E8%F6+%E8+%E8%ED%E4%E8%E2%E8%E4%F3%E0%EB%FC%ED%FB%F5+%EF%F0%E5%E4%EF%F0%E8%ED%E8%EC%E0%F2%E5%EB%E5%E9&sort=-1",
					sdf.parse("07/01/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=129-%D4%C7.+%CE+%E3%EE%F1%F3%E4%E0%F0%F1%F2%E2%E5%ED%ED%EE%E9+%F0%E5%E3%E8%F1%F2%F0%E0%F6%E8%E8+%FE%F0%E8%E4%E8%F7%E5%F1%EA%E8%F5+%EB%E8%F6+%E8+%E8%ED%E4%E8%E2%E8%E4%F3%E0%EB%FC%ED%FB%F5+%EF%F0%E5%E4%EF%F0%E8%ED%E8%EC%E0%F2%E5%EB%E5%E9&sort=-1",
					sdf.parse("07/01/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=129-%D4%C7.+%CE+%E3%EE%F1%F3%E4%E0%F0%F1%F2%E2%E5%ED%ED%EE%E9+%F0%E5%E3%E8%F1%F2%F0%E0%F6%E8%E8+%FE%F0%E8%E4%E8%F7%E5%F1%EA%E8%F5+%EB%E8%F6+%E8+%E8%ED%E4%E8%E2%E8%E4%F3%E0%EB%FC%ED%FB%F5+%EF%F0%E5%E4%EF%F0%E8%ED%E8%EC%E0%F2%E5%EB%E5%E9&sort=-1",
					sdf.parse("07/01/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=129-%D4%C7.+%CE+%E3%EE%F1%F3%E4%E0%F0%F1%F2%E2%E5%ED%ED%EE%E9+%F0%E5%E3%E8%F1%F2%F0%E0%F6%E8%E8+%FE%F0%E8%E4%E8%F7%E5%F1%EA%E8%F5+%EB%E8%F6+%E8+%E8%ED%E4%E8%E2%E8%E4%F3%E0%EB%FC%ED%FB%F5+%EF%F0%E5%E4%EF%F0%E8%ED%E8%EC%E0%F2%E5%EB%E5%E9&sort=-1",
					sdf.parse("07/01/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=129-%D4%C7.+%CE+%E3%EE%F1%F3%E4%E0%F0%F1%F2%E2%E5%ED%ED%EE%E9+%F0%E5%E3%E8%F1%F2%F0%E0%F6%E8%E8+%FE%F0%E8%E4%E8%F7%E5%F1%EA%E8%F5+%EB%E8%F6+%E8+%E8%ED%E4%E8%E2%E8%E4%F3%E0%EB%FC%ED%FB%F5+%EF%F0%E5%E4%EF%F0%E8%ED%E8%EC%E0%F2%E5%EB%E5%E9&sort=-1",
					sdf.parse("07/01/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=129-%D4%C7.+%CE+%E3%EE%F1%F3%E4%E0%F0%F1%F2%E2%E5%ED%ED%EE%E9+%F0%E5%E3%E8%F1%F2%F0%E0%F6%E8%E8+%FE%F0%E8%E4%E8%F7%E5%F1%EA%E8%F5+%EB%E8%F6+%E8+%E8%ED%E4%E8%E2%E8%E4%F3%E0%EB%FC%ED%FB%F5+%EF%F0%E5%E4%EF%F0%E8%ED%E8%EC%E0%F2%E5%EB%E5%E9&sort=-1",
					sdf.parse("07/01/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=129-%D4%C7.+%CE+%E3%EE%F1%F3%E4%E0%F0%F1%F2%E2%E5%ED%ED%EE%E9+%F0%E5%E3%E8%F1%F2%F0%E0%F6%E8%E8+%FE%F0%E8%E4%E8%F7%E5%F1%EA%E8%F5+%EB%E8%F6+%E8+%E8%ED%E4%E8%E2%E8%E4%F3%E0%EB%FC%ED%FB%F5+%EF%F0%E5%E4%EF%F0%E8%ED%E8%EC%E0%F2%E5%EB%E5%E9&sort=-1",
					sdf.parse("07/01/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=129-%D4%C7.+%CE+%E3%EE%F1%F3%E4%E0%F0%F1%F2%E2%E5%ED%ED%EE%E9+%F0%E5%E3%E8%F1%F2%F0%E0%F6%E8%E8+%FE%F0%E8%E4%E8%F7%E5%F1%EA%E8%F5+%EB%E8%F6+%E8+%E8%ED%E4%E8%E2%E8%E4%F3%E0%EB%FC%ED%FB%F5+%EF%F0%E5%E4%EF%F0%E8%ED%E8%EC%E0%F2%E5%EB%E5%E9&sort=-1",
					sdf.parse("07/01/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=129-%D4%C7.+%CE+%E3%EE%F1%F3%E4%E0%F0%F1%F2%E2%E5%ED%ED%EE%E9+%F0%E5%E3%E8%F1%F2%F0%E0%F6%E8%E8+%FE%F0%E8%E4%E8%F7%E5%F1%EA%E8%F5+%EB%E8%F6+%E8+%E8%ED%E4%E8%E2%E8%E4%F3%E0%EB%FC%ED%FB%F5+%EF%F0%E5%E4%EF%F0%E8%ED%E8%EC%E0%F2%E5%EB%E5%E9&sort=-1",
					sdf.parse("07/01/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=129-%D4%C7.+%CE+%E3%EE%F1%F3%E4%E0%F0%F1%F2%E2%E5%ED%ED%EE%E9+%F0%E5%E3%E8%F1%F2%F0%E0%F6%E8%E8+%FE%F0%E8%E4%E8%F7%E5%F1%EA%E8%F5+%EB%E8%F6+%E8+%E8%ED%E4%E8%E2%E8%E4%F3%E0%EB%FC%ED%FB%F5+%EF%F0%E5%E4%EF%F0%E8%ED%E8%EC%E0%F2%E5%EB%E5%E9&sort=-1",
					sdf.parse("07/01/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("01/27/2007")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("01/27/2007")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("01/27/2007")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("01/27/2007")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("01/27/2007")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("01/27/2007")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("01/27/2007")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("01/27/2007")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("01/27/2007")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("01/27/2007")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("01/27/2007")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("01/27/2007")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("01/27/2007")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("01/27/2007")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("01/27/2007")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("01/27/2007")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("10/26/2006")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("10/26/2006")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("10/26/2006")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("10/26/2006")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("10/26/2006")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("10/26/2006")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("10/26/2006")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("10/26/2006")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("10/26/2006")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("10/26/2006")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("10/26/2006")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("10/26/2006")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("10/26/2006")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("10/26/2006")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("10/26/2006")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("10/26/2006")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("10/26/2006")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=152-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EF%E5%F0%F1%EE%ED%E0%EB%FC%ED%FB%F5+%E4%E0%ED%ED%FB%F5&sort=-1",
					sdf.parse("10/26/2006")));
			dtoList.add(
					new TestDto("http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=+46-%D4%C7&sort=-1",
							sdf.parse("03/05/1999")));
			dtoList.add(
					new TestDto("http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=+46-%D4%C7&sort=-1",
							sdf.parse("03/05/1999")));
			dtoList.add(
					new TestDto("http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=+46-%D4%C7&sort=-1",
							sdf.parse("03/05/1999")));
			dtoList.add(
					new TestDto("http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=+46-%D4%C7&sort=-1",
							sdf.parse("03/05/1999")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=224-%D4%C7+%CE+%EF%F0%EE%F2%E8%E2%EE%E4%E5%E9%F1%F2%E2%E8%E8+%ED%E5%EF%F0%E0%E2%EE%EC%E5%F0%ED%EE%EC%F3+%E8%F1%EF%EE%EB%FC%E7%EE%E2%E0%ED%E8%FE+%E8%ED%F1%E0%E9%E4%E5%F0%F1%EA%EE%E9+%E8%ED%F4%EE%F0%EC%E0%F6%E8%E8+%E8+%EC%E0%ED%E8%EF%F3%EB%E8%F0%EE%E2%E0%ED%E8%FE+%F0%FB%ED%EA%EE%EC&sort=-1",
					sdf.parse("01/23/2011")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=224-%D4%C7+%CE+%EF%F0%EE%F2%E8%E2%EE%E4%E5%E9%F1%F2%E2%E8%E8+%ED%E5%EF%F0%E0%E2%EE%EC%E5%F0%ED%EE%EC%F3+%E8%F1%EF%EE%EB%FC%E7%EE%E2%E0%ED%E8%FE+%E8%ED%F1%E0%E9%E4%E5%F0%F1%EA%EE%E9+%E8%ED%F4%EE%F0%EC%E0%F6%E8%E8+%E8+%EC%E0%ED%E8%EF%F3%EB%E8%F0%EE%E2%E0%ED%E8%FE+%F0%FB%ED%EA%EE%EC&sort=-1",
					sdf.parse("01/23/2011")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=224-%D4%C7+%CE+%EF%F0%EE%F2%E8%E2%EE%E4%E5%E9%F1%F2%E2%E8%E8+%ED%E5%EF%F0%E0%E2%EE%EC%E5%F0%ED%EE%EC%F3+%E8%F1%EF%EE%EB%FC%E7%EE%E2%E0%ED%E8%FE+%E8%ED%F1%E0%E9%E4%E5%F0%F1%EA%EE%E9+%E8%ED%F4%EE%F0%EC%E0%F6%E8%E8+%E8+%EC%E0%ED%E8%EF%F3%EB%E8%F0%EE%E2%E0%ED%E8%FE+%F0%FB%ED%EA%EE%EC&sort=-1",
					sdf.parse("01/23/2011")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=224-%D4%C7+%CE+%EF%F0%EE%F2%E8%E2%EE%E4%E5%E9%F1%F2%E2%E8%E8+%ED%E5%EF%F0%E0%E2%EE%EC%E5%F0%ED%EE%EC%F3+%E8%F1%EF%EE%EB%FC%E7%EE%E2%E0%ED%E8%FE+%E8%ED%F1%E0%E9%E4%E5%F0%F1%EA%EE%E9+%E8%ED%F4%EE%F0%EC%E0%F6%E8%E8+%E8+%EC%E0%ED%E8%EF%F3%EB%E8%F0%EE%E2%E0%ED%E8%FE+%F0%FB%ED%EA%EE%EC&sort=-1",
					sdf.parse("01/23/2011")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=224-%D4%C7+%CE+%EF%F0%EE%F2%E8%E2%EE%E4%E5%E9%F1%F2%E2%E8%E8+%ED%E5%EF%F0%E0%E2%EE%EC%E5%F0%ED%EE%EC%F3+%E8%F1%EF%EE%EB%FC%E7%EE%E2%E0%ED%E8%FE+%E8%ED%F1%E0%E9%E4%E5%F0%F1%EA%EE%E9+%E8%ED%F4%EE%F0%EC%E0%F6%E8%E8+%E8+%EC%E0%ED%E8%EF%F3%EB%E8%F0%EE%E2%E0%ED%E8%FE+%F0%FB%ED%EA%EE%EC&sort=-1",
					sdf.parse("01/23/2011")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=224-%D4%C7+%CE+%EF%F0%EE%F2%E8%E2%EE%E4%E5%E9%F1%F2%E2%E8%E8+%ED%E5%EF%F0%E0%E2%EE%EC%E5%F0%ED%EE%EC%F3+%E8%F1%EF%EE%EB%FC%E7%EE%E2%E0%ED%E8%FE+%E8%ED%F1%E0%E9%E4%E5%F0%F1%EA%EE%E9+%E8%ED%F4%EE%F0%EC%E0%F6%E8%E8+%E8+%EC%E0%ED%E8%EF%F3%EB%E8%F0%EE%E2%E0%ED%E8%FE+%F0%FB%ED%EA%EE%EC&sort=-1",
					sdf.parse("01/23/2011")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=127-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%ED%E5%F1%EE%F1%F2%EE%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8+%28%E1%E0%ED%EA%F0%EE%F2%F1%F2%E2%E5%29&sort=-1",
					sdf.parse("11/25/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=127-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%ED%E5%F1%EE%F1%F2%EE%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8+%28%E1%E0%ED%EA%F0%EE%F2%F1%F2%E2%E5%29&sort=-1",
					sdf.parse("11/25/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=127-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%ED%E5%F1%EE%F1%F2%EE%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8+%28%E1%E0%ED%EA%F0%EE%F2%F1%F2%E2%E5%29&sort=-1",
					sdf.parse("11/25/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=127-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%ED%E5%F1%EE%F1%F2%EE%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8+%28%E1%E0%ED%EA%F0%EE%F2%F1%F2%E2%E5%29&sort=-1",
					sdf.parse("11/25/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=127-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%ED%E5%F1%EE%F1%F2%EE%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8+%28%E1%E0%ED%EA%F0%EE%F2%F1%F2%E2%E5%29&sort=-1",
					sdf.parse("11/25/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=127-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%ED%E5%F1%EE%F1%F2%EE%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8+%28%E1%E0%ED%EA%F0%EE%F2%F1%F2%E2%E5%29&sort=-1",
					sdf.parse("11/25/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=127-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%ED%E5%F1%EE%F1%F2%EE%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8+%28%E1%E0%ED%EA%F0%EE%F2%F1%F2%E2%E5%29&sort=-1",
					sdf.parse("11/25/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=127-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%ED%E5%F1%EE%F1%F2%EE%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8+%28%E1%E0%ED%EA%F0%EE%F2%F1%F2%E2%E5%29&sort=-1",
					sdf.parse("11/25/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=127-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%ED%E5%F1%EE%F1%F2%EE%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8+%28%E1%E0%ED%EA%F0%EE%F2%F1%F2%E2%E5%29&sort=-1",
					sdf.parse("11/25/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=127-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%ED%E5%F1%EE%F1%F2%EE%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8+%28%E1%E0%ED%EA%F0%EE%F2%F1%F2%E2%E5%29&sort=-1",
					sdf.parse("11/25/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=173-%D4%C7.%CE%E1+%EE%F1%EE%E1%E5%ED%ED%EE%F1%F2%FF%F5+%F4%E8%ED%E0%ED%F1%EE%E2%FB%F5+%EE%EF%E5%F0%E0%F6%E8%E9+%F1+%E8%ED%EE%F1%F2%F0%E0%ED%ED%FB%EC%E8+%F4%E8%E7%E8%F7%E5%F1%EA%E8%EC%E8+%E8+%FE%F0%E8%E4%E8%F7%E5%F1%EA%E8%EC%E8+%EB%E8%F6%E0%EC%E8&sort=-1",
					sdf.parse("06/30/2014")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=+%E8%ED%F1%E0%E9%E4%E5%F0%EE%E2+%EE%F0%E3%E0%ED%E8%E7%E0%F2%EE%F0%E0%EC+%F2%EE%F0%E3%EE%E2%EB%E8+%F4%E8%ED%E0%ED%F1%EE%E2%FB%EC%E8+%E8%ED%F1%F2%F0%F3%EC%E5%ED%F2%E0%EC%E8%2C+%E8%ED%EE%F1%F2%F0%E0%ED%ED%EE%E9+%E2%E0%EB%FE%F2%EE%E9+%E8+%28%E8%EB%E8%29+%F2%EE%E2%E0%F0%E0%EC%E8&sort=-1",
					sdf.parse("08/30/2013")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=+%E8%ED%F1%E0%E9%E4%E5%F0%EE%E2+%EE%F0%E3%E0%ED%E8%E7%E0%F2%EE%F0%E0%EC+%F2%EE%F0%E3%EE%E2%EB%E8+%F4%E8%ED%E0%ED%F1%EE%E2%FB%EC%E8+%E8%ED%F1%F2%F0%F3%EC%E5%ED%F2%E0%EC%E8%2C+%E8%ED%EE%F1%F2%F0%E0%ED%ED%EE%E9+%E2%E0%EB%FE%F2%EE%E9+%E8+%28%E8%EB%E8%29+%F2%EE%E2%E0%F0%E0%EC%E8&sort=-1",
					sdf.parse("07/27/2012")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=86-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%D6%E5%ED%F2%F0%E0%EB%FC%ED%EE%EC+%E1%E0%ED%EA%E5+%D0%EE%F1%F1%E8%E9%F1%EA%EE%E9+%D4%E5%E4%E5%F0%E0%F6%E8%E8+%28%C1%E0%ED%EA%E5+%D0%EE%F1%F1%E8%E8%29&sort=-1",
					sdf.parse("07/13/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=86-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%D6%E5%ED%F2%F0%E0%EB%FC%ED%EE%EC+%E1%E0%ED%EA%E5+%D0%EE%F1%F1%E8%E9%F1%EA%EE%E9+%D4%E5%E4%E5%F0%E0%F6%E8%E8+%28%C1%E0%ED%EA%E5+%D0%EE%F1%F1%E8%E8%29&sort=-1",
					sdf.parse("07/13/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=86-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%D6%E5%ED%F2%F0%E0%EB%FC%ED%EE%EC+%E1%E0%ED%EA%E5+%D0%EE%F1%F1%E8%E9%F1%EA%EE%E9+%D4%E5%E4%E5%F0%E0%F6%E8%E8+%28%C1%E0%ED%EA%E5+%D0%EE%F1%F1%E8%E8%29&sort=-1",
					sdf.parse("07/13/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=86-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%D6%E5%ED%F2%F0%E0%EB%FC%ED%EE%EC+%E1%E0%ED%EA%E5+%D0%EE%F1%F1%E8%E9%F1%EA%EE%E9+%D4%E5%E4%E5%F0%E0%F6%E8%E8+%28%C1%E0%ED%EA%E5+%D0%EE%F1%F1%E8%E8%29&sort=-1",
					sdf.parse("07/13/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=395-1-%D4%C7.+%CE+%E1%E0%ED%EA%E0%F5+%E8+%E1%E0%ED%EA%EE%E2%F1%EA%EE%E9+%E4%E5%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8&sort=-1",
					sdf.parse("02/05/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=395-1-%D4%C7.+%CE+%E1%E0%ED%EA%E0%F5+%E8+%E1%E0%ED%EA%EE%E2%F1%EA%EE%E9+%E4%E5%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8&sort=-1",
					sdf.parse("02/05/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=395-1-%D4%C7.+%CE+%E1%E0%ED%EA%E0%F5+%E8+%E1%E0%ED%EA%EE%E2%F1%EA%EE%E9+%E4%E5%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8&sort=-1",
					sdf.parse("02/05/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=395-1-%D4%C7.+%CE+%E1%E0%ED%EA%E0%F5+%E8+%E1%E0%ED%EA%EE%E2%F1%EA%EE%E9+%E4%E5%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8&sort=-1",
					sdf.parse("02/05/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=395-1-%D4%C7.+%CE+%E1%E0%ED%EA%E0%F5+%E8+%E1%E0%ED%EA%EE%E2%F1%EA%EE%E9+%E4%E5%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8&sort=-1",
					sdf.parse("02/05/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=395-1-%D4%C7.+%CE+%E1%E0%ED%EA%E0%F5+%E8+%E1%E0%ED%EA%EE%E2%F1%EA%EE%E9+%E4%E5%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8&sort=-1",
					sdf.parse("02/05/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=395-1-%D4%C7.+%CE+%E1%E0%ED%EA%E0%F5+%E8+%E1%E0%ED%EA%EE%E2%F1%EA%EE%E9+%E4%E5%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8&sort=-1",
					sdf.parse("02/05/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=395-1-%D4%C7.+%CE+%E1%E0%ED%EA%E0%F5+%E8+%E1%E0%ED%EA%EE%E2%F1%EA%EE%E9+%E4%E5%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8&sort=-1",
					sdf.parse("02/05/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=395-1-%D4%C7.+%CE+%E1%E0%ED%EA%E0%F5+%E8+%E1%E0%ED%EA%EE%E2%F1%EA%EE%E9+%E4%E5%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8&sort=-1",
					sdf.parse("02/05/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=395-1-%D4%C7.+%CE+%E1%E0%ED%EA%E0%F5+%E8+%E1%E0%ED%EA%EE%E2%F1%EA%EE%E9+%E4%E5%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8&sort=-1",
					sdf.parse("02/05/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=395-1-%D4%C7.+%CE+%E1%E0%ED%EA%E0%F5+%E8+%E1%E0%ED%EA%EE%E2%F1%EA%EE%E9+%E4%E5%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8&sort=-1",
					sdf.parse("02/05/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=395-1-%D4%C7.+%CE+%E1%E0%ED%EA%E0%F5+%E8+%E1%E0%ED%EA%EE%E2%F1%EA%EE%E9+%E4%E5%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8&sort=-1",
					sdf.parse("02/05/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=395-1-%D4%C7.+%CE+%E1%E0%ED%EA%E0%F5+%E8+%E1%E0%ED%EA%EE%E2%F1%EA%EE%E9+%E4%E5%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8&sort=-1",
					sdf.parse("02/05/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=395-1-%D4%C7.+%CE+%E1%E0%ED%EA%E0%F5+%E8+%E1%E0%ED%EA%EE%E2%F1%EA%EE%E9+%E4%E5%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8&sort=-1",
					sdf.parse("02/05/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=395-1-%D4%C7.+%CE+%E1%E0%ED%EA%E0%F5+%E8+%E1%E0%ED%EA%EE%E2%F1%EA%EE%E9+%E4%E5%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8&sort=-1",
					sdf.parse("02/05/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=395-1-%D4%C7.+%CE+%E1%E0%ED%EA%E0%F5+%E8+%E1%E0%ED%EA%EE%E2%F1%EA%EE%E9+%E4%E5%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8&sort=-1",
					sdf.parse("02/05/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=395-1-%D4%C7.+%CE+%E1%E0%ED%EA%E0%F5+%E8+%E1%E0%ED%EA%EE%E2%F1%EA%EE%E9+%E4%E5%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8&sort=-1",
					sdf.parse("02/05/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=395-1-%D4%C7.+%CE+%E1%E0%ED%EA%E0%F5+%E8+%E1%E0%ED%EA%EE%E2%F1%EA%EE%E9+%E4%E5%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8&sort=-1",
					sdf.parse("02/05/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=395-1-%D4%C7.+%CE+%E1%E0%ED%EA%E0%F5+%E8+%E1%E0%ED%EA%EE%E2%F1%EA%EE%E9+%E4%E5%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8&sort=-1",
					sdf.parse("02/05/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=395-1-%D4%C7.+%CE+%E1%E0%ED%EA%E0%F5+%E8+%E1%E0%ED%EA%EE%E2%F1%EA%EE%E9+%E4%E5%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8&sort=-1",
					sdf.parse("02/05/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=395-1-%D4%C7.+%CE+%E1%E0%ED%EA%E0%F5+%E8+%E1%E0%ED%EA%EE%E2%F1%EA%EE%E9+%E4%E5%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8&sort=-1",
					sdf.parse("02/05/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=395-1-%D4%C7.+%CE+%E1%E0%ED%EA%E0%F5+%E8+%E1%E0%ED%EA%EE%E2%F1%EA%EE%E9+%E4%E5%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8&sort=-1",
					sdf.parse("02/05/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=395-1-%D4%C7.+%CE+%E1%E0%ED%EA%E0%F5+%E8+%E1%E0%ED%EA%EE%E2%F1%EA%EE%E9+%E4%E5%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8&sort=-1",
					sdf.parse("02/05/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=395-1-%D4%C7.+%CE+%E1%E0%ED%EA%E0%F5+%E8+%E1%E0%ED%EA%EE%E2%F1%EA%EE%E9+%E4%E5%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8&sort=-1",
					sdf.parse("02/05/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=161-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%ED%E0%F6%E8%EE%ED%E0%EB%FC%ED%EE%E9+%EF%EB%E0%F2%E5%E6%ED%EE%E9+%F1%E8%F1%F2%E5%EC%E5&sort=-1",
					sdf.parse("09/30/2011")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=161-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%ED%E0%F6%E8%EE%ED%E0%EB%FC%ED%EE%E9+%EF%EB%E0%F2%E5%E6%ED%EE%E9+%F1%E8%F1%F2%E5%EC%E5&sort=-1",
					sdf.parse("09/30/2011")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=161-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%ED%E0%F6%E8%EE%ED%E0%EB%FC%ED%EE%E9+%EF%EB%E0%F2%E5%E6%ED%EE%E9+%F1%E8%F1%F2%E5%EC%E5&sort=-1",
					sdf.parse("09/30/2011")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=161-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%ED%E0%F6%E8%EE%ED%E0%EB%FC%ED%EE%E9+%EF%EB%E0%F2%E5%E6%ED%EE%E9+%F1%E8%F1%F2%E5%EC%E5&sort=-1",
					sdf.parse("09/30/2011")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=161-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%ED%E0%F6%E8%EE%ED%E0%EB%FC%ED%EE%E9+%EF%EB%E0%F2%E5%E6%ED%EE%E9+%F1%E8%F1%F2%E5%EC%E5&sort=-1",
					sdf.parse("09/30/2011")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=161-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%ED%E0%F6%E8%EE%ED%E0%EB%FC%ED%EE%E9+%EF%EB%E0%F2%E5%E6%ED%EE%E9+%F1%E8%F1%F2%E5%EC%E5&sort=-1",
					sdf.parse("09/30/2011")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=161-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%ED%E0%F6%E8%EE%ED%E0%EB%FC%ED%EE%E9+%EF%EB%E0%F2%E5%E6%ED%EE%E9+%F1%E8%F1%F2%E5%EC%E5&sort=-1",
					sdf.parse("09/30/2011")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=161-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%ED%E0%F6%E8%EE%ED%E0%EB%FC%ED%EE%E9+%EF%EB%E0%F2%E5%E6%ED%EE%E9+%F1%E8%F1%F2%E5%EC%E5&sort=-1",
					sdf.parse("09/30/2011")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=161-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%ED%E0%F6%E8%EE%ED%E0%EB%FC%ED%EE%E9+%EF%EB%E0%F2%E5%E6%ED%EE%E9+%F1%E8%F1%F2%E5%EC%E5&sort=-1",
					sdf.parse("09/30/2011")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=161-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%ED%E0%F6%E8%EE%ED%E0%EB%FC%ED%EE%E9+%EF%EB%E0%F2%E5%E6%ED%EE%E9+%F1%E8%F1%F2%E5%EC%E5&sort=-1",
					sdf.parse("09/30/2011")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=195-%D4%C7.+%CA%EE%E4%E5%EA%F1+%D0%EE%F1%F1%E8%E9%F1%EA%EE%E9+%D4%E5%E4%E5%F0%E0%F6%E8%E8+%EE%E1+%E0%E4%EC%E8%ED%E8%F1%F2%F0%E0%F2%E8%E2%ED%FB%F5+%EF%F0%E0%E2%EE%ED%E0%F0%F3%F8%E5%ED%E8%FF%F5&sort=-1",
					sdf.parse("07/01/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=195-%D4%C7.+%CA%EE%E4%E5%EA%F1+%D0%EE%F1%F1%E8%E9%F1%EA%EE%E9+%D4%E5%E4%E5%F0%E0%F6%E8%E8+%EE%E1+%E0%E4%EC%E8%ED%E8%F1%F2%F0%E0%F2%E8%E2%ED%FB%F5+%EF%F0%E0%E2%EE%ED%E0%F0%F3%F8%E5%ED%E8%FF%F5&sort=-1",
					sdf.parse("07/01/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=195-%D4%C7.+%CA%EE%E4%E5%EA%F1+%D0%EE%F1%F1%E8%E9%F1%EA%EE%E9+%D4%E5%E4%E5%F0%E0%F6%E8%E8+%EE%E1+%E0%E4%EC%E8%ED%E8%F1%F2%F0%E0%F2%E8%E2%ED%FB%F5+%EF%F0%E0%E2%EE%ED%E0%F0%F3%F8%E5%ED%E8%FF%F5&sort=-1",
					sdf.parse("07/01/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=195-%D4%C7.+%CA%EE%E4%E5%EA%F1+%D0%EE%F1%F1%E8%E9%F1%EA%EE%E9+%D4%E5%E4%E5%F0%E0%F6%E8%E8+%EE%E1+%E0%E4%EC%E8%ED%E8%F1%F2%F0%E0%F2%E8%E2%ED%FB%F5+%EF%F0%E0%E2%EE%ED%E0%F0%F3%F8%E5%ED%E8%FF%F5&sort=-1",
					sdf.parse("07/01/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=195-%D4%C7.+%CA%EE%E4%E5%EA%F1+%D0%EE%F1%F1%E8%E9%F1%EA%EE%E9+%D4%E5%E4%E5%F0%E0%F6%E8%E8+%EE%E1+%E0%E4%EC%E8%ED%E8%F1%F2%F0%E0%F2%E8%E2%ED%FB%F5+%EF%F0%E0%E2%EE%ED%E0%F0%F3%F8%E5%ED%E8%FF%F5&sort=-1",
					sdf.parse("07/01/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=195-%D4%C7.+%CA%EE%E4%E5%EA%F1+%D0%EE%F1%F1%E8%E9%F1%EA%EE%E9+%D4%E5%E4%E5%F0%E0%F6%E8%E8+%EE%E1+%E0%E4%EC%E8%ED%E8%F1%F2%F0%E0%F2%E8%E2%ED%FB%F5+%EF%F0%E0%E2%EE%ED%E0%F0%F3%F8%E5%ED%E8%FF%F5&sort=-1",
					sdf.parse("07/01/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=195-%D4%C7.+%CA%EE%E4%E5%EA%F1+%D0%EE%F1%F1%E8%E9%F1%EA%EE%E9+%D4%E5%E4%E5%F0%E0%F6%E8%E8+%EE%E1+%E0%E4%EC%E8%ED%E8%F1%F2%F0%E0%F2%E8%E2%ED%FB%F5+%EF%F0%E0%E2%EE%ED%E0%F0%F3%F8%E5%ED%E8%FF%F5&sort=-1",
					sdf.parse("07/01/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=195-%D4%C7.+%CA%EE%E4%E5%EA%F1+%D0%EE%F1%F1%E8%E9%F1%EA%EE%E9+%D4%E5%E4%E5%F0%E0%F6%E8%E8+%EE%E1+%E0%E4%EC%E8%ED%E8%F1%F2%F0%E0%F2%E8%E2%ED%FB%F5+%EF%F0%E0%E2%EE%ED%E0%F0%F3%F8%E5%ED%E8%FF%F5&sort=-1",
					sdf.parse("07/01/2002")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=14-%D4%C7.+%CE%E1+%EE%E1%F9%E5%F1%F2%E2%E0%F5+%F1+%EE%E3%F0%E0%ED%E8%F7%E5%ED%ED%EE%E9+%EE%F2%E2%E5%F2%F1%F2%E2%E5%ED%ED%EE%F1%F2%FC%FE&sort=-1",
					sdf.parse("03/01/1998")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=38-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%F0%E5%EA%EB%E0%EC%E5&sort=-1",
					sdf.parse("07/01/2006")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=135-%D4%C7.+%CE%E1+%EE%F6%E5%ED%EE%F7%ED%EE%E9+%E4%E5%FF%F2%E5%EB%FC%ED%EE%F1%F2%E8+%E2+%D0%EE%F1%F1%E8%E9%F1%EA%EE%E9+%D4%E5%E4%E5%F0%E0%F6%E8%E8&sort=-1",
					sdf.parse("08/03/1998")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=51-%D4%C7.+%C3%F0%E0%E6%E4%E0%ED%F1%EA%E8%E9+%EA%EE%E4%E5%EA%F1+%D0%EE%F1%F1%E8%E9%F1%EA%EE%E9+%D4%E5%E4%E5%F0%E0%F6%E8%E8.+%D7%E0%F1%F2%FC+%EF%E5%F0%E2%E0%FF&sort=-1",
					sdf.parse("11/30/1994")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=14-%D4%C7.+%C3%F0%E0%E6%E4%E0%ED%F1%EA%E8%E9+%EA%EE%E4%E5%EA%F1+%D0%EE%F1%F1%E8%E9%F1%EA%EE%E9+%D4%E5%E4%E5%F0%E0%F6%E8%E8.+%D7%E0%F1%F2%FC+%E2%F2%EE%F0%E0%FF&sort=-1",
					sdf.parse("01/26/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=14-%D4%C7.+%C3%F0%E0%E6%E4%E0%ED%F1%EA%E8%E9+%EA%EE%E4%E5%EA%F1+%D0%EE%F1%F1%E8%E9%F1%EA%EE%E9+%D4%E5%E4%E5%F0%E0%F6%E8%E8.+%D7%E0%F1%F2%FC+%E2%F2%EE%F0%E0%FF&sort=-1",
					sdf.parse("01/26/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=14-%D4%C7.+%C3%F0%E0%E6%E4%E0%ED%F1%EA%E8%E9+%EA%EE%E4%E5%EA%F1+%D0%EE%F1%F1%E8%E9%F1%EA%EE%E9+%D4%E5%E4%E5%F0%E0%F6%E8%E8.+%D7%E0%F1%F2%FC+%E2%F2%EE%F0%E0%FF&sort=-1",
					sdf.parse("01/26/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=14-%D4%C7.+%C3%F0%E0%E6%E4%E0%ED%F1%EA%E8%E9+%EA%EE%E4%E5%EA%F1+%D0%EE%F1%F1%E8%E9%F1%EA%EE%E9+%D4%E5%E4%E5%F0%E0%F6%E8%E8.+%D7%E0%F1%F2%FC+%E2%F2%EE%F0%E0%FF&sort=-1",
					sdf.parse("01/26/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=14-%D4%C7.+%C3%F0%E0%E6%E4%E0%ED%F1%EA%E8%E9+%EA%EE%E4%E5%EA%F1+%D0%EE%F1%F1%E8%E9%F1%EA%EE%E9+%D4%E5%E4%E5%F0%E0%F6%E8%E8.+%D7%E0%F1%F2%FC+%E2%F2%EE%F0%E0%FF&sort=-1",
					sdf.parse("01/26/1996")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=++%CF%D0%C8%CA%C0%C7+%D4%C5%C4%C5%D0%C0%CB%DC%CD%CE%C9+%D1%CB%D3%C6%C1%DB+%CF%CE+%D4%C8%CD%C0%CD%D1%CE%C2%DB%CC+%D0%DB%CD%CA%C0%CC+++N+10-4%2F%EF%E7-%ED&sort=-1",
					sdf.parse("06/04/2010")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=149-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%AB%CE%E1+%E8%ED%F4%EE%F0%EC%E0%F6%E8%E8%2C+%E8%ED%F4%EE%F0%EC%E0%F6%E8%EE%ED%ED%FB%F5+%F2%E5%F5%ED%EE%EB%EE%E3%E8%FF%F5+%E8+%E7%E0%F9%E8%F2%E5+%E8%ED%F4%EE%F0%EC%E0%F6%E8%E8&sort=-1",
					sdf.parse("07/29/2006")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=149-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%AB%CE%E1+%E8%ED%F4%EE%F0%EC%E0%F6%E8%E8%2C+%E8%ED%F4%EE%F0%EC%E0%F6%E8%EE%ED%ED%FB%F5+%F2%E5%F5%ED%EE%EB%EE%E3%E8%FF%F5+%E8+%E7%E0%F9%E8%F2%E5+%E8%ED%F4%EE%F0%EC%E0%F6%E8%E8&sort=-1",
					sdf.parse("07/29/2006")));
			dtoList.add(new TestDto(
					"http://pravo.gov.ru/proxy/ips/?searchres=&bpas=cd00000&intelsearch=98-%D4%C7.+%D4%E5%E4%E5%F0%E0%EB%FC%ED%FB%E9+%E7%E0%EA%EE%ED+%EE+%EA%EE%EC%EC%E5%F0%F7%E5%F1%EA%EE%E9+%F2%E0%E9%ED%E5&sort=-1",
					sdf.parse("08/16/2004")));
			dtoList.add(new TestDto("http://www.fedsfm.ru/documents/rfm/4755", sdf.parse("07/16/2020")));
			dtoList.add(new TestDto("http://www.fedsfm.ru/documents/rfm/4755", sdf.parse("04/26/2021")));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return dtoList;
	}

	public Document getJsoupDocument(String url) {
		Document doc = null;
		try {
			doc = Jsoup.connect(url).get();
		} catch (IOException e) {
			System.out.println("The Web link :: " + url + " is not available");
		}
		return doc;
	}

	public void runTask(Document doc, TestDto testDto) {
		Map<String, List<String>> resultMap = new HashMap<>();
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
		List<String> urlList = new ArrayList<>();
		Elements tables = doc.select("table");
		Pattern pattern1 = Pattern.compile("от\\s*(\\d*\\.\\d*\\.\\d*)\\s*г", Pattern.DOTALL | Pattern.UNICODE_CASE);
		for (Element e : tables) {
			Elements list = e.getElementsByClass("tiny");

			String uniqueNum = e.select("td>input").attr("name").substring(6);
			String url2 = "http://pravo.gov.ru/proxy/ips/?docbody=&link_id=0&nd=" + uniqueNum
					+ "&intelsearch=&firstDoc=1";

			for (Element listItem : list) {
				Matcher matcher1 = pattern1.matcher(listItem.text());

				if (matcher1.find()) {
					Date patternDate = null;
					try {
						patternDate = sdf.parse(matcher1.group(1));
					} catch (ParseException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					System.out.println("Pattern Date ::: " + patternDate);
					System.out.println("Effective Date ::: " + testDto.getEffectiveDate());
					System.out.println(patternDate.equals(testDto.getEffectiveDate()));
					if (patternDate.equals(testDto.getEffectiveDate())) {
						System.out.println(url2 + "*********");
						urlList.add(url2);
						continue;
					} else {
						if (!resultMap.containsKey(testDto.getUrl()))
							resultMap.put(testDto.getUrl(), null);
					}
				}

			}
			resultMap.put(testDto.getUrl(), urlList);
		}

		for (Map.Entry resultMapKV : resultMap.entrySet()) {
			String url = resultMapKV.getKey().toString() + ((((List<String>) resultMapKV.getValue()) != null
					|| !((List<String>) resultMapKV.getValue()).isEmpty()) ? "true" : "false");
			try {
				storeUnreachableURLs(url, "D:\\var\\tmp\\ParseableDocPaths");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
}
