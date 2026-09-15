package com.fit.badminton.expense;
import java.util.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface ExpenseRepository extends JpaRepository<Expense,Long>{
 List<Expense> findBySessionIdOrderByIdAsc(Long sessionId);
 @Modifying @Query("delete from Expense e where e.session.id=:sessionId") void deleteBySessionId(@Param("sessionId") Long sessionId);
}
