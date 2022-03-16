package com.kpmg.rcm.sourcing.common.dto;

import java.util.List;

import com.kpmg.rcm.sourcing.common.json.dto.Granule;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class RecordChangeWrapper {

    private Granule granule;
    private List<RecordChange> recordChanges;
}
