package com.fdbpay.repositories;

import com.fdbpay.models.entity.User;
import com.fdbpay.models.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByPhone(String phone);

    boolean existsByPhone(String phone);

    boolean existsByReferralCode(String referralCode);

    Optional<User> findByReferralCode(String referralCode);

    Optional<User> findByEmail(String email);
}
