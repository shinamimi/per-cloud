package com.cloud.backend.entity;

import com.cloud.backend.enums.RoleEnum;
import com.cloud.backend.enums.UserStatusEnum;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体 —— 对应数据库 t_user 表。
 *
 * key 字段说明：
 * - role：角色枚举，存储为 TINYINT（0=USER, 1=OPERATOR, 2=ADMIN, 3=SUPER_ADMIN），
 *   但谨慎注意 EnumOrdinalTypeHandler 用 ordinal() 而非 getValue() 写入数据库，
 *   所以这里 ordinal 必须与 value 含义一致（USER 在最前 ordinal=0, value=0）
 * - quota：用户空间配额（字节），默认在 FileConstants.DEFAULT_QUOTA 中定义
 * - usedSpace：已使用空间（字节），上传/删除时更新
 * - status：用户状态，NORMAL=1 正常，DISABLED=0 禁用
 */
@Data
public class User {

    private Long id;
    private String username;
    private String password;
    private String email;
    private String nickname;
    private String avatar;
    private RoleEnum role;
    private Long quota;
    private Long usedSpace;
    private UserStatusEnum status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}