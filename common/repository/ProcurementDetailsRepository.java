package com.kpmg.rcm.sourcing.common.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kpmg.rcm.sourcing.common.entity.ProcurementDetails;

@Repository
public interface ProcurementDetailsRepository extends JpaRepository<ProcurementDetails, Integer> {

	@Query(value = "select pd.modified_date from procurement_details pd where pd.file_name=:fileName", nativeQuery = true)
	LocalDateTime findModifiedDateByFileName(@Param("fileName") String fileName);

	/*@Query("SELECT TOP 1 * FROM procurement_details where LOWER(file_name) = :fileName  and source_details_id = :sourceId "
			+ " ORDER BY ID DESC ")
	ProcurementDetails findLatestFileByFileNameAndSourceId(@Param("fileName") String fileName,
			@Param("sourceId") Integer sourceId);
*/
	ProcurementDetails findFirstByFileNameAndSourceDetailsIdOrderByIdDesc(String fileName, Integer sourceId);

	ProcurementDetails findFirstBySourceDetailsIdOrderByIdDesc(Integer sourceId);

	//List<ProcurementDetails> findByModifiedDateAndSourceDetailsIdOrderByIdDesc(String modifiedDate, Integer sourceId);

}
