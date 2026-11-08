package com.dilip.transaction.repository;
import com.dilip.transaction.entity.PipelineRun; import org.springframework.data.jpa.repository.JpaRepository; import java.util.Optional;
public interface PipelineRunRepository extends JpaRepository<PipelineRun,String>{Optional<PipelineRun> findBySourceFile(String file);}