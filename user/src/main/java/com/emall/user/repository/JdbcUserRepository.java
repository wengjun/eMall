package com.emall.user.repository;

import com.emall.common.crypto.FieldEncryptor;
import com.emall.user.domain.UserAccount;
import com.emall.user.domain.UserStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class JdbcUserRepository implements UserRepository {
    private final JdbcTemplate jdbcTemplate;
    private final FieldEncryptor fieldEncryptor;

    public JdbcUserRepository(JdbcTemplate jdbcTemplate, FieldEncryptor fieldEncryptor) {
        this.jdbcTemplate = jdbcTemplate;
        this.fieldEncryptor = fieldEncryptor;
    }

    @Override
    public UserAccount save(UserAccount user) {
        String mobileHash = fieldEncryptor.lookupHash(user.mobile());
        String encryptedMobile = fieldEncryptor.encrypt(user.mobile());
        int updated = jdbcTemplate.update("""
                UPDATE user_account
                SET mobile = ?, mobile_ciphertext = ?, mobile_hash = ?, nickname = ?, status = ?, updated_at = ?
                WHERE user_id = ?
                """,
                mobileHash, encryptedMobile, mobileHash, user.nickname(), user.status().name(),
                Timestamp.from(user.updatedAt()), user.userId());
        if (updated == 0) {
            jdbcTemplate.update("""
                    INSERT INTO user_account
                        (user_id, mobile, mobile_ciphertext, mobile_hash, nickname, status, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    user.userId(), mobileHash, encryptedMobile, mobileHash, user.nickname(), user.status().name(),
                    Timestamp.from(user.createdAt()), Timestamp.from(user.updatedAt()));
        }
        return user;
    }

    @Override
    public Optional<UserAccount> findById(long userId) {
        return jdbcTemplate.query("SELECT * FROM user_account WHERE user_id = ?", this::map, userId)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<UserAccount> findByMobile(String mobile) {
        String mobileHash = fieldEncryptor.lookupHash(mobile);
        return jdbcTemplate.query("""
                SELECT * FROM user_account
                WHERE mobile_hash = ? OR mobile = ?
                """, this::map, mobileHash, mobile)
                .stream()
                .findFirst();
    }

    private UserAccount map(ResultSet rs, int rowNum) throws SQLException {
        String encryptedMobile = rs.getString("mobile_ciphertext");
        String mobile = encryptedMobile == null || encryptedMobile.isBlank()
                ? rs.getString("mobile")
                : fieldEncryptor.decrypt(encryptedMobile);
        return new UserAccount(
                rs.getLong("user_id"),
                mobile,
                rs.getString("nickname"),
                UserStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }
}
