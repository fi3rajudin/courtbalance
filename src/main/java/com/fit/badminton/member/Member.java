package com.fit.badminton.member;
import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="member")
public class Member {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false,length=120) private String name; @Column(length=80) private String nickname; @Column(nullable=false) private boolean active=true;
 @Column(name="qr_storage_path",length=500) private String qrStoragePath; @Column(name="payment_note",length=255) private String paymentNote;
 @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now(); @Column(name="updated_at",nullable=false) private Instant updatedAt=Instant.now();
 protected Member(){} public Member(String name,String nickname,String paymentNote){this.name=name;this.nickname=nickname;this.paymentNote=paymentNote;}
 public Long getId(){return id;} public String getName(){return name;} public String getNickname(){return nickname;} public boolean isActive(){return active;} public String getQrStoragePath(){return qrStoragePath;} public String getPaymentNote(){return paymentNote;}
 public String displayName(){return nickname!=null&&!nickname.isBlank()?nickname:name;}
 public void update(String n,String nn,String note,boolean a){name=n;nickname=nn;paymentNote=note;active=a;updatedAt=Instant.now();} public void setQrStoragePath(String p){qrStoragePath=p;updatedAt=Instant.now();}
}
