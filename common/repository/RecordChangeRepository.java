package com.kpmg.rcm.sourcing.common.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kpmg.rcm.sourcing.common.entity.RecordChangeModel;

@Repository
public interface RecordChangeRepository extends JpaRepository<RecordChangeModel, Long> {

	@Query(value = "from RecordChangeModel rc where rc.sourceId= :sourceId and rc.fileName= :fileName")
	List<RecordChangeModel> findBySourceIdAndFileName(Integer sourceId, String fileName);

	@Query(value = "from RecordChangeModel rc where rc.sourceId= :sourceId")
	RecordChangeModel findBySourceId(@Param("sourceId") Integer sourceId);

	// TODO Query to find duplicate granule Ids
	// TODO select granule_id, COUNT(1) cnt from record_change
	// TODO group by granule_id
	// TODO having count(1) > 1
	// TODO order by cnt desc;

	boolean existsBySourceId(Integer sourceId);

	RecordChangeModel findByGranuleId(String commonId);

	List<RecordChangeModel> findByRootNode(String rootNode);
}