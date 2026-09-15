package com.fit.badminton.session; import com.fit.badminton.member.Member; import jakarta.persistence.*;
@Entity @Table(name="session_participant",uniqueConstraints=@UniqueConstraint(columnNames={"session_id","member_id"}))
public class SessionParticipant {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="session_id",nullable=false) private BadmintonSession session; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="member_id",nullable=false) private Member member; @Column(name="member_name_snapshot",nullable=false) private String memberNameSnapshot;
 protected SessionParticipant(){} public SessionParticipant(BadmintonSession s,Member m,String snap){session=s;member=m;memberNameSnapshot=snap;}
 public Long getId(){return id;} public BadmintonSession getSession(){return session;} public Member getMember(){return member;} public String getMemberNameSnapshot(){return memberNameSnapshot;}
}
