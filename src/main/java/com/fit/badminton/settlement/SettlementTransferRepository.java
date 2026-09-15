package com.fit.badminton.settlement;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SettlementTransferRepository extends JpaRepository<SettlementTransfer,Long>{
 List<SettlementTransfer> findBySessionIdAndActiveTrueOrderByIdAsc(Long sessionId);
 boolean existsBySessionIdAndActiveTrue(Long sessionId);
 @Query("select t from SettlementTransfer t join fetch t.session s where t.id=:id and t.active=true and s.publicToken=:token")
 Optional<SettlementTransfer> findPublic(@Param("token") String token,@Param("id") Long id);
}
