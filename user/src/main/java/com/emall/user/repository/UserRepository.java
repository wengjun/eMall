package com.emall.user.repository;

import com.emall.user.domain.UserAccount;
import java.util.Optional;

public interface UserRepository {
    UserAccount save(UserAccount user);

    Optional<UserAccount> findById(long userId);

    Optional<UserAccount> findByMobile(String mobile);
}
