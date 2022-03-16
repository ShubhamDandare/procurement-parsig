package com.kpmg.rcm.sourcing.common.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Data
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
//@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SourceFieldMappingResponse {

	private String jurisdictionName;
	private String sourceName;
	private String sourceLocation;
	private Boolean downloadedFlag;
	private Integer sourceMappingId;
	private Integer sourceId;
	private String fileType;
	private LocalDate startYear;
	private LocalDate endYear;
	private String mappingLevel;
	private String fileName;

}
