package com.kpmg.rcm.sourcing.common.util;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kpmg.rcm.sourcing.common.config.cache.ConfigCache;
import com.kpmg.rcm.sourcing.common.json.dto.Linkage;
import com.kpmg.rcm.sourcing.common.json.dto.Linkage__1;

@Component
public class ParallelTableLinkageUtil {

	@Autowired
	private ConfigCache configCache;

	public List<Linkage> getGranuleLinkagesFromParallelTable(String citation, String sourceUrl) {
		List<Linkage> linkages = new ArrayList<>();
		if (!citation.contains(",")) {
			List<String> ptaList = configCache.getLinkageCodes(citation);
			if (ptaList != null) {
				for (String citationValue : ptaList) {
					// remove brackets & "" values
					citationValue = citationValue.replaceAll("\"|\"|[\\[\\]]", "");
					linkages.add(Linkage.builder().type("parallel_table_of_authority").text(citationValue)
							.citation(citationValue).build());
				}
			}
		}
		return linkages;
	}

	public List<Linkage__1> getSubGranuleLinkagesFromParallelTable(String citation, String sourceUrl) {
		List<Linkage__1> linkages = new ArrayList<>();
		if (!citation.contains(",")) {
			List<String> ptaList = configCache.getLinkageCodes(citation);
			if (ptaList != null) {
				for (String citationValue : ptaList) {
					// remove brackets & "" values
					citationValue = citationValue.replaceAll("\"|\"|[\\[\\]]", "");
					linkages.add(Linkage__1.builder().type("parallel_table_of_authority").text(citationValue)
							.citation(citationValue).build());
				}
			}
		}
		return linkages;
	}
}
