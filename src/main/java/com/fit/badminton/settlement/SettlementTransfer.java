package com.fit.badminton.settlement;
import com.fit.badminton.member.Member;
import com.fit.badminton.session.BadmintonSession;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="settlement_transfer")
public class SettlementTransfer {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="session_id",nullable=false) private BadmintonSession session;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="payer_member_id",nullable=false) private Member payer;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="payee_member_id",nullable=false) private Member payee;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal amount;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private TransferStatus status=TransferStatus.PENDING;
 @Column(nullable=false) private boolean active=true;
 @Column(name="paid_at") private Instant paidAt;
 @Column(name="undo_until") private Instant undoUntil;
 @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now();
 @Column(name="updated_at",nullable=false) private Instant updatedAt=Instant.now();
 protected SettlementTransfer(){}
 public SettlementTransfer(BadmintonSession s,Member p,Member e,BigDecimal a){session=s;payer=p;payee=e;amount=a;}
 public Long getId(){return id;} public BadmintonSession getSession(){return session;} public Member getPayer(){return payer;} public Member getPayee(){return payee;} public BigDecimal getAmount(){return amount;} public TransferStatus getStatus(){return status;} public boolean isActive(){return active;} public Instant getPaidAt(){return paidAt;} public Instant getUndoUntil(){return undoUntil;}
 public void markPaid(Instant now){status=TransferStatus.PAID;paidAt=now;undoUntil=now.plusSeconds(300);updatedAt=now;}
 public void markPending(Instant now){status=TransferStatus.PENDING;paidAt=null;undoUntil=null;updatedAt=now;}
 public void deactivate(Instant now){active=false;updatedAt=now;}
}
