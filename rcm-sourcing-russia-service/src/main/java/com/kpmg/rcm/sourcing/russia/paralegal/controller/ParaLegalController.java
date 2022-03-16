package com.kpmg.rcm.sourcing.russia.paralegal.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kpmg.rcm.sourcing.common.dto.SourceFieldMappingResponse;
import com.kpmg.rcm.sourcing.common.service.ConfigService;
import com.kpmg.rcm.sourcing.russia.paralegal.service.ParaLegalService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/paralegal")
@Slf4j
public class ParaLegalController {

	@Autowired
	ParaLegalService service;
	
	@Autowired
	private ConfigService configService;

	@GetMapping("/procure")
	public void procure(@RequestParam(value = "lawType") String srcValue) {
		ResponseEntity<SourceFieldMappingResponse> sourceDetails = configService.getSourceDetails("RU",
				"PARALEGAL");
		SourceFieldMappingResponse sourceFieldMappingResponse = sourceDetails.getBody();
		try {
			service.procure(srcValue,sourceFieldMappingResponse,false);
		} catch (Exception e) {
			log.error("Error Reading service procure :: srcValue" + srcValue, e);
		}
	}

	@GetMapping("/parse")
	public void parse(@RequestParam(value = "lawType") String srcValue) {
		ResponseEntity<SourceFieldMappingResponse> sourceDetails = configService.getSourceDetails("RU",
				"PARALEGAL");
		SourceFieldMappingResponse sourceFieldMappingResponse = sourceDetails.getBody();
		try {
			service.parse(srcValue,sourceFieldMappingResponse,null);
		} catch (Exception e) {
			log.error("Error parsing" + srcValue, e);
		}
	}
	
	@GetMapping("/gsprocure")
	public void gsprocure(@RequestParam(value = "lawType") String srcValue) {
		ResponseEntity<SourceFieldMappingResponse> sourceDetails = configService.getSourceDetails("RU",
				"PARALEGAL");
		SourceFieldMappingResponse sourceFieldMappingResponse = sourceDetails.getBody();
		try {
			service.procure(srcValue,sourceFieldMappingResponse,true);
		} catch (Exception e) {
			log.error("Error Reading service procure :: srcValue" + srcValue, e);
		}
	}
	
	@GetMapping("/gsparse")
	public void gsparse(@RequestParam(value = "lawType") String srcValue) {
		ResponseEntity<SourceFieldMappingResponse> sourceDetails = configService.getSourceDetails("RU",
				"PARALEGAL");
		SourceFieldMappingResponse sourceFieldMappingResponse = sourceDetails.getBody();
		try {
			service.parse(srcValue,sourceFieldMappingResponse,true);
		} catch (Exception e) {
			log.error("Error parsing" + srcValue, e);
		}
	}
}
