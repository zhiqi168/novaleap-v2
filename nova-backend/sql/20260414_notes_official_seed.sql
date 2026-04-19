SET NAMES utf8mb4;
USE novaleap;

-- 上线前正式手记初始化数据
-- 说明：
-- 1. 本文件写入 20 条已审核通过的正式手记
-- 2. 通过 title 去重，重复执行不会重复插入
-- 3. 作为官方内容导入，user_id 为空，status=1，deleted=0

INSERT INTO notes (
    title, content, summary, category, emoji, author, user_id, view_count, status, deleted,
    reject_reason, audit_source, audited_at, created_at, updated_at
)
SELECT
    'Spring 事务为什么会失效：排查时先看代理，再看异常',
    '很多事务失效问题并不是数据库出了故障，而是方法没有经过 Spring 代理，或者异常在业务层被提前吞掉了。

排查时建议先确认调用入口是不是容器管理的 Bean，再确认目标方法是否满足事务增强条件，最后检查异常传播链路是否真的触发回滚规则。

真正稳定的排查方式不是猜测配置，而是按“代理、方法、异常、提交结果”这条链路逐层定位。',
    '事务失效最常见的问题不在数据库，而在代理链路和异常传播链路。',
    '后端实践',
    '📘',
    'Nova 编辑部',
    NULL,
    328,
    1,
    0,
    NULL,
    'MANUAL',
    '2026-04-10 10:00:00',
    '2026-04-10 09:30:00',
    '2026-04-10 10:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM notes WHERE title = 'Spring 事务为什么会失效：排查时先看代理，再看异常');

INSERT INTO notes (
    title, content, summary, category, emoji, author, user_id, view_count, status, deleted,
    reject_reason, audit_source, audited_at, created_at, updated_at
)
SELECT
    'Redis 缓存设计的三条底线：别让缓存比数据库更难维护',
    '缓存不是加上就一定快，很多系统真正的问题是失效策略、回源策略和降级方案没有提前想清楚。

设计缓存时，至少要回答三个问题：什么时候失效，失效后如何回源，缓存不可用时系统如何降级。只有这三个问题清楚了，缓存才是性能工具，而不是复杂度放大器。

缓存命中率很重要，但一致性边界和异常处理能力更重要。',
    '缓存设计不能只盯命中率，更要先想清楚失效、回源和降级。',
    '后端实践',
    '🧰',
    'Nova 编辑部',
    NULL,
    285,
    1,
    0,
    NULL,
    'MANUAL',
    '2026-04-10 14:20:00',
    '2026-04-10 14:00:00',
    '2026-04-10 14:20:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM notes WHERE title = 'Redis 缓存设计的三条底线：别让缓存比数据库更难维护');

INSERT INTO notes (
    title, content, summary, category, emoji, author, user_id, view_count, status, deleted,
    reject_reason, audit_source, audited_at, created_at, updated_at
)
SELECT
    'MySQL 索引优化不是多建索引，而是减少无效访问',
    '索引优化的重点从来不是“索引数量够不够多”，而是核心查询是否走在正确的访问路径上。

排查时要重点看 SQL 是否命中预期索引、是否发生回表、排序和分页是否抵消了索引收益，以及联合索引是否真的符合最左匹配规则。

真正有效的索引优化，本质上是在减少不必要的数据扫描和回表成本。',
    '索引优化的核心不是堆索引，而是让核心查询减少无效访问。',
    '后端实践',
    '🗂',
    'Nova 编辑部',
    NULL,
    246,
    1,
    0,
    NULL,
    'MANUAL',
    '2026-04-11 09:40:00',
    '2026-04-11 09:10:00',
    '2026-04-11 09:40:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM notes WHERE title = 'MySQL 索引优化不是多建索引，而是减少无效访问');

INSERT INTO notes (
    title, content, summary, category, emoji, author, user_id, view_count, status, deleted,
    reject_reason, audit_source, audited_at, created_at, updated_at
)
SELECT
    '接口幂等的四种常见方案，我更推荐先从业务唯一键开始',
    '支付回调、重复提交、消息重复消费，本质上都在考验系统是否具备幂等能力。

常见做法包括前端防重复提交、服务端幂等令牌、业务唯一键约束和状态机控制。相比单纯在代码里堆判断，业务唯一键更贴近真实业务事实，也更稳定。

如果模型天然具备“这件事只能成功一次”的表达能力，很多幂等问题会简单很多。',
    '幂等治理最稳的落点通常不是多写 if，而是给业务事实加上唯一约束。',
    '工程实践',
    '🪝',
    'Nova 编辑部',
    NULL,
    221,
    1,
    0,
    NULL,
    'MANUAL',
    '2026-04-11 11:30:00',
    '2026-04-11 11:00:00',
    '2026-04-11 11:30:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM notes WHERE title = '接口幂等的四种常见方案，我更推荐先从业务唯一键开始');

INSERT INTO notes (
    title, content, summary, category, emoji, author, user_id, view_count, status, deleted,
    reject_reason, audit_source, audited_at, created_at, updated_at
)
SELECT
    '从一次慢 SQL 排查里学到的事：先缩小范围，再谈优化',
    '慢 SQL 排查最怕一上来就改索引、改语句、改参数，却没有先确认问题属于哪一类。

更高效的方式是先拿到真实 SQL 和发生时段，再判断是偶发还是稳定复现，然后通过执行计划确认问题落在扫描、排序、回表还是锁等待，最后再决定优化动作。

先定位，再优化，才不会在错误方向上越改越乱。',
    '慢 SQL 优化前先判断问题落点，否则方向错了，动作越多越容易失真。',
    '工程实践',
    '📊',
    'Nova 编辑部',
    NULL,
    194,
    1,
    0,
    NULL,
    'MANUAL',
    '2026-04-11 16:10:00',
    '2026-04-11 15:30:00',
    '2026-04-11 16:10:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM notes WHERE title = '从一次慢 SQL 排查里学到的事：先缩小范围，再谈优化');

INSERT INTO notes (
    title, content, summary, category, emoji, author, user_id, view_count, status, deleted,
    reject_reason, audit_source, audited_at, created_at, updated_at
)
SELECT
    '简历改版时如何避免技术债扩散：先统一表达，再统一格式',
    '很多简历问题看起来像“内容不够”，其实更常见的是表达结构不统一，导致亮点无法稳定呈现。

改简历时，建议先统一每段项目经历的表达框架，例如背景、目标、动作、结果，再去处理标题、时间、技术栈和格式细节。

先有统一结构，再做局部润色，才能避免越改越乱。',
    '简历优化的第一步不是堆内容，而是先统一表达结构。',
    '职业成长',
    '📝',
    'Nova 编辑部',
    NULL,
    167,
    1,
    0,
    NULL,
    'MANUAL',
    '2026-04-12 09:20:00',
    '2026-04-12 08:50:00',
    '2026-04-12 09:20:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM notes WHERE title = '简历改版时如何避免技术债扩散：先统一表达，再统一格式');

INSERT INTO notes (
    title, content, summary, category, emoji, author, user_id, view_count, status, deleted,
    reject_reason, audit_source, audited_at, created_at, updated_at
)
SELECT
    '面试中回答项目经历的结构化模板：背景、动作、权衡、结果',
    '很多项目经历不是没有内容，而是回答时没有稳定结构，导致重点被细节打散。

更适合面试的表达方式是先交代业务背景和目标，再说明自己负责的动作，然后补充关键设计取舍，最后给出结果和复盘。

有结构的表达，不只会让内容更清晰，也能帮助面试官快速判断你的边界和深度。',
    '项目经历回答最怕散，结构化模板能显著提升表达稳定性。',
    '面试复盘',
    '🎯',
    'Nova 编辑部',
    NULL,
    312,
    1,
    0,
    NULL,
    'MANUAL',
    '2026-04-12 10:40:00',
    '2026-04-12 10:00:00',
    '2026-04-12 10:40:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM notes WHERE title = '面试中回答项目经历的结构化模板：背景、动作、权衡、结果');

INSERT INTO notes (
    title, content, summary, category, emoji, author, user_id, view_count, status, deleted,
    reject_reason, audit_source, audited_at, created_at, updated_at
)
SELECT
    '如何把八股文变成自己的表达：先理解场景，再压缩语言',
    '八股文最大的问题不是知识点本身，而是很多人只记住了答案，没有建立“它在什么场景里有用”的连接。

更好的训练方式是先为知识点找到真实场景，再把答案压缩成两三句话，最后补一个可以展开的细节。这样在面试现场更容易自然表达，而不是机械背诵。

能把知识点讲成自己的语言，才算真正完成吸收。',
    '八股文真正要练的不是记忆，而是把场景和表达连接起来。',
    '学习方法',
    '🧠',
    'Nova 编辑部',
    NULL,
    276,
    1,
    0,
    NULL,
    'MANUAL',
    '2026-04-12 13:10:00',
    '2026-04-12 12:40:00',
    '2026-04-12 13:10:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM notes WHERE title = '如何把八股文变成自己的表达：先理解场景，再压缩语言');

INSERT INTO notes (
    title, content, summary, category, emoji, author, user_id, view_count, status, deleted,
    reject_reason, audit_source, audited_at, created_at, updated_at
)
SELECT
    '学习新技术时我的三层拆解法：概念、链路、边界',
    '很多技术资料看了不少，却仍然记不住，往往是因为只停留在概念层，没有建立执行链路和边界条件。

更稳的学习方式是把新知识拆成三层：它是什么，它怎么运行，它在什么情况下失效或不适合。这样既能帮助记忆，也更适合后续复盘和面试表达。

真正可迁移的理解，一定包含边界感。',
    '学技术不只记概念，更要建立链路和边界，才有可迁移性。',
    '学习方法',
    '🧩',
    'Nova 编辑部',
    NULL,
    201,
    1,
    0,
    NULL,
    'MANUAL',
    '2026-04-12 15:20:00',
    '2026-04-12 14:50:00',
    '2026-04-12 15:20:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM notes WHERE title = '学习新技术时我的三层拆解法：概念、链路、边界');

INSERT INTO notes (
    title, content, summary, category, emoji, author, user_id, view_count, status, deleted,
    reject_reason, audit_source, audited_at, created_at, updated_at
)
SELECT
    '后端接口设计里最容易忽略的边界：权限、幂等、可观测性',
    '接口能跑通，不等于设计已经完整。很多线上问题并不是出在 happy path，而是权限边界、重复提交和排障能力没有提前设计。

设计接口时，至少要同步确认谁能调用、重复调用会发生什么、出了问题能否快速定位。这些信息和参数定义一样重要。

成熟的接口设计，从来不是只有请求和响应。',
    '接口设计真正的成熟度，在于是否补齐权限、幂等和可观测性边界。',
    '工程实践',
    '🔍',
    'Nova 编辑部',
    NULL,
    183,
    1,
    0,
    NULL,
    'MANUAL',
    '2026-04-12 18:00:00',
    '2026-04-12 17:20:00',
    '2026-04-12 18:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM notes WHERE title = '后端接口设计里最容易忽略的边界：权限、幂等、可观测性');

INSERT INTO notes (
    title, content, summary, category, emoji, author, user_id, view_count, status, deleted,
    reject_reason, audit_source, audited_at, created_at, updated_at
)
SELECT
    '从日志、指标、链路三层做故障排查，比盯单个报错更有效',
    '看到一条报错就立刻修改，是最常见也最容易误判的排障方式。

更可靠的做法是同时看三层信息：日志告诉你哪里异常，指标告诉你影响范围，链路告诉你卡在上下游的哪个位置。三层结合之后，定位速度和准确率都会明显提升。

如果一次故障只能靠经验记忆解决，往往说明可观测性建设还不够。',
    '排障不能只盯一条报错，把日志、指标和链路结合起来才更稳。',
    '工程实践',
    '📡',
    'Nova 编辑部',
    NULL,
    238,
    1,
    0,
    NULL,
    'MANUAL',
    '2026-04-13 09:00:00',
    '2026-04-13 08:20:00',
    '2026-04-13 09:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM notes WHERE title = '从日志、指标、链路三层做故障排查，比盯单个报错更有效');

INSERT INTO notes (
    title, content, summary, category, emoji, author, user_id, view_count, status, deleted,
    reject_reason, audit_source, audited_at, created_at, updated_at
)
SELECT
    '为什么团队需要代码评审清单：稳定质量靠流程，不靠临场发挥',
    '好的代码评审不是谁挑谁的问题，而是团队一起降低线上风险的过程。

如果没有评审清单，关注点会很随机。今天看命名，明天看空指针，后天看权限，质量就很难稳定。至少应该覆盖业务逻辑、权限风险、异常路径、测试补充和兼容性影响。

把个人经验沉淀成团队流程，比一次评审多挑几个问题更有价值。',
    '代码评审真正的价值，是把经验沉淀成稳定可复用的质量流程。',
    '工程实践',
    '✅',
    'Nova 编辑部',
    NULL,
    156,
    1,
    0,
    NULL,
    'MANUAL',
    '2026-04-13 10:20:00',
    '2026-04-13 09:50:00',
    '2026-04-13 10:20:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM notes WHERE title = '为什么团队需要代码评审清单：稳定质量靠流程，不靠临场发挥');

INSERT INTO notes (
    title, content, summary, category, emoji, author, user_id, view_count, status, deleted,
    reject_reason, audit_source, audited_at, created_at, updated_at
)
SELECT
    '做题之后为什么一定要写复盘：知识只有被整理过才算自己的',
    '很多人把“做完一道题”当成“掌握了一类题”，但如果没有复盘，几天后往往只剩下模糊印象。

复盘至少要回答四个问题：这道题考什么，自己卡在哪，正确解法的关键转折点是什么，下次再遇到相似题如何更快判断。

题库负责输入，复盘负责沉淀。没有沉淀的输入，很难形成长期优势。',
    '做题后的复盘不是附加动作，而是把输入沉淀成判断框架的关键步骤。',
    '学习方法',
    '✍️',
    'Nova 编辑部',
    NULL,
    267,
    1,
    0,
    NULL,
    'MANUAL',
    '2026-04-13 11:40:00',
    '2026-04-13 11:10:00',
    '2026-04-13 11:40:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM notes WHERE title = '做题之后为什么一定要写复盘：知识只有被整理过才算自己的');

INSERT INTO notes (
    title, content, summary, category, emoji, author, user_id, view_count, status, deleted,
    reject_reason, audit_source, audited_at, created_at, updated_at
)
SELECT
    '系统设计题如何在十分钟内搭骨架：入口、存储、异步、风控',
    '系统设计题最容易越讲越散，根本原因通常不是不会，而是没有先搭结构。

一个更稳的骨架是先讲入口层，包括鉴权、限流和接入，再讲存储模型和一致性策略，然后补充异步削峰和通知链路，最后补上审计、监控和降级方案。

先搭骨架，再展开细节，表达会稳定很多。',
    '系统设计题先讲骨架，再补细节，表达和思路都会清晰很多。',
    '面试复盘',
    '🏗',
    'Nova 编辑部',
    NULL,
    341,
    1,
    0,
    NULL,
    'MANUAL',
    '2026-04-13 13:20:00',
    '2026-04-13 12:40:00',
    '2026-04-13 13:20:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM notes WHERE title = '系统设计题如何在十分钟内搭骨架：入口、存储、异步、风控');

INSERT INTO notes (
    title, content, summary, category, emoji, author, user_id, view_count, status, deleted,
    reject_reason, audit_source, audited_at, created_at, updated_at
)
SELECT
    'JVM 调优之前先回答五个问题，不要一上来就改参数',
    '很多 JVM 调优没有效果，不是因为参数不够多，而是问题定义不清楚。

在动参数之前，至少要先回答：当前症状是什么，触发场景是什么，目标是吞吐优先还是停顿优先，GC 日志和监控是否完整，是否存在明显的对象生命周期问题。

先定义问题，再定义目标，最后才是参数调整。',
    'JVM 调优真正的起点不是参数表，而是问题定义和目标澄清。',
    '后端实践',
    '☕',
    'Nova 编辑部',
    NULL,
    229,
    1,
    0,
    NULL,
    'MANUAL',
    '2026-04-13 15:00:00',
    '2026-04-13 14:20:00',
    '2026-04-13 15:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM notes WHERE title = 'JVM 调优之前先回答五个问题，不要一上来就改参数');

INSERT INTO notes (
    title, content, summary, category, emoji, author, user_id, view_count, status, deleted,
    reject_reason, audit_source, audited_at, created_at, updated_at
)
SELECT
    '如何给自己做一次可落地的周复盘：只盯目标，不要堆待办',
    '很多周复盘最后写成流水账，记录了做过什么，却没回答哪些动作真正有效。

更有用的复盘方式是只保留四块内容：本周最重要的结果、带来结果的动作、投入高但收益低的动作、下周只保留的关键动作。

复盘的价值不是制造更多待办，而是帮助自己收敛节奏。',
    '高质量周复盘的重点是识别有效动作，而不是继续堆积待办。',
    '职业成长',
    '🗓',
    'Nova 编辑部',
    NULL,
    142,
    1,
    0,
    NULL,
    'MANUAL',
    '2026-04-13 16:20:00',
    '2026-04-13 15:50:00',
    '2026-04-13 16:20:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM notes WHERE title = '如何给自己做一次可落地的周复盘：只盯目标，不要堆待办');

INSERT INTO notes (
    title, content, summary, category, emoji, author, user_id, view_count, status, deleted,
    reject_reason, audit_source, audited_at, created_at, updated_at
)
SELECT
    '从需求评审开始减少返工：先确认边界，再确认异常流',
    '很多返工并不是开发阶段才产生的，而是在需求评审时就埋下了隐患。

评审时如果只谈主流程，不谈边界和异常，开发、测试和产品对最终结果的理解就很容易分叉。更稳的做法是把权限不足、重复提交、回滚策略、审核失败和兼容旧数据这些问题提前问清楚。

很多返工，最后追根到底都是前期边界没有谈明白。',
    '减少返工最有效的阶段往往不是开发中，而是需求评审时先把边界和异常流问清楚。',
    '工程实践',
    '📎',
    'Nova 编辑部',
    NULL,
    176,
    1,
    0,
    NULL,
    'MANUAL',
    '2026-04-13 18:10:00',
    '2026-04-13 17:30:00',
    '2026-04-13 18:10:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM notes WHERE title = '从需求评审开始减少返工：先确认边界，再确认异常流');

INSERT INTO notes (
    title, content, summary, category, emoji, author, user_id, view_count, status, deleted,
    reject_reason, audit_source, audited_at, created_at, updated_at
)
SELECT
    '线上事故复盘不要只写原因，更要写识别信号和预防动作',
    '事故复盘如果只写“发生了什么”和“根因是什么”，往往还不够，因为经验很难真正复用。

一份更完整的复盘，应该补上最早可识别的信号、当时为什么没有提前发现、后续如何建立告警或阻断动作，以及责任归属和完成时限。

复盘的目标不是追责，而是让同类问题更早被发现、更容易被挡住。',
    '事故复盘要补齐识别信号和预防动作，经验才能真正复用。',
    '工程实践',
    '🚨',
    'Nova 编辑部',
    NULL,
    187,
    1,
    0,
    NULL,
    'MANUAL',
    '2026-04-14 09:10:00',
    '2026-04-14 08:30:00',
    '2026-04-14 09:10:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM notes WHERE title = '线上事故复盘不要只写原因，更要写识别信号和预防动作');

INSERT INTO notes (
    title, content, summary, category, emoji, author, user_id, view_count, status, deleted,
    reject_reason, audit_source, audited_at, created_at, updated_at
)
SELECT
    '个人知识库为什么要坚持统一格式：检索效率比记录热情更重要',
    '知识库最怕的不是记得少，而是记得很多却找不到、用不上。

统一标题结构、标签规则、摘要写法和复盘模板，能显著降低后续检索成本。长期来看，检索效率远比一时的记录热情更重要。

能被快速找到并再次使用的内容，才算真正进入了你的知识系统。',
    '知识库长期可用的关键，是统一格式和检索效率，而不是短期记录冲动。',
    '学习方法',
    '📚',
    'Nova 编辑部',
    NULL,
    158,
    1,
    0,
    NULL,
    'MANUAL',
    '2026-04-14 09:50:00',
    '2026-04-14 09:20:00',
    '2026-04-14 09:50:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM notes WHERE title = '个人知识库为什么要坚持统一格式：检索效率比记录热情更重要');

INSERT INTO notes (
    title, content, summary, category, emoji, author, user_id, view_count, status, deleted,
    reject_reason, audit_source, audited_at, created_at, updated_at
)
SELECT
    '写给准备秋招的自己：节奏比刷量更重要',
    '秋招准备最容易陷入的误区，是把每天做了多少题、看了多少内容，误当成了真正进步。

更重要的是建立稳定节奏：固定输入、固定复盘、固定查漏补缺。只要节奏稳定，积累会自然产生；如果节奏失控，再大的刷量也很难沉淀。

准备期最怕的不是慢，而是乱。',
    '秋招准备不是比谁刷得多，而是比谁能维持稳定而可持续的成长节奏。',
    '职业成长',
    '🌱',
    'Nova 编辑部',
    NULL,
    198,
    1,
    0,
    NULL,
    'MANUAL',
    '2026-04-14 10:50:00',
    '2026-04-14 10:10:00',
    '2026-04-14 10:50:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM notes WHERE title = '写给准备秋招的自己：节奏比刷量更重要');
