package com.dilip.transaction.repository;
import com.dilip.transaction.entity.Account; import org.springframework.data.jpa.repository.JpaRepository;
public interface AccountRepository extends JpaRepository<Account,String>{}