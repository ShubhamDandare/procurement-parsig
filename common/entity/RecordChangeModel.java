package com.kpmg.rcm.sourcing.common.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.annotations.DynamicUpdate;

import lombok.Data;

@Data
@DynamicUpdate
@Entity
public class RecordChangeModel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "granule_id")
	private String granuleId;

	@Column(name = "checksum_current")
	private String checksumCurrent;

	@Column(name = "checksum_previous")
	private String checksumPrevious;

	@Column(name = "source_id")
	private Integer sourceId;

	@Column(name = "file_name")
	private String fileName;

	@Column(name = "root_node")
	private String rootNode;

	@Column(name = "created_date")
	private LocalDateTime createdDate;

	@Column(name = "updated_date")
	private LocalDateTime updatedDate;

}
