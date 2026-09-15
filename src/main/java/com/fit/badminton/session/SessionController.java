package com.fit.badminton.session;

import com.fit.badminton.auth.*;
import com.fit.badminton.payment.*;
import com.fit.badminton.session.dto.*;
import com.fit.badminton.settlement.*;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/sessions")
public class SessionController {
    private final SessionService service;
    private final UserAccountService users;
    private final PaymentAuditRepository audits;

    public SessionController(SessionService s, UserAccountService u, PaymentAuditRepository a) {
        service = s;
        users = u;
        audits = a;
    }

    @GetMapping
    public List<SessionDetailResponse> list() {
        return service.history();
    }

    @GetMapping("/{id}")
    public SessionDetailResponse get(@PathVariable long id) {
        return service.detail(id);
    }

    @PostMapping
    public SessionDetailResponse create(@Valid @RequestBody SessionUpsertRequest r, Authentication a) {
        return service.create(r, users.getByUsername(a.getName()));
    }

    @PutMapping("/{id}")
    public SessionDetailResponse update(@PathVariable long id, @Valid @RequestBody SessionUpsertRequest r) {
        return service.update(id, r);
    }

    @PostMapping("/{id}/calculate")
    public SettlementPreview calc(@PathVariable long id) {
        return service.calculateDraft(id);
    }

    @PostMapping("/{id}/open")
    public SessionDetailResponse open(@PathVariable long id) {
        return service.open(id);
    }

    @PostMapping("/{id}/complete")
    public SessionDetailResponse complete(@PathVariable long id) {
        return service.complete(id);
    }

    @PostMapping("/{id}/reopen")
    public SessionDetailResponse reopen(@PathVariable long id) {
        return service.reopen(id);
    }

    public record AuditView(Long id, Long transferId, String action, String changedBy, Instant changedAt) {
    }

    @GetMapping("/{id}/payment-audit")
    public List<AuditView> audit(@PathVariable long id) {
        service.get(id);
        return audits.findForSession(id).stream().map(a -> new AuditView(a.getId(), a.getTransfer().getId(),
                a.getAction().name(), a.getChangedByType().name(), a.getChangedAt())).toList();
    }
}
