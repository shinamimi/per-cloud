package com.cloud.backend.service.system;

import com.cloud.backend.dto.LoginRequest;
import com.cloud.backend.dto.LoginResponse;
import com.cloud.backend.dto.RegisterRequest;
import com.cloud.backend.dto.ResetPasswordRequest;
import com.cloud.backend.dto.SendCodeRequest;

/**
 * 认证服务接口 —— 登录、注册、验证码发送、找回/重置密码。
 *
 * 设计思路：
 * 1. 登录/注册成功签发 JWT 并记录操作日志；登录失败累计次数，超限锁定账号
 * 2. 验证码按场景隔离（注册/重置/登录），并带发送冷却
 * 3. 邮件验证、开放注册等行为受管理端开关控制
 *
 * 修改指引：
 * - 【习惯】想改"登录流程（锁定/验证码/JWT）" → login() 对应 AuthServiceImpl.login()（失败累计 LoginAttemptService、
 *   达阈值落库 LOCKED、过锁定期自动解锁；登录验证码开关；成功签发 JWT + 写 LOGIN 日志含 IP）；
 *   改动影响账号安全策略与登录态
 * - 【习惯】想改"注册流程（开关/唯一性/默认配额/自动登录）" → register()（开放注册开关、邮箱验证、取
 *   default-quota-user、签发 JWT + 写 REGISTER 日志）；改动影响新用户准入与初始配额
 * - 【习惯】想改"验证码发送（冷却/场景隔离）" → sendCode()/sendForgotPasswordCode() → CaptchaService.generateAndStore/
 *   setCooldown 与 EmailService.sendCaptchaMail；改动影响发信频率与验证码场景
 * - 【习惯】想改"重置密码" → resetPassword()（邮件验证开关开启时校验验证码，BCrypt 加密入库）；改动影响密码重置安全边界
 * - 【习惯】操作日志：登录/注册内联写 OperationLog（LOGIN/REGISTER）；改动影响 OperationLogService 与登录日志
 * - 【习惯】新增方法 → 需同步实现类 AuthServiceImpl 与 AuthController
 */
public interface AuthService {

    /**
     * 账号密码登录。
     * 前置条件：账号存在且未禁用；验证码开关开启时须校验通过。
     * 失败时累计失败次数，达阈值锁定账号；成功返回 JWT。
     */
    LoginResponse login(LoginRequest request, String ip);

    /**
     * 注册并自动登录。
     * 前置条件：开放注册开关开启、用户名/邮箱未占用；邮件验证开启时须校验邮箱验证码。
     * 新用户配额取管理端配置的默认值；成功后直接返回 JWT。
     */
    LoginResponse register(RegisterRequest request, String ip);

    /**
     * 发送注册/登录/重置场景的验证码邮件，同一邮箱有发送冷却时间。
     */
    void sendCode(SendCodeRequest request);

    /**
     * 发送忘记密码验证码邮件。
     * 前置条件：邮箱已注册（未注册返回用户不存在）；受发送冷却限制。
     */
    void sendForgotPasswordCode(String email);

    /**
     * 校验邮箱验证码并重置密码。
     * 前置条件：邮件验证开关开启时验证码必须通过；邮箱必须已注册。
     */
    void resetPassword(ResetPasswordRequest request);
}
