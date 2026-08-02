<!--
  SystemConfigCenterView —— 系统配置中心（管理端）。
  设计依据：docs/system-config-center.md + ADR-009。
  - 左侧分组 Tab + 右侧表单，各组独立保存；输入框留空 = 恢复配置文件默认值
  - 敏感分组（系统功能/邮件服务）仅 ADMIN+ 可编辑，OPERATOR 只读展示（禁用输入框）
  - 存储限制分组内含"老用户配额批量调整"区（日期范围 + 角色/状态过滤 + 预览明细）
-->
<template>
  <div class="system-config">
    <h2 class="page-title">系统配置中心</h2>

    <el-alert
      v-if="!canEditSensitive"
      type="info"
      :closable="false"
      show-icon
      title="当前为只读模式：系统功能与邮件服务分组仅 ADMIN 及以上可编辑"
      class="readonly-tip"
    />

    <el-card shadow="never">
      <el-tabs v-model="activeTab" tab-position="left" class="config-tabs">
        <!-- ============ 上传限制 ============ -->
        <el-tab-pane label="上传限制" name="upload">
          <div class="pane">
            <p class="pane-desc">单文件大小上限与同时上传任务数（VIP 差异化）。输入框留空保存 = 恢复默认。</p>
            <el-form label-width="180px" class="config-form">
              <el-form-item label="普通用户单文件上限">
                <div class="size-row">
                  <el-input-number v-model="upload.maxSizeUser" :min="0" :precision="2" :step="0.5" />
                  <el-select v-model="upload.maxSizeUserUnit" style="width: 90px">
                    <el-option v-for="u in SIZE_UNITS" :key="u" :label="u" :value="u" />
                  </el-select>
                </div>
              </el-form-item>
              <el-form-item label="VIP 单文件上限">
                <div class="size-row">
                  <el-input-number v-model="upload.maxSizeVip" :min="0" :precision="2" :step="0.5" />
                  <el-select v-model="upload.maxSizeVipUnit" style="width: 90px">
                    <el-option v-for="u in SIZE_UNITS" :key="u" :label="u" :value="u" />
                  </el-select>
                </div>
              </el-form-item>
              <el-form-item label="普通用户并发上传数">
                <el-input-number v-model="upload.maxConcurrentUser" :min="1" />
              </el-form-item>
              <el-form-item label="VIP 并发上传数">
                <el-input-number v-model="upload.maxConcurrentVip" :min="1" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="saving.upload" @click="saveUpload">保存上传限制</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- ============ 存储限制 ============ -->
        <el-tab-pane label="存储限制" name="storage">
          <div class="pane">
            <p class="pane-desc">新用户默认配额只影响以后注册的用户；存量用户请使用下方批量调整。</p>
            <el-form label-width="180px" class="config-form">
              <el-form-item label="普通用户默认配额">
                <div class="size-row">
                  <el-input-number v-model="storage.defaultQuotaUser" :min="0" :precision="2" :step="1" />
                  <el-select v-model="storage.defaultQuotaUserUnit" style="width: 90px">
                    <el-option v-for="u in SIZE_UNITS" :key="u" :label="u" :value="u" />
                  </el-select>
                </div>
              </el-form-item>
              <el-form-item label="VIP 用户默认配额">
                <div class="size-row">
                  <el-input-number v-model="storage.defaultQuotaVip" :min="0" :precision="2" :step="1" />
                  <el-select v-model="storage.defaultQuotaVipUnit" style="width: 90px">
                    <el-option v-for="u in SIZE_UNITS" :key="u" :label="u" :value="u" />
                  </el-select>
                </div>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="saving.storage" @click="saveStorage">保存存储限制</el-button>
              </el-form-item>
            </el-form>

            <el-divider />

            <!-- 老用户配额批量调整 -->
            <h3 class="sub-title">老用户配额批量调整</h3>
            <el-form label-width="120px" class="config-form">
              <el-form-item label="注册日期" required>
                <el-date-picker
                  v-model="batch.dateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  value-format="YYYY-MM-DD"
                />
              </el-form-item>
              <el-form-item label="角色">
                <el-select v-model="batch.role" style="width: 150px">
                  <el-option label="全部" value="ALL" />
                  <el-option label="普通用户" value="USER" />
                  <el-option label="VIP" value="VIP" />
                </el-select>
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="batch.status" style="width: 150px">
                  <el-option label="全部" value="ALL" />
                  <el-option
                    v-for="opt in metaStore.getGroup(MetaGroup.USER_STATUS)"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="新配额（普通）">
                <div class="size-row">
                  <el-input-number v-model="batch.targetQuotaUser" :min="0" :precision="2" :step="1" />
                  <el-select v-model="batch.targetUnit" style="width: 90px">
                    <el-option v-for="u in SIZE_UNITS" :key="u" :label="u" :value="u" />
                  </el-select>
                </div>
              </el-form-item>
              <el-form-item label="新配额（VIP）">
                <div class="size-row">
                  <el-input-number v-model="batch.targetQuotaVip" :min="0" :precision="2" :step="1" />
                  <span class="unit-suffix">同单位</span>
                </div>
              </el-form-item>
              <el-form-item>
                <el-button :loading="saving.batch" @click="previewBatch">预览受影响用户</el-button>
                <el-button type="danger" :loading="saving.batch" :disabled="!batch.previewCount" @click="confirmBatch">
                  执行批量调整
                </el-button>
                <el-button v-if="batch.previewCount > 0" type="primary" link @click="showBatchDetail">
                  查看明细（{{ batch.previewCount }} 人）
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- ============ 会话安全 ============ -->
        <el-tab-pane label="会话安全" name="session">
          <div class="pane">
            <p class="pane-desc">Access Token 有效期、验证码有效期、登录失败锁定、密码重置有效期。</p>
            <el-form label-width="180px" class="config-form">
              <el-form-item label="Access Token 有效期（分钟）">
                <el-input-number v-model="session.accessTokenTtlMinutes" :min="1" :step="30" />
              </el-form-item>
              <el-form-item label="验证码有效期（秒）">
                <el-input-number v-model="session.captchaTtlSeconds" :min="30" :step="30" />
              </el-form-item>
              <el-form-item label="登录失败锁定次数">
                <el-input-number v-model="session.loginLockThreshold" :min="1" />
              </el-form-item>
              <el-form-item label="登录锁定时间（分钟）">
                <el-input-number v-model="session.loginLockDurationMinutes" :min="1" :step="5" />
              </el-form-item>
              <el-form-item label="密码重置有效期（分钟）">
                <el-input-number v-model="session.resetPasswordTtlMinutes" :min="5" :step="5" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="saving.session" @click="saveSession">保存会话安全</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- ============ 缓存策略 ============ -->
        <el-tab-pane label="缓存策略" name="cache">
          <div class="pane">
            <p class="pane-desc">
              各类缓存 TTL（秒）。验证码/文件预览两项本期为预留项，暂不生效。
            </p>
            <el-form label-width="180px" class="config-form">
              <el-form-item label="验证码缓存 TTL（秒）">
                <el-input-number v-model="cache.captcha" :min="30" :step="30" />
              </el-form-item>
              <el-form-item label="登录失败计数 TTL（秒）">
                <el-input-number v-model="cache.loginAttempt" :min="30" :step="60" />
              </el-form-item>
              <el-form-item label="黑名单 Token TTL（秒）">
                <el-input-number v-model="cache.blacklist" :min="60" :step="60" />
              </el-form-item>
              <el-form-item label="文件预览缓存 TTL（秒）">
                <el-input-number v-model="cache.filePreview" :min="0" :step="60" />
              </el-form-item>
              <el-form-item label="下载链接 TTL（分钟）">
                <el-input-number v-model="cache.downloadLinkMinutes" :min="1" :step="5" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="saving.cache" @click="saveCache">保存缓存策略</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- ============ 系统功能（ADMIN） ============ -->
        <el-tab-pane label="系统功能" name="system">
          <div class="pane">
            <p class="pane-desc">全局功能开关，保存即生效。敏感分组，仅 ADMIN 及以上可编辑。</p>
            <el-form label-width="180px" class="config-form">
              <el-form-item label="开放注册">
                <el-switch v-model="system.allowRegister" :disabled="!canEditSensitive" />
              </el-form-item>
              <el-form-item label="允许游客分享">
                <el-switch v-model="system.allowGuestShare" :disabled="!canEditSensitive" />
              </el-form-item>
              <el-form-item label="邮件验证">
                <el-switch v-model="system.enableMailVerify" :disabled="!canEditSensitive" />
              </el-form-item>
              <el-form-item label="登录验证码">
                <el-switch v-model="system.enableCaptcha" :disabled="!canEditSensitive" />
              </el-form-item>
              <el-form-item label="操作日志">
                <el-switch v-model="system.enableOperationLog" :disabled="!canEditSensitive" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="saving.system" :disabled="!canEditSensitive" @click="saveSystem">
                  保存系统功能
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- ============ 文件管理 ============ -->
        <el-tab-pane label="文件管理" name="file">
          <div class="pane">
            <p class="pane-desc">回收站保留天数与分享相关默认值（分享项为预留配置）。</p>
            <el-form label-width="180px" class="config-form">
              <el-form-item label="回收站保留天数">
                <el-input-number v-model="fileSettings.recycleBinDays" :min="1" />
              </el-form-item>
              <el-form-item label="分享默认有效期（天）">
                <el-input-number v-model="fileSettings.shareDefaultValidDays" :min="1" />
              </el-form-item>
              <el-form-item label="分享最长有效期（天）">
                <el-input-number v-model="fileSettings.shareMaxValidDays" :min="1" />
              </el-form-item>
              <el-form-item label="同一文件最大分享次数">
                <el-input-number v-model="fileSettings.shareMaxCountPerFile" :min="0" />
              </el-form-item>
              <el-form-item label="分享默认要求提取码">
                <el-switch v-model="fileSettings.shareDefaultRequirePassword" />
              </el-form-item>
              <el-form-item label="分享默认下载策略">
                <el-select v-model="fileSettings.shareDefaultDownloadPolicy" style="width: 200px">
                  <el-option label="允许下载（可设次数上限）" value="ALLOW" />
                  <el-option label="禁止下载（仅可预览）" value="DENY" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="saving.file" @click="saveFile">保存文件管理</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- ============ 团队默认值 ============ -->
        <el-tab-pane label="团队" name="team">
          <div class="pane">
            <p class="pane-desc">团队模块默认值：新团队配额、每人团队数上限、团队回收站保留天数、团队最大成员数。</p>
            <el-form label-width="180px" class="config-form">
              <el-form-item label="每人团队数上限">
                <el-input-number v-model="teamSettings.maxPerUser" :min="1" :max="100" />
              </el-form-item>
              <el-form-item label="新团队默认配额">
                <div class="quota-input-row">
                  <el-input-number
                    v-model="teamSettings.defaultQuota"
                    :min="1"
                    style="flex: 1"
                  />
                  <el-select v-model="teamSettings.defaultQuotaUnit" style="width: 90px">
                    <el-option v-for="unit in SIZE_UNITS" :key="unit" :label="unit" :value="unit" />
                  </el-select>
                </div>
              </el-form-item>
              <el-form-item label="团队回收站保留天数">
                <el-input-number v-model="teamSettings.recycleBinDays" :min="1" :max="3650" />
              </el-form-item>
              <el-form-item label="团队最大成员数">
                <el-input-number v-model="teamSettings.maxMembers" :min="1" :max="1000" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="saving.team" @click="saveTeam">保存团队配置</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- ============ 邮件服务（ADMIN） ============ -->
        <el-tab-pane label="邮件服务" name="mail">
          <div class="pane">
            <p class="pane-desc">
              配置邮件发送服务器（支持转发或自有域名直发）。含密码的配置为敏感信息，仅 ADMIN 及以上可编辑；密码留空保存 = 不修改。
            </p>
            <el-alert
              v-if="canEditSensitive"
              type="warning"
              :closable="false"
              show-icon
              class="mail-domain-tip"
            >
              <template #title>如何配置：到你的邮件服务商官网搜索「SMTP 设置」，按文档填写下面各字段</template>
              查询步骤：① 打开你的邮箱服务商帮助中心，搜索「SMTP 服务器地址 / 端口 / 加密方式」；② 服务器地址、端口、加密方式三者以服务商文档为准（例如 465 对应 SSL，587 对应 STARTTLS）；③ 发件人邮箱填写你的域名邮箱账号（如 noreply@你的域名.com），登录账号、登录密码用服务商提供的 SMTP 专用账号与授权码。服务器地址与发件人邮箱需属于同一域名，否则会被拒收。
            </el-alert>
            <el-form label-width="180px" class="config-form">
              <el-form-item label="启用邮件服务">
                <el-switch v-model="mail.enabled" :disabled="!canEditSensitive" />
              </el-form-item>
              <el-form-item label="发件人邮箱">
                <el-input v-model="mail.from" placeholder="如 noreply@你的域名.com" :disabled="!canEditSensitive" />
              </el-form-item>
              <el-form-item label="发件人名称">
                <el-input v-model="mail.fromName" placeholder="如 Cloud 云盘" :disabled="!canEditSensitive" />
              </el-form-item>
              <el-form-item label="服务器地址">
                <el-input v-model="mail.host" placeholder="如 smtp.你的域名.com 或 smtp-relay.brevo.com" :disabled="!canEditSensitive" />
              </el-form-item>
              <el-form-item label="服务器端口">
                <el-input-number v-model="mail.port" :min="1" :max="65535" :disabled="!canEditSensitive" />
              </el-form-item>
              <el-form-item label="加密方式">
                <el-select v-model="mail.encryption" :disabled="!canEditSensitive" style="width: 200px">
                  <el-option label="STARTTLS（端口 587）" value="STARTTLS" />
                  <el-option label="SSL（端口 465）" value="SSL" />
                  <el-option label="无加密" value="NONE" />
                </el-select>
              </el-form-item>
              <el-form-item label="登录账号">
                <el-input v-model="mail.username" placeholder="SMTP 登录账号" :disabled="!canEditSensitive" />
              </el-form-item>
              <el-form-item label="登录密码">
                <el-input
                  v-model="mail.password"
                  type="password"
                  show-password
                  placeholder="留空 = 不修改当前密码"
                  :disabled="!canEditSensitive"
                />
              </el-form-item>
              <el-form-item label="发送频率限制（秒）">
                <el-input-number v-model="mail.frequencyLimit" :min="5" :step="5" :disabled="!canEditSensitive" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="saving.mail" :disabled="!canEditSensitive" @click="saveMail">
                  保存邮件服务
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- ============ 日志 ============ -->
        <el-tab-pane label="日志" name="log">
          <div class="pane">
            <p class="pane-desc">日志保存天数，超出部分由每日定时任务清理（03:30）。</p>
            <el-form label-width="180px" class="config-form">
              <el-form-item label="操作日志保存天数">
                <el-input-number v-model="logSettings.operationDays" :min="1" :max="3650" />
              </el-form-item>
              <el-form-item label="登录日志保存天数">
                <el-input-number v-model="logSettings.loginDays" :min="1" :max="3650" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="saving.log" @click="saveLog">保存日志配置</el-button>
                <el-button @click="openLogs('all')">查看操作日志</el-button>
                <el-button @click="openLogs('login')">查看登录日志</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 批量调整明细弹窗 -->
    <el-dialog v-model="batchDetail.visible" title="受影响用户明细" width="860px" :close-on-click-modal="false" draggable>
      <UserPreviewTable
        :users="batchDetail.users"
        :target-quota-user="batchDetail.targetQuotaUser"
        :target-quota-vip="batchDetail.targetQuotaVip"
      />
    </el-dialog>

    <!-- 日志查询弹窗 -->
    <el-dialog v-model="logDialog.visible" title="日志查询" width="900px" :close-on-click-modal="false" draggable>
      <el-tabs v-model="logDialog.tab" @tab-change="switchLogTab">
        <el-tab-pane label="操作日志" name="all" />
        <el-tab-pane label="登录日志" name="login" />
      </el-tabs>
      <el-table v-loading="logDialog.loading" :data="logDialog.records" stripe>
        <el-table-column label="时间" prop="createdAt" width="170" />
        <el-table-column label="用户" width="140">
          <template #default="{ row }">
            {{ row.username || `#${row.userId}` }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ opLabel(row.operation) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="详情" prop="detail" show-overflow-tooltip />
        <el-table-column label="IP" prop="ip" width="140" />
      </el-table>
      <div class="pager-row">
        <el-pagination
          v-model:current-page="logDialog.page"
          v-model:page-size="logDialog.size"
          layout="total, prev, pager, next, sizes"
          :total="logDialog.total"
          :page-sizes="[10, 20, 50, 100]"
          @current-change="loadLogs"
          @size-change="onLogSizeChange"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/*
 * 系统配置中心 —— 核心逻辑：
 *
 * 1. 数据来源：GET /api/admin/settings 一次拉取全部分组，各分组表单独立编辑、独立保存。
 * 2. 单位换算：容量字段（上传上限/默认配额）展示为"数值 + MB/GB 选择器"，
 *    提交时按所选单位换算为字节（复用 utils/format.ts 的 UNIT_BYTES）。
 * 3. 留空恢复默认：数字输入框清空后提交 undefined → 请求体字段为 null → 后端删除配置行。
 * 4. 敏感分组只读：userStore.role 数值枚举 >= Role.ADMIN 才可编辑（后端 SecurityConfig 双保险）。
 * 5. 批量调整：preview=true 预览（返回明细 + 数量），确认后 preview=false 执行（幂等）。
 */
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminSettings,
  updateUploadSettings,
  updateStorageSettings,
  updateSessionSettings,
  updateCacheSettings,
  updateSystemSettings,
  updateFileSettings,
  updateMailSettings,
  updateLogSettings,
  updateTeamSettings,
  quotaBatchUsers,
  queryLogs,
} from '@/api/admin/settings'
import type {
  AdminUserResponse,
  CacheSettings,
  FileSettings,
  LogItem,
  LogSettings,
  MailSettings,
  SessionSettings,
  StorageSettings,
  SystemSettings,
  UploadSettings,
} from '@/types/admin'
import { SIZE_UNITS, UNIT_BYTES, type SizeUnit } from '@/utils/format'
import { MetaGroup } from '@/types/meta'
import { useMetaStore } from '@/stores/meta'
import { useUserStore } from '@/stores/user'
import { Role } from '@/types/user'
import UserPreviewTable from '@/components/common/UserPreviewTable.vue'

const metaStore = useMetaStore()
const userStore = useUserStore()

/** 敏感分组（系统/邮件）仅 ADMIN+ 可编辑 */
const canEditSensitive = computed(() => userStore.role >= Role.ADMIN)

const activeTab = ref('upload')

const saving = reactive({
  upload: false,
  storage: false,
  session: false,
  cache: false,
  system: false,
  file: false,
  mail: false,
  log: false,
  team: false,
  batch: false,
})

/* ========== 分组表单状态 ========== */

const upload = reactive({
  maxSizeUser: 0,
  maxSizeUserUnit: 'MB' as SizeUnit,
  maxSizeVip: 0,
  maxSizeVipUnit: 'MB' as SizeUnit,
  maxConcurrentUser: 1,
  maxConcurrentVip: 1,
})

const storage = reactive({
  defaultQuotaUser: 0,
  defaultQuotaUserUnit: 'GB' as SizeUnit,
  defaultQuotaVip: 0,
  defaultQuotaVipUnit: 'GB' as SizeUnit,
})

const session = reactive<SessionSettings>({
  accessTokenTtlMinutes: 1440,
  captchaTtlSeconds: 300,
  loginLockThreshold: 5,
  loginLockDurationMinutes: 30,
  resetPasswordTtlMinutes: 30,
})

const cache = reactive<CacheSettings>({
  captcha: 300,
  loginAttempt: 1800,
  blacklist: 86400,
  filePreview: 0,
  downloadLinkMinutes: 10,
})

const system = reactive<SystemSettings>({
  allowRegister: true,
  allowGuestShare: false,
  enableMailVerify: true,
  enableCaptcha: false,
  enableOperationLog: true,
})

/** 系统开关说明（模板内已展开为 5 个独立表单项） */
const systemSwitches: Array<{ key: keyof SystemSettings; label: string }> = [
  { key: 'allowRegister', label: '开放注册' },
  { key: 'allowGuestShare', label: '允许游客分享' },
  { key: 'enableMailVerify', label: '邮件验证' },
  { key: 'enableCaptcha', label: '登录验证码' },
  { key: 'enableOperationLog', label: '操作日志' },
]

const fileSettings = reactive<FileSettings>({
  recycleBinDays: 30,
  shareDefaultValidDays: 7,
  shareMaxValidDays: 30,
  shareMaxCountPerFile: 0,
  shareDefaultRequirePassword: false,
  shareDefaultDownloadPolicy: 'ALLOW',
})

const mail = reactive<MailSettings>({
  enabled: true,
  host: '',
  port: 587,
  encryption: 'STARTTLS',
  username: '',
  password: null,
  from: '',
  fromName: '',
  frequencyLimit: 60,
})

const logSettings = reactive<LogSettings>({
  operationDays: 30,
  loginDays: 30,
})

/** 团队默认值（defaultQuota 按所选单位输入，提交时换算为字节） */
const teamSettings = reactive({
  maxPerUser: 10,
  defaultQuota: 10,
  defaultQuotaUnit: 'GB' as SizeUnit,
  recycleBinDays: 30,
  maxMembers: 50,
})

/* ========== 批量调整状态 ========== */

const batch = reactive({
  dateRange: null as [string, string] | null,
  role: 'ALL' as 'ALL' | 'USER' | 'VIP',
  status: 'ALL' as 'ALL' | 'NORMAL' | 'DISABLED' | 'LOCKED' | 'INACTIVE',
  targetQuotaUser: 5,
  targetQuotaVip: 100,
  targetUnit: 'GB' as SizeUnit,
  previewCount: 0,
})

const batchDetail = reactive({
  visible: false,
  users: [] as AdminUserResponse[],
  targetQuotaUser: 0,
  targetQuotaVip: 0,
})

/* ========== 日志查询状态 ========== */

const logDialog = reactive({
  visible: false,
  tab: 'all' as 'all' | 'login',
  loading: false,
  records: [] as LogItem[],
  total: 0,
  page: 1,
  size: 20,
})

/* ========== 数据加载 ========== */

function toUnit(bytes: number, unit: SizeUnit): number {
  return bytes / UNIT_BYTES[unit]
}

function loadSettings() {
  return getAdminSettings().then((s) => {
    upload.maxSizeUser = toUnit(s.upload.maxSizeUser, upload.maxSizeUserUnit)
    upload.maxSizeVip = toUnit(s.upload.maxSizeVip, upload.maxSizeVipUnit)
    upload.maxConcurrentUser = s.upload.maxConcurrentUser
    upload.maxConcurrentVip = s.upload.maxConcurrentVip

    storage.defaultQuotaUser = toUnit(s.storage.defaultQuotaUser, storage.defaultQuotaUserUnit)
    storage.defaultQuotaVip = toUnit(s.storage.defaultQuotaVip, storage.defaultQuotaVipUnit)

    Object.assign(session, s.session)
    Object.assign(cache, s.cache)
    Object.assign(system, s.system)
    Object.assign(fileSettings, s.file, {
      shareDefaultValidDays: s.file.shareDefaultValidDays ?? 0,
      shareMaxValidDays: s.file.shareMaxValidDays ?? 0,
      shareMaxCountPerFile: s.file.shareMaxCountPerFile ?? 0,
      shareDefaultRequirePassword: s.file.shareDefaultRequirePassword ?? false,
      shareDefaultDownloadPolicy: s.file.shareDefaultDownloadPolicy ?? 'ALLOW',
    })
    Object.assign(mail, s.mail, { password: null })
    Object.assign(logSettings, s.log)
    Object.assign(teamSettings, s.team, {
      defaultQuota: toUnit(s.team.defaultQuota, teamSettings.defaultQuotaUnit),
    })
  })
}

/* ========== 提交工具 ========== */

/** 空/undefined → null（恢复默认），否则换算为字节 */
function sizeToBytesOrNull(value: number | undefined | null, unit: SizeUnit): number | null {
  if (value === undefined || value === null || value === 0 || Number.isNaN(value)) return null
  return Math.round(value * UNIT_BYTES[unit])
}

function orNull(value: number | undefined): number | null {
  if (value === undefined || Number.isNaN(value)) return null
  return value
}

/* ========== 各分组保存 ========== */

async function saveUpload() {
  saving.upload = true
  try {
    await updateUploadSettings({
      maxSizeUser: sizeToBytesOrNull(upload.maxSizeUser, upload.maxSizeUserUnit),
      maxSizeVip: sizeToBytesOrNull(upload.maxSizeVip, upload.maxSizeVipUnit),
      maxConcurrentUser: orNull(upload.maxConcurrentUser),
      maxConcurrentVip: orNull(upload.maxConcurrentVip),
    })
    ElMessage.success('上传限制已保存')
  } catch {
    // 错误已在拦截器中提示
  } finally {
    saving.upload = false
  }
}

async function saveStorage() {
  saving.storage = true
  try {
    await updateStorageSettings({
      defaultQuotaUser: sizeToBytesOrNull(storage.defaultQuotaUser, storage.defaultQuotaUserUnit),
      defaultQuotaVip: sizeToBytesOrNull(storage.defaultQuotaVip, storage.defaultQuotaVipUnit),
    })
    ElMessage.success('存储限制已保存（只影响新注册用户）')
  } catch {
    // 错误已在拦截器中提示
  } finally {
    saving.storage = false
  }
}

async function saveSession() {
  saving.session = true
  try {
    await updateSessionSettings({
      accessTokenTtlMinutes: orNull(session.accessTokenTtlMinutes),
      captchaTtlSeconds: orNull(session.captchaTtlSeconds),
      loginLockThreshold: orNull(session.loginLockThreshold),
      loginLockDurationMinutes: orNull(session.loginLockDurationMinutes),
      resetPasswordTtlMinutes: orNull(session.resetPasswordTtlMinutes),
    })
    ElMessage.success('会话安全已保存（新登录生效）')
  } catch {
    // 错误已在拦截器中提示
  } finally {
    saving.session = false
  }
}

async function saveCache() {
  saving.cache = true
  try {
    await updateCacheSettings({
      captcha: orNull(cache.captcha),
      loginAttempt: orNull(cache.loginAttempt),
      blacklist: orNull(cache.blacklist),
      filePreview: orNull(cache.filePreview),
      downloadLinkMinutes: orNull(cache.downloadLinkMinutes),
    })
    ElMessage.success('缓存策略已保存')
  } catch {
    // 错误已在拦截器中提示
  } finally {
    saving.cache = false
  }
}

async function saveSystem() {
  saving.system = true
  try {
    await updateSystemSettings({ ...system })
    ElMessage.success('系统功能已保存，立即生效')
  } catch {
    // 错误已在拦截器中提示
  } finally {
    saving.system = false
  }
}

async function saveFile() {
  saving.file = true
  try {
    await updateFileSettings({
      recycleBinDays: orNull(fileSettings.recycleBinDays),
      shareDefaultValidDays: orNull(fileSettings.shareDefaultValidDays),
      shareMaxValidDays: orNull(fileSettings.shareMaxValidDays),
      shareMaxCountPerFile: orNull(fileSettings.shareMaxCountPerFile),
      shareDefaultRequirePassword: fileSettings.shareDefaultRequirePassword,
      shareDefaultDownloadPolicy: fileSettings.shareDefaultDownloadPolicy,
    })
    ElMessage.success('文件管理配置已保存')
  } catch {
    // 错误已在拦截器中提示
  } finally {
    saving.file = false
  }
}

async function saveMail() {
  // SMTP 密码修改二次确认（敏感操作）
  if (mail.password && mail.password.trim().length > 0) {
    try {
      await ElMessageBox.confirm(
        '您正在修改 SMTP 密码，保存后新密码将立即生效。确认修改？',
        '修改 SMTP 密码',
        { confirmButtonText: '确认修改', cancelButtonText: '取消', type: 'warning' },
      )
    } catch {
      return
    }
  }
  saving.mail = true
  try {
    await updateMailSettings({
      enabled: mail.enabled,
      host: mail.host || null,
      port: mail.port || null,
      encryption: mail.encryption || null,
      username: mail.username || null,
      password: mail.password && mail.password.trim().length > 0 ? mail.password : null,
      from: mail.from || null,
      fromName: mail.fromName || null,
      frequencyLimit: orNull(mail.frequencyLimit),
    })
    ElMessage.success('邮件服务配置已保存')
  } catch {
    // 错误已在拦截器中提示
  } finally {
    saving.mail = false
  }
}

async function saveLog() {
  saving.log = true
  try {
    await updateLogSettings({
      operationDays: orNull(logSettings.operationDays),
      loginDays: orNull(logSettings.loginDays),
    })
    ElMessage.success('日志配置已保存')
  } catch {
    // 错误已在拦截器中提示
  } finally {
    saving.log = false
  }
}

async function saveTeam() {
  saving.team = true
  try {
    await updateTeamSettings({
      maxPerUser: orNull(teamSettings.maxPerUser),
      defaultQuota: sizeToBytesOrNull(teamSettings.defaultQuota, teamSettings.defaultQuotaUnit),
      recycleBinDays: orNull(teamSettings.recycleBinDays),
      maxMembers: orNull(teamSettings.maxMembers),
    })
    ElMessage.success('团队配置已保存')
  } catch {
    // 错误已在拦截器中提示
  } finally {
    saving.team = false
  }
}

/* ========== 老用户配额批量调整 ========== */

function batchRequest(preview: boolean) {
  const [startDate, endDate] = batch.dateRange ?? []
  if (!startDate || !endDate) {
    ElMessage.warning('请选择注册日期范围')
    return null
  }
  return {
    startDate,
    endDate,
    role: batch.role,
    status: batch.status,
    targetQuotaUser: Math.round(batch.targetQuotaUser * UNIT_BYTES[batch.targetUnit]),
    targetQuotaVip: Math.round(batch.targetQuotaVip * UNIT_BYTES[batch.targetUnit]),
    preview,
  }
}

/** 预览：返回受影响用户数量 + 明细 */
async function previewBatch() {
  const req = batchRequest(true)
  if (!req) return
  saving.batch = true
  try {
    const res = await quotaBatchUsers(req)
    batch.previewCount = res.count
    batchDetail.users = res.users
    batchDetail.targetQuotaUser = req.targetQuotaUser
    batchDetail.targetQuotaVip = req.targetQuotaVip
    if (res.count === 0) {
      ElMessage.info('没有符合条件的用户')
    } else {
      ElMessage.success(`共 ${res.count} 个用户受影响`)
    }
  } catch {
    // 错误已在拦截器中提示
  } finally {
    saving.batch = false
  }
}

/** 执行：二次确认后批量修改配额 */
async function confirmBatch() {
  const req = batchRequest(false)
  if (!req) return
  try {
    await ElMessageBox.confirm(
      `将为 ${batch.previewCount} 个用户设置新配额（普通 ${batch.targetQuotaUser} ${batch.targetUnit} / VIP ${batch.targetQuotaVip} ${batch.targetUnit}），该操作可重复执行。确认继续？`,
      '执行批量调整',
      { confirmButtonText: '确认执行', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  saving.batch = true
  try {
    const res = await quotaBatchUsers(req)
    ElMessage.success(`已调整 ${res.count} 个用户的配额`)
    batch.previewCount = 0
    batchDetail.users = []
  } catch {
    // 错误已在拦截器中提示
  } finally {
    saving.batch = false
  }
}

function showBatchDetail() {
  batchDetail.visible = true
}

/* ========== 日志查询 ========== */

/** 打开日志弹窗（type：all=操作日志，login=登录日志） */
function openLogs(type: 'all' | 'login') {
  logDialog.tab = type
  logDialog.page = 1
  logDialog.visible = true
  loadLogs()
}

function switchLogTab() {
  logDialog.page = 1
  loadLogs()
}

function onLogSizeChange() {
  logDialog.page = 1
  loadLogs()
}

async function loadLogs() {
  logDialog.loading = true
  try {
    const res = await queryLogs({
      operation: logDialog.tab === 'login' ? 'LOGIN' : undefined,
      page: logDialog.page,
      size: logDialog.size,
    })
    logDialog.records = res.records
    logDialog.total = res.total
  } catch {
    // 错误已在拦截器中提示
  } finally {
    logDialog.loading = false
  }
}

/** 操作类型字典 label 兜底（枚举名直显） */
function opLabel(operation: string): string {
  const opt = metaStore.getGroup(MetaGroup.OPERATION_TYPE).find((o) => o.value === operation)
  return opt?.label ?? operation
}

onMounted(() => {
  metaStore.loadIfNeeded()
  loadSettings().catch(() => {
    // 错误已在拦截器中提示
  })
})
</script>

<style scoped>
.system-config {
  max-width: 1100px;
}

.page-title {
  margin: 0 0 24px;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.readonly-tip {
  margin-bottom: 16px;
}

.mail-domain-tip {
  margin: 0 0 16px;
}

.config-tabs :deep(.el-tabs__item) {
  height: 44px;
  line-height: 44px;
}

.pane {
  min-height: 360px;
  padding: 0 8px;
}

.pane-desc {
  margin: 0 0 16px;
  font-size: 13px;
  color: #909399;
}

.sub-title {
  margin: 24px 0 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.config-form {
  max-width: 640px;
}

.size-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.unit-suffix {
  font-size: 13px;
  color: #909399;
  margin-left: 4px;
}

.pager-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
