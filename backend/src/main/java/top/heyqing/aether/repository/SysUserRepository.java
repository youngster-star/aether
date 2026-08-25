package top.heyqing.aether.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import top.heyqing.aether.model.entity.SysUser;

/**
 * 管理员表数据访问（BackEnd-Plan §6.2 sys_user）
 */
public interface SysUserRepository extends JpaRepository<SysUser, Long> {

    /**
     * 按登录名查询（固定 cryptex）
     */
    Optional<SysUser> findByUsername(String username);
}
