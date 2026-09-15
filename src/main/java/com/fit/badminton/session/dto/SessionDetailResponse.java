package com.fit.badminton.session.dto;
import com.fit.badminton.session.SessionStatus;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
public record SessionDetailResponse(Long id,String publicToken,String title,LocalDate sessionDate,String notes,SessionStatus status,List<ParticipantView> participants,List<ExpenseView> expenses,List<TransferView> transfers,BigDecimal totalCost,Instant completedAt){
 public record ParticipantView(Long memberId,String name){}
 public record ExpenseView(Long id,String type,String description,BigDecimal amount,Long paidByMemberId,String paidByName){}
 public record TransferView(Long id,Long payerMemberId,String payerName,Long payeeMemberId,String payeeName,BigDecimal amount,String status,Instant paidAt,Instant undoUntil){}
}
