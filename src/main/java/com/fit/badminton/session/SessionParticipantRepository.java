package com.fit.badminton.session;
import java.util.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface SessionParticipantRepository extends JpaRepository<SessionParticipant,Long>{
 List<SessionParticipant> findBySessionIdOrderByIdAsc(Long sessionId);
 @Modifying @Query("delete from SessionParticipant p where p.session.id=:sessionId") void deleteBySessionId(@Param("sessionId") Long sessionId);
}
