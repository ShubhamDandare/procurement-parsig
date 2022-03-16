package com.kpmg.rcm.sourcing.common.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// INFO: do not add lombok @data [Harsh]
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
@Entity
public class ProcurementDetails {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Integer id;

	@Column(name = "source_details_id")
	private Integer sourceDetailsId;

	@Column(name = "downloaded_file_location")
	private String downloadedFileLocation;

	@Column(name = "cmm_file_location")
	private String cmmFileLocation;

	@Column(name = "version_number")
	private Integer versionNumber;

	@Column(name = "created_date")
	private LocalDateTime createdDate;

	@Column(name = "modified_date")
	private String modifiedDate;

	@Column(name = "updated_date")
	private LocalDateTime updatedDate;

	@Column(name = "file_name")
	private String fileName;

}
