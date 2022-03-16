package com.kpmg.rcm.sourcing.common.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kpmg.rcm.sourcing.common.json.dto.SubGranule;

@Component
public class DuplicateSubGranuleUtil {

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
