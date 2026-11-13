package com.medico.repo;

import com.medico.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRole(com.medico.model.Role role);

    /** Accounts created before roles existed have role = NULL; treat them as patients. */
    @Transactional @Modifying
    @Query("update User u set u.role = com.medico.model.Role.PATIENT where u.role is null")
    int backfillRoles();
}
