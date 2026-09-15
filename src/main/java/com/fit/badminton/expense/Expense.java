package com.fit.badminton.expense; import com.fit.badminton.member.Member; import com.fit.badminton.session.BadmintonSession; import jakarta.persistence.*; import java.math.BigDecimal; import java.time.Instant;
@Entity @Table(name="expense")
public class Expense {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="session_id",nullable=false) private BadmintonSession session; @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private ExpenseType type; @Column(length=255) private String description; @Column(nullable=false,precision=12,scale=2) private BigDecimal amount; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="paid_by_member_id",nullable=false) private Member paidBy;
 @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now(); @Column(name="updated_at",nullable=false) private Instant updatedAt=Instant.now();
 protected Expense(){} public Expense(BadmintonSession s,ExpenseType t,String d,BigDecimal a,Member p){session=s;type=t;description=d;amount=a;paidBy=p;}
 public Long getId(){return id;} public ExpenseType getType(){return type;} public String getDescription(){return description;} public BigDecimal getAmount(){return amount;} public Member getPaidBy(){return paidBy;} public BadmintonSession getSession(){return session;}
 public void updateMetadata(ExpenseType t,String d){type=t;description=d;updatedAt=Instant.now();}
}
