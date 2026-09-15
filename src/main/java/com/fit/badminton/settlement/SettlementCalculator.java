package com.fit.badminton.settlement;
import java.math.*; import java.util.*;
public class SettlementCalculator {
 public SettlementResult calculate(List<Long> participantIds,List<ExpenseShareInput> expenses){
   if(participantIds==null||participantIds.isEmpty()) throw new IllegalArgumentException("At least one participant is required");
   List<Long> ids=participantIds.stream().distinct().sorted().toList(); if(ids.size()!=participantIds.size()) throw new IllegalArgumentException("Duplicate participant");
   Set<Long> idSet=new HashSet<>(ids); BigDecimal total=BigDecimal.ZERO.setScale(2); Map<Long,BigDecimal> advanced=new HashMap<>(); ids.forEach(id->advanced.put(id,BigDecimal.ZERO.setScale(2)));
   for(ExpenseShareInput e:expenses==null?List.<ExpenseShareInput>of():expenses){ if(e.amount()==null||e.amount().scale()>2) throw new IllegalArgumentException("Expense amount must have at most 2 decimals"); if(e.amount().signum()<0) throw new IllegalArgumentException("Expense cannot be negative"); if(!idSet.contains(e.paidByMemberId())) throw new IllegalArgumentException("Expense payer must be a participant"); BigDecimal a=e.amount().setScale(2); total=total.add(a); advanced.put(e.paidByMemberId(),advanced.get(e.paidByMemberId()).add(a)); }
   long cents=total.movePointRight(2).longValueExact(), base=cents/ids.size(), rem=cents%ids.size(); Map<Long,BigDecimal> shares=new LinkedHashMap<>();
   for(int i=0;i<ids.size();i++){ long c=base+(i<rem?1:0); shares.put(ids.get(i),BigDecimal.valueOf(c,2)); }
   List<MemberBalance> balances=new ArrayList<>(); List<BalanceWork> debtors=new ArrayList<>(), creditors=new ArrayList<>();
   for(long id:ids){ BigDecimal net=advanced.get(id).subtract(shares.get(id)).setScale(2); balances.add(new MemberBalance(id,advanced.get(id),shares.get(id),net)); if(net.signum()<0)debtors.add(new BalanceWork(id,net.negate())); else if(net.signum()>0)creditors.add(new BalanceWork(id,net)); }
   List<SettlementInstruction> transfers=new ArrayList<>(); int di=0,ci=0;
   while(di<debtors.size()&&ci<creditors.size()){ BalanceWork d=debtors.get(di), c=creditors.get(ci); BigDecimal amt=d.remaining.min(c.remaining).setScale(2); if(amt.signum()>0)transfers.add(new SettlementInstruction(d.id,c.id,amt)); d.remaining=d.remaining.subtract(amt); c.remaining=c.remaining.subtract(amt); if(d.remaining.signum()==0)di++; if(c.remaining.signum()==0)ci++; }
   BigDecimal netSum=balances.stream().map(MemberBalance::netBalance).reduce(BigDecimal.ZERO,BigDecimal::add); if(netSum.compareTo(BigDecimal.ZERO)!=0) throw new IllegalStateException("Settlement does not balance");
   return new SettlementResult(total,Collections.unmodifiableMap(shares),List.copyOf(balances),List.copyOf(transfers));
 }
 private static class BalanceWork{final long id;BigDecimal remaining;BalanceWork(long i,BigDecimal r){id=i;remaining=r;}}
}
