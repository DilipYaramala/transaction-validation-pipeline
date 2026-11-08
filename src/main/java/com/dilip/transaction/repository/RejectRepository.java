package com.dilip.transaction.repository;
import com.dilip.transaction.entity.RejectedTransaction; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface RejectRepository extends JpaRepository<RejectedTransaction,Long>{List<RejectedTransaction> findByRunId(String id);}