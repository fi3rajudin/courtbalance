package com.fit.badminton.payment;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fit.badminton.auth.UserAccount;
import com.fit.badminton.auth.UserRole;
import com.fit.badminton.member.Member;
import com.fit.badminton.session.BadmintonSession;
import com.fit.badminton.session.SessionStatus;
import com.fit.badminton.settlement.SettlementTransfer;
import com.fit.badminton.settlement.SettlementTransferRepository;
import com.fit.badminton.settlement.TransferStatus;
import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PaymentServiceTest {
    private static SettlementTransfer openTransfer() {
        UserAccount owner = new UserAccount("fit", "hash", UserRole.OWNER);
        BadmintonSession session = new BadmintonSession(
                "Friday Badminton", LocalDate.of(2026, 9, 18), null, owner, "token");
        session.setStatus(SessionStatus.OPEN);
        Member payer = new Member("Ali", null, null);
        Member payee = new Member("Fit", null, null);
        return new SettlementTransfer(session, payer, payee, new BigDecimal("30.00"));
    }

    @Test
    void publicMarkPaidStartsFiveMinuteUndoWindow() {
        SettlementTransferRepository transfers = mock(SettlementTransferRepository.class);
        PaymentAuditRepository audits = mock(PaymentAuditRepository.class);
        Instant now = Instant.parse("2026-09-10T06:00:00Z");
        SettlementTransfer transfer = openTransfer();
        when(transfers.findPublic("token", 1L)).thenReturn(Optional.of(transfer));

        PaymentService service = new PaymentService(transfers, audits, Clock.fixed(now, ZoneOffset.UTC));
        service.markPaid("token", 1L);

        assertThat(transfer.getStatus()).isEqualTo(TransferStatus.PAID);
        assertThat(transfer.getPaidAt()).isEqualTo(now);
        assertThat(transfer.getUndoUntil()).isEqualTo(now.plusSeconds(300));
        verify(audits).save(any(PaymentAudit.class));
    }

    @Test
    void publicUndoAfterFiveMinutesIsRejected() {
        SettlementTransferRepository transfers = mock(SettlementTransferRepository.class);
        PaymentAuditRepository audits = mock(PaymentAuditRepository.class);
        Instant paidAt = Instant.parse("2026-09-10T06:00:00Z");
        SettlementTransfer transfer = openTransfer();
        transfer.markPaid(paidAt);
        when(transfers.findPublic("token", 1L)).thenReturn(Optional.of(transfer));

        PaymentService service = new PaymentService(
                transfers, audits, Clock.fixed(paidAt.plusSeconds(301), ZoneOffset.UTC));

        assertThatThrownBy(() -> service.undoPublic("token", 1L))
                .hasMessageContaining("expired");
        assertThat(transfer.getStatus()).isEqualTo(TransferStatus.PAID);
    }
}
