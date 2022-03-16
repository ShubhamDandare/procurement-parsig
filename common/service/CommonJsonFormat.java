package com.kpmg.rcm.sourcing.common.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.kpmg.rcm.sourcing.common.json.dto.Granule;
import com.kpmg.rcm.sourcing.common.json.dto.Note;
import com.kpmg.rcm.sourcing.common.json.dto.Note__1;
import com.kpmg.rcm.sourcing.common.json.dto.SubGranule;
import com.kpmg.rcm.sourcing.common.json.dto.System;
import com.kpmg.rcm.sourcing.common.util.MemoryUtil;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CommonJsonFormat {

	static Map<String, String> specialCharMap;
	static {
		specialCharMap = new LinkedHashMap<>();
		specialCharMap.put(" ", " ");
		// specialCharMap.put("“", "'");
		// specialCharMap.put("”", "'");
		specialCharMap.put(" ", "");
		// specialCharMap.put("’", "'");
		specialCharMap.put("•", " ");
		specialCharMap.put(" ", " ");
		specialCharMap.put(" ", " ");
		specialCharMap.put(" ", " ");
		specialCharMap.put("\\s+", " ");
	}

	/*
	 * public static void main1(String[] args) { String s =
	 * "Department of War designated Department of the Army and title of Secretary of War changed to Secretary of the Army by section 205(a) of act July 26, 1947, ch. 343, title II, 61 Stat. 501. Section 205(a) of act July 26, 1947, was repealed by section 53 of act Aug. 10, 1956, ch. 1041, 70A Stat. 641. Section 1 of act Aug. 10, 1956, enacted “Title 10, Armed Forces” which in sections 3010 to 3013 continued Department of the Army under administrative supervision of Secretary of the Army."
	 * ; CommonJsonFormat cc = new CommonJsonFormat(); String rs =
	 * cc.replaceSpecialChar(s); java.lang.System.out.println(rs); }
	 */

	public void handleCommonJsonFormatting(Granule granule) {
		try {
			toLowerCase(granule);
			removeSpecialChar(granule);
			updateCommonId(granule);
			updateSubgranuleForDuplicateId(granule.getSubGranules(), granule.getSystem().getCommonId());
		} catch (Exception e) {
			log.error("Error in formatting json", e);
		}
	}

	private void removeSpecialChar(Granule granule) {
		if (granule == null)
			return;

		String heading = granule.getSource().getHeading();
		granule.getSource().setHeading(replaceSpecialChar(heading));

		String content = granule.getSource().getContent();
		granule.getSource().setContent(replaceSpecialChar(content));

		if (granule.getSource().getNotes() != null) {
			for (Note note : granule.getSource().getNotes()) {
				note.setContent(replaceSpecialChar(note.getContent()));
				note.setHeading(replaceSpecialChar(note.getHeading()));
			}
		}
		// INFO assuming FR has only one granule with no children
		List<SubGranule> subGranules = granule.getSubGranules();

		if (subGranules == null || subGranules.isEmpty())
			return;

		for (SubGranule subGranule : subGranules) {
			String subHeading = subGranule.getHeading();
			subGranule.setHeading(replaceSpecialChar(subHeading));

			String subContent = subGranule.getContent();
			subGranule.setContent(replaceSpecialChar(subContent));

			if (subGranule.getNotes() != null) {
				for (Note__1 note : subGranule.getNotes()) {
					note.setContent(replaceSpecialChar(note.getContent()));
					note.setHeading(replaceSpecialChar(note.getHeading()));
				}
			}
		}
	}

	private String replaceSpecialChar(String text) {
		if (!StringUtils.isEmpty(text)) {
			for (Map.Entry<String, String> entry : specialCharMap.entrySet()) {
				text = text.replaceAll(entry.getKey(), entry.getValue());
			}
			text = text.trim();
		}
		return text;
	}

	private void updateCommonId(Granule granule) {

		if (granule == null)
			return;

		// INFO assuming FR has only one granule with no children
		List<SubGranule> subGranules = granule.getSubGranules();

		if (subGranules == null || subGranules.isEmpty())
			return;

		String rootParentId = granule.getSystem().getCommonId();

		// INFO iterate over subgranules
		for (int i = 0; i < subGranules.size(); i++) {
			SubGranule subGranule = subGranules.get(i);

			// INFO Process Children
			List<String> children = subGranule.getChildId();
			if (!CollectionUtils.isEmpty(children)) {
				boolean isAlreadyCommonId = false;
				List<String> updatedChildren = new ArrayList<>();
				for (int j = 0; j < children.size(); j++) {
					String child = children.get(j);
					if (!child.contains("/")) {
						updatedChildren.add(rootParentId + "/" + child);
					} else {
						isAlreadyCommonId = true;
						break;
					}
				}
				if (!isAlreadyCommonId) {
					MemoryUtil.clear(children);
					subGranule.setChildId(updatedChildren);
				}
			}

			// INFO Process Parent
			if (StringUtils.isNotBlank(subGranule.getParentId())) {
				String parentId = subGranule.getParentId();
				if (!parentId.contains("/")) {
					subGranule.setParentId(rootParentId + "/" + subGranule.getParentId());
				}
			}
		}

	}

	private void toLowerCase(Granule granule) {
		System system = granule.getSystem();
		granule.setKey(granule.getKey().toLowerCase());
		system.setId(system.getId().toLowerCase());
		system.setCommonId(system.getCommonId().toLowerCase());

		if (granule.getSource().getParent() != null) {
			granule.getSource().getParent().forEach(parent -> {
				parent.setCommonId(parent.getCommonId().toLowerCase());
			});
		}

		if (granule.getSource().getChild() != null) {
			granule.getSource().getChild().forEach(child -> {
				child.setCommonId(child.getCommonId().toLowerCase());
			});
		}

		List<SubGranule> subGranules = granule.getSubGranules();
		if (subGranules != null && !subGranules.isEmpty()) {
			subGranules.forEach(subg -> {

				subg.setId(subg.getId().toLowerCase());
				// subg.setCommonId(subg.getCommonId().toLowerCase());
				if (StringUtils.isNotBlank(subg.getParentId())) {
					String parentId = subg.getParentId();
					subg.setParentId(parentId.toLowerCase());
				}

				List<String> children = subg.getChildId();
				if (!CollectionUtils.isEmpty(children)) {
					List<String> updatedChildren = new ArrayList<>();
					children.forEach(child -> {
						updatedChildren.add(child.toLowerCase());
					});
					subg.setChildId(updatedChildren);
				}
			});
		}
	}

	public void updateSubgranuleForDuplicateId(List<SubGranule> sg, String granuleCommonId) {
		if (sg != null && !sg.isEmpty()) {
			Map<String, Integer> subGranuleCount = new HashMap<>();
			sg.forEach(subg -> {
				String id = subg.getId();
				Integer subgranuleCount = subGranuleCount.get(id);
				if (subgranuleCount != null) {
					int increaseByOne = subgranuleCount + 1;
					subGranuleCount.put(id, increaseByOne);
					String subGIdAfterIncrement = subg.getId() + "-" + increaseByOne;
					subg.setId(subGIdAfterIncrement);
					subg.setCommonId(granuleCommonId + "/" + subGIdAfterIncrement);
					updateAllChildsId(sg, subg);
				} else {
					subGranuleCount.put(id, 1);
				}
			});
		}
	}

	private void updateAllChildsId(List<SubGranule> sg, SubGranule subg) {
		if (subg != null && subg.getChildId() != null) {
			subg.getChildId().forEach(child -> {

				SubGranule subg1 = findSubgranuleById(sg, child);
				if (subg1 != null) {
					subg1.setParentId(subg.getId());
				}

			});
		}
	}

	private SubGranule findSubgranuleById(List<SubGranule> sg, String id) {
		Optional<SubGranule> grn = sg.stream().filter(subg -> subg.getId().equals(id)).findFirst();
		if (grn.isPresent()) {
			return grn.get();
		} else {
			return null;
		}
	}

}
