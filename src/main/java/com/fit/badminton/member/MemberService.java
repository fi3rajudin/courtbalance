package com.fit.badminton.member;
import com.fit.badminton.common.NotFoundException; import com.fit.badminton.member.dto.*; import java.util.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service
public class MemberService { private final MemberRepository repo; public MemberService(MemberRepository r){repo=r;}
 @Transactional public MemberResponse create(MemberRequest r){return map(repo.save(new Member(r.name(),r.nickname(),r.paymentNote())));}
 @Transactional public MemberResponse update(long id,MemberRequest r){Member m=getRequired(id);m.update(r.name(),r.nickname(),r.paymentNote(),r.active());return map(m);}
 public Member getRequired(long id){return repo.findById(id).orElseThrow(()->new NotFoundException("Member not found"));}
 public List<MemberResponse> list(boolean activeOnly){return (activeOnly?repo.findByActiveTrueOrderByNameAsc():repo.findAllByOrderByNameAsc()).stream().map(this::map).toList();}
 public MemberResponse get(long id){return map(getRequired(id));} public MemberResponse map(Member m){return new MemberResponse(m.getId(),m.getName(),m.getNickname(),m.displayName(),m.isActive(),m.getQrStoragePath()!=null,m.getPaymentNote());}
 @Transactional public void updateQrPath(long id,String p){getRequired(id).setQrStoragePath(p);}
}
