package top.heyqing.aether.model.entity;

import java.time.LocalDateTime;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 管理员表（单账户，BackEnd-Plan §6.2 sys_user）
 */
@Entity
@Table(name = "sys_user")
public class SysUser extends BaseEntity {

    /** 登录名（固定 cryptex） */
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /** 密码哈希（Argon2） */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /** 状态：1 正常 0 禁用 */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    /** 最后登录时间 */
    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    /** 最后登录 IP */
    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public String getLastLoginIp() {
        return lastLoginIp;
    }

    public void setLastLoginIp(String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
    }
}
