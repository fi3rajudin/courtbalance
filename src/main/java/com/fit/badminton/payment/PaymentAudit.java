package com.fit.badminton.payment;
import com.fit.badminton.settlement.SettlementTransfer; import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="payment_audit")
public class PaymentAudit {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="transfer_id",nullable=false) private SettlementTransfer transfer;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=40) private PaymentAuditAction action;
 @Enumerated(EnumType.STRING) @Column(name="changed_by_type",nullable=false,length=30) private ChangedByType changedByType;
 @Column(name="changed_at",nullable=false) private Instant changedAt;
 protected PaymentAudit(){} public PaymentAudit(SettlementTransfer t,PaymentAuditAction a,ChangedByType b,Instant at){transfer=t;action=a;changedByType=b;changedAt=at;}
 public Long getId(){return id;} public SettlementTransfer getTransfer(){return transfer;} public PaymentAuditAction getAction(){return action;} public ChangedByType getChangedByType(){return changedByType;} public Instant getChangedAt(){return changedAt;}
}
