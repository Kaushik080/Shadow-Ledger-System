package com.example.shadow_ledger_service.repository;

import com.example.shadow_ledger_service.model.LedgerEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test 2: SQL Window Function Test
 * Tests that balance is correctly computed using window functions
 */
@DataJpaTest
@Transactional
public class ShadowBalanceWindowFunctionTest {

    @Autowired
    private LedgerRepository ledgerRepository;

    @Test
    public void testWindowFunctionBalanceComputation() {
        // Create test events in deterministic order
        String accountId = "TEST-WIN-A10";

        // Event 1: Credit 1000
        LedgerEvent event1 = new LedgerEvent();
        event1.setEventId("E-WIN-001");
        event1.setAccountId(accountId);
        event1.setType("credit");
        event1.setAmount(new BigDecimal("1000.00"));
        event1.setTimestamp(1000L);
        ledgerRepository.save(event1);

        // Event 2: Debit 250
        LedgerEvent event2 = new LedgerEvent();
        event2.setEventId("E-WIN-002");
        event2.setAccountId(accountId);
        event2.setType("debit");
        event2.setAmount(new BigDecimal("250.00"));
        event2.setTimestamp(2000L);
        ledgerRepository.save(event2);

        // Event 3: Credit 500
        LedgerEvent event3 = new LedgerEvent();
        event3.setEventId("E-WIN-003");
        event3.setAccountId(accountId);
        event3.setType("credit");
        event3.setAmount(new BigDecimal("500.00"));
        event3.setTimestamp(3000L);
        ledgerRepository.save(event3);

        // Event 4: Debit 100
        LedgerEvent event4 = new LedgerEvent();
        event4.setEventId("E-WIN-004");
        event4.setAccountId(accountId);
        event4.setType("debit");
        event4.setAmount(new BigDecimal("100.00"));
        event4.setTimestamp(4000L);
        ledgerRepository.save(event4);

        // Get running balance using window function
        List<Object[]> results = ledgerRepository.computeBalanceWithWindow(accountId);

        // Verify we have all events
        assertThat(results).hasSize(4);

        // Verify running balance progression
        // After E-WIN-001: +1000 = 1000
        // After E-WIN-002: -250 = 750
        // After E-WIN-003: +500 = 1250
        // After E-WIN-004: -100 = 1150

        // Get final balance
        BigDecimal finalBalance = ledgerRepository.getFinalBalance(accountId);
        assertThat(finalBalance).isEqualByComparingTo(new BigDecimal("1150.00"));

        // Verify last event
        String lastEventId = ledgerRepository.getLastEventId(accountId);
        assertThat(lastEventId).isEqualTo("E-WIN-004");
    }

    @Test
    public void testDeterministicOrdering_SameTimestamp() {
        // Test that events with same timestamp are ordered by eventId
        String accountId = "TEST-ORDER-A20";
        long timestamp = 5000L;

        // Create events with same timestamp but different eventIds
        LedgerEvent event1 = new LedgerEvent();
        event1.setEventId("E-ORDER-002"); // Later alphabetically
        event1.setAccountId(accountId);
        event1.setType("credit");
        event1.setAmount(new BigDecimal("200.00"));
        event1.setTimestamp(timestamp);
        ledgerRepository.save(event1);

        LedgerEvent event2 = new LedgerEvent();
        event2.setEventId("E-ORDER-001"); // Earlier alphabetically
        event2.setAccountId(accountId);
        event2.setType("credit");
        event2.setAmount(new BigDecimal("100.00"));
        event2.setTimestamp(timestamp);
        ledgerRepository.save(event2);

        // Retrieve ordered events
        List<LedgerEvent> orderedEvents = ledgerRepository.findByAccountIdOrderByTimestampAscEventIdAsc(accountId);

        // Verify ordering: E-ORDER-001 should come before E-ORDER-002
        assertThat(orderedEvents).hasSize(2);
        assertThat(orderedEvents.get(0).getEventId()).isEqualTo("E-ORDER-001");
        assertThat(orderedEvents.get(1).getEventId()).isEqualTo("E-ORDER-002");

        // Verify final balance (100 + 200 = 300)
        BigDecimal finalBalance = ledgerRepository.getFinalBalance(accountId);
        assertThat(finalBalance).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    public void testBalanceComputation_OnlyCredits() {
        String accountId = "TEST-CREDIT-ONLY";

        LedgerEvent event1 = new LedgerEvent();
        event1.setEventId("E-CREDIT-001");
        event1.setAccountId(accountId);
        event1.setType("credit");
        event1.setAmount(new BigDecimal("100.00"));
        event1.setTimestamp(1000L);
        ledgerRepository.save(event1);

        LedgerEvent event2 = new LedgerEvent();
        event2.setEventId("E-CREDIT-002");
        event2.setAccountId(accountId);
        event2.setType("credit");
        event2.setAmount(new BigDecimal("200.00"));
        event2.setTimestamp(2000L);
        ledgerRepository.save(event2);

        BigDecimal finalBalance = ledgerRepository.getFinalBalance(accountId);
        assertThat(finalBalance).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    public void testBalanceComputation_OnlyDebits() {
        String accountId = "TEST-DEBIT-ONLY";

        // Start with a credit
        LedgerEvent event1 = new LedgerEvent();
        event1.setEventId("E-DEBIT-INIT");
        event1.setAccountId(accountId);
        event1.setType("credit");
        event1.setAmount(new BigDecimal("1000.00"));
        event1.setTimestamp(1000L);
        ledgerRepository.save(event1);

        LedgerEvent event2 = new LedgerEvent();
        event2.setEventId("E-DEBIT-001");
        event2.setAccountId(accountId);
        event2.setType("debit");
        event2.setAmount(new BigDecimal("100.00"));
        event2.setTimestamp(2000L);
        ledgerRepository.save(event2);

        LedgerEvent event3 = new LedgerEvent();
        event3.setEventId("E-DEBIT-002");
        event3.setAccountId(accountId);
        event3.setType("debit");
        event3.setAmount(new BigDecimal("200.00"));
        event3.setTimestamp(3000L);
        ledgerRepository.save(event3);

        BigDecimal finalBalance = ledgerRepository.getFinalBalance(accountId);
        assertThat(finalBalance).isEqualByComparingTo(new BigDecimal("700.00"));
    }

    @Test
    public void testEmptyAccount_ReturnsZeroBalance() {
        String accountId = "TEST-EMPTY-ACCOUNT";

        BigDecimal finalBalance = ledgerRepository.getFinalBalance(accountId);
        assertThat(finalBalance).isEqualByComparingTo(BigDecimal.ZERO);

        String lastEventId = ledgerRepository.getLastEventId(accountId);
        assertThat(lastEventId).isNull();
    }
}

