package com.kpmg.rcm.sourcing.russia.paralegal.controller;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import com.kpmg.rcm.sourcing.common.json.dto.SubGranule;
import com.kpmg.rcm.sourcing.common.json.dto.System;

import lombok.extern.slf4j.Slf4j;
@Slf4j
public class tempParsers {
	void parser4(Document doc, List<SubGranule> subGranuleList, System system){
			Elements para=doc.getElementsByClass("M");
			Map docDetails = new HashMap();
			String articleMetadata="";			
			boolean isHeadingContniued=true,isArticleContinued=false,isSubArticleContinued=false,isNoteStarted=false;
			Pattern articlePattern=Pattern.compile("Статья\\s+\\d+-*\\d*+\\s*\\.*\\s*(.*)",Pattern.DOTALL| Pattern.UNICODE_CASE);
			Pattern articleMetaDataPattern=Pattern.compile("(Статья\\s+\\d+-*\\d*+\\s*\\.*)",Pattern.DOTALL| Pattern.UNICODE_CASE);
			Pattern notePattern=Pattern.compile("(Президент Российской Федерации.*)",Pattern.DOTALL| Pattern.UNICODE_CASE);
			Pattern citationPattern=Pattern.compile(".*(N\\s*\\d*-ФЗ$)",Pattern.DOTALL| Pattern.UNICODE_CASE);
			Pattern articleTokenPattern=Pattern.compile("Статья\\s+\\d+-*\\d*+\\s*\\.*",Pattern.DOTALL| Pattern.UNICODE_CASE);
			for (TextNode textNode : para.first().textNodes()) {
				Matcher matcher101=articleTokenPattern.matcher(textNode.text());
				String[]textArticles=textNode.text().split("Статья\\s+\\d+-*\\d*+\\s*\\.*");
				if(textArticles.length>1) {
					textNode.after("<span>"+textArticles[0]+"</span>");
					for(int i=1;i<textArticles.length;i++){
						if(matcher101.find()) {
							String match=matcher101.group(0);
							textNode.after("<span>"+match+textArticles[i]+ "</span>");
						}
					}
				}
				else {
					textNode.after("<span>"+textNode.text() + "</span>");
				}
				textNode.remove();
				}
			for(Element span:para.first().children()) {
				Matcher matcher1=articlePattern.matcher(span.text());
				Matcher matcher2=notePattern.matcher(span.text());
				Matcher matcher3=citationPattern.matcher(span.text());
				Matcher matcher4=articleMetaDataPattern.matcher(span.text());
				if(matcher1.find() && !isNoteStarted) {
					isHeadingContniued=false;
					isArticleContinued=true;
					
					if(matcher4.find()) {
						articleMetadata=matcher4.group(1);
						docDetails.put(articleMetadata,span.text());
					}
				}
				else if(matcher2.find()) {
				
					matcher3.find();
					isNoteStarted=true;
					isArticleContinued=false;
					docDetails.put("Note",span.text());
					docDetails.put("Citation",matcher3.group(1));
				}
				else if(isArticleContinued) {
					docDetails.put(articleMetadata,docDetails.get(articleMetadata)+span.text());
				}
				else if(isHeadingContniued) {
					docDetails.put("Heading",docDetails.get("Heading")!=null?docDetails.get("Heading"):""+span.text());
				}
				else if(isNoteStarted) {
					docDetails.put("Notes",docDetails.get("Notes")+span.text());
				}
				
			}			
	}
	
	
	void gsParser(Document doc, List<SubGranule> subGranuleList, System system) {
			Elements para=doc.select("p");
			Map docDetails = new HashMap();
			String articleMetadata="";	
			String subArticleMetadata="";
			String subSubArticleMetadata="";
			boolean isHeadingContniued=true,isArticleContinued=false,isSubArticleContinued=false,isNoteStarted=false,isSubSubArticleContinued=false;
			Pattern articlePattern=Pattern.compile("Статья\\s+\\d+-*\\d*+\\s*\\.*\\s*(.*)",Pattern.DOTALL| Pattern.UNICODE_CASE);
			Pattern articleMetaDataPattern=Pattern.compile("(Статья\\s+\\d+-*\\d*+)\\s*\\.*",Pattern.DOTALL| Pattern.UNICODE_CASE);
			Pattern notePattern=Pattern.compile("(Президент Российской Федерации.*)",Pattern.DOTALL| Pattern.UNICODE_CASE);
			Pattern citationPattern=Pattern.compile(".*(№\\s*\\d*-ФЗ$)",Pattern.DOTALL| Pattern.UNICODE_CASE);
			Pattern subArticleMetaDataPattern=Pattern.compile("^(\\d+\\s*)\\..*",Pattern.DOTALL| Pattern.UNICODE_CASE);
			Pattern subArticlePattern=Pattern.compile("^\\d+\\s*\\.(.*)",Pattern.DOTALL| Pattern.UNICODE_CASE);
			Pattern subSubArticlePattern=Pattern.compile("^\\d+\\s*\\)(.*)",Pattern.DOTALL| Pattern.UNICODE_CASE);
			Pattern subSubArticleMetaDataPattern=Pattern.compile("(^\\d+\\s*)\\).*",Pattern.DOTALL| Pattern.UNICODE_CASE);
			for(Element span:para) {
				Matcher matcher1=articlePattern.matcher(span.text());
				Matcher matcher2=notePattern.matcher(span.text());
				Matcher matcher3=citationPattern.matcher(span.text());
				Matcher matcher4=articleMetaDataPattern.matcher(span.text());
				Matcher matcher5=subArticleMetaDataPattern.matcher(span.text());
				Matcher matcher6=subArticlePattern.matcher(span.text());
				Matcher matcher7=subSubArticleMetaDataPattern.matcher(span.text());
				Matcher matcher8=subSubArticlePattern.matcher(span.text());
				if(matcher1.find() && !isNoteStarted) {
					isHeadingContniued=false;
					isArticleContinued=true;
					
					if(matcher4.find()) {
						articleMetadata=matcher4.group(1);
						docDetails.put(articleMetadata,span.text());
					}
				}
				else if(matcher2.find()) {
					matcher3.find();
					isNoteStarted=true;
					isArticleContinued=false;
					isSubSubArticleContinued=false;
					docDetails.put("Note",span.text());
					
				}
				else if(matcher3.find()) {
					docDetails.put("Citation",matcher3.group(1));
				}
				else if(matcher6.find() && matcher5.find() && isArticleContinued) {
					subArticleMetadata=matcher5.group(1);
					String subArticle=matcher6.group(1);
					isSubArticleContinued=true;
					isArticleContinued=false;
					docDetails.put(articleMetadata+"#"+subArticleMetadata, subArticle);	
		
				}
				else if(matcher8.find() && matcher7.find()) {
					subSubArticleMetadata=matcher7.group(1);
					String subSubArticle=matcher8.group(1);
					isSubArticleContinued=false;
					isSubSubArticleContinued=true;
					docDetails.put(articleMetadata+"#"+subArticleMetadata+"#"+subSubArticleMetadata, subSubArticle);	
		
				}
				else if(isSubArticleContinued) {
					String subArticleKey = articleMetadata+"#"+subArticleMetadata;
					docDetails.put(subArticleKey, (docDetails.get(subArticleKey)!=null? docDetails.get(subArticleKey):"")+span.text());
				}
				else if(isSubSubArticleContinued) {
					String subSubArticleKey=articleMetadata+"#"+subArticleMetadata+"#"+subSubArticleMetadata;
					docDetails.put(subSubArticleKey, (docDetails.get(subSubArticleKey)!=null?docDetails.get(subSubArticleKey):"")+span.text());
				}
				else if(isArticleContinued) {
					docDetails.put(articleMetadata,(docDetails.get(articleMetadata)!=null?docDetails.get(articleMetadata):"")+span.text());
				}
				else if(isHeadingContniued) {
					docDetails.put("Heading",(docDetails.get("Heading")!=null?docDetails.get("Heading"):"")+span.text());
				}
				else if(isNoteStarted) {
					isSubArticleContinued=false;
					docDetails.put("Note",docDetails.get("Note")+span.text());
				}
				
				
			}
	}
	
	
	
}
