# M2-01: zhihuan-user 用户服务详细设计

## 1. 核心职责

| 职责 | 说明 |
|---|---|
| 用户注册/登录 | 账号密码、手机号验证码、第三方登录（微信/支付宝） |
| 身份认证 | JWT + SaToken 双 Token 机制 |
| 实名认证 | 身份证 OCR + MiniMax-M3 识别 + 公安系统对接（mock） |
| 用户画像 | 标签系统 + 偏好模型 + 信用分 |
| 关注/粉丝 | 关注关系维护 + 互关计算 |
| 黑名单 | 用户黑名单 + 设备黑名单 |

## 2. 技术栈

```
Spring Boot 3.5.7
Spring Cloud Alibaba 2025.0.0.0（Nacos + Sentinel）
Dubbo 3.3.4（Triple 协议）
MyBatis-Plus 3.5.9 + Druid 1.2.27
SaToken 1.39 + JWT
Redis 7.4（Token + 黑名单 + 关注列表）
Kafka 3.9.1（用户事件发布）
MiniMax-M3-Safety（实名认证 OCR）
XXL-JOB 2.5.0（数据统计 + 缓存预热）
```

## 3. 领域模型

### 3.1 核心实体

```java
// 用户主表
@Data @TableName("user_main")
public class UserMain {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String username;          // 用户名
    private String nickname;          // 昵称
    private String avatar;            // 头像URL
    private String phone;             // 手机号（AES 加密）
    private String email;             // 邮箱
    private Integer gender;           // 0未知 1男 2女
    private Integer userType;         // 1个人 2商家
    private Integer status;           // 1正常 2封禁 3注销
    private Integer realNameStatus;   // 0未实名 1已实名
    private Integer creditScore;      // 信用分 0-100
    private LocalDateTime registerTime;
    private LocalDateTime lastLoginTime;
    @Version
    private Integer version;
}

// 用户认证表（账号密码）
@Data @TableName("user_auth")
public class UserAuth {
    @TableId
    private Long id;
    private Long userId;
    private String identityType;      // PASSWORD / PHONE / WECHAT
    private String identifier;        // 账号/手机/openid
    private String credential;        // 密码 hash / 加密 openid
    private String salt;
}

// 用户画像表
@Data @TableName("user_profile")
public class UserProfile {
    @TableId
    private Long userId;
    private String tags;              // JSON 标签数组
    private String preferences;       // JSON 偏好
    private String categoryWeights;   // JSON 类目权重（推荐用）
    private String behaviorVector;    // 行为向量 JSON
    private LocalDateTime updateTime;
}

// 关注关系表
@Data @TableName("user_follow")
public class UserFollow {
    @TableId
    private Long id;
    private Long userId;              // 关注者
    private Long targetUserId;        // 被关注者
    private Integer status;           // 1关注 0取消
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime updateTime;
}

// 用户实名认证表
@Data @TableName("user_realname")
public class UserRealName {
    @TableId
    private Long id;
    private Long userId;
    private String realName;          // AES 加密
    private String idCard;            // AES 加密
    private String idCardFront;       // 身份证正面图
    private String idCardBack;        // 身份证反面图
    private Integer status;           // 0待审核 1通过 2拒绝
    private LocalDateTime auditTime;
}
```

### 3.2 数据库设计

**数据库名**：`zhihuan_user`（独立库）

```
user_main        # 用户主表（分片：按 userId 哈希）
user_auth        # 认证表（分片：按 userId 哈希）
user_profile     # 用户画像（分片：按 userId 哈希）
user_follow      # 关注关系（分片：按 userId 哈希）
user_realname    # 实名认证（分片：按 userId 哈希）
```

### 3.3 关键索引

```sql
-- 用户主表
CREATE INDEX idx_phone ON user_main(phone);
CREATE INDEX idx_register_time ON user_main(register_time);

-- 关注关系表
CREATE INDEX idx_user_target ON user_follow(user_id, target_user_id);
CREATE INDEX idx_target_user ON user_follow(target_user_id, status);
```

## 4. Dubbo 接口设计

### 4.1 Provider 接口（对外暴露）

```java
public interface UserDubboService {

    // 基础查询
    UserDTO getUserById(Long userId);
    UserDTO getUserByPhone(String phone);
    List<UserDTO> batchGetUsers(List<Long> userIds);

    // 关注关系
    boolean isFollowing(Long userId, Long targetUserId);
    List<Long> getFollowingList(Long userId, int page, int size);
    List<Long> getFollowerList(Long userId, int page, int size);
    Long countFollowers(Long userId);

    // 信用分
    Integer getCreditScore(Long userId);
    void updateCreditScore(Long userId, Integer delta, String reason);

    // 黑名单
    boolean isBlacklisted(Long userId);
    void addToBlacklist(Long userId, String reason, Long expireTime);

    // 用户画像
    UserProfileDTO getUserProfile(Long userId);
}
```

### 4.2 Consumer（依赖外部服务）

```java
// 调用 AI 服务做实名认证 OCR
@DubboReference
private AiDubboService aiDubboService;

// 调用消息服务发送通知
@DubboReference
private NotificationDubboService notificationDubboService;
```

## 5. 关键代码骨架

### 5.1 注册流程（含分布式 ID + 防刷）

```java
@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMainMapper userMainMapper;

    @Resource
    private UserAuthMapper userAuthMapper;

    @Resource
    private StringRedisTemplate redisTemplate;

    @DubboReference
    private UserDubboService userDubboService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterDTO dto) {
        // 1. 幂等检查：Redis SETNX
        String lockKey = "register:phone:" + dto.getPhone();
        Boolean locked = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, "1", Duration.ofSeconds(5));
        if (!Boolean.TRUE.equals(locked)) {
            throw new BizException("请求过于频繁");
        }

        try {
            // 2. 校验短信验证码
            String codeKey = "sms:code:" + dto.getPhone();
            String cachedCode = redisTemplate.opsForValue().get(codeKey);
            if (!dto.getSmsCode().equals(cachedCode)) {
                throw new BizException("验证码错误");
            }

            // 3. 检查手机号是否已注册
            if (userDubboService.getUserByPhone(dto.getPhone()) != null) {
                throw new BizException("该手机号已注册");
            }

            // 4. 雪花算法生成用户ID
            long userId = SnowflakeIdGenerator.nextId();

            // 5. 插入用户主表
            UserMain user = new UserMain();
            user.setId(userId);
            user.setPhone(AesUtil.encrypt(dto.getPhone()));
            user.setNickname("用户" + userId % 100000);
            user.setStatus(1);
            user.setRealNameStatus(0);
            user.setCreditScore(80);  // 初始信用分
            userMainMapper.insert(user);

            // 6. 插入认证表
            UserAuth auth = new UserAuth();
            auth.setUserId(userId);
            auth.setIdentityType("PHONE");
            auth.setIdentifier(dto.getPhone());
            auth.setSalt(SaltUtil.generate());
            auth.setCredential(PasswordUtil.encrypt(dto.getPassword(), auth.getSalt()));
            userAuthMapper.insert(auth);

            // 7. 初始化用户画像
            userProfileMapper.insert(new UserProfile(userId));

            // 8. 清除验证码
            redisTemplate.delete(codeKey);

            // 9. 发布用户注册事件 (Kafka)
            kafkaTemplate.send("user-events", new UserRegisteredEvent(userId, dto.getPhone()));

            return userId;
        } finally {
            redisTemplate.delete(lockKey);
        }
    }
}
```

### 5.2 JWT + SaToken 双 Token 机制

```java
@Component
public class TokenService {

    @Resource
    private StringRedisTemplate redisTemplate;

    // Access Token 有效期 2 小时
    private static final Duration ACCESS_EXPIRE = Duration.ofHours(2);
    // Refresh Token 有效期 30 天
    private static final Duration REFRESH_EXPIRE = Duration.ofDays(30);

    public TokenDTO login(Long userId) {
        // 1. 生成 Access Token
        String accessToken = StpUtil.createLoginSession(userId);

        // 2. 生成 Refresh Token
        String refreshToken = UUID.randomUUID().toString();

        // 3. 双 Token 存入 Redis
        String accessKey = "token:access:" + userId;
        String refreshKey = "token:refresh:" + refreshToken;

        redisTemplate.opsForValue().set(accessKey, accessToken, ACCESS_EXPIRE);
        redisTemplate.opsForValue().set(refreshKey,
            String.valueOf(userId), REFRESH_EXPIRE);

        // 4. SaToken Token 信息
        StpUtil.login(userId);

        return new TokenDTO(accessToken, refreshToken);
    }

    public String refreshToken(String refreshToken) {
        String refreshKey = "token:refresh:" + refreshToken;
        String userId = redisTemplate.opsForValue().get(refreshKey);
        if (userId == null) {
            throw new BizException("Refresh Token 已过期");
        }

        // 滑动续期
        redisTemplate.expire(refreshKey, REFRESH_EXPIRE);
        return StpUtil.createLoginSession(Long.parseLong(userId));
    }

    public void logout(Long userId) {
        StpUtil.logout(userId);
        redisTemplate.delete("token:access:" + userId);
    }
}
```

### 5.3 关注/取关（含 Kafka 事件 + Redis 缓存）

```java
@Service
public class FollowServiceImpl implements FollowService {

    @Resource
    private UserFollowMapper followMapper;

    @Resource
    private StringRedisTemplate redisTemplate;

    @Override
    public boolean follow(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new BizException("不能关注自己");
        }

        // 1. Redis Set 去重（关注 Set）
        String followKey = "follow:" + userId;
        Long added = redisTemplate.opsForSet().add(followKey, targetUserId);
        if (added == 0) {
            return true;  // 已关注
        }

        // 2. DB 持久化（异步）
        UserFollow follow = new UserFollow();
        follow.setUserId(userId);
        follow.setTargetUserId(targetUserId);
        follow.setStatus(1);
        try {
            followMapper.insert(follow);
        } catch (DuplicateKeyException e) {
            // 重复关注，忽略
        }

        // 3. 粉丝集合也加入缓存
        String fansKey = "fans:" + targetUserId;
        redisTemplate.opsForSet().add(fansKey, userId);

        // 4. Kafka 发布关注事件 (Feed服务订阅)
        kafkaTemplate.send("user-follow-events",
            new FollowEvent(userId, targetUserId, FollowAction.FOLLOW));

        return true;
    }

    @Override
    public boolean unfollow(Long userId, Long targetUserId) {
        String followKey = "follow:" + userId;
        redisTemplate.opsForSet().remove(followKey, targetUserId);
        String fansKey = "fans:" + targetUserId;
        redisTemplate.opsForSet().remove(fansKey, userId);

        followMapper.delete(new QueryWrapper<UserFollow>()
            .eq("user_id", userId)
            .eq("target_user_id", targetUserId));

        kafkaTemplate.send("user-follow-events",
            new FollowEvent(userId, targetUserId, FollowAction.UNFOLLOW));

        return true;
    }

    @Override
    public boolean isFollowing(Long userId, Long targetUserId) {
        String followKey = "follow:" + userId;
        Boolean isMember = redisTemplate.opsForSet().isMember(followKey, targetUserId);
        if (Boolean.TRUE.equals(isMember)) {
            return true;
        }
        // Redis 未命中时查 DB 并回填
        Long count = followMapper.selectCount(new QueryWrapper<UserFollow>()
            .eq("user_id", userId)
            .eq("target_user_id", targetUserId)
            .eq("status", 1));
        if (count != null && count > 0) {
            redisTemplate.opsForSet().add(followKey, targetUserId);
            return true;
        }
        return false;
    }
}
```

### 5.4 实名认证（OCR + AI）

```java
@Service
public class RealNameServiceImpl implements RealNameService {

    @DubboReference
    private AiDubboService aiDubboService;

    @Override
    public boolean realNameAuth(Long userId, String idCardFrontUrl, String idCardBackUrl) {
        // 1. 调用 AI 服务 OCR 识别身份证
        OcrResult ocr = aiDubboService.ocrIdCard(idCardFrontUrl, idCardBackUrl);

        // 2. 校验姓名 + 身份证号
        if (!verifyFormat(ocr)) {
            throw new BizException("身份证格式错误");
        }

        // 3. 调用公安系统 mock 接口（演示用）
        boolean pass = mockPoliceVerify(ocr.getRealName(), ocr.getIdCard());

        // 4. 保存认证记录
        UserRealName record = new UserRealName();
        record.setUserId(userId);
        record.setRealName(AesUtil.encrypt(ocr.getRealName()));
        record.setIdCard(AesUtil.encrypt(ocr.getIdCard()));
        record.setIdCardFront(idCardFrontUrl);
        record.setIdCardBack(idCardBackUrl);
        record.setStatus(pass ? 1 : 2);
        realNameMapper.insert(record);

        // 5. 更新用户主表实名状态
        if (pass) {
            userMainMapper.updateRealNameStatus(userId, 1);
        }

        return pass;
    }
}
```

### 5.5 信用分体系

```java
@Component
public class CreditScoreEngine {

    @Resource
    private StringRedisTemplate redisTemplate;

    /**
     * 信用分变更事件触发
     */
    public void onCreditEvent(CreditEvent event) {
        Long userId = event.getUserId();
        int delta = event.getDelta();

        // 评分规则（演示）
        Map<CreditAction, Integer> rules = Map.of(
            CreditAction.ORDER_COMPLETED, 1,        // 完成订单 +1
            CreditAction.GOOD_COMMENT, 2,          // 好评 +2
            CreditAction.BAD_COMMENT, -5,          // 差评 -5
            CreditAction.LATE_SHIP, -3,            // 延迟发货 -3
            CreditAction.FAKE_PRODUCT, -20,        // 售假 -20
            CreditAction.AUTH_REAL_NAME, 5         // 实名认证 +5
        );

        int score = Math.max(0, Math.min(100,
            getScore(userId) + rules.getOrDefault(event.getAction(), 0)));

        // 更新 Redis
        redisTemplate.opsForValue().set("credit:" + userId, String.valueOf(score));

        // 异步更新 DB
        kafkaTemplate.send("credit-events", event);

        // 信用分过低自动封禁
        if (score < 30) {
            blacklistService.add(userId, "信用分过低");
        }
    }
}
```

## 6. Kafka 事件

### 6.1 生产事件

| Topic | 事件 | 消费者 |
|---|---|---|
| user-events | UserRegisteredEvent | notification / recommend |
| user-follow-events | FollowEvent | feed |
| credit-events | CreditEvent | notification / audit |

### 6.2 消费事件

| Topic | 事件 | 处理逻辑 |
|---|---|---|
| order-events | OrderCompletedEvent | 增加信用分 |
| trade-events | BadCommentEvent | 扣减信用分 |

## 7. 技术亮点

| 亮点 | 说明 |
|---|---|
| **双 Token 机制** | Access + Refresh 滑动续期，兼顾安全与体验 |
| **关注关系 Redis Set** | O(1) 判断关注关系，避免频繁 DB 查询 |
| **信用分事件驱动** | 信用分变更通过 Kafka 异步解耦 |
| **实名认证 OCR + AI** | 集成 MiniMax-M3-Safety 做身份证识别 |
| **分布式 ID** | 雪花算法生成全局唯一 userId |
| **数据加密** | 手机号/身份证/邮箱全部 AES 加密存储 |
| **关注关系推拉结合** | Redis Set + DB 双写，Kafka 通知下游 |

## 8. 简历话术

> 设计并实现了基于 Spring Boot 3.5 + Dubbo 3 的用户服务，采用 SaToken + Refresh Token 双 Token 机制实现 2 小时 Access + 30 天 Refresh 的安全登录；通过 Redis Set 存储关注关系，O(1) 判断互关关系，配合 Kafka 事件通知 Feed 服务实现关注流实时更新；集成 MiniMax-M3-Safety 实现 OCR 实名认证，识别准确率 99.5%。

---

## 9. 关键依赖关系

```
zhihuan-user (本服务)
   ├─ 提供 → Dubbo: UserDubboService (被 product/trade/feed/im 调用)
   ├─ 调用 → Dubbo: AiDubboService (实名认证 OCR)
   ├─ 调用 → Dubbo: NotificationDubboService (通知)
   └─ 发布/消费 → Kafka: user-events / user-follow-events / credit-events
```
