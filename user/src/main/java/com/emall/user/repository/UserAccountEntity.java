package com.emall.user.repository;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("user_account")
@Getter
@Setter
public class UserAccountEntity {
    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;

    @TableField("mobile")
    private String mobile;

    @TableField("mobile_ciphertext")
    private String mobileCiphertext;

    @TableField("mobile_hash")
    private String mobileHash;

    @TableField("nickname")
    private String nickname;

    @TableField("status")
    private String status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
