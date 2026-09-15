package com.fit.badminton.session;

import com.fit.badminton.auth.UserAccount;
import com.fit.badminton.common.*;
import com.fit.badminton.expense.*;
import com.fit.badminton.member.*;
import com.fit.badminton.session.dto.*;
import com.fit.badminton.settlement.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {
  private final BadmintonSessionRepository sessions;
  private final SessionParticipantRepository participants;
  private final ExpenseRepository expenses;
  private final MemberService members;
  private final SettlementTransferRepository transfers;
  private final SettlementService settlements;

  public SessionService(BadmintonSessionRepository s, SessionParticipantRepository p, ExpenseRepository e,
      MemberService m, SettlementTransferRepository t, SettlementService ss) {
    sessions = s;
    participants = p;
    expenses = e;
    members = m;
    transfers = t;
    settlements = ss;
  }

  @Transactional
  public SessionDetailResponse create(SessionUpsertRequest r, UserAccount user) {
    validate(r);
    BadmintonSession s = sessions
        .save(new BadmintonSession(r.title(), r.sessionDate(), r.notes(), user, UUID.randomUUID().toString()));
    replaceFinancial(s, r, true);
    return detail(s.getId());
  }

  @Transactional
  public SessionDetailResponse update(long id, SessionUpsertRequest r) {
    validate(r);
    BadmintonSession s = get(id);
    boolean changed = isFinancialChanged(s, r);
    boolean hasActive = transfers.existsBySessionIdAndActiveTrue(id);
    if (changed && hasActive && !r.recalculateSettlement())
      throw new ConflictException("Settlement recalculation confirmation required");
    s.updateNonFinancial(r.title(), r.sessionDate(), r.notes());
    if (changed) {
      replaceFinancial(s, r, false);
      if (hasActive || s.getStatus() != SessionStatus.DRAFT)
        settlements.recalculateAndPersist(id);
    } else {
      updateExpenseMetadata(s, r);
    }
    return detail(id);
  }

  private void validate(SessionUpsertRequest r) {
    if (r.participantIds() == null || r.participantIds().isEmpty())
      throw new IllegalArgumentException("At least one participant is required");
    Set<Long> ids = new HashSet<>(r.participantIds());
    if (ids.size() != r.participantIds().size())
      throw new IllegalArgumentException("Duplicate participant");
    for (var e : r.expenses() == null ? List.<ExpenseInput>of() : r.expenses())
      if (!ids.contains(e.paidByMemberId()))
        throw new IllegalArgumentException("Expense payer must be a participant");
  }

  private boolean isFinancialChanged(BadmintonSession s, SessionUpsertRequest r) {
    Set<Long> oldp = new HashSet<>(
        participants.findBySessionIdOrderByIdAsc(s.getId()).stream().map(x -> x.getMember().getId()).toList());
    if (!oldp.equals(new HashSet<>(r.participantIds())))
      return true;
    List<Expense> old = expenses.findBySessionIdOrderByIdAsc(s.getId());
    List<ExpenseInput> ne = r.expenses() == null ? List.of() : r.expenses();
    if (old.size() != ne.size())
      return true;
    for (int i = 0; i < old.size(); i++) {
      Expense a = old.get(i);
      ExpenseInput b = ne.get(i);
      if (a.getAmount().compareTo(b.amount()) != 0 || !a.getPaidBy().getId().equals(b.paidByMemberId()))
        return true;
    }
    return false;
  }

  private void updateExpenseMetadata(BadmintonSession s, SessionUpsertRequest r) {
    List<Expense> old = expenses.findBySessionIdOrderByIdAsc(s.getId());
    List<ExpenseInput> ne = r.expenses() == null ? List.of() : r.expenses();
    for (int i = 0; i < old.size(); i++)
      old.get(i).updateMetadata(ne.get(i).type(), ne.get(i).description());
  }

  private void replaceFinancial(BadmintonSession s, SessionUpsertRequest r, boolean fresh) {
    Map<Long, String> snaps = new HashMap<>();
    if (!fresh)
      for (SessionParticipant p : participants.findBySessionIdOrderByIdAsc(s.getId()))
        snaps.put(p.getMember().getId(), p.getMemberNameSnapshot());
    participants.deleteBySessionId(s.getId());
    expenses.deleteBySessionId(s.getId());
    for (Long id : r.participantIds()) {
      Member m = members.getRequired(id);
      participants.save(new SessionParticipant(s, m, snaps.getOrDefault(id, m.displayName())));
    }
    for (ExpenseInput x : r.expenses() == null ? List.<ExpenseInput>of() : r.expenses()) {
      Member payer = members.getRequired(x.paidByMemberId());
      expenses.save(new Expense(s, x.type(), x.description(), x.amount().setScale(2), payer));
    }
  }

  public BadmintonSession get(long id) {
    return sessions.findById(id).orElseThrow(() -> new NotFoundException("Session not found"));
  }

  @Transactional(readOnly = true)
public SessionDetailResponse detail(long id) {
    BadmintonSession s = get(id);
    List<SessionParticipant> ps = participants.findBySessionIdOrderByIdAsc(id);

    Map<Long, String> names = new HashMap<>();
    ps.forEach(p -> names.put(p.getMember().getId(), p.getMemberNameSnapshot()));

    List<Expense> es = expenses.findBySessionIdOrderByIdAsc(id);
    List<SettlementTransfer> ts = transfers.findBySessionIdAndActiveTrueOrderByIdAsc(id);

    BigDecimal total = es.stream()
        .map(Expense::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    return new SessionDetailResponse(
        s.getId(),
        s.getPublicToken(),
        s.getTitle(),
        s.getSessionDate(),
        s.getNotes(),
        s.getStatus(),

        ps.stream()
            .map(p -> new SessionDetailResponse.ParticipantView(
                p.getMember().getId(),
                p.getMemberNameSnapshot()))
            .toList(),

        es.stream()
            .map(e -> new SessionDetailResponse.ExpenseView(
                e.getId(),
                e.getType().name(),
                e.getDescription(),
                e.getAmount(),
                e.getPaidBy().getId(),
                names.getOrDefault(
                    e.getPaidBy().getId(),
                    e.getPaidBy().displayName())))
            .toList(),

        ts.stream()
            .map(t -> new SessionDetailResponse.TransferView(
                t.getId(),
                t.getPayer().getId(),
                names.getOrDefault(
                    t.getPayer().getId(),
                    t.getPayer().displayName()),
                t.getPayee().getId(),
                names.getOrDefault(
                    t.getPayee().getId(),
                    t.getPayee().displayName()),
                t.getAmount(),
                t.getStatus().name(),
                t.getPaidAt(),
                t.getUndoUntil()))
            .toList(),

        total,
        s.getCompletedAt());
}

@Transactional(readOnly = true)
public List<SessionDetailResponse> history() {
    return sessions.findAllByOrderBySessionDateDescIdDesc()
        .stream()
        .sorted(
            Comparator
                .comparingInt((BadmintonSession s) -> statusOrder(s.getStatus()))
                .thenComparing(
                    BadmintonSession::getSessionDate,
                    Comparator.reverseOrder()
                )
                .thenComparing(
                    BadmintonSession::getId,
                    Comparator.reverseOrder()
                )
        )
        .map(s -> detail(s.getId()))
        .toList();
}

private int statusOrder(SessionStatus status) {
    return switch (status) {
        case DRAFT -> 0;
        case OPEN -> 1;
        case COMPLETED -> 2;
    };
}

  @Transactional
  public SettlementPreview calculateDraft(long id) {
    BadmintonSession s = get(id);
    if (s.getStatus() != SessionStatus.DRAFT)
      throw new ConflictException("Recalculation from this action is allowed only while the session is DRAFT");
    return settlements.recalculateAndPersist(id);
  }

  @Transactional
  public SessionDetailResponse open(long id) {
    BadmintonSession s = get(id);
    if (s.getStatus() == SessionStatus.OPEN)
      return detail(id);
    if (s.getStatus() == SessionStatus.COMPLETED)
      throw new ConflictException("Completed session must be reopened instead");
    settlements.recalculateAndPersist(id);
    s.setStatus(SessionStatus.OPEN);
    return detail(id);
  }

  @Transactional
  public SessionDetailResponse complete(long id) {
    BadmintonSession s = get(id);
    if (s.getStatus() != SessionStatus.OPEN)
      throw new ConflictException("Only an OPEN session can be completed");
    boolean pending = transfers.findBySessionIdAndActiveTrueOrderByIdAsc(id).stream()
        .anyMatch(t -> t.getStatus() == TransferStatus.PENDING);
    if (pending)
      throw new ConflictException("All settlement transfers must be PAID before completing the session");
    s.setStatus(SessionStatus.COMPLETED);
    return detail(id);
  }

  @Transactional
  public SessionDetailResponse reopen(long id) {
    BadmintonSession s = get(id);
    if (s.getStatus() != SessionStatus.COMPLETED)
      throw new ConflictException("Only a COMPLETED session can be reopened");
    s.setStatus(SessionStatus.OPEN);
    return detail(id);
  }
}
