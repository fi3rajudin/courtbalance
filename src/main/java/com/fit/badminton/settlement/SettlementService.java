package com.fit.badminton.settlement;
import com.fit.badminton.common.NotFoundException;
import com.fit.badminton.expense.*;
import com.fit.badminton.member.*;
import com.fit.badminton.payment.*;
import com.fit.badminton.session.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettlementService {
 private final BadmintonSessionRepository sessions;
 private final SessionParticipantRepository participants;
 private final ExpenseRepository expenses;
 private final SettlementTransferRepository transfers;
 private final MemberRepository members;
 private final PaymentAuditRepository audits;
 private final SettlementCalculator calculator=new SettlementCalculator();
 public SettlementService(BadmintonSessionRepository s,SessionParticipantRepository p,ExpenseRepository e,SettlementTransferRepository t,MemberRepository m,PaymentAuditRepository a){sessions=s;participants=p;expenses=e;transfers=t;members=m;audits=a;}
 public SettlementPreview preview(long id){BadmintonSession s=sessions.findById(id).orElseThrow(()->new NotFoundException("Session not found"));return compute(s);}
 private SettlementPreview compute(BadmintonSession s){
   List<Long> ids=participants.findBySessionIdOrderByIdAsc(s.getId()).stream().map(x->x.getMember().getId()).toList();
   List<ExpenseShareInput> ex=expenses.findBySessionIdOrderByIdAsc(s.getId()).stream().map(x->new ExpenseShareInput(x.getPaidBy().getId(),x.getAmount())).toList();
   SettlementResult r=calculator.calculate(ids,ex);return new SettlementPreview(r.totalCost(),r.shares(),r.balances(),r.transfers());
 }
 @Transactional public SettlementPreview recalculateAndPersist(long id){
   BadmintonSession s=sessions.findById(id).orElseThrow(()->new NotFoundException("Session not found"));
   SettlementPreview p=compute(s); Instant now=Instant.now();
   for(SettlementTransfer old:transfers.findBySessionIdAndActiveTrueOrderByIdAsc(id)){
     if(old.getStatus()==TransferStatus.PAID) audits.save(new PaymentAudit(old,PaymentAuditAction.RECALCULATION_RESET,ChangedByType.SYSTEM,now));
     old.deactivate(now);
   }
   for(SettlementInstruction i:p.transfers()){
     Member payer=members.findById(i.payerMemberId()).orElseThrow();Member payee=members.findById(i.payeeMemberId()).orElseThrow();
     transfers.save(new SettlementTransfer(s,payer,payee,i.amount()));
   }
   return p;
 }
}
