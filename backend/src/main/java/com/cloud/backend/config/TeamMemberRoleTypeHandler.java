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
 */
public class TeamMemberRoleTypeHandler extends BaseTypeHandler<TeamMemberRole> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, TeamMemberRole parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setInt(i, parameter.getValue());
    }

    @Override
    public TeamMemberRole getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : TeamMemberRole.fromValue(value);
    }

    @Override
    public TeamMemberRole getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int value = rs.getInt(columnIndex);
        return rs.wasNull() ? null : TeamMemberRole.fromValue(value);
    }

    @Override
    public TeamMemberRole getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int value = cs.getInt(columnIndex);
        return cs.wasNull() ? null : TeamMemberRole.fromValue(value);
    }
}
