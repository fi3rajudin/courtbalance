package com.fit.badminton.publicview;

import com.fit.badminton.common.NotFoundException;
import com.fit.badminton.expense.*;
import com.fit.badminton.member.Member;
import com.fit.badminton.payment.PaymentService;
import com.fit.badminton.publicview.dto.*;
import com.fit.badminton.session.*;
import com.fit.badminton.settlement.*;
import com.fit.badminton.storage.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/public/sessions")
public class PublicSessionController {
    private final BadmintonSessionRepository sessions;
    private final SessionParticipantRepository participants;
    private final ExpenseRepository expenses;
    private final SettlementTransferRepository transfers;
    private final SettlementService settlement;
    private final PaymentService payments;
    private final QrStorageService storage;

    public PublicSessionController(BadmintonSessionRepository s, SessionParticipantRepository p, ExpenseRepository e,
            SettlementTransferRepository t, SettlementService ss, PaymentService ps, QrStorageService qs) {
        sessions = s;
        participants = p;
        expenses = e;
        transfers = t;
        settlement = ss;
        payments = ps;
        storage = qs;
    }

    @GetMapping("/{token}")
    @Transactional(readOnly = true)
    public PublicSessionResponse view(@PathVariable String token) {
        BadmintonSession s = sessions.findByPublicToken(token)
                .orElseThrow(() -> new NotFoundException("Session not found"));
        if (s.getStatus() == SessionStatus.DRAFT)
            throw new NotFoundException("Session not found");
        var ps = participants.findBySessionIdOrderByIdAsc(s.getId());
        Map<Long, String> names = new HashMap<>();
        ps.forEach(p -> names.put(p.getMember().getId(), p.getMemberNameSnapshot()));
        SettlementPreview pre = settlement.preview(s.getId());
        var es = expenses.findBySessionIdOrderByIdAsc(s.getId());
        var ts = transfers.findBySessionIdAndActiveTrueOrderByIdAsc(s.getId());
        return new PublicSessionResponse(s.getTitle(), s.getSessionDate(), s.getNotes(), s.getStatus().name(),
                pre.totalCost(), ps.size(),
                ps.stream()
                        .map(p -> new PublicSessionResponse.Participant(p.getMember().getId(),
                                p.getMemberNameSnapshot(), pre.shares().get(p.getMember().getId())))
                        .toList(),
                es.stream()
                        .map(e -> new PublicSessionResponse.ExpenseItem(e.getType().name(), e.getDescription(),
                                e.getAmount(), names.getOrDefault(e.getPaidBy().getId(), e.getPaidBy().displayName())))
                        .toList(),
                ts.stream().map(t -> new PublicSessionResponse.TransferItem(t.getId(), t.getPayer().getId(),
                        names.getOrDefault(t.getPayer().getId(), t.getPayer().displayName()), t.getPayee().getId(),
                        names.getOrDefault(t.getPayee().getId(), t.getPayee().displayName()), t.getAmount(),
                        t.getStatus().name(), t.getPaidAt(), t.getUndoUntil())).toList());
    }

    @PostMapping("/{token}/transfers/{id}/paid")
    public Map<String, Object> paid(@PathVariable String token, @PathVariable long id) {
        SettlementTransfer t = payments.markPaid(token, id);
        return Map.of("id", t.getId(), "status", t.getStatus().name(), "paidAt", t.getPaidAt().toString(), "undoUntil",
                t.getUndoUntil().toString());
    }

    @PostMapping("/{token}/transfers/{id}/undo")
    public Map<String, Object> undo(@PathVariable String token, @PathVariable long id) {
        SettlementTransfer t = payments.undoPublic(token, id);
        return Map.of("id", t.getId(), "status", t.getStatus().name());
    }

    @GetMapping("/{token}/payees/{memberId}/qr")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> qr(@PathVariable String token, @PathVariable long memberId) {
        BadmintonSession s = sessions.findByPublicToken(token)
                .orElseThrow(() -> new NotFoundException("Session not found"));
        if (s.getStatus() == SessionStatus.DRAFT)
            throw new NotFoundException("Session not found");
        Member payee = transfers.findBySessionIdAndActiveTrueOrderByIdAsc(s.getId()).stream()
                .filter(t -> t.getPayee().getId().equals(memberId)).map(SettlementTransfer::getPayee).findFirst()
                .orElseThrow(() -> new NotFoundException("Payee not found in this session"));
        if (payee.getQrStoragePath() == null)
            throw new NotFoundException("No QR saved for this payee");
        QrPayload q = storage.load(payee.getQrStoragePath());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(q.contentType()))
                .cacheControl(CacheControl.noStore()).body(q.bytes());
    }
}
