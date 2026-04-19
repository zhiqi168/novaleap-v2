SET NAMES utf8mb4;
USE novaleap;

-- JavaGuide 官方题库初始化 SQL
-- 适用表：questions
-- 设计原则：
-- 1. 仅插入 source_type=OFFICIAL 的首批公开题库
-- 2. 通过标题去重，重复执行不会重复灌库
-- 3. 分类码严格对齐当前项目 question_categories：java/spring/db/redis/network/linux
--
-- 参考来源（公开页面）：
-- https://javaguide.cn/home.html
-- https://javaguide.cn/interview-preparation/backend-interview-plan.html
-- https://javaguide.cn/cs-basics/network/other-network-questions.html

INSERT INTO questions (
    title, content, standard_answer, difficulty, category, tags, view_count, status, source_type, custom_bank_id, owner_user_id, created_at
)
SELECT
    'Java 为什么只有值传递？',
    '请解释 Java 为什么只有值传递，并分别说明基本类型参数和引用类型参数在方法调用中的表现。',
    'Java 方法调用时传入的永远是实参副本，因此本质上只有值传递。基本类型传递的是具体数值的副本，方法内修改不会影响外部变量；引用类型传递的是引用地址的副本，因此方法内可以通过这个副本修改对象内部状态，但如果把引用重新指向新对象，不会影响外部引用本身。',
    2, 'java', 'JavaGuide官方题库,JavaGuide,java,基础', 0, 1, 'OFFICIAL', NULL, NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE title = 'Java 为什么只有值传递？');

INSERT INTO questions (
    title, content, standard_answer, difficulty, category, tags, view_count, status, source_type, custom_bank_id, owner_user_id, created_at
)
SELECT
    '== 和 equals() 有什么区别？',
    '请从基本类型、引用类型以及 String 等常见场景出发，说明 == 和 equals() 的区别。',
    '对于基本类型，== 比较的是值；对于引用类型，== 比较的是两个引用是否指向同一个对象。equals() 是 Object 的实例方法，默认实现与 == 一样比较地址，但很多类例如 String、Integer、BigDecimal 都会重写 equals()，用于比较对象的业务语义是否相等。因此面试里要强调：是否比较内容，取决于该类是否正确重写了 equals()。',
    1, 'java', 'JavaGuide官方题库,JavaGuide,java,基础', 0, 1, 'OFFICIAL', NULL, NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE title = '== 和 equals() 有什么区别？');

INSERT INTO questions (
    title, content, standard_answer, difficulty, category, tags, view_count, status, source_type, custom_bank_id, owner_user_id, created_at
)
SELECT
    'String、StringBuilder、StringBuffer 的区别是什么？',
    '请说明 String、StringBuilder、StringBuffer 的可变性、线程安全性和适用场景。',
    'String 不可变，适合读多写少和常量语义场景；每次拼接通常会生成新对象。StringBuilder 可变但线程不安全，单线程下字符串频繁拼接优先使用它，性能最好。StringBuffer 也是可变的，但方法大多带同步，线程安全而性能相对低一些。总结就是：默认优先 StringBuilder，多线程共享并且确实需要同步时才考虑 StringBuffer。',
    1, 'java', 'JavaGuide官方题库,JavaGuide,java,字符串', 0, 1, 'OFFICIAL', NULL, NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE title = 'String、StringBuilder、StringBuffer 的区别是什么？');

INSERT INTO questions (
    title, content, standard_answer, difficulty, category, tags, view_count, status, source_type, custom_bank_id, owner_user_id, created_at
)
SELECT
    '深拷贝和浅拷贝有什么区别？',
    '请说明深拷贝和浅拷贝的定义、行为差异，以及在包含引用字段的对象中会出现什么问题。',
    '浅拷贝只复制对象本身以及第一层字段值，如果字段里有引用类型，复制后新旧对象仍共享同一份内部对象；深拷贝会把引用链上的对象也一起复制，新旧对象之间互不影响。浅拷贝实现简单、开销小，但容易出现联动修改；深拷贝隔离性更好，但实现更复杂、成本更高。实际开发中要结合对象结构和一致性要求来选。',
    2, 'java', 'JavaGuide官方题库,JavaGuide,java,对象拷贝', 0, 1, 'OFFICIAL', NULL, NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE title = '深拷贝和浅拷贝有什么区别？');

INSERT INTO questions (
    title, content, standard_answer, difficulty, category, tags, view_count, status, source_type, custom_bank_id, owner_user_id, created_at
)
SELECT
    'HashMap 和 ConcurrentHashMap 有什么区别？',
    '请从线程安全、实现思路和使用场景三个角度说明 HashMap 与 ConcurrentHashMap 的区别。',
    'HashMap 线程不安全，适合单线程或外部已做并发控制的场景；并发环境下可能出现数据覆盖、读取异常等问题。ConcurrentHashMap 是并发容器，JDK8 以后以数组 + 链表/红黑树为基础，通过 CAS、synchronized 和更细粒度的并发控制提升线程安全和性能。面试回答时不要只说一个安全一个不安全，还要补充它是为了在高并发下兼顾吞吐量和可见性。',
    2, 'java', 'JavaGuide官方题库,JavaGuide,java,集合,并发', 0, 1, 'OFFICIAL', NULL, NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE title = 'HashMap 和 ConcurrentHashMap 有什么区别？');

INSERT INTO questions (
    title, content, standard_answer, difficulty, category, tags, view_count, status, source_type, custom_bank_id, owner_user_id, created_at
)
SELECT
    '什么是 IoC 和 AOP？',
    '请解释 Spring 中 IoC 和 AOP 的核心思想，并说明它们分别解决什么问题。',
    'IoC 指控制反转，本质是对象创建和依赖关系维护不再由业务代码手动 new，而是交给 Spring 容器统一管理，这样能降低耦合、方便扩展和测试。AOP 指面向切面编程，把日志、事务、权限、监控这类横切关注点从核心业务中抽离出来，通过代理在方法执行前后统一织入。IoC 解决的是对象管理问题，AOP 解决的是通用逻辑复用问题。',
    1, 'spring', 'JavaGuide官方题库,JavaGuide,spring,IOC,AOP', 0, 1, 'OFFICIAL', NULL, NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE title = '什么是 IoC 和 AOP？');

INSERT INTO questions (
    title, content, standard_answer, difficulty, category, tags, view_count, status, source_type, custom_bank_id, owner_user_id, created_at
)
SELECT
    'Spring Bean 的作用域有哪些？',
    '请列举 Spring Bean 常见作用域，并说明 singleton、prototype、request、session 的差异。',
    'singleton 是默认作用域，整个 Spring 容器通常只有一个实例，适合无状态服务；prototype 每次获取都会创建新对象，适合有状态且不希望共享的场景；request 作用域在一次 HTTP 请求内复用同一个 Bean；session 作用域在同一个会话内复用。回答时最好再补一句：大多数业务 Bean 推荐保持无状态并使用 singleton，便于复用和并发处理。',
    1, 'spring', 'JavaGuide官方题库,JavaGuide,spring,bean', 0, 1, 'OFFICIAL', NULL, NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE title = 'Spring Bean 的作用域有哪些？');

INSERT INTO questions (
    title, content, standard_answer, difficulty, category, tags, view_count, status, source_type, custom_bank_id, owner_user_id, created_at
)
SELECT
    'Spring 事务什么时候会失效？',
    '请说明 Spring 声明式事务常见失效场景，并给出排查思路。',
    '常见失效场景包括：同类内部自调用导致代理失效、方法不是 public、异常被吞掉或异常类型不满足回滚规则、数据库引擎不支持事务、手动创建对象绕过 Spring 容器等。排查事务问题时要先确认是否真正走到代理，再看传播行为、回滚规则、异常处理方式和底层数据源配置。面试里最好强调：Spring 事务本质依赖代理机制。',
    2, 'spring', 'JavaGuide官方题库,JavaGuide,spring,事务', 0, 1, 'OFFICIAL', NULL, NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE title = 'Spring 事务什么时候会失效？');

INSERT INTO questions (
    title, content, standard_answer, difficulty, category, tags, view_count, status, source_type, custom_bank_id, owner_user_id, created_at
)
SELECT
    'Spring Boot 自动配置原理是什么？',
    '请说明 Spring Boot 自动配置的核心流程，以及它为什么能做到开箱即用。',
    'Spring Boot 的自动配置核心在于按条件装配。启动类上的相关注解会触发自动配置加载，框架读取 classpath、配置文件和环境信息，再根据 @Conditional 系列条件决定哪些 Bean 需要注入。starter 负责把一组常见依赖和默认配置打包好，自动配置类负责在满足条件时创建默认 Bean，所以开发者只需要少量配置就能快速跑起来，同时又可以通过自定义 Bean 覆盖默认行为。',
    2, 'spring', 'JavaGuide官方题库,JavaGuide,spring,spring-boot,自动配置', 0, 1, 'OFFICIAL', NULL, NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE title = 'Spring Boot 自动配置原理是什么？');

INSERT INTO questions (
    title, content, standard_answer, difficulty, category, tags, view_count, status, source_type, custom_bank_id, owner_user_id, created_at
)
SELECT
    'CHAR 和 VARCHAR 有什么区别？',
    '请说明 MySQL 中 CHAR 与 VARCHAR 的存储特点、性能差异和适用场景。',
    'CHAR 是定长字符串，长度不足会补齐，适合长度固定且更新较少的字段，例如国家码、性别标记；VARCHAR 是变长字符串，会按实际内容存储并额外保存长度信息，适合昵称、标题、地址这类长度变化较大的字段。CHAR 在某些固定长度场景读取更直接，但会浪费空间；VARCHAR 更节省存储，不过行记录变长后可能带来页分裂等问题。',
    1, 'db', 'JavaGuide官方题库,JavaGuide,mysql,字段类型', 0, 1, 'OFFICIAL', NULL, NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE title = 'CHAR 和 VARCHAR 有什么区别？');

INSERT INTO questions (
    title, content, standard_answer, difficulty, category, tags, view_count, status, source_type, custom_bank_id, owner_user_id, created_at
)
SELECT
    '什么是回表、覆盖索引和最左前缀匹配？',
    '请结合 InnoDB 索引结构，解释回表、覆盖索引、最左前缀匹配分别是什么意思。',
    '回表是指先通过二级索引查到主键，再根据主键回聚簇索引取完整数据；覆盖索引是查询所需字段都在索引里，直接从索引返回结果，不需要回表；最左前缀匹配是联合索引生效时通常要从最左列开始连续匹配。优化 SQL 时如果能让查询尽量命中覆盖索引、减少回表次数，性能通常会更好。',
    2, 'db', 'JavaGuide官方题库,JavaGuide,mysql,索引', 0, 1, 'OFFICIAL', NULL, NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE title = '什么是回表、覆盖索引和最左前缀匹配？');

INSERT INTO questions (
    title, content, standard_answer, difficulty, category, tags, view_count, status, source_type, custom_bank_id, owner_user_id, created_at
)
SELECT
    '为什么 InnoDB 推荐使用自增主键？',
    '请说明 InnoDB 为什么通常推荐使用自增主键，而不是随机主键或业务主键。',
    'InnoDB 的主键索引是聚簇索引，数据行本身就按主键顺序存放。使用自增主键时，新纪录通常只需要追加到索引末尾，页分裂更少，插入局部性更好，维护成本更低。若使用随机 UUID 这类离散主键，容易导致频繁页分裂、缓存命中率下降和空间碎片增加。当然，如果业务上已有稳定且短小的天然主键，也可以结合实际权衡。',
    2, 'db', 'JavaGuide官方题库,JavaGuide,mysql,主键,InnoDB', 0, 1, 'OFFICIAL', NULL, NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE title = '为什么 InnoDB 推荐使用自增主键？');

INSERT INTO questions (
    title, content, standard_answer, difficulty, category, tags, view_count, status, source_type, custom_bank_id, owner_user_id, created_at
)
SELECT
    'MySQL 事务隔离级别有哪些？',
    '请列出 MySQL 常见事务隔离级别，并说明它们分别主要解决了哪些并发读写问题。',
    '常见隔离级别有读未提交、读已提交、可重复读、串行化。读未提交可能出现脏读、不可重复读和幻读；读已提交解决了脏读，但仍可能有不可重复读和幻读；可重复读在 InnoDB 中结合 MVCC 与间隙锁，通常能较好地解决不可重复读并尽量控制幻读；串行化隔离最强，但并发性能最差。MySQL InnoDB 默认是可重复读。',
    2, 'db', 'JavaGuide官方题库,JavaGuide,mysql,事务', 0, 1, 'OFFICIAL', NULL, NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE title = 'MySQL 事务隔离级别有哪些？');

INSERT INTO questions (
    title, content, standard_answer, difficulty, category, tags, view_count, status, source_type, custom_bank_id, owner_user_id, created_at
)
SELECT
    'MySQL 中的 redo log、undo log、binlog 分别有什么作用？',
    '请从崩溃恢复、一致性、回滚与主从复制等角度说明 redo log、undo log、binlog 的作用。',
    'redo log 是 InnoDB 的重做日志，主要用于崩溃恢复，保证已经提交的数据不会因为宕机丢失；undo log 记录修改前的数据版本，用于事务回滚以及 MVCC 的一致性读；binlog 是 MySQL Server 层的归档日志，主要用于主从复制和数据恢复。面试里通常要强调三者分层不同、用途不同，但又会在事务提交链路里互相配合。',
    3, 'db', 'JavaGuide官方题库,JavaGuide,mysql,日志', 0, 1, 'OFFICIAL', NULL, NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE title = 'MySQL 中的 redo log、undo log、binlog 分别有什么作用？');

INSERT INTO questions (
    title, content, standard_answer, difficulty, category, tags, view_count, status, source_type, custom_bank_id, owner_user_id, created_at
)
SELECT
    'Redis 为什么这么快？',
    '请从数据结构、线程模型和 IO 模型等方面说明 Redis 高性能的原因。',
    'Redis 快的核心原因包括：大部分数据在内存中操作，避免了磁盘随机 IO；核心命令执行路径短，底层数据结构针对高频场景做了优化；单线程命令执行模型减少了线程切换和锁竞争的成本；同时配合高效的网络 IO 多路复用处理大量连接。回答时不要只说单线程快，真正关键是内存操作 + 简洁模型 + 合理的数据结构设计。',
    1, 'redis', 'JavaGuide官方题库,JavaGuide,redis,基础', 0, 1, 'OFFICIAL', NULL, NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE title = 'Redis 为什么这么快？');

INSERT INTO questions (
    title, content, standard_answer, difficulty, category, tags, view_count, status, source_type, custom_bank_id, owner_user_id, created_at
)
SELECT
    'Redis 的过期删除策略和内存淘汰策略是什么？',
    '请说明 Redis 过期键删除策略与内存淘汰策略的区别，并列举常见策略。',
    '过期删除解决的是已经设置 TTL 的 key 何时清理，Redis 会结合惰性删除和定期删除来平衡实时性与性能；内存淘汰解决的是内存达到上限后如何腾空间，常见策略包括 noeviction、allkeys-lru、volatile-lru、allkeys-lfu、allkeys-random 等。两者关注点不同，一个是处理过期数据，一个是处理内存不足问题。',
    2, 'redis', 'JavaGuide官方题库,JavaGuide,redis,过期,淘汰', 0, 1, 'OFFICIAL', NULL, NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE title = 'Redis 的过期删除策略和内存淘汰策略是什么？');

INSERT INTO questions (
    title, content, standard_answer, difficulty, category, tags, view_count, status, source_type, custom_bank_id, owner_user_id, created_at
)
SELECT
    '缓存穿透、缓存击穿、缓存雪崩分别是什么？',
    '请分别解释缓存穿透、缓存击穿、缓存雪崩的含义，并说明常见治理方案。',
    '缓存穿透是请求的数据既不在缓存也不在数据库，导致大量请求直接打到数据库，常见方案有布隆过滤器、缓存空对象和参数校验；缓存击穿是某个热点 key 在失效瞬间有大量并发请求同时回源，常见方案有互斥锁、逻辑过期、热点永不过期；缓存雪崩是大量 key 同时失效或缓存整体不可用，常见方案有过期时间加随机值、多级缓存、限流降级和快速故障转移。',
    2, 'redis', 'JavaGuide官方题库,JavaGuide,redis,缓存问题', 0, 1, 'OFFICIAL', NULL, NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE title = '缓存穿透、缓存击穿、缓存雪崩分别是什么？');

INSERT INTO questions (
    title, content, standard_answer, difficulty, category, tags, view_count, status, source_type, custom_bank_id, owner_user_id, created_at
)
SELECT
    'Redis 持久化机制 RDB 和 AOF 有什么区别？',
    '请说明 RDB 和 AOF 的工作方式、优缺点，以及生产上如何做取舍。',
    'RDB 是在某个时间点把内存快照持久化到磁盘，恢复速度快、文件紧凑，但可能丢失最近一次快照之后的数据；AOF 通过追加写命令记录数据变更，数据更完整，可通过重写控制文件体积，但恢复通常比 RDB 慢。生产中经常是两者结合使用：既保留 RDB 作为快速恢复基础，又利用 AOF 提升数据安全性。',
    2, 'redis', 'JavaGuide官方题库,JavaGuide,redis,持久化', 0, 1, 'OFFICIAL', NULL, NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE title = 'Redis 持久化机制 RDB 和 AOF 有什么区别？');

INSERT INTO questions (
    title, content, standard_answer, difficulty, category, tags, view_count, status, source_type, custom_bank_id, owner_user_id, created_at
)
SELECT
    'OSI 七层模型和 TCP/IP 四层模型有什么关系？',
    '请说明 OSI 七层模型与 TCP/IP 四层模型的层次划分，以及常见协议分别位于哪一层。',
    'OSI 七层模型更偏理论，自上而下是应用层、表示层、会话层、传输层、网络层、数据链路层、物理层；TCP/IP 模型更贴近实际实现，通常分为应用层、传输层、网络层、网络接口层。HTTP、DNS 属于应用层，TCP、UDP 属于传输层，IP 属于网络层。回答这题重点不是死背层数，而是说明每层负责什么、常见协议处于哪一层。',
    1, 'network', 'JavaGuide官方题库,JavaGuide,network,分层模型', 0, 1, 'OFFICIAL', NULL, NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE title = 'OSI 七层模型和 TCP/IP 四层模型有什么关系？');

INSERT INTO questions (
    title, content, standard_answer, difficulty, category, tags, view_count, status, source_type, custom_bank_id, owner_user_id, created_at
)
SELECT
    'HTTP 和 HTTPS 有什么区别？',
    '请从安全性、端口、证书和性能角度比较 HTTP 与 HTTPS。',
    'HTTP 明文传输，默认端口 80，本身不提供加密、身份校验和完整性保护；HTTPS 本质是在 HTTP 与 TCP 之间加入 TLS，默认端口 443，通过证书、加密和消息校验来提升传输安全。HTTPS 会多出握手和加解密成本，但现代硬件和协议优化已经显著降低了性能损耗，生产系统通常默认优先 HTTPS。',
    1, 'network', 'JavaGuide官方题库,JavaGuide,network,http,https', 0, 1, 'OFFICIAL', NULL, NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE title = 'HTTP 和 HTTPS 有什么区别？');

INSERT INTO questions (
    title, content, standard_answer, difficulty, category, tags, view_count, status, source_type, custom_bank_id, owner_user_id, created_at
)
SELECT
    'TCP 为什么要三次握手和四次挥手？',
    '请分别说明 TCP 三次握手和四次挥手的目的，为什么不是两次握手或三次挥手。',
    '三次握手的目标是让双方都确认自己和对方的收发能力正常，并同步初始序列号，避免历史连接请求误建立连接；两次握手无法让服务端确认客户端是否收到自己的确认。四次挥手是因为 TCP 是全双工通信，双方关闭发送通道通常需要分别发送 FIN 和 ACK，所以断开连接时往往是两端各自独立完成关闭流程。面试里如果能顺带讲到 TIME_WAIT 会更完整。',
    2, 'network', 'JavaGuide官方题库,JavaGuide,network,tcp', 0, 1, 'OFFICIAL', NULL, NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE title = 'TCP 为什么要三次握手和四次挥手？');

INSERT INTO questions (
    title, content, standard_answer, difficulty, category, tags, view_count, status, source_type, custom_bank_id, owner_user_id, created_at
)
SELECT
    'Cookie、Session、Token 有什么区别？',
    '请说明 Cookie、Session、Token 各自负责什么，以及它们在登录认证中的典型配合方式。',
    'Cookie 是浏览器端的小型存储载体，通常用于保存会话标识或其他少量信息；Session 是服务端会话数据，常见做法是把 sessionId 放在 Cookie 中，服务端根据 sessionId 查会话；Token 通常是服务端签发给客户端的认证凭证，例如 JWT，客户端后续请求主动携带它。三者不是完全互斥关系，Cookie 更像载体，Session 和 Token 更偏认证方案中的状态表达方式。',
    2, 'network', 'JavaGuide官方题库,JavaGuide,network,认证,http', 0, 1, 'OFFICIAL', NULL, NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE title = 'Cookie、Session、Token 有什么区别？');

INSERT INTO questions (
    title, content, standard_answer, difficulty, category, tags, view_count, status, source_type, custom_bank_id, owner_user_id, created_at
)
SELECT
    '进程和线程有什么区别？',
    '请从资源分配、调度切换、通信方式和崩溃影响范围等角度说明进程与线程的区别。',
    '进程是资源分配的基本单位，拥有独立地址空间；线程是 CPU 调度的基本单位，同一进程内的线程共享堆和方法区等资源。进程间隔离性更强，但创建和切换成本更高；线程切换更轻量、通信更方便，但一个线程写错内存更容易影响整个进程。简单说，进程更重、更安全，线程更轻、更高效。',
    1, 'linux', 'JavaGuide官方题库,JavaGuide,linux,操作系统,进程线程', 0, 1, 'OFFICIAL', NULL, NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE title = '进程和线程有什么区别？');

INSERT INTO questions (
    title, content, standard_answer, difficulty, category, tags, view_count, status, source_type, custom_bank_id, owner_user_id, created_at
)
SELECT
    '什么是用户态和内核态？为什么要有系统调用？',
    '请解释用户态和内核态的职责划分，以及应用程序为什么不能直接执行所有敏感操作。',
    '用户态是应用程序运行的受限模式，权限较低；内核态拥有更高权限，可以直接访问硬件和执行关键系统管理操作。之所以需要系统调用，是为了让应用程序在受控前提下请求操作系统完成文件读写、网络通信、进程管理等敏感操作。这样的隔离设计有助于提升系统稳定性和安全性，避免普通程序直接破坏整个系统。',
    2, 'linux', 'JavaGuide官方题库,JavaGuide,linux,操作系统,内核', 0, 1, 'OFFICIAL', NULL, NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE title = '什么是用户态和内核态？为什么要有系统调用？');

INSERT INTO questions (
    title, content, standard_answer, difficulty, category, tags, view_count, status, source_type, custom_bank_id, owner_user_id, created_at
)
SELECT
    'Linux 线上排查高 CPU 或端口占用时常用哪些命令？',
    '请给出线上排查高 CPU、内存异常、端口占用、日志定位时常用的 Linux 命令和思路。',
    '常见命令包括 top 或 htop 看整体资源占用，ps -ef 或 ps aux 配合 grep 找进程，free -h 看内存，df -h 看磁盘，netstat -tunlp 或 ss -tunlp 看端口占用，lsof -i 查看端口对应进程，tail -f 和 grep 用于实时看日志。面试回答最好体现排查顺序：先看系统负载，再定位进程，再看线程、端口和日志，最后结合应用监控交叉验证。',
    1, 'linux', 'JavaGuide官方题库,JavaGuide,linux,排查,命令', 0, 1, 'OFFICIAL', NULL, NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE title = 'Linux 线上排查高 CPU 或端口占用时常用哪些命令？');

INSERT INTO questions (
    title, content, standard_answer, difficulty, category, tags, view_count, status, source_type, custom_bank_id, owner_user_id, created_at
)
SELECT
    '死锁的必要条件有哪些？如何避免死锁？',
    '请说明死锁的四个必要条件，并给出常见的死锁预防和规避思路。',
    '死锁通常满足四个必要条件：互斥、请求并保持、不可剥夺、循环等待。避免死锁的思路就是尽量破坏其中一个条件，例如统一资源加锁顺序来破坏循环等待，一次性申请所需资源来减少请求并保持，引入超时机制或死锁检测来及时恢复。面试里如果能结合数据库锁或 Java 多线程加锁场景举例，会更有说服力。',
    2, 'linux', 'JavaGuide官方题库,JavaGuide,linux,操作系统,死锁', 0, 1, 'OFFICIAL', NULL, NULL, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM questions WHERE title = '死锁的必要条件有哪些？如何避免死锁？');
