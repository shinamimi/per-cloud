# Java 基础 × 本云盘系统 × Spring — 使用场景化复习指南

> 不按"语法点逐个背"，而是每个知识点都先用一行讲清**基础知识**（概念定义），再从四个场景角度展开：**本系统如何使用**、**本系统不使用需要怎么样**、**使用情况分类**、**在 Spring 中的作用**。
> 引用的文件行号以 `backend/src/main/java/com/cloud/backend/` 为根，可对照源码断点验证。
> 一句话心智模型：**Spring 是"用 Java 特性搭起来的框架"，本系统的每个业务能力都能映射回一个 Java 基础点。**

---

## 一、Java 基础语法

### 1.1 数据类型、运算符、流程控制

| 角度 | 内容 |
| --- | --- |
| 基础知识 | 基本类型 8 种（byte/short/int/long/float/double/char/boolean）+ 引用类型；运算符分算术/比较/逻辑/位运算/三元，注意优先级与短路（`&&`/`||`）；流程控制 if-else、switch、for、while、do-while；switch 表达式（Java 14+）可返回值、箭头语法不穿透。 |
| 本系统如何使用 | 数字类型：配额/文件大小用 `long`（`UserServiceImpl.calculateTotalQuota`）、分片数 `Math.ceil`（`UploadServiceImpl.init:173`）、大小格式化 `Math.log10`/`Math.pow`（`FileUtil.formatSize:120`）。字符串：路径拼接、`substring` 取扩展名（`FileUtil.getExtension:50`）。流程控制：`switch` 表达式映射 MIME 类型（`FileUtil.getMimeType:60`）、`if-else` 链路取客户端 IP（`IpUtil.getClientIp`）、`while` 循环做同名文件自动加序号（`FileUtil.resolveUniqueName:132`）、for 循环收集缺失分片（`UploadServiceImpl.merge:325`）。 |
| 本系统不使用需要怎么样 | 数字用字符串存→配额、分片、限流全要 `parseLong` 且容易 NPE；没有 `switch` 表达式→MIME 映射退化成一大串 if-else；没有位/取模运算→分片策略、哈希校验无法实现。这些是最底层能力，缺了连"上传一个文件"都写不出来。 |
| 使用情况分类 | ① 数值计算（配额/大小/分片）② 字符串处理（文件名/IP/路径）③ 流程分支（状态判断、类型映射）④ 边界与容错（null 判断、`<=0` 校验）。 |
| Spring 中的作用 | Spring 把 yml 字符串转成基本类型/枚举（`@ConfigurationProperties`、`@Value("${quota.default-user:5368709120}")`）；SpEL 表达式求值建立在 Java 运算模型之上；配置校验本质是类型转换 + 流程判断。 |

### 1.2 类与对象、构造函数

| 角度 | 内容 |
| --- | --- |
| 基础知识 | 类 = 属性 + 行为的模板，对象 = 类的实例（用 `new` 在堆上创建）；构造器与类同名、无返回值，用于初始化对象，可重载；不写任何构造器时编译器补默认无参构造器。 |
| 本系统如何使用 | DTO（`Result<T>`、`LoginRequest`）、Entity（`File`、`User`）都是"类即数据结构"；构造器注入是装配主流——`AuthServiceImpl` 注入 8 个依赖、`DownloadServiceImpl` 注入 7 个（`DownloadServiceImpl.java:81`）；不可变对象用 final 字段（`LoginUser.java:36`，构造器一次赋值）；枚举带构造器（`ErrorCode(int code, String message)`）。 |
| 本系统不使用需要怎么样 | 不用构造器注入→退化为 `@Autowired` 字段注入，出现循环依赖、对象中途可被改、难以单测；Entity 没 setter/getter→MyBatis 映射和 JSON 序列化全失效；对象可变→JWT 黑名单等共享状态易被并发破坏。 |
| 使用情况分类 | ① 数据传输对象（DTO，请求/响应载体）② 领域实体（Entity，对应表）③ 依赖装配（构造器注入）④ 不可变对象（LoginUser、ShareTokenGenerator 的常量）。 |
| Spring 中的作用 | Spring 通过反射调用构造器创建 Bean；构造器注入是官方推荐的 DI 方式——能发现循环依赖、保证 Bean 在使用前就完整、与 final 配合实现不可变。容器把"`new` 一个对象"升级为"管理一个对象的完整生命周期"。 |

### 1.3 方法、方法重载与重写

| 角度 | 内容 |
| --- | --- |
| 基础知识 | 方法 = 可命名的可复用代码块；**重载** = 同类中方法名相同、参数列表不同（编译期按参数定方法）；**重写** = 子类覆写父类方法，要求签名一致、访问权限不能更严、抛异常不能更宽（运行期动态绑定），用 `@Override` 校验。 |
| 本系统如何使用 | 重写（override）：9 组 Service 接口-实现（`UploadService`/`UploadServiceImpl`）、`JwtTokenUtil`/`JwtTokenUtilImpl`、`OncePerRequestFilter.doFilterInternal`（`JwtAuthenticationFilter.java:57`）、`UserDetails` 的 `isEnabled`（`LoginUser.java:77`）。重载（overload）：`Result.success(T)`/`success()`/`fail(...)` 静态工厂（`Result.java:36-66`）、`BusinessException` 两个构造器、`StorageService` 多个 `upload` 变体。 |
| 本系统不使用需要怎么样 | 没有重写→无法面向接口编程，Controller 直接耦合具体实现类，替换存储后端（MinIO→OSS）、单测 mock、加 AOP 切面全部做不到；没有重载→`Result.success()` 每种情况都得写不同方法名。 |
| 使用情况分类 | ① 接口实现（多态，框架回调必须重写）② 静态工厂重载（简化调用）③ 构造器重载（不同参数拼装对象）④ 父类模板方法覆写（框架扩展点）。 |
| Spring 中的作用 | 动态代理（JDK 代理按接口、CGLIB 按子类）依赖"方法可重写"实现 `@Transactional`/`@Log` 织入；Bean 替换/子类代理 = 重写在容器层的应用；重载是 Spring 大量 API（`RestTemplate.exchange` 等）简化调用面的手段。 |

### 1.4 包和访问权限

| 角度 | 内容 |
| --- | --- |
| 基础知识 | 包 = 命名空间（对应目录），避免类名冲突；`import` 引入外部类型；访问修饰符四级：`public`（任意）、`protected`（继承 + 同包）、默认包私有（同包）、`private`（本类），控制可见性与封装边界。 |
| 本系统如何使用 | 按层分包：`controller/service/config/security/enums/entity/mapper/dto/exception`；`private final` 字段 + `@Getter`（`BusinessException`）、`private` 构造器 + final 类防止工具类被实例化/继承（`ShareTokenGenerator.java:18`）、接口方法默认 public 作为对外契约。 |
| 本系统不使用需要怎么样 | 不按包分层→依赖方向混乱、循环依赖随手就来；访问权限不收敛→内部实现（如 token 字符集 ALPHABET、锁前缀）被外部到处引用，改动牵一发动全身；工具类可实例化→浪费且语义错误。 |
| 使用情况分类 | ① 分层分包（controller→service→mapper 单向依赖）② 封装内部实现（private 字段/构造器、final 类）③ 暴露契约（public 接口/方法）④ 同包协作（默认包私有，本系统少用）。 |
| Spring 中的作用 | `@SpringBootApplication` 按**根包**做组件扫描（包即边界）；代理/反射用 `setAccessible(true)` 可绕过访问权限实例化；Spring 官方建议按包控制依赖方向，与 DDD 分层一致。 |

### 1.5 基本输入输出、异常处理

| 角度 | 内容 |
| --- | --- |
| 基础知识 | IO 通过**流**读写数据：字节流（InputStream/OutputStream）与字符流（Reader/Writer）；IO 抛受检异常 `IOException` 必须处理。异常体系顶层 `Throwable` 分 `Error`（不可恢复）与 `Exception`（受检，必须声明/捕获）和 `RuntimeException`（不受检，可不处理）；语法 try-catch-finally，`try-with-resources`（Java 7+）自动关闭资源。 |
| 本系统如何使用 | IO：流式读写对象存储——`DigestInputStream` 边传边算 SHA-256（`UploadServiceImpl.merge:348`）、`SequenceInputStream` 合并分片、`ZipOutputStream` 打包（`DownloadServiceImpl.pack`）、`MultipartFile.getInputStream()` 读上传分片；`try-with-resources` 保证流关闭。异常：Service 抛 `BusinessException`，Controller 全程无 try-catch，`@RestControllerAdvice` 统一兜底（`GlobalExceptionHandler.java:38`）；`IOException` 等受检异常在局部 catch 并转为业务异常（`UploadServiceImpl.uploadChunk:257`）。 |
| 本系统不使用需要怎么样 | 不掌握 try-with-resources→流泄漏、句柄耗尽、zip 文件损坏；异常不统一抛收→每个 Controller 手写 try-catch 拼 `Result.fail`，代码膨胀且响应格式不一致；受检异常处处声明→业务代码被 try-catch 淹没。 |
| 使用情况分类 | ① 流处理（文件/网络/对象存储，务必关闭）② 装饰流（Digest/Sequence/Filter 流组合能力）③ 受检异常局部消化（IO、网络）④ 运行时异常上抛（业务错误码）。 |
| Spring 中的作用 | `MultipartFile`/`Resource` 抽象统一文件、类路径、URL 来源；`MessageConverter` 封装了 JSON 输入输出；异常体系 `NestedRuntimeException` 包装底层异常，`@Transactional` 靠运行时异常自动回滚。 |

---

## 二、面向对象编程

### 2.1 封装、继承、多态

| 角度 | 内容 |
| --- | --- |
| 基础知识 | 三大特性：**封装**（隐藏内部状态，只暴露必要接口）、**继承**（is-a，子类复用并扩展父类，Java 单继承）、**多态**（编译期看引用类型、运行期看实际对象类型，动态绑定调用哪个方法）。 |
| 本系统如何使用 | 封装：Entity `private` + getter/setter、工具类静态方法（`FileUtil`/`IpUtil`）、权限判断集中在 `AuthorizationPolicy`、规则注释写在类头。继承：`LoginUser implements UserDetails`、`JwtAuthenticationFilter extends OncePerRequestFilter`、`TeamMemberRoleTypeHandler extends BaseTypeHandler`。多态：9 组接口-实现、`StorageService` 抽象可换实现、`EnumOrdinalTypeHandler` 按枚举多态映射。 |
| 本系统不使用需要怎么样 | 不封装→同名单文件命名、IP 获取等规则散落各 Service，改一处漏一处；不继承框架基类→过滤器、TypeHandler 进不了框架调用链；不多态→`StorageService` 写死 MinIO，换 OSS/S3 要改所有调用方。 |
| 使用情况分类 | ① 接口多态（依赖注入、可替换实现）② 继承框架基类（复用骨架 + 框架回调）③ 数据封装（Entity/DTO）④ 逻辑封装（工具类、集中规则）。 |
| Spring 中的作用 | AOP 是"运行时多态"：`@Transactional`/`@Log` 通过代理把横切逻辑织入方法；Spring 自身大量用接口（`BeanFactory`、`ApplicationContext`、`MessageSource`）提供扩展点；单例 Bean + 无状态设计依赖封装避免共享可变状态。 |

### 2.2 抽象类与接口

| 角度 | 内容 |
| --- | --- |
| 基础知识 | **抽象类**：不可实例化，可有成员字段和已实现方法，`abstract` 方法留给子类实现，单继承；**接口**：纯契约，方法默认 public abstract（Java 8+ 可有 default/static/private 方法带实现），字段只能是常量，可多实现；接口体现"做什么"，抽象类体现"是什么+默认怎么做"。 |
| 本系统如何使用 | 接口驱动：`FileService`/`UploadService`/`ShareService`/`JwtTokenUtil`/`StorageService` 等接口定义契约，`impl` 子包实现；**无自定义抽象类**，公共骨架用接口 + 组合（注入多个依赖）代替；框架抽象类被动继承：`OncePerRequestFilter`（模板：已过滤标记 + doFilterInternal 钩子）、`BaseTypeHandler`（类型转换骨架）。 |
| 本系统不使用需要怎么样 | 没有接口→Controller `new` 具体实现，无法 mock、无法切面、无法多实现，扩展点全死；抽象类用于"子类复用骨架 + 留钩子"，本项目场景用组合表达，若硬用继承会造成耦合。 |
| 使用情况分类 | ① 业务契约（接口定义做什么，实现怎么做的分离）② 框架模板骨架（继承框架抽象类、覆写钩子）③ 组合优于继承（复用行为 → 注入依赖而非继承父类）。 |
| Spring 中的作用 | 接口是 Bean 的"类型契约"，按接口注入可在运行时替换实现（配置切换、测试替身）；Template Method 模式是 Spring 内部常用的骨架复用方式（`JdbcTemplate`、`RestTemplate`）；`@Configuration` 类本身也可被代理增强。 |

### 2.3 内部类、静态内部类、匿名内部类

| 角度 | 内容 |
| --- | --- |
| 基础知识 | 内部类 = 定义在类内部的类，可以访问外部类私有成员（编译后带 this 引用）；**静态内部类** = 不隐式持有外部引用，可独立创建；**局部内部类** = 方法内定义；**匿名内部类** = 无类名、一次性实现接口/类（Java 8 后多用 lambda 替代）。 |
| 本系统如何使用 | 静态内部类：`DownloadServiceImpl.BatchTask`（纯数据载体，不隐式持有外部引用，避免内存泄漏，`DownloadServiceImpl.java:310`）。匿名内部类：几乎为零，被 lambda 取代——线程工厂 `runnable -> { ... }`（`DownloadServiceImpl.java:92`）、Security 配置的 `auth ->`/`cors ->` 链式 lambda、流式 API 的 lambda。 |
| 本系统不使用需要怎么样 | 状态载体用非静态内部类→隐式持有外部 Service 引用，即使任务结束也无法回收；没有 lambda→线程回调、配置、流式操作全写成匿名内部类样板代码。 |
| 使用情况分类 | ① 静态内部类（纯数据结构，推荐）② 非静态内部类（需要外部状态，本项目避免）③ lambda/方法引用替代匿名内部类（函数式回调、配置）。 |
| Spring 中的作用 | Spring 内部大量匿名内部类定义回调（如 `RowMapper`）；组件扫描默认不扫内部类（避免误注册）；lambda 让 `@Bean` 配置、`@EventListener`、线程池任务表达更简洁——`SecurityConfig` 的 `.csrf(AbstractHttpConfigurer::disable)` 就是方法引用。 |

### 2.4 equals、hashCode、toString 原理

| 角度 | 内容 |
| --- | --- |
| 基础知识 | `==` 比较引用地址，`equals` 比较逻辑内容（Object 默认就是 `==`，可重写）；`hashCode` 返回散列值用于 HashMap/HashSet 定位桶；**契约**：equals 相等的两个对象 hashCode 必须相等（反之不必）。重写 equals 必须同时重写 hashCode，否则放进散列容器会取不到；`toString` 默认"类名@hash"，重写用于日志/调试展示。 |
| 本系统如何使用 | Lombok `@Data`（`File.java:49`、`Result.java`）统一生成 equals/hashCode/toString；实体判等走**主键/唯一索引**而非 equals（同名文件靠 `findByUserIdAndParentIdAndName` 查库，`FileUtil.resolveUniqueName:135`）；去重靠 DB 唯一索引（`uk_user_parent_name`）与 Redis Set 成员判断（分片序号 `isMember`），业务代码几乎不依赖对象 equals。 |
| 本系统不使用需要怎么样 | 若把实体放 `HashSet`/做 `HashMap` 的 key，全字段 `@Data` 会让"从 DB 查出的半字段对象"与完整对象判等失败；误用可变字段做 hash 会破坏散列契约（改字段后 hash 变、再也取不到）。 |
| 使用情况分类 | ① 值对象/逻辑相等（全字段或业务字段 equals）② 实体（主键相等即可，本项目交给 DB）③ 集合去重场景（本项目用 DB 唯一索引/Redis 替代）④ 序列化显示（toString 用于日志）。 |
| Spring 中的作用 | Spring 配置类去重、Bean 定义比较内部依赖 equals/hashCode；缓存 key 构造（如 `@Cacheable` 的 key 生成）常要求对象正确实现 equals；但序列化框架（Jackson）不依赖 equals，实体进 Redis/JSON 靠字段映射。 |

---

## 三、Java 核心类库

### 3.1 java.lang / java.util / java.io / java.nio

| 角度 | 内容 |
| --- | --- |
| 基础知识 | java.lang 自动导入（Object/String/包装类/Math/System/异常）；java.util 提供集合、日期与工具；java.io 是阻塞字节/字符流；java.nio 引入 Buffer/Channel/Selector（非阻塞，是高并发网络如 Netty 的基础）。包装类有缓存池（Integer 等 -128~127），自动装箱/拆箱。 |
| 本系统如何使用 | java.lang：`String` 路径/文件名处理、`Long.parseLong`（分片元数据）、`Math` 分片计算、`System.currentTimeMillis`（打包任务超时，`DownloadServiceImpl:142`）、`HexFormat` SHA-256 转十六进制、`RuntimeException` 异常体系、`switch` 模式匹配（`AuthorizationPolicy.java:44`）。java.util：`List/Set/Map`、`Date`（JWT 签发，`JwtTokenUtilImpl:57`）、`Collections.enumeration`（`UploadServiceImpl:346`）、`Optional` 判空。java.io/nio：`InputStream`/`SequenceInputStream`/`DigestInputStream`/`ZipOutputStream`/`Files.deleteIfExists`（`DownloadServiceImpl:240`）。 |
| 本系统不使用需要怎么样 | 这是最底层依赖，缺了集合、时间、字符串、流全要自研；IO 抽象不统一，分片合并、zip 打包、哈希计算没法组合；时间不统一（`Date` vs `LocalDateTime`）会导致 JWT 与 MyBatis 类型转换出错。 |
| 使用情况分类 | ① 字符串/数字（命名、大小、ID）② 时间（JWT 过期、任务超时、日志清理）③ 集合容器（业务数据）④ 流式 IO（文件/网络/对象存储）⑤ 并发集合（`ConcurrentHashMap`）。 |
| Spring 中的作用 | Spring 整个建立在 JDK 类库之上：Bean 注册表本质是 `Map`；`Resource`/`StreamUtils` 统一资源访问；`java.time` 在 Spring Boot 3+ 全面取代 `Date`；Jackson/MyBatis 依赖反射与类型系统。 |

### 3.2 集合框架：List/Set/Map、HashMap/HashSet、ConcurrentHashMap

| 角度 | 内容 |
| --- | --- |
| 基础知识 | 两大体系：`Collection`（List 有序可重/Set 无序不可重/Queue 队列）与 `Map`（K-V）。`HashMap`：数组 + 链表 + 红黑树（桶数≥64 且链长≥8 转红黑树），key 靠 hashCode 定位、equals 比较，允许 null，扩容因子 0.75、初始容量 16，非线程安全；`HashSet` 内部就是 HashMap；`LinkedHashMap` 保插入序。`ConcurrentHashMap`：Java 8+ 对桶用 CAS + `synchronized`（只锁头节点），读不加锁，线程安全。 |
| 本系统如何使用 | `List.of` 不可变列表（权限集合 `LoginUser.java:58`、token 字符集 `ShareTokenGenerator.java:21`）；`Set.of` 扩展名白名单/分类（`FileUtil.java:34-48`）；`HashMap` 存上传元数据（`UploadServiceImpl:175`）；`ConcurrentHashMap` 存打包任务表（`DownloadServiceImpl.java:79`，打包线程写、查询线程读）；`stream().collect(groupingBy(File::getParentId))` 目录分组（`DownloadServiceImpl:274`）；`HashSet` 做 zip 条目名去重（`DownloadServiceImpl:215`）；`ArrayDeque` 做目录 BFS。 |
| 本系统不使用需要怎么样 | 打包任务表用 `HashMap`→并发写坏结构、遍历时修改死循环；分片"已上传"用 `ArrayList` 判断存在→O(n) 且无 TTL；可变集合做常量→被意外 add 破坏白名单。 |
| 使用情况分类 | ① 不可变集合（常量/白名单/权限，防修改）② 并发集合（多线程共享状态 → ConcurrentHashMap）③ 普通集合（请求内局部数据）④ 流式处理（过滤/分组/排序/收集）。 |
| Spring 中的作用 | Spring 容器本质是 Map（beanName→BeanDefinition/单例对象）；`@Configuration` 单例缓存用并发集合保证容器线程安全；Security 的权限集合用 `Set` 保证唯一；Bean 解析大量用 Stream 处理配置与依赖关系。 |

### 3.3 常用工具类：Arrays、Collections、Objects

| 角度 | 内容 |
| --- | --- |
| 基础知识 | **Arrays**（对数组：sort/binarySearch/copyOf/asList）、**Collections**（对集合：sort/shuffle/reverse/synchronizedXxx/unmodifiableXxx）、**Objects**（obj1.equals(null) 空安全比较、hash、requireNonNull）——都是对容器/对象高频操作的静态封装，同时把很多"该自己写"的样板集中起来。 |
| 本系统如何使用 | `Collections.enumeration(...)` 把分片流列表转 Enumeration 喂给 `SequenceInputStream`（`UploadServiceImpl:346`）；`Collections`/`Objects`/`Arrays` 直接用得少；大量**自建领域工具类**集中规则：`FileUtil`（扩展名/分类/大小格式化）、`IdUtil`（UUID/MinIO 路径）、`IpUtil`（代理头取 IP）、`AuthorizationPolicy`（权限判断）、`ShareTokenGenerator`（去混淆短码）。 |
| 本系统不使用需要怎么样 | 不集中工具类→同名命名、MIME 映射、IP 解析、对象路径规则散落各 Service，一处改三处漏；JDK 工具（`Objects.requireNonNull`、`Collections.emptyList`）能在判空、返回空集合时少写样板。 |
| 使用情况分类 | ① JDK 通用工具（判空/排序/包装，少而精）② 领域工具（集中业务规则：文件/IP/ID/权限）③ 静态工具 vs Bean：无状态纯函数用 static，有依赖规则用 Bean 注入。 |
| Spring 中的作用 | Spring 内部大量 `CollectionUtils`/`ObjectUtils`/`StringUtils` 做空安全与便捷操作；`ReflectionUtils` 封装反射调 Bean；组件扫描用类路径工具；写业务时"通用能力给 JDK，业务规则自建工具类"是与 Spring 一致的分工。 |

---

## 四、异常与日志

### 4.1 try-catch-finally

| 角度 | 内容 |
| --- | --- |
| 基础知识 | try 包裹可能抛异常的代码；catch 按异常类型匹配（选最具体的那个）；**finally 无论是否异常必定执行**（用于释放资源）；`try-with-resources` 对实现 `AutoCloseable` 的资源自动关闭（多资源逆序关闭）；注意 finally 中 return 会覆盖 try 的返回值，抛异常会覆盖返回值。 |
| 本系统如何使用 | `finally` 保证**资源/锁必定释放**：合并分布式锁 + 清理上传上下文（`UploadServiceImpl.merge:397-403`）、临时 zip 清理（`DownloadServiceImpl.pack:237-244`）；`try-with-resources` 保证流关闭（分片流、`ZipOutputStream`、`DigestInputStream`）；局部 catch 转业务语义：分片读取 `IOException`→`FILE_UPLOAD_FAILED`（`UploadServiceImpl:257`）、上传超限→`FILE_TOO_LARGE`、反序列化失败→400。 |
| 本系统不使用需要怎么样 | 锁不 finally 释放→并发 merge 永远拿不到锁，上传死锁；流不关→句柄耗尽、合并文件损坏；异常不分类→所有失败都返回 500，前端无法区分"参数错/超限/未登录"。 |
| 使用情况分类 | ① 资源释放（流/锁/临时文件 → finally 或 try-with-resources）② 业务失败分支（捕获后转成对应 ErrorCode）③ 外部边界兜底（catch RuntimeException 记录日志、回滚补偿）。 |
| Spring 中的作用 | `@Transactional` 由 AOP 织入 try-catch：方法抛运行时异常→`TransactionAspectSupport` 回滚；`JdbcTemplate` 内部自己 try-catch 关闭连接；Spring 也依赖 finally 释放锁/连接（Redis、数据源的归还）。 |

### 4.2 自定义异常

| 角度 | 内容 |
| --- | --- |
| 基础知识 | 继承 `Exception`（受检，强制处理）或 `RuntimeException`（不受检，业务异常常用）的普通类，通常带错误码 + 消息字段和多个构造器；作用 = 把底层异常/某个业务失败翻译成有明确语义的异常，调用方按类型或错误码统一处理，避免"层层 catch 又没意义"。 |
| 本系统如何使用 | `BusinessException extends RuntimeException`，携带 `ErrorCode`（`BusinessException.java:26`）；Service 层 `throw new BusinessException(ErrorCode.FILE_QUOTA_EXCEEDED)` 即可；`GlobalExceptionHandler` 用 `@ExceptionHandler` 按异常类型分派——业务异常/`@Valid` 校验失败/约束违规/反序列化失败/上传超限/兜底 500（`GlobalExceptionHandler.java:43-87`）。 |
| 本系统不使用需要怎么样 | 处处手动拼 `Result.fail`→Controller 与 Service 纠缠，错误码散落无法统一；不按类型分派→校验错误、业务错误、系统错误全混在一起；兜底异常直接暴露堆栈→生产信息泄漏。 |
| 使用情况分类 | ① 业务异常（带错误码，前端按 code 处理）② 参数校验异常（框架抛，统一提取消息）③ 客户端错误（反序列化/上传超限→400/明确提示）④ 兜底异常（未知错误记日志、不泄漏内部细节）。 |
| Spring 中的作用 | `@RestControllerAdvice` + `@ExceptionHandler` 是 Spring MVC 的异常分派中心，把"在哪抛、在哪处理"彻底分离；Spring 自身有完整异常体系（`DataAccessException`、`NoSuchBeanDefinitionException`）把底层异常翻译成统一语义——本系统的 `BusinessException` 就是这个思路的业务化。 |

### 4.3 日志框架：Log4j、SLF4J

| 角度 | 内容 |
| --- | --- |
| 基础知识 | 级别（依次提高）trace < debug < info < warn < error，只有高于阈值的才输出；门面 API（SLF4J/Commons Logging）统一接口，实现（Logback/Log4j2/JUL）可插拔替换；用占位符 `{}` 传参避免无用字符串拼接；Appender 决定输出目标（控制台/文件/DB），Layout 定格式，Filter 过滤。 |
| 本系统如何使用 | SLF4J + Logback（Spring Boot 默认实现），`LoggerFactory.getLogger(...)` 打点（`GlobalExceptionHandler:41`、各 Service）；`logback-spring.xml` 按 profile 分流：project/mybatis/spring/error/application 独立文件 + 滚动策略（100MB/30 天）；`@Log` 切面把**操作审计日志落库** `t_operation_log`（`LogAspect.java:50`）；`FileCleanupTask` 定时清理过期日志。 |
| 本系统不使用需要怎么样 | 无统一日志→上传失败、定时任务异常、慢 SQL 全无迹可查；不按级别/模块分流→生产排障大海捞针；审计不落库→管理员操作无法追溯；日志全打 System.out→无级别、无滚动、无结构化。 |
| 使用情况分类 | ① 运行时排障日志（debug/info/warn/error，按场景分级）② 业务审计日志（落库、可查可审计）③ 框架分层日志（MyBatis SQL、Spring 内部）④ 生产 vs 开发分流（减少 IO、隐藏敏感）。 |
| Spring 中的作用 | Spring Boot 用 SLF4J 作为统一门面，屏蔽 Log4j/Logback 差异；starter 自动配置 Logback；`logging.level.xxx` 可在运行时/配置调级别（如把 MyBatis 调到 DEBUG 看 SQL）；Spring 事件、启动日志都走这套体系。 |

---

## 五、多线程与并发

### 5.1 Thread 类与 Runnable 接口

| 角度 | 内容 |
| --- | --- |
| 基础知识 | 线程是轻量级进程，一个 JVM 多个线程共享堆/方法区、各占独立栈；`Thread` 是线程对象（可用 `new Thread()` 或匿名类），`Runnable` 是**任务抽象**（推荐，解耦线程与任务）；启动用 `start()`（内部调 run 开新线程），直接调 `run()` 只是普通方法；线程状态：new→runnable→blocked/waiting/timed-waiting→terminated；daemon 线程不阻止 JVM 退出；`Callable` + `Future` 可拿返回值。 |
| 本系统如何使用 | 只用线程池不用裸 `new Thread`：`packExecutor.execute(() -> pack(task))` 提交 Runnable（`DownloadServiceImpl:144`）；自定义 `ThreadFactory` 命名线程 `pack-task` 并设 daemon（`DownloadServiceImpl.java:92-96`）；`@Scheduled` 定时方法（`FileCleanupTask`）由 Spring 调度线程池执行。 |
| 本系统不使用需要怎么样 | 每次打包都 `new Thread`→线程无上限、不可复用、无法优雅关闭，高并发直接拖垮 JVM；不命名线程→排查线程 dump 分不清是哪个任务。 |
| 使用情况分类 | ① 后台任务（打包、清理 → 线程池提交 Runnable）② 回调（WebSocket 广播、事件监听）③ 定时（@Scheduled）④ 直接 new Thread（仅临时/极简场景，本系统避免）。 |
| Spring 中的作用 | `@Scheduled`/`@Async` 底层都是"线程池 + Runnable"；Spring 的 `TaskExecutor` 抽象统一线程模型，容器管理线程生命周期与关闭；线程与请求的绑定关系（一次请求一个线程）是理解 Filter→Controller→Service 调用链的前提。 |

### 5.2 synchronized、volatile

| 角度 | 内容 |
| --- | --- |
| 基础知识 | 线程安全三性：**原子性**（操作不可分割）、**可见性**（一线程修改对另一线程立即可见）、**有序性**（重排不破坏语义）。`synchronized` 是 JVM 内建锁（锁对象是 Monitor），同时保证互斥 + 可见性 + 有序性，可重入、非公平，有锁升级（无锁→偏向→轻量→重量）；`volatile` 只保证可见性与有序性（禁止指令重排），**不保证原子性**（i++ 这类读改写仍需加锁）。 |
| 本系统如何使用 | **未直接使用**；进程内并发靠 `ConcurrentHashMap`（打包任务表结构性安全，`DownloadServiceImpl:79`）+ 无状态 Bean；进程间互斥靠 **Redis 分布式锁**（`setIfAbsent`，`UploadServiceImpl.merge:315`）与 **DB 原子 SQL**（`UPDATE ... WHERE status=NORMAL`，`ShareMapper.incrementDownloadCountIfAllowed`）。 |
| 本系统不使用需要怎么样 | 打包任务表用普通 HashMap→并发丢任务；共享可变状态无保护→配额扣错、计数错乱；多实例部署时若只加 `synchronized`→锁只在本 JVM 内有效，跨实例并发仍穿透。 |
| 使用情况分类 | 四档递进：① 进程内互斥（synchronized/ReentrantLock）② 进程内可见性（volatile）③ 跨进程互斥（Redis/DB 锁）④ 无共享状态（不可变对象/无状态 Bean——最省心）。 | 
| Spring 中的作用 | Spring 单例 Bean 默认多线程共享，所以强制要求**无状态**；容器内部用锁保护元数据（如 `DefaultSingletonBeanRegistry`）；`@Transactional` 的隔离级别在 DB 层解决并发；跨节点场景交给分布式锁，这是单体→微服务演进时的核心区别。 |

### 5.3 ReentrantLock、Condition

| 角度 | 内容 |
| --- | --- |
| 基础知识 | JUC 提供的显式锁：`lock()`/`unlock()` 必须 **finally 释放**；可中断（`lockInterruptibly`）、可超时（`tryLock`、返回 boolean）、可公平（构造参数公平锁按等待时长排队）；`Condition` 与锁绑定，`await`/`signal`/`signalAll` 替代 `wait`/`notify`，一个锁可挂多个条件队列（如生产者、消费者各一个条件），支持精确定向唤醒。 |
| 本系统如何使用 | **未直接使用**；需要"锁"的场景全部落在 Redis：合并分片用 `setIfAbsent(lockKey, "1", 5min)` 拿分布式锁，`finally` 里 `redis.delete` 释放（`UploadServiceImpl.java:315/399`）。 |
| 本系统不使用需要怎么样 | 不锁住合并→两个请求同时 merge 同一个 uploadId：重复建文件记录、重复扣配额、对象互相覆盖；只用本地 `ReentrantLock`→多实例部署时各 JVM 一把锁，形同虚设。 |
| 使用情况分类 | ① 进程内可中断/公平锁（单机、带超时）② 分布式锁（多实例，本系统的落点：Redis setIfAbsent + TTL + finally 释放）③ 无锁/乐观（DB 条件 UPDATE、版本号）。 |
| Spring 中的作用 | 本地锁常用于 Spring 内部同步（单例创建、缓存加载）；`@Cacheable` 并发下可配 sync 用本地锁；但业务跨节点互斥要交给 Redis/Zookeeper——理解"本地锁 vs 分布式锁"的分界是并发面试高频点。 |

### 5.4 线程池（ExecutorService）

| 角度 | 内容 |
| --- | --- |
| 基础知识 | `ThreadPoolExecutor` 七个参数：corePoolSize（核心常驻）、maximumPoolSize（上限）、keepAliveTime（非核心空闲存活）、workQueue（任务队列）、threadFactory（线程名/是否 daemon）、handler（拒绝策略：AbortPolicy 抛异常 / CallerRunsPolicy 调用者执行 / DiscardPolicy / DiscardOldestPolicy）。执行顺序：核心线程 → 进队列 → 建非核心线程 → 拒绝。`Executors` 提供 newFixedThreadPool / newCachedThreadPool（无界不够可控）/ newSingleThreadExecutor / newScheduledThreadPool。 |
| 本系统如何使用 | `Executors.newFixedThreadPool(2, ...)`（`DownloadServiceImpl.java:92`）：打包任务异步执行、WebSocket 推送进度、完成后查状态/预签名 URL；任务状态存 `ConcurrentHashMap`（**进程内存态，重启丢**）；对照上传流程用 Redis 存状态（**可恢复**）——两种异步方案的对比落点（`UploadServiceImpl`）。 |
| 本系统不使用需要怎么样 | 同步打包→大文件包阻塞 HTTP 请求直到超时；用 `newCachedThreadPool` 打包→并发打包无限创建线程；不用守护线程/不关池→应用退出时打包线程卡住。 |
| 使用情况分类 | ① 固定大小池（限制并发，多余任务排队——打包 2 并发）② 单线程池/调度池（@Scheduled 定时）③ 自定义 ThreadFactory（命名、daemon）④ 任务队列（固定池内部 LinkedBlockingQueue 排队）。 |
| Spring 中的作用 | Spring 封装 `ThreadPoolTaskExecutor` 统一管理池的创建/初始化/销毁；`@Async`/`@Scheduled` 由池驱动，可配 `TaskDecorator` 传递请求上下文（TraceId/用户）；内存态 vs 分布式状态（Redis/MQ）是单体与微服务异步设计的分水岭。 |

### 5.5 并发工具类：CountDownLatch、Semaphore、CyclicBarrier

| 角度 | 内容 |
| --- | --- |
| 基础知识 | **CountDownLatch**：一次性计数门闩，主线程 `await` 等计数器 `countDown` 归零（不可复用）；**Semaphore**：信号量计数限流，`acquire`/`release` 控制同时访问数（可用来限池化资源）；**CyclicBarrier**：可复用屏障，N 个线程齐到 `await` 才放行，可带回调任务。对比：Latch 是"等 N 个事件"，Barrier 是"N 个线程互相等齐"。 |
| 本系统如何使用 | **未直接用 JDK 并发工具**，但语义被 Redis 实现：Semaphore ≈ 进行中上传任务数限流（`redis.opsForSet().size()` 计数 + 超限惰性清理，`UploadServiceImpl.checkConcurrentTasks:206`）；CountDownLatch/CyclicBarrier 无对应场景（打包是单线程异步）。 |
| 本系统不使用需要怎么样 | 不控并发任务数→一个用户疯狂并发上传拖垮服务；用本地 Semaphore→多实例下各算各的，限流失效；等待"分片齐了再合并"若用 CountDownLatch 在单机内存做→重启即失效。 |
| 使用情况分类 | ① 限流/信号量（Semaphore，本系统落点在 Redis Set 计数）② 等待子任务完成（CountDownLatch，如批量通知）③ 多线程屏障（CyclicBarrier，并行分片处理后汇合）——分布式场景三者都要换成 Redis/协调器。 |
| Spring 中的作用 | Spring 内部用 CountDownLatch 做容器启动门闩（等刷新完成再发布 ReadyEvent）；Spring Batch/异步批处理用并发工具分片同步；面试常问"单机工具怎么升级为分布式"——本项目 `upload:uploading:{userId}` 就是信号量的分布式版本。 |

### 5.6 原子类（AtomicInteger 等）

| 角度 | 内容 |
| --- | --- |
| 基础知识 | `AtomicInteger`/`AtomicLong`/`AtomicReference` 等基于 **CAS**（比较并交换）+ volatile + Unsafe，无锁、无阻塞：`incrementAndGet`/`getAndAdd`/`compareAndSet`；高并发统计可用 `LongAdder`（分段累加、空间换时间）；CAS 存在 ABA 问题，用 `AtomicStampedReference`（带版本号）解决。 |
| 本系统如何使用 | **未直接用 `java.util.concurrent.atomic`**；"原子性"由基础设施提供：DB 原子 SQL 扣配额 `userMapper.updateUsedSpace(userId, delta)`（`UserServiceImpl:373`，`used_space = used_space + #{delta}` 防并发覆盖）；Redis `increment()` 登录失败计数（`LoginAttemptService.java:54`，首次设 TTL）；Redis `setIfAbsent` 拿锁。 |
| 本系统不使用需要怎么样 | 配额增减"先查后改"（read→calc→update）→两个上传并发都基于旧值计算，覆盖丢失，配额被超扣/超发；失败计数不用原子 increment→并发错密码漏计数、锁定失效。 |
| 使用情况分类 | ① 进程内原子变量（AtomicInteger 等，单 JVM 计数）② 跨进程原子（Redis INCR、DB `SET column = column + n`）③ CAS 思想的分布式落地（条件 UPDATE、乐观锁版本号）。 |
| Spring 中的作用 | Spring 内部计数/缓存用原子类；MyBatis 乐观锁插件基于版本号 CAS；面试核心是"**把 JUC 原子类背后的 CAS 思想迁移到 Redis/DB 原子命令**"——本项目 `incrementDownloadCountIfAllowed`（`ShareMapper.java:49`）即"受影响行数判断"式 CAS。 |

### 5.7 常见并发面试题：死锁、线程安全、阻塞队列

| 角度 | 内容 |
| --- | --- |
| 基础知识 | **死锁四条件**：互斥、持有并等待、不可剥夺、循环等待——破坏任一即可避免（本系统靠锁超时 + finally 释放破除后两条）；**线程安全** = 多线程下行为正确，手段按优先级：无状态 > 不可变 > 并发容器 > 加锁；**阻塞队列**（ArrayBlockingQueue 有界 / LinkedBlockingQueue 可设界 / SynchronousQueue 直接交接）`put`/`take` 会阻塞，是生产者-消费者模型与线程池内部队列的基础。 |
| 本系统如何使用 | 死锁：分布式锁带 **5 分钟 TTL** + `finally` 释放（`UploadServiceImpl:315/399`），锁不会被永久占用。线程安全：打包任务表用 `ConcurrentHashMap`、Service 全部无状态、共享状态只经 Redis/DB。阻塞队列：未显式使用，固定线程池内部队列（LinkedBlockingQueue）自然排队打包任务；上传"并发任务超限"就是队列满语义（`UPLOAD_TASK_EXCEEDED`）。 |
| 本系统不使用需要怎么样 | 锁无超时/不 finally 释放→并发 merge 死锁，上传永久挂起；任务表用 HashMap→打包线程写、查询线程读数据错乱；无界任务堆积→内存 OOM（本项目固定池 2 并发即天然背压）。 |
| 使用情况分类 | ① 死锁三要素与规避（加锁顺序、锁超时、finally 释放）② 线程安全策略分级（无状态 > 不可变 > 并发容器 > 加锁）③ 阻塞队列语义（有界背压 vs 无界 OOM）④ 锁粒度（表锁 vs 行/记录级——Redis key 粒度即记录级）。 |
| Spring 中的作用 | Spring 单例 Bean 多线程共享的线程安全模型是必考；事务 + 锁的配合（先锁后查、锁外事务提交）；Spring 默认线程池队列无界是常见调优坑；"进程内锁 vs 分布式锁 vs 乐观锁"是单体演进微服务的面试主线。 |

---

## 六、JVM 原理基础

### 6.1 内存结构（方法区、堆、栈）

| 角度 | 内容 |
| --- | --- |
| 基础知识 | 运行时数据区按线程分：**堆**（所有线程共享，存对象实例，GC 主战场，分新生代/老年代）、**虚拟/本地方法栈**（每线程独立，栈帧含局部变量表、操作数栈，压栈出栈管理方法调用）、**方法区**（类元信息、常量、静态变量，Java 8 起改为本地内存的**元空间 Metaspace**，取代堆内永久代）、**程序计数器**（当前字节码行号）。栈满抛 StackOverflowError，堆满抛 OutOfMemoryError。 |
| 本系统如何使用 | 堆：单例 Bean、Entity、DTO、Redis/DB 数据对象。栈：每个 HTTP 请求一个线程，Filter→Controller→Service→Mapper 调用链帧与局部变量（分片流、临时集合、`BatchTask` 引用）；方法调用层级如 `pack()→writeZipEntry()→transferTo()`。方法区/元空间：类定义（212 个类）、常量池（`ErrorCode` 枚举、`FileUtil` 的 `Set.of` 常量）、Lombok 生成的字节码。 |
| 本系统不使用需要怎么样 | 不理解"大文件不进堆"→把 zip 全部读进内存导致 OOM；递归打包目录树过深→栈溢出；不知道常量池→枚举/常量每次 new 一个对象，浪费堆。 |
| 使用情况分类 | ① 堆（对象分配、GC 主战场）② 栈（每请求一线程、调用帧、局部变量）③ 方法区/元空间（类定义、常量、代理类）④ 直接内存/堆外（MinIO/Netty 缓冲区，本项目对象存储不占堆）。 |
| Spring 中的作用 | Spring 单例 Bean 常驻堆、请求对象短暂存活（年轻代）；Bean 定义、代理类（CGLIB）占元空间；理解"容器启动加载了什么进内存"才能解释启动慢、内存占用与 JVM 调优。 |

### 6.2 GC 原理

| 角度 | 内容 |
| --- | --- |
| 基础知识 | 判定对象可回收：**可达性分析**（从 GC Roots——栈中局部变量、静态变量、常量、JNI 引用出发，不可达即回收；引用计数法有循环引用缺陷）；回收算法：标记-清除（有碎片）/复制（新生代用，空间换时间）/标记-整理（老年代用）；分代假设：对象朝生夕灭——新生代 Minor GC 多、老年代 Major/Full GC 少；G1 是 JDK 11+ 默认（Region 分区、可预测停顿），后续有 ZGC/Shenandoah 超低延迟。 |
| 本系统如何使用 | 对象绝大多数是**短命对象**（每个请求的 DTO/Entity/分片流）→年轻代频繁 Minor GC；长命数据放外部存储：上传元数据/验证码/锁定计数放 Redis（带 TTL 自过期）、大文件放 MinIO、打包产物是对象存储里的 zip 而非堆对象；未配置 JVM 参数（默认 G1，Java 21）。 |
| 本系统不使用需要怎么样 | 把打包产物、上传分片全装进内存集合→老年代迅速膨胀、Full GC 卡顿甚至 OOM；无 TTL 策略→Redis/内存中的过期状态永不清，垃圾堆积。 |
| 使用情况分类 | ① 对象分配与存活（短命→年轻代；Bean/缓存→老年代）② 大对象与外部存储（大文件不进堆 → 对象存储直链）③ 缓存策略（堆内 Caffeine vs 堆外 Redis）④ GC 参数（本系统默认 G1，未调参）。 |
| Spring 中的作用 | Spring Bean 生命周期长→常驻老年代；请求级对象年轻代→高吞吐场景的 GC 压力主要在 Minor GC；缓存框架（Caffeine/Ehcache）在堆内，容量设计要结合堆大小；Spring 无状态设计让"对象用完即弃"成为 GC 友好的前提。 |

### 6.3 类加载机制

| 角度 | 内容 |
| --- | --- |
| 基础知识 | 生命周期五步：加载（读字节码生成 Class 对象）→ 验证 → 准备（静态变量赋默认值）→ 解析（符号引用转直接引用）→ 初始化（执行 static 块/静态赋初值）；触发初始化：new、访问静态成员、反射、main 等。**双亲委派**：App → Platform/Extension → Bootstrap 自底向上委托，父加载器能加载就由父加载，保证核心类（如 java.lang.String）不被篡改且全局唯一；需要自己加载 SPI/插件时才打破委派。 |
| 本系统如何使用 | Spring Boot fat jar 启动：Bootstrap→Platform→App 三层双亲委派 + Spring Boot `Launcher` 自定义加载器（从 jar 内嵌 BOOT-INF 加载）；`@SpringBootApplication` 组件扫描 = 类加载 + 注解处理；运行时动态生成类：MyBatis mapper 代理、`@ConfigurationProperties` 绑定、CGLIB 代理（`@Transactional`/`@Log` 生效原理）。 |
| 本系统不使用需要怎么样 | 不理解双亲委派→无法排查依赖冲突、SPI 失效、驱动重复加载；不理解代理类生成→无法解释"`this.xxx()` 本类自调用为什么 `@Transactional`/`@Log` 不生效"（没走代理对象）。 |
| 使用情况分类 | ① 双亲委派（默认机制、防类重复）② 破坏委派（SPI：JDBC Driver、Spring Boot Launcher、MyBatis 类型处理）③ 动态生成类（CGLIB 代理、注解处理器）④ 热部署（devtools 类加载器隔离，本项目未用）。 |
| Spring 中的作用 | Spring 容器是"类加载器之上的对象工厂"：组件扫描（`ClassPathScanningCandidateComponentProvider`）加载候选类→反射实例化→代理增强；Bean 定义首次访问才加载；理解类加载才能理解 `@Transactional` 代理失效、父子容器、插件化扩展等经典问题。 |

### 6.4 常见性能调优概念

| 角度 | 内容 |
| --- | --- |
| 基础知识 | 调优目标是吞吐量/响应时间/资源消耗三者平衡；按层排查：**CPU**（计算热点、锁竞争）、**内存**（堆大小、GC 频率、内存泄漏）、**IO**（慢 SQL、磁盘、网络）、**线程**（池参数、上下文切换）；手段：JVM 参数 `-Xms`/`-Xmx`/`-XX` 调堆与 GC、连接池/线程池参数、加索引与缓存、异步化削峰；用监控工具（jstack/jmap/jstat/JMC/JFR、GC 日志）先定位再调优，不要盲目加参。 |
| 本系统如何使用 | SQL 层：索引设计（对照 `EXPLAIN` 实验，联合索引 `(user_id, parent_id, status)`）；缓存层：Redis 扛热点——验证码 TTL/登录失败计数/上传元数据/分享去重（`RedisConstants`）；IO 层：**流式处理不整装**——zip 逐条目 `transferTo`（`DownloadServiceImpl:252`）、分片流 `SequenceInputStream` 边传边算哈希、预签名 URL 让文件流不经过后端；并发层：固定线程池限制打包并发（2）、Redis Set 限流上传任务数；连接池：DB/Redis 默认连接池。 |
| 本系统不使用需要怎么样 | 查询不走索引→全表扫描拖垮 DB；大文件/大 zip 整装内存→OOM；文件经后端转发→带宽与内存双压力；打包同步执行→请求阻塞；并发上传不限制→单用户打满服务。 |
| 使用情况分类 | ① 数据库层（索引/EXPLAIN/连接池/原子 SQL）② 缓存层（Redis 命中率、TTL、去重）③ IO 层（流式、预签名直链、对象存储）④ 并发层（线程池参数、限流、背压）。 |
| Spring 中的作用 | Spring Boot Actuator 提供监控指标（线程/堆/请求）；`@Transactional(readOnly=true)` 优化只读事务；缓存抽象 `@Cacheable` 统一缓存接入；连接池由 starter 自动配置（HikariCP）；调优常从"Spring 管理的资源"入手：连接数、线程池、缓存、日志级别。 |

---

## 附：建议复习路径

按"一条业务链路覆盖多个基础点"的方式走，避免孤立背语法：

1. **登录链路**（覆盖：异常/自定义异常、反射+注解、IO/网络、Redis 原子计数、JWT/时间）——`AuthController.login` → `UserDetailsServiceImpl` → `JwtTokenUtilImpl` → `LoginAttemptService` → `JwtAuthenticationFilter`
2. **分片上传链路**（覆盖：集合框架、流式 IO/装饰流、哈希、分布式锁、并发限流、枚举/泛型）——`UploadServiceImpl.init` → `uploadChunk` → `merge`
3. **打包下载链路**（覆盖：线程池/Runnable、ConcurrentHashMap、内部类、try-with-resources、IO）——`DownloadServiceImpl.createBatchTask` → `pack`
4. **JVM 体检**：`jps`/`jstack` 看打包线程、`jmap -histo` 看堆对象、GC 日志理解 Minor/Full GC

关联文档：`docs/notes/cloud-learning-guide.md`（操作实验）、`docs/HLD.md`（架构）、`docs/DATABASE.md`（索引与 SQL）。
