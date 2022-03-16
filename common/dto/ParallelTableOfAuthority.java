package com.kpmg.rcm.sourcing.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class ParallelTableOfAuthority {

    private String label;
    private String ptaCode;
    private String cfrCode;

}

