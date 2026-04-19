<template>
  <div class="min-h-screen bg-bg-base px-4 py-8 sm:px-6 lg:px-8">
    <div class="mx-auto w-full max-w-5xl rounded-3xl border border-border-subtle bg-bg-surface p-7 shadow-float sm:p-9">
      <div class="max-w-3xl">
        <h1 class="text-3xl font-bold text-text-primary">隐私政策</h1>
        <p class="mt-3 text-sm leading-7 text-text-secondary">
          我们非常重视你的个人信息与隐私保护。本政策用于说明 NovaLeap 知跃在你使用账号体系、学习工具、社区互动和 AI 功能过程中，如何收集、使用、保存、共享和保护你的信息，以及你可以如何行使相关权利。
        </p>
        <div class="mt-4 flex flex-wrap gap-x-6 gap-y-2 text-xs text-text-secondary">
          <span>生效日期：2026年4月19日</span>
          <span>最近更新：2026年4月19日</span>
          <span>联系邮箱：zhiqi@novaleap.xyz</span>
        </div>
      </div>

      <div class="mt-8 space-y-8">
        <section
          v-for="section in sections"
          :key="section.title"
          class="rounded-2xl border border-border-subtle/70 bg-bg-base/40 p-5 sm:p-6"
        >
          <h2 class="text-lg font-semibold text-text-primary">{{ section.title }}</h2>

          <p
            v-for="paragraph in section.paragraphs"
            :key="paragraph"
            class="mt-3 text-sm leading-7 text-text-secondary"
          >
            {{ paragraph }}
          </p>

          <ul v-if="section.items?.length" class="mt-4 space-y-3">
            <li
              v-for="(item, index) in section.items"
              :key="item"
              class="flex gap-3 text-sm leading-7 text-text-secondary"
            >
              <span class="font-semibold text-text-primary">{{ index + 1 }}.</span>
              <span>{{ item }}</span>
            </li>
          </ul>
        </section>
      </div>

      <div class="mt-8 rounded-2xl border border-border-subtle bg-bg-base p-4 text-xs leading-6 text-text-secondary">
        特别说明：本政策基于 NovaLeap 当前代码中已实现的数据流程撰写。根据仓库现状，我们已确认平台存在邮箱验证码、游客模式、JWT 登录、访问统计、内容互动、Cloudflare Turnstile 风控、Resend 邮件发送及 OpenAI 兼容 AI 服务调用等数据处理场景。若你的正式部署地域、运营主体或第三方服务商发生变化，应同步更新本政策并评估是否需要补充单独同意、个人信息出境或未成年人保护等专项机制。
      </div>

      <div class="mt-8 flex justify-end">
        <router-link to="/login" class="font-semibold text-text-primary hover:underline">
          返回登录
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
const sections = [
  {
    title: '一、适用范围',
    paragraphs: [
      '本隐私政策适用于你通过 novaleap.xyz 及其相关页面访问和使用 NovaLeap 知跃提供的注册登录、邮箱验证码、游客体验、题库、笔记、愿望墙、排行榜、个人资料、AI 陪练、简历分析、访问统计等服务的情形。',
      '如本政策与具体功能页面另行向你说明的数据处理规则不一致，以该页面单独说明为准；如无单独说明，则以本政策为准。'
    ],
  },
  {
    title: '二、我们可能收集的信息',
    items: [
      '账号与身份信息：你注册、登录或找回登录能力时提交或产生的邮箱账号、密码摘要、验证码校验记录、昵称、头像、角色类型、登录状态信息等。',
      '使用与安全信息：访问路径、访问时间、操作记录、登录 IP、设备或浏览器信息、visitorId、风控校验信息、日志信息以及用于识别异常访问、刷量、攻击和滥用行为的安全数据。',
      '内容与互动信息：你主动提交的笔记、评论、愿望内容、自定义题库文本、简历文本、AI 对话消息、图片、个人资料信息以及相应的点赞、浏览、评论和排行数据。',
      '联系与反馈信息：当你通过联系邮箱、意见反馈或申诉渠道联系我们时，你主动提供的姓名、邮箱、说明材料和沟通记录。'
    ],
  },
  {
    title: '三、我们如何使用这些信息',
    items: [
      '用于完成账号注册、登录鉴权、验证码校验、游客身份生成、个人资料展示与账号安全管理。',
      '用于向你发送验证邮件、重要服务通知、异常登录提醒或与你所发起操作直接相关的信息。',
      '用于提供题库、笔记、愿望墙、排行榜、个人主页与其他社区互动功能，并对内容进行审核、展示、统计和运营维护。',
      '用于提供 AI 陪练、简历分析、题目讲解、笔记摘要、愿望审核等智能化功能，并在必要范围内将你输入的文本或图片发送至相应模型服务处理。',
      '用于统计访问量、识别访客来源、优化页面性能、改进产品体验、开展故障排查、安全审计和满足法律法规要求。'
    ],
  },
  {
    title: '四、公开展示与社区可见性',
    paragraphs: [
      '当你发布笔记、评论、愿望墙内容，或在排行榜、个人资料、互动数据中参与展示时，你的昵称、头像、部分公开内容、发布时间、互动计数及与该内容相关的必要信息可能被其他用户看到。',
      '请你谨慎填写和发布内容，避免主动公开身份证号、住址、联系电话、详细简历原件、银行卡、医疗记录、商业秘密或他人的个人信息。'
    ],
  },
  {
    title: '五、浏览器存储与登录状态',
    items: [
      '为维持你的登录状态、页面跳转体验与访客识别能力，我们可能在浏览器本地存储中保存必要的登录态信息、用户概要信息或 visitorId。',
      '你可以通过清理浏览器缓存、本地存储或主动退出登录来删除相关本地数据，但这可能影响自动登录、游客身份延续或部分页面功能。'
    ],
  },
  {
    title: '六、信息共享、委托处理与第三方服务',
    paragraphs: [
      '我们不会出售你的个人信息。仅在实现具体功能、履行法定义务或保护合法权益所必需的范围内，我们才会与第三方服务商共享必要信息。',
      '根据当前代码实现，我们会使用以下类型的第三方服务：'
    ],
    items: [
      'Cloudflare Turnstile：用于人机验证、风险识别与防刷防滥用，可能处理你的 IP、设备环境和校验结果等必要信息。',
      'Resend：用于发送邮箱验证码或服务邮件，可能处理你的邮箱地址、发信内容和必要的发送日志。',
      'OpenAI 兼容 AI 服务供应商：用于 AI 陪练、简历分析、题目讲解、笔记摘要和部分内容审核，可能处理你主动提交的文本、图片及上下文片段。',
      '法律法规、司法机关、行政机关或监管部门依法要求提供时，我们可能在法定范围内披露必要信息。',
      '如未来发生经营合并、分立、资产转让或其他类似交易，涉及个人信息转移的，我们将依法履行通知或其他必要程序。'
    ],
  },
  {
    title: '七、信息保存期限',
    items: [
      '我们会在实现本政策所述目的所必需的最短期限内保存你的个人信息；保存期限届满后，我们会根据适用法律法规要求进行删除或匿名化处理。',
      '为满足交易留痕、内容安全、争议处理、审计合规或监管要求，部分日志、风控记录、内容审核记录或备份信息可能在必要期间内继续保留。',
      '游客模式下的数据保留能力与正式账号不同，我们不承诺游客数据具有长期、完整、可迁移的保存效果。'
    ],
  },
  {
    title: '八、我们如何保护你的信息',
    items: [
      '我们采取与现有技术条件相适应的合理安全措施保护你的信息，包括访问控制、接口鉴权、限流与风控、日志审计、最小必要授权等。',
      '根据当前后端实现，账号密码不会以明文保存，而是以不可逆加密摘要方式存储与校验。',
      '尽管如此，互联网并非绝对安全的环境。请你妥善保管账号、验证码和终端设备，避免在不可信网络环境下泄露个人信息。'
    ],
  },
  {
    title: '九、你的权利',
    items: [
      '你有权访问、查阅、更新和更正你主动提交的部分资料，例如昵称、头像等已开放的信息。',
      '在符合法律法规规定的前提下，你有权申请删除相关个人信息、撤回部分授权、获取对本政策的解释说明，或就个人信息处理事宜进行投诉和反馈。',
      '如你需要行使前述权利，可发送邮件至 zhiqi@novaleap.xyz 与我们联系。为保护你的账号和信息安全，我们可能要求你提供必要的身份核验材料。'
    ],
  },
  {
    title: '十、未成年人保护',
    paragraphs: [
      'NovaLeap 当前面向计算机专业在校生与应届生等学习者，不以十四周岁以下未成年人为主要服务对象。',
      '如你是未成年人，请在监护人同意和指导下使用本服务；如监护人发现未成年人未经同意向我们提供了个人信息，请及时通过联系邮箱通知我们，我们将在核实后尽快处理。'
    ],
  },
  {
    title: '十一、政策更新与联系我们',
    paragraphs: [
      '我们可能根据法律法规变化、产品功能调整、第三方服务变化或运营需要对本政策进行更新。更新后的版本将在相关页面展示，并自页面载明日期起生效。',
      '如你对本政策或个人信息保护相关事宜有任何疑问、意见、建议、投诉或举报，可通过 zhiqi@novaleap.xyz 与我们联系。'
    ],
  },
]
</script>
