package com.fit.badminton.session;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BadmintonSessionRepository extends JpaRepository<BadmintonSession, Long> {
    Optional<BadmintonSession> findByPublicToken(String token);

    List<BadmintonSession> findAllByOrderBySessionDateDescIdDesc();
}
