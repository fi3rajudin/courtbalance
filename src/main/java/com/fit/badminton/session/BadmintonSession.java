package com.fit.badminton.session;
import com.fit.badminton.auth.UserAccount; import jakarta.persistence.*; import java.time.*;
@Entity @Table(name="badminton_session")
public class BadmintonSession {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="public_token",nullable=false,unique=true,length=80) private String publicToken; @Column(nullable=false,length=150) private String title;
 @Column(name="session_date",nullable=false) private LocalDate sessionDate; @Column(columnDefinition="TEXT") private String notes; @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private SessionStatus status=SessionStatus.DRAFT;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="created_by_user_id",nullable=false) private UserAccount createdBy;
 @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now(); @Column(name="updated_at",nullable=false) private Instant updatedAt=Instant.now(); @Column(name="completed_at") private Instant completedAt;
 protected BadmintonSession(){} public BadmintonSession(String t,LocalDate d,String n,UserAccount u,String token){title=t;sessionDate=d;notes=n;createdBy=u;publicToken=token;}
 public Long getId(){return id;} public String getPublicToken(){return publicToken;} public String getTitle(){return title;} public LocalDate getSessionDate(){return sessionDate;} public String getNotes(){return notes;} public SessionStatus getStatus(){return status;} public Instant getCompletedAt(){return completedAt;}
 public void updateNonFinancial(String t,LocalDate d,String n){title=t;sessionDate=d;notes=n;updatedAt=Instant.now();}
 public void setStatus(SessionStatus s){status=s;updatedAt=Instant.now();completedAt=s==SessionStatus.COMPLETED?Instant.now():null;}
}
