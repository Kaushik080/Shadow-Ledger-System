package com.example.drift_correction_service.repository;

import com.example.drift_correction_service.model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

/**
 * Repository to query ledger data (read-only access to shadow ledger)
 */
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, String> {

    @Query(value = """
        SELECT COALESCE(SUM(CASE WHEN type='credit' THEN amount ELSE -amount END), 0) 
        FROM ledger 
        WHERE account_id = :accountId
    """, nativeQuery = true)
    BigDecimal getShadowBalance(@Param("accountId") String accountId);
}

