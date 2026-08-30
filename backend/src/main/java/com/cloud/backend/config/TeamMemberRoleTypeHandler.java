package com.cloud.backend.config;

import com.cloud.backend.enums.TeamMemberRole;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
