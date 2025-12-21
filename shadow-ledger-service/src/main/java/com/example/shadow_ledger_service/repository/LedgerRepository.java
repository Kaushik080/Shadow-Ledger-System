package com.example.shadow_ledger_service.repository;

import com.example.shadow_ledger_service.model.LedgerEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface LedgerRepository extends JpaRepository<LedgerEvent, String> {
    Optional<LedgerEvent> findByEventId(String eventId);

    List<LedgerEvent> findByAccountIdOrderByTimestampAscEventIdAsc(String accountId);

    /**
     * Compute shadow balance using SQL window function
     * This calculates running balance with deterministic ordering
     */
    @Query(value = """
        SELECT 
            account_id,
            event_id,
            type,
            amount,
            timestamp,
            SUM(CASE WHEN type='credit' THEN amount ELSE -amount END) 
                OVER (PARTITION BY account_id ORDER BY timestamp ASC, event_id ASC) as running_balance
        FROM ledger 
        WHERE account_id = :accountId
        ORDER BY timestamp ASC, event_id ASC
    """, nativeQuery = true)
    List<Object[]> computeBalanceWithWindow(@Param("accountId") String accountId);

    /**
     * Get final balance for an account
     */
    @Query(value = """
        SELECT COALESCE(SUM(CASE WHEN type='credit' THEN amount ELSE -amount END), 0) 
        FROM ledger 
        WHERE account_id = :accountId
    """, nativeQuery = true)
    BigDecimal getFinalBalance(@Param("accountId") String accountId);

    /**
     * Get last event ID for an account
     */
    @Query(value = """
        SELECT event_id 
        FROM ledger 
        WHERE account_id = :accountId 
        ORDER BY timestamp DESC, event_id DESC 
        LIMIT 1
    """, nativeQuery = true)
    String getLastEventId(@Param("accountId") String accountId);
}

