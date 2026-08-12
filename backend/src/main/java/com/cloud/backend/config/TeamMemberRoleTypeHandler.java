package com.cloud.backend.config;

import com.cloud.backend.enums.TeamMemberRole;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * TeamMemberRole 自定义 TypeHandler —— DB 存枚举 value（0-成员 10-管理员 20-所有者），
 * 非 ordinal，不能使用 EnumOrdinalTypeHandler。
 *
 * 修改指引：
 * - 【习惯】修改写入 DB 的值          → setNonNullParameter() 中 parameter.getValue()；改动后影响落库的角色值
 * - 【习惯】修改读回枚举的方式        → getNullableResult() 中 TeamMemberRole.fromValue()；改动后影响角色反查
 * - 【习惯】新增中间档位角色          → 在 TeamMemberRole 枚举追加常量（value 取未占用档位，如 5/15）；
 *                             本处理器按 value 存取、fromValue 兜底 MEMBER，新增档位无需数据迁移，
 *                             但需在团队权限判断处补充新档位逻辑
 * - 【习惯】修改脏数据兜底            → TeamMemberRole.fromValue() 返回的兜底枚举；改动后影响未知 value 的默认角色
 */
public class TeamMemberRoleTypeHandler extends BaseTypeHandler<TeamMemberRole> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, TeamMemberRole parameter, JdbcType jdbcType)
            throws SQLException {
        /** 【统一】改后需同步 TeamMemberRole.value 枚举定义+读取方(TeamMemberRole.fromValue)（枚举 value 数值） */
        ps.setInt(i, parameter.getValue());
    }

    @Override
    public TeamMemberRole getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        /** 【统一】改后需同步 TeamMemberRole.value 枚举定义+写入方(TeamMemberRoleTypeHandler.setNonNullParameter)（枚举 value 数值） */
        return rs.wasNull() ? null : TeamMemberRole.fromValue(value);
    }

    @Override
    public TeamMemberRole getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int value = rs.getInt(columnIndex);
        /** 【统一】改后需同步 TeamMemberRole.value 枚举定义+写入方(TeamMemberRoleTypeHandler.setNonNullParameter)（枚举 value 数值） */
        return rs.wasNull() ? null : TeamMemberRole.fromValue(value);
    }

    @Override
    public TeamMemberRole getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int value = cs.getInt(columnIndex);
        /** 【统一】改后需同步 TeamMemberRole.value 枚举定义+写入方(TeamMemberRoleTypeHandler.setNonNullParameter)（枚举 value 数值） */
        return cs.wasNull() ? null : TeamMemberRole.fromValue(value);
    }
}
