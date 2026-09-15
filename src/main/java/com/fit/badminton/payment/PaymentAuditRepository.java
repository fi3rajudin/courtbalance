package com.fit.badminton.payment;
import java.util.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface PaymentAuditRepository extends JpaRepository<PaymentAudit,Long>{
 @Query("select a from PaymentAudit a join fetch a.transfer t where t.session.id=:sessionId order by a.changedAt asc") List<PaymentAudit> findForSession(@Param("sessionId") Long sessionId);
}
