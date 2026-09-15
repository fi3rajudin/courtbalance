package com.fit.badminton.payment;

import org.springframework.beans.factory.annotation.Autowired;
import com.fit.badminton.auth.UserRole;
import com.fit.badminton.common.*;
import com.fit.badminton.session.SessionStatus;
import com.fit.badminton.settlement.*;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private final SettlementTransferRepository transfers;
    private final PaymentAuditRepository audits;
    private final Clock clock;

    @Autowired
    public PaymentService(SettlementTransferRepository t, PaymentAuditRepository a) {
        this(t, a, Clock.systemUTC());
    }

    PaymentService(SettlementTransferRepository t, PaymentAuditRepository a, Clock c) {
        transfers = t;
        audits = a;
        clock = c;
    }

    @Transactional
    public SettlementTransfer markPaid(String token, long id) {
        SettlementTransfer t = publicTransfer(token, id);
        requireOpen(t);
        if (t.getStatus() == TransferStatus.PAID)
            return t;
        Instant now = clock.instant();
        t.markPaid(now);
        audits.save(new PaymentAudit(t, PaymentAuditAction.MARK_PAID, ChangedByType.PUBLIC_MEMBER, now));
        return t;
    }

    @Transactional
    public SettlementTransfer undoPublic(String token, long id) {
        SettlementTransfer t = publicTransfer(token, id);
        requireOpen(t);
        if (t.getStatus() != TransferStatus.PAID)
            throw new ConflictException("Transfer is not paid");
        Instant now = clock.instant();
        if (t.getUndoUntil() == null || now.isAfter(t.getUndoUntil()))
            throw new ConflictException("Public undo window has expired");
        t.markPending(now);
        audits.save(new PaymentAudit(t, PaymentAuditAction.UNDO, ChangedByType.PUBLIC_MEMBER, now));
        return t;
    }

    @Transactional
    public SettlementTransfer markUnpaidAdmin(long id, UserRole role) {
        SettlementTransfer t = transfers.findById(id).filter(SettlementTransfer::isActive)
                .orElseThrow(() -> new NotFoundException("Transfer not found"));
        Instant now = clock.instant();
        t.markPending(now);
        audits.save(new PaymentAudit(t, PaymentAuditAction.ADMIN_RESET,
                role == UserRole.OWNER ? ChangedByType.OWNER : ChangedByType.ADMIN, now));
        return t;
    }

    private SettlementTransfer publicTransfer(String token, long id) {
        return transfers.findPublic(token, id).orElseThrow(() -> new NotFoundException("Transfer not found"));
    }

    private void requireOpen(SettlementTransfer t) {
        if (t.getSession().getStatus() != SessionStatus.OPEN)
            throw new ConflictException("Payments can only be changed while the session is OPEN");
    }
}
