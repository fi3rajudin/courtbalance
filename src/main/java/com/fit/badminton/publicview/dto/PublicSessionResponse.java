package com.fit.badminton.publicview.dto; import java.math.BigDecimal; import java.time.*; import java.util.*;
public record PublicSessionResponse(String title,LocalDate sessionDate,String notes,String status,BigDecimal totalCost,int participantCount,List<Participant> participants,List<ExpenseItem> expenses,List<TransferItem> transfers){
 public record Participant(Long memberId,String name,BigDecimal share){} public record ExpenseItem(String type,String description,BigDecimal amount,String paidBy){} public record TransferItem(Long id,Long payerMemberId,String payer,Long payeeMemberId,String payee,BigDecimal amount,String status,Instant paidAt,Instant undoUntil){}
}
