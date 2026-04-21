<template>
  <div class="relative w-screen h-[100dvh] overflow-hidden flex items-center justify-center font-sans tracking-wide" :class="loginThemeClass">
    
    <!-- 背景炫彩装饰块，提供柔和的色彩层次感 -->
    <div v-if="showMediaBackground" class="absolute inset-0 z-0 overflow-hidden" :class="loginBackgroundClass">
      <video
        :src="customBackgroundVideo"
        class="h-full w-full object-cover object-center select-none"
        :class="loginBackgroundVideoClass"
        autoplay
        muted
        loop
        playsinline
        preload="metadata"
        disablepictureinpicture
      ></video>

      <!-- Legacy image / overlay background branch retained for rollback reference only.
      <img
        :src="customBackgroundImage"
        alt=""
        class="h-full w-full object-cover object-center select-none"
        :class="loginBackgroundImageClass"
        draggable="false"
      />
      <div class="absolute inset-0" :class="loginBackgroundOverlayClass"></div>
      <div class="absolute inset-0" :class="loginBackgroundToneClass"></div>
      -->
    </div>

    <!-- Legacy gradient fallback retained for rollback reference only.
    <div class="absolute inset-0 z-0 bg-[#F6F1F8]">
      <div class="absolute top-[-15%] left-[-10%] w-[60%] h-[70%] bg-[#D4D5F8] rounded-full filter blur-[120px] opacity-60"></div>
      <div class="absolute top-[20%] right-[-10%] w-[50%] h-[60%] bg-[#F0D5E6] rounded-full filter blur-[140px] opacity-70"></div>
      <div class="absolute bottom-[-20%] left-[20%] w-[50%] h-[60%] bg-[#E0E7FF] rounded-full filter blur-[150px] opacity-80"></div>
    </div>
    -->

    <!-- Legacy particle background retained for rollback reference only.
    <canvas
      ref="canvasRef"
      class="absolute inset-x-0 top-0 w-full h-full pointer-events-none z-10 opacity-90"
    ></canvas>
    -->

    <!-- 玻璃拟态登录面板，包含品牌标识与交互表单 -->
    <div
      class="relative z-20 flex w-[95%] md:w-[92%] max-w-[1320px] h-[88dvh] md:h-[800px] max-h-[940px] rounded-[32px] flex-col md:flex-row overflow-hidden"
      :class="loginPanelClass"
    >
      
      <!-- 左侧装饰区域：包含动态表情交互与视觉美化 -->
      <div
        class="w-full md:w-[45%] lg:w-[48%] relative overflow-hidden hidden md:flex items-center justify-center shadow-inner"
        :class="loginVisualPaneClass"
      >
        <!-- Legacy left-pane dark gradient retained for rollback reference only.
        <div class="absolute inset-0 bg-gradient-to-tr from-slate-900/60 to-transparent pointer-events-none z-0"></div>
        -->

        <!-- 基础面部几何图形 -->
        <svg width="0" height="0" class="absolute">
          <defs>
            <linearGradient id="flatYellow" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stop-color="#FFE040" />
              <stop offset="100%" stop-color="#FFAD00" />
            </linearGradient>
            <!-- 基础面部几何图形 -->
            <path id="mouth-base" d="M 22 58 Q 50 53 78 58 C 76 84 24 84 22 58 Z" />
            <clipPath id="mouth-clip">
               <use href="#mouth-base" />
            </clipPath>
          </defs>
        </svg>

        <!-- 动态表情 1 -->
        <div class="absolute top-[12%] left-[22%] z-10 jelly-1">
          <div style="transition: transform 0.1s ease-out">
            <svg viewBox="0 0 100 100" class="w-48 h-48 drop-shadow-[0_15px_25px_rgba(0,0,0,0.2)] cursor-pointer group transition-all duration-300 hover:scale-105">
              <!-- 面部高光与交互变换 -->
              <circle cx="50" cy="50" r="48" fill="url(#flatYellow)"/>
              
              <!-- 闂傚倷绀侀幖顐λ囬柆宥呯；闁绘ɑ妞块弫瀣喐鎼达絿鐭夌€广儱顦崡鎶芥煏韫囧ň鍋撻崘鎻掝棄闂傚倷绀侀幉鈥趁洪敃鍌氶棷妞ゆ梻鈷堥悞浠嬫煙閻戞﹩娈曢柛瀣у墲缁绘盯宕卞Δ浣侯洶濠碘剝褰冮妶鎼佸蓟閻斿壊妲归幖绮光偓鍐茬婵犳鍠栭敃銉ョ暦濡偐涓嶆繛鎴欏灩鍞悷婊冪箻钘濋柍鍝勬噺閳锋垹鐥鐐村櫧闁绘帊绮欓弻娑㈠箛閳轰礁顬嬮梺鍛婏供閸撶喎顫忓ú顏咁棃闁冲搫瀚ˇ浼存⒑閸涘﹥鐓ラ柣顓炲€块獮鍡涘礃椤曞懏鏅㈤梺鍛婃处閸樿偐绮婇敃鍌涒拺闁告稑锕ｇ欢閬嶆煕濡亽鍋㈢€殿噮鍋婂畷濂稿即閻斿弶瀚兼俊鐐€栧濠氬磻閹剧粯鐓熼煫鍥ㄦ⒒缁犵偟鈧娲忛崹鑺ヤ繆閸洖骞㈡俊顖濐潐閸炲姊绘担瑙勫仩闁稿海绮穱濠囧炊椤掍礁娈為梺缁樺姦閸撴稓绮婚崜褉鍋撻崗澶婁壕闂侀€炲苯澧撮柕鍡曠劍缁绘繈宕惰琚濋梻浣规偠閸庮噣寮插┑瀣婵鍩栭崐鐢告煥濠靛棗鈧懓鈻嶉崶鈺冪＜濠㈣泛顭堥崑銏ゆ煛?-->
              <g :style="{ transform: `translate(${currentOffset.x * 0.25}px, ${currentOffset.y * 0.25}px)` }">
                <g v-if="!isPasswordFocused || showPassword">
                  <!-- 表情包各部分构建细节 -->
                  <path d="M 24 24 Q 32 18 40 24" stroke="#8A5A19" stroke-width="3" stroke-linecap="round" fill="none"/>
                  <path d="M 60 24 Q 68 18 76 24" stroke="#8A5A19" stroke-width="3" stroke-linecap="round" fill="none"/>
                  <!-- 明亮且跟踪鼠标的高光眼球 -->
                  <ellipse cx="32" cy="40" rx="9" ry="12" fill="#FFFFFF" stroke="#8A5A19" stroke-width="1.5"/>
                  <ellipse cx="68" cy="40" rx="9" ry="12" fill="#FFFFFF" stroke="#8A5A19" stroke-width="1.5"/>
                  <!-- 基础面部几何图形 -->
                  <circle :cx="32 + currentOffset.x * 0.4" :cy="40 + currentOffset.y * 0.4" r="5" fill="#1E293B" />
                  <circle :cx="68 + currentOffset.x * 0.4" :cy="40 + currentOffset.y * 0.4" r="5" fill="#1E293B" />
                  <circle :cx="30 + currentOffset.x * 0.4" :cy="38 + currentOffset.y * 0.4" r="1.5" fill="white" />
                  <circle :cx="66 + currentOffset.x * 0.4" :cy="38 + currentOffset.y * 0.4" r="1.5" fill="white" />
                  <!-- 基础面部几何图形 -->
                  <use href="#mouth-base" fill="#6B3200" stroke="#8A5A19" stroke-width="2" stroke-linejoin="round"/>
                  <g clip-path="url(#mouth-clip)">
                    <rect x="18" y="52" width="64" height="12" fill="#FFFFFF"/>
                    <ellipse cx="50" cy="79" rx="16" ry="10" fill="#FF6B6B"/>
                  </g>
                </g>
                <g v-else>
                  <!-- 基础面部几何图形 -->
                  <path d="M 22 38 L 32 46 L 22 54" stroke="#8A5A19" stroke-width="4" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
                  <path d="M 78 38 L 68 46 L 78 54" stroke="#8A5A19" stroke-width="4" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
                  <circle cx="50" cy="70" r="6" fill="#8A5A19" />
                  <ellipse cx="20" cy="60" rx="9" ry="5" fill="#FF8A8A" opacity="0.8"/>
                  <ellipse cx="80" cy="60" rx="9" ry="5" fill="#FF8A8A" opacity="0.8"/>
                </g>
              </g>
            </svg>
          </div>
        </div>

        <!-- 动态表情 2 -->
        <div class="absolute bottom-[16%] left-[8%] z-20 jelly-2">
          <div style="transition: transform 0.1s ease-out">
            <svg viewBox="0 0 100 100" class="w-32 h-32 drop-shadow-[0_10px_20px_rgba(0,0,0,0.2)] hover:rotate-6 transition-all duration-300 transform -rotate-12">
              <circle cx="50" cy="50" r="48" fill="url(#flatYellow)"/>
              
              <g :style="{ transform: `translate(${currentOffset.x * 0.25}px, ${currentOffset.y * 0.25}px)` }">
                <g v-if="!isPasswordFocused || showPassword">
                  <path d="M 24 24 Q 32 18 40 24" stroke="#8A5A19" stroke-width="3" stroke-linecap="round" fill="none"/>
                  <path d="M 60 24 Q 68 18 76 24" stroke="#8A5A19" stroke-width="3" stroke-linecap="round" fill="none"/>
                  <ellipse cx="32" cy="40" rx="9" ry="12" fill="#FFFFFF" stroke="#8A5A19" stroke-width="1.5"/>
                  <ellipse cx="68" cy="40" rx="9" ry="12" fill="#FFFFFF" stroke="#8A5A19" stroke-width="1.5"/>
                  <circle :cx="32 + currentOffset.x * 0.4" :cy="40 + currentOffset.y * 0.4" r="5" fill="#1E293B" />
                  <circle :cx="68 + currentOffset.x * 0.4" :cy="40 + currentOffset.y * 0.4" r="5" fill="#1E293B" />
                  <circle :cx="30 + currentOffset.x * 0.4" :cy="38 + currentOffset.y * 0.4" r="1.5" fill="white" />
                  <circle :cx="66 + currentOffset.x * 0.4" :cy="38 + currentOffset.y * 0.4" r="1.5" fill="white" />
                  <!-- 面部高光与交互变换 -->
                  <ellipse cx="20" cy="52" rx="7" ry="4" fill="#FF8A8A" opacity="0.6"/>
                  <ellipse cx="80" cy="52" rx="7" ry="4" fill="#FF8A8A" opacity="0.6"/>
                  <!-- 明亮且跟踪鼠标的高光眼球 -->
                  <path d="M 38 65 Q 50 76 62 65" stroke="#8A5A19" stroke-width="3.5" stroke-linecap="round" fill="none"/>
                </g>
                <g v-else>
                  <!-- 基础面部几何图形 -->
                  <path d="M 26 38 L 38 50 M 26 50 L 38 38" stroke="#8A5A19" stroke-width="4" stroke-linecap="round" fill="none"/>
                  <path d="M 62 38 L 74 50 M 62 50 L 74 38" stroke="#8A5A19" stroke-width="4" stroke-linecap="round" fill="none"/>
                  <path d="M 85 20 Q 95 30 85 40 Q 75 30 85 20 Z" fill="#60A5FA" opacity="0.9"/>
                  <path d="M 40 70 Q 50 60 60 70" stroke="#8A5A19" stroke-width="3" stroke-linecap="round" fill="none"/>
                </g>
              </g>
            </svg>
          </div>
        </div>

        <!-- 动态表情 3 -->
        <div class="absolute bottom-[20%] right-[10%] z-10 jelly-3">
          <div style="transition: transform 0.1s ease-out">
            <svg viewBox="0 0 100 100" class="w-[140px] h-[140px] drop-shadow-[0_20px_35px_rgba(0,0,0,0.2)] transition-all duration-300 transform rotate-12 hover:rotate-0">
              <circle cx="50" cy="50" r="48" fill="url(#flatYellow)"/>
              
              <g :style="{ transform: `translate(${currentOffset.x * 0.25}px, ${currentOffset.y * 0.25}px)` }">
                <g v-if="!isPasswordFocused || showPassword">
                  <!-- 表情包各部分构建细节 -->
                  <path d="M 26 26 Q 32 20 38 26" stroke="#8A5A19" stroke-width="3" stroke-linecap="round" fill="none"/>
                  <path d="M 62 26 Q 68 20 74 26" stroke="#8A5A19" stroke-width="3" stroke-linecap="round" fill="none"/>
                  <!-- 基础面部几何图形 -->
                  <ellipse cx="32" cy="42" rx="7" ry="9" fill="#FFFFFF" stroke="#8A5A19" stroke-width="1.5" />
                  <ellipse cx="68" cy="42" rx="7" ry="9" fill="#FFFFFF" stroke="#8A5A19" stroke-width="1.5" />
                  <!-- 基础面部几何图形 -->
                  <circle :cx="32 + currentOffset.x * 0.4" :cy="42 + currentOffset.y * 0.4" r="4" fill="#1E293B" />
                  <circle :cx="68 + currentOffset.x * 0.4" :cy="42 + currentOffset.y * 0.4" r="4" fill="#1E293B" />
                  <circle :cx="31 + currentOffset.x * 0.4" :cy="40 + currentOffset.y * 0.4" r="1" fill="white" />
                  <circle :cx="67 + currentOffset.x * 0.4" :cy="40 + currentOffset.y * 0.4" r="1" fill="white" />
                  <!-- 面部高光与交互变换 -->
                  <ellipse cx="20" cy="52" rx="7" ry="4" fill="#FF8A8A" opacity="0.6"/>
                  <ellipse cx="80" cy="52" rx="7" ry="4" fill="#FF8A8A" opacity="0.6"/>
                  <!-- 面部高光与交互变换 -->
                  <path d="M 38 65 Q 50 76 62 65" stroke="#8A5A19" stroke-width="3.5" stroke-linecap="round" fill="none"/>
                </g>
                <g v-else>
                  <!-- 基础面部几何图形 -->
                  <path d="M 22 46 Q 32 38 42 46" stroke="#8A5A19" stroke-width="4" stroke-linecap="round" fill="none"/>
                  <path d="M 58 46 Q 68 38 78 46" stroke="#8A5A19" stroke-width="4" stroke-linecap="round" fill="none"/>
                  <path d="M 45 68 L 55 68" stroke="#8A5A19" stroke-width="3" stroke-linecap="round" fill="none"/>
                </g>
              </g>
            </svg>
          </div>
        </div>

      </div>

      <!-- 基础面部几何图形 -->
      <div
        class="flex-1 w-full md:w-[52%] flex flex-col justify-center px-7 sm:px-12 lg:px-20 py-9 md:py-8 relative overflow-y-auto"
        :class="showMediaBackground ? 'auth-side-readable' : ''"
      >
        
        <div class="mb-6 z-30">
          <div class="flex w-fit max-w-full items-center gap-4 mb-6 auth-brand-strip">
            <img src="@/assets/logo.png" alt="logo" class="h-10 w-auto object-contain drop-shadow-[0_8px_18px_rgba(15,23,42,0.18)]" />
            <div class="flex flex-col">
              <h1 class="text-[24px] font-bold tracking-tight font-display flex items-baseline gap-2 leading-none auth-readable-heading">
                NovaLeap <span :class="logoAccentClass">知跃</span>
              </h1>
            </div>
          </div>

          <div class="flex p-1 bg-slate-200/50 backdrop-blur-md rounded-2xl w-full max-w-[460px] mx-auto lg:mx-0 auth-readable-segment">
            <button
              type="button"
              class="flex-1 py-2.5 text-sm font-bold rounded-xl transition-all duration-300"
              :class="activeTab === 'login' ? activeSegmentClass : inactiveSegmentClass"
              @click="router.push('/login')"
            >
              登录
            </button>
            <button
              type="button"
              class="flex-1 py-2.5 text-sm font-bold rounded-xl transition-all duration-300"
              :class="activeTab === 'register' ? activeSegmentClass : inactiveSegmentClass"
              @click="router.push('/register')"
            >
              注册
            </button>
          </div>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-6 w-full max-w-[460px] mx-auto lg:mx-0 z-30 relative">
          <p v-if="error" class="text-xs text-red-600 bg-red-50/80 border border-red-200 shadow-sm rounded-xl px-4 py-3 text-center mb-1">
            {{ error }}
          </p>

          <div>
            <label class="block text-[10px] font-bold tracking-widest text-[#475569] mb-1.5 uppercase opacity-80 auth-readable-label">
              账号 / {{ activeTab === 'login' ? '登录账号' : (activeTab === 'forgot' ? '重置目标邮箱' : '用于注册的邮箱') }}
            </label>
            <input type="text" v-model="form.username" required placeholder="请输入邮箱"
                   class="w-full bg-white/70 border border-slate-200/80 rounded-[14px] px-5 py-[12px] text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-[4px] focus:ring-indigo-500/10 focus:border-indigo-500 transition-all duration-300 shadow-sm auth-readable-input" />
          </div>

          <div v-if="activeTab === 'login'" class="flex p-1 bg-slate-100/70 rounded-xl auth-readable-segment">
            <button
              type="button"
              class="flex-1 py-2 text-xs font-bold rounded-lg transition-all duration-300"
              :class="!isCodeLoginMode ? activeSegmentClass : inactiveSegmentClass"
              @click="switchLoginMode('password')"
            >
              密码登录
            </button>
            <button
              type="button"
              class="flex-1 py-2 text-xs font-bold rounded-lg transition-all duration-300"
              :class="isCodeLoginMode ? activeSegmentClass : inactiveSegmentClass"
              @click="switchLoginMode('code')"
            >
              验证码登录
            </button>
          </div>

          <div v-if="showEmailCodeField">
            <label class="block text-[10px] font-bold tracking-widest text-[#475569] mb-1.5 uppercase opacity-80 auth-readable-label">
              验证码 / 获取自邮箱
            </label>
            <div class="relative flex items-center">
              <input type="text" v-model="form.emailCode" required placeholder="输入 6 位验证码"
                     class="w-full bg-white/70 border border-slate-200/80 rounded-[14px] px-5 py-[12px] text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-[4px] focus:ring-indigo-500/10 focus:border-indigo-500 transition-all duration-300 shadow-sm pr-[110px] auth-readable-input" />
              <button type="button" @click.prevent="handleSendCode" :disabled="sendCodeCooldown > 0 || isSendingCode"
                      class="absolute right-2 text-xs font-bold px-3 py-1.5 rounded-lg transition-all"
                      :class="(sendCodeCooldown > 0 || isSendingCode) ? sendCodeDisabledClass : sendCodeActiveClass">
                <span v-if="isSendingCode" class="w-3 h-3 border border-slate-400 border-t-transparent rounded-full animate-spin inline-block align-middle mr-1"></span>
                {{ isSendingCode ? '正在发送...' : (sendCodeCooldown > 0 ? `${sendCodeCooldown}s 后重新获取` : '获取验证码') }}
              </button>
            </div>
          </div>

          <div v-if="showPasswordField" class="relative">
            <div class="flex items-center justify-between mb-1.5">
              <label class="block text-[10px] font-bold tracking-widest text-[#475569] uppercase opacity-80 auth-readable-label">
                密码 / {{ activeTab === 'login' ? '登录密码' : (activeTab === 'forgot' ? '你的新密码' : '设置你的密码') }}
              </label>
              <button v-if="activeTab === 'login' && !isCodeLoginMode" type="button" @click="router.push('/forgot-password')" class="text-[11px] font-bold transition-colors auth-readable-link">忘记密码了？点此重置</button>
              <button v-if="activeTab === 'forgot'" type="button" @click="router.push('/login')" class="text-[11px] font-bold transition-colors auth-readable-muted hover:text-slate-800">想起密码了？返回登录</button>
            </div>
            <div class="relative group/pwd">
              <input :type="showPassword ? 'text' : 'password'" v-model="form.password" :required="showPasswordField" placeholder="请输入您的密码"
                     @focus="isPasswordFocused = true" @blur="isPasswordFocused = false"
                     class="w-full bg-white/70 border border-slate-200/80 rounded-[14px] px-5 pr-12 py-[12px] text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-[4px] focus:ring-purple-500/10 focus:border-purple-500 transition-all duration-300 shadow-sm tracking-[0.2em] auth-readable-input"
                     :class="{'!border-rose-400 !text-rose-600 focus:!border-rose-400 focus:!ring-rose-500/20 !bg-rose-50/50': form.password && form.password.length < 6}" />
              <button type="button"
                      class="absolute right-3 top-1/2 -translate-y-1/2 p-1.5 rounded-lg text-slate-500 hover:text-indigo-700 hover:bg-white/70 focus:outline-none transition-all duration-200"
                      @mousedown.prevent
                      @click="showPassword = !showPassword"
                      aria-label="切换密码可见性">
                <!-- 眼睛打开：密码可见 -->
                <svg v-if="showPassword" xmlns="http://www.w3.org/2000/svg" class="h-[18px] w-[18px]" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                  <path stroke-linecap="round" stroke-linejoin="round" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                </svg>
                <!-- 眼睛关闭：密码隐藏 -->
                <svg v-else xmlns="http://www.w3.org/2000/svg" class="h-[18px] w-[18px]" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12c1.292 4.338 5.31 7.5 10.066 7.5.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88"/>
                </svg>
              </button>
            </div>
            <transition enter-active-class="transition ease-out duration-200" enter-from-class="opacity-0 -translate-y-1" enter-to-class="opacity-100 translate-y-0" leave-active-class="transition ease-in duration-150" leave-from-class="opacity-100 translate-y-0" leave-to-class="opacity-0 -translate-y-1">
              <div v-if="form.password && form.password.length < 6" class="absolute -bottom-4 left-2 text-[10px] text-rose-500 font-bold tracking-wider flex items-center gap-1 z-10">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clip-rule="evenodd" /></svg>
                密码长度不能少于 6 位
              </div>
            </transition>
          </div>

          <div v-if="activeTab !== 'login'" class="relative">
            <label class="block text-[10px] font-bold tracking-widest text-[#475569] mb-1.5 uppercase opacity-80 auth-readable-label">再次确认 / 确认密码</label>
            <div class="relative group/cpwd">
              <input :type="showPassword ? 'text' : 'password'" v-model="form.confirmPassword" required placeholder="请再次输入防手滑输错"
                     @focus="isPasswordFocused = true" @blur="isPasswordFocused = false"
                     class="w-full bg-white/70 border border-slate-200/80 rounded-[14px] px-5 pr-12 py-[12px] text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-[4px] focus:ring-purple-500/10 focus:border-purple-500 transition-all duration-300 shadow-sm tracking-[0.2em] auth-readable-input"
                     :class="{'!border-rose-400 !text-rose-600 focus:!border-rose-400 focus:!ring-rose-500/20 !bg-rose-50/50': form.confirmPassword && form.password !== form.confirmPassword}" />
              <button type="button"
                      class="absolute right-3 top-1/2 -translate-y-1/2 p-1.5 rounded-lg text-slate-500 hover:text-indigo-700 hover:bg-white/70 focus:outline-none transition-all duration-200"
                      @mousedown.prevent
                      @click="showPassword = !showPassword"
                      aria-label="切换确认密码可见性">
                <svg v-if="showPassword" xmlns="http://www.w3.org/2000/svg" class="h-[18px] w-[18px]" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                  <path stroke-linecap="round" stroke-linejoin="round" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                </svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" class="h-[18px] w-[18px]" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12c1.292 4.338 5.31 7.5 10.066 7.5.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88"/>
                </svg>
              </button>
            </div>
            <transition enter-active-class="transition ease-out duration-200" enter-from-class="opacity-0 -translate-y-1" enter-to-class="opacity-100 translate-y-0" leave-active-class="transition ease-in duration-150" leave-from-class="opacity-100 translate-y-0" leave-to-class="opacity-0 -translate-y-1">
              <div v-if="form.confirmPassword && form.password !== form.confirmPassword" class="absolute -bottom-4 left-2 text-[10px] text-rose-500 font-bold tracking-wider flex items-center gap-1 z-10">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clip-rule="evenodd" /></svg>
                两次输入不一致
              </div>
            </transition>
          </div>

          <div v-if="activeTab === 'register'">
            <label class="block text-[10px] font-bold tracking-widest text-[#475569] mb-1.5 uppercase opacity-80 auth-readable-label">昵称 / 给自己起个响亮的名字</label>
            <input type="text" v-model="form.nickname" placeholder="我们应该怎么称呼你呢？(可选)"
                   class="w-full bg-white/70 border border-slate-200/80 rounded-[14px] px-5 py-[12px] text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-[4px] focus:ring-indigo-500/10 focus:border-indigo-500 transition-all duration-300 shadow-sm auth-readable-input" />
          </div>

          <label v-if="activeTab === 'register'" class="text-[12px] inline-flex items-start gap-2 mt-1 auth-readable-muted">
            <input v-model="form.consent" type="checkbox" class="mt-0.5" />
            <span>我已认真阅读并同意
              <router-link to="/terms" class="font-bold auth-readable-link">《用户协议》</router-link>
              和
              <router-link to="/privacy" class="font-bold auth-readable-link">《隐私政策》</router-link>
            </span>
          </label>

          <div v-if="turnstileSiteKey" class="mt-2">
            <div
              class="cf-turnstile"
              :data-sitekey="turnstileSiteKey"
              data-callback="novaTurnstileCallback"
              :data-action="activeTab"
            />
          </div>

          <div class="pt-2 space-y-4">
            <button
              type="submit"
              :disabled="isSubmitDisabled"
              class="w-full relative overflow-hidden text-white font-bold py-[12px] rounded-full transition-all duration-300 shadow-lg hover:shadow-xl disabled:opacity-70 disabled:cursor-not-allowed pointer-events-auto"
              :class="primaryActionClass">
              <span class="relative z-10 flex items-center justify-center tracking-widest text-sm">
                <span v-if="loading" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin mr-3"></span>
                {{ loading ? '处理中...' : (activeTab === 'login' ? '登录' : (activeTab === 'forgot' ? '确认重置' : '开启旅程 / 注册')) }}
              </span>
            </button>

            <!-- <button
              v-if="activeTab === 'login'"
              type="button"
              @click="router.push('/register')"
              class="mx-auto block text-[11px] font-bold text-slate-500 hover:text-indigo-600 transition-colors"
            >
              还没有账号吗？立即前往注册
            </button> -->

            <button
              v-if="activeTab === 'login'"
              type="button"
              @click="handleGuestLogin"
              :disabled="guestLoading"
              class="w-full relative overflow-hidden backdrop-blur-md font-bold py-[12px] rounded-full transition-all duration-300 shadow-sm mt-2 disabled:opacity-70 flex justify-center items-center group"
              :class="guestActionClass"
            >
              <span
                v-if="guestLoading"
                class="w-4 h-4 border-2 border-slate-400 border-t-slate-800 rounded-full animate-spin mr-2"
              ></span>

              <span
                v-else
                class="mr-2 text-sm transform group-hover:scale-110 transition-transform"
              >
                (•̀ᴗ•́)و
              </span>

              <span class="tracking-wider text-xs">
                {{ guestLoading ? '正在开启访客体验...' : '访客体验模式（只读进入）' }}
              </span>
            </button>
          </div>
        </form>

      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

// Active login background asset.
const customBackgroundVideo = '/login-backgrounds/login-loop-premium.mp4'
// Legacy background switching / asset mappings retained for rollback reference only.
// const LOGIN_BACKGROUND_MODE = 'video-overlay'
// const customBackgroundImage = '/login-backgrounds/login-spiderman.png'
// const showVideoBackground = LOGIN_BACKGROUND_MODE === 'video-overlay'
// const isSoftOverlayBackground = LOGIN_BACKGROUND_MODE === 'soft-overlay'
// const showCustomBackground = LOGIN_BACKGROUND_MODE === 'soft-overlay' || LOGIN_BACKGROUND_MODE === 'image'
// const showParticles = LOGIN_BACKGROUND_MODE === 'particles'
// const loginBackgroundImageClass = isSoftOverlayBackground ? 'login-bg-image-soft' : 'login-bg-image-strong'
// const loginBackgroundOverlayClass = showVideoBackground ? 'login-bg-overlay-video' : (isSoftOverlayBackground ? 'login-bg-overlay-soft' : 'login-bg-overlay-strong')
// const loginBackgroundToneClass = showVideoBackground ? 'login-bg-tone-video' : (isSoftOverlayBackground ? 'login-bg-tone-soft' : 'login-bg-tone-strong')
// Rollback video option: /login-backgrounds/login-loop-amber.mp4

const showMediaBackground = true
const showParticles = false
const loginThemeClass = 'login-theme-video'
const loginBackgroundClass = 'login-bg-video'
const loginBackgroundVideoClass = 'login-bg-video-media'
const loginPanelClass = 'login-panel-video'
const loginVisualPaneClass = 'login-visual-pane-video'
const activeSegmentClass = 'bg-[rgba(248,251,255,0.92)] text-[#30465d] shadow-[0_12px_24px_rgba(30,41,59,0.12)]'
const inactiveSegmentClass = 'text-[#627487] hover:text-[#2f455a]'
const sendCodeDisabledClass = 'bg-[rgba(226,232,240,0.82)] text-[#7b8794]'
const sendCodeActiveClass = 'bg-[rgba(232,240,248,0.94)] text-[#3f5973] hover:bg-[rgba(220,232,243,0.98)]'
const primaryActionClass = 'login-submit-video'
const guestActionClass = 'login-guest-video'
const logoAccentClass = 'text-[#f89aa5]'

const router = useRouter()
const route = useRoute()
const { login, register, guestLogin, sendEmailCode, resetPassword } = useAuth()

// Tab 标签页切换逻辑：根据当前路径自动识别 Login/Register/Forgot 状态
const activeTab = computed(() => {
  if (route.path === '/register') return 'register'
  if (route.path === '/forgot-password') return 'forgot'
  return 'login'
})

const form = reactive({ 
  username: '', 
  password: '', 
  confirmPassword: '', 
  nickname: '',
  emailCode: '',
  consent: false
})

const sendCodeCooldown = ref(0)
const isSendingCode = ref(false)

const loading = ref(false)
const guestLoading = ref(false)
const error = ref('')
const showPassword = ref(false)
const loginMode = ref('password')

// Turnstile 人机验证配置：从环境变量获取站点 Key
const turnstileSiteKey = import.meta.env.VITE_TURNSTILE_SITEKEY || ''
const turnstileToken = ref('')

const isPasswordFocused = ref(false)

const isCodeLoginMode = computed(() => activeTab.value === 'login' && loginMode.value === 'code')
const showEmailCodeField = computed(() => activeTab.value !== 'login' || isCodeLoginMode.value)
const showPasswordField = computed(() => activeTab.value !== 'login' || !isCodeLoginMode.value)

const canRegister = computed(() => {
  if (activeTab.value === 'forgot') {
    return form.username && form.password && (form.password === form.confirmPassword) && form.emailCode
  }
  return form.username && form.password && (form.password === form.confirmPassword) && form.emailCode
})

const isSubmitDisabled = computed(() => {
  if (loading.value) return true
  if (activeTab.value === 'register' || activeTab.value === 'forgot') {
    return !canRegister.value
  }
  if (activeTab.value === 'login') {
    if (!form.username) return true
    return isCodeLoginMode.value ? !form.emailCode : !form.password
  }
  return false
})

const targetOffset = reactive({ x: 0, y: 0 })
const currentOffset = reactive({ x: 0, y: 0 })

let animationFrameId = null
let sendCodeTimer = null
let mouseX = window.innerWidth / 2 
let mouseY = window.innerHeight / 2

const updateEyeTargets = () => {
  if (isPasswordFocused.value && !showPassword.value) {
    targetOffset.x = 0
    targetOffset.y = 0
    return
  }

  const centerX = window.innerWidth * 0.25
  const centerY = window.innerHeight * 0.5
  
  const dx = mouseX - centerX
  const dy = mouseY - centerY
  
  const dist = Math.sqrt(dx*dx + dy*dy)
  const maxOffset = 18 
  
  if (dist > 0) {
    const proportion = Math.min(dist / 600, 1) 
    targetOffset.x = (dx / dist) * maxOffset * proportion
    targetOffset.y = (dy / dist) * maxOffset * proportion
  }
}

const animateEyes = () => {
  updateEyeTargets()
  currentOffset.x += (targetOffset.x - currentOffset.x) * 0.15
  currentOffset.y += (targetOffset.y - currentOffset.y) * 0.15
  animationFrameId = requestAnimationFrame(animateEyes)
}

const handleMouseMove = (e) => {
  mouseX = e.clientX
  mouseY = e.clientY
}

// Canvas 背景粒子动画系统初始化
const canvasRef = ref(null)
let ctx = null
let particleAnimationFrameId = null
let canvasResizeHandler = null
const particles = []
const particleCount = 160 

class Particle {
  constructor(canvas) {
    this.canvas = canvas
    this.x = Math.random() * this.canvas.width
    this.y = Math.random() * this.canvas.height
    this.originXRatio = Math.random()
    this.originYRatio = Math.random()
    this.vx = 0 
    this.vy = 0
    this.size = Math.random() * 2.2 + 1.2 
    this.history = [] 
    this.maxHistory = 6 
    const colors = ['#FF007F', '#00F0FF', '#9333EA', '#10B981'] 
    this.color = colors[Math.floor(Math.random() * colors.length)]
  }
  update() {
    this.history.push({ x: this.x, y: this.y })
    if (this.history.length > this.maxHistory) this.history.shift()
    const dx = mouseX - this.x
    const dy = mouseY - this.y
    const distance = Math.sqrt(dx * dx + dy * dy)
    const targetX = this.originXRatio * this.canvas.width
    const targetY = this.originYRatio * this.canvas.height
    if (distance > 10 && distance < 450) {
      const pull = (450 - distance) / 1800 
      this.vx += (dx / distance) * pull
      this.vy += (dy / distance) * pull
    } else {
      const dxOrigin = targetX - this.x
      const dyOrigin = targetY - this.y
      this.vx += dxOrigin * 0.065
      this.vy += dyOrigin * 0.065
    }
    this.x += this.vx
    this.y += this.vy
    if (this.x < -20 || this.x > this.canvas.width + 20 || this.y < -20 || this.y > this.canvas.height + 20) this.history = []
    const maxSpeed = 5.0
    const speed = Math.sqrt(this.vx * this.vx + this.vy * this.vy)
    if (speed > maxSpeed) {
      this.vx = (this.vx / speed) * maxSpeed
      this.vy = (this.vy / speed) * maxSpeed
    }
    this.vx *= 0.90
    this.vy *= 0.90
  }
  draw(ctx) {
    if (this.history.length > 1) {
      ctx.beginPath()
      ctx.moveTo(this.history[0].x, this.history[0].y)
      for (let i = 1; i < this.history.length; i++) ctx.lineTo(this.history[i].x, this.history[i].y)
      ctx.strokeStyle = this.color
      ctx.lineWidth = this.size * 0.6
      ctx.globalAlpha = 0.35
      ctx.stroke()
    }
    ctx.shadowBlur = 12 
    ctx.shadowColor = this.color
    ctx.beginPath()
    ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2)
    ctx.fillStyle = this.color
    ctx.globalAlpha = 0.9
    ctx.fill()
    ctx.shadowBlur = 0 
  }
}

const initCanvas = () => {
  if (!showParticles || !canvasRef.value) return
  ctx = canvasRef.value.getContext('2d') 
  particles.length = 0
  const resize = () => {
    canvasRef.value.width = window.innerWidth
    canvasRef.value.height = window.innerHeight
  }
  canvasResizeHandler = resize
  window.addEventListener('resize', resize)
  resize()
  for (let i = 0; i < particleCount; i++) particles.push(new Particle(canvasRef.value))
  const renderParticles = () => {
    if (!canvasRef.value || !ctx) return
    ctx.clearRect(0, 0, canvasRef.value.width, canvasRef.value.height)
    for (let i = 0; i < particles.length; i++) {
        particles[i].update()
        particles[i].draw(ctx)
    }
    particleAnimationFrameId = requestAnimationFrame(renderParticles)
  }
  particleAnimationFrameId = requestAnimationFrame(renderParticles)
}

onMounted(() => {
  if (showParticles) initCanvas()
  if (turnstileSiteKey) {
    window.novaTurnstileCallback = (token) => { turnstileToken.value = token || '' }
    
    const hasScript = !!document.querySelector('script[data-nova-turnstile]')
    if (!hasScript) {
      const script = document.createElement('script')
      script.src = 'https://challenges.cloudflare.com/turnstile/v0/api.js'
      script.async = true
      script.defer = true
      script.setAttribute('data-nova-turnstile', '1')
      document.head.appendChild(script)
    }
  }

  window.addEventListener('mousemove', handleMouseMove)
  animateEyes()
})

onBeforeUnmount(() => {
  window.removeEventListener('mousemove', handleMouseMove)
  if (canvasResizeHandler) window.removeEventListener('resize', canvasResizeHandler)
  if (animationFrameId) cancelAnimationFrame(animationFrameId)
  if (particleAnimationFrameId) cancelAnimationFrame(particleAnimationFrameId)
  if (sendCodeTimer) clearInterval(sendCodeTimer)
  ctx = null
  particles.length = 0
})

const handleSubmit = async () => {
  if (activeTab.value === 'login') {
    await handleLogin()
  } else if (activeTab.value === 'register') {
    await handleRegister()
  } else if (activeTab.value === 'forgot') {
    await handleResetPassword()
  }
}

const switchLoginMode = (mode) => {
  loginMode.value = mode === 'code' ? 'code' : 'password'
  error.value = ''
}

const handleLogin = async () => {
  if (!form.username) return
  if (isCodeLoginMode.value && !form.emailCode) return
  if (!isCodeLoginMode.value && !form.password) return
  error.value = ''
  if (turnstileSiteKey && !turnstileToken.value) {
    error.value = '请先完成人机验证'
    return
  }
  loading.value = true
  try {
    if (isCodeLoginMode.value) {
      await login(form.username, {
        loginType: 'code',
        emailCode: form.emailCode,
        turnstileToken: turnstileToken.value,
      })
    } else {
      await login(form.username, {
        loginType: 'password',
        password: form.password,
        turnstileToken: turnstileToken.value,
      })
    }
  } catch (e) {
    error.value = e.message || '登录异常'
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  if (!canRegister.value) return
  if (!form.consent) {
    error.value = '请先勾选同意《用户协议》和《隐私政策》'
    return
  }
  error.value = ''
  loading.value = true
  try {
    await register({
      username: form.username,
      password: form.password,
      nickname: form.nickname,
      confirmPassword: form.confirmPassword,
      consent: form.consent,
      emailCode: form.emailCode,
      turnstileToken: turnstileToken.value
    })
    // 注册成功后跳转至登录页面，需清空刚填写的表单，呈现干净的登录面板
    form.username = ''
    form.password = ''
    form.confirmPassword = ''
    form.nickname = ''
    form.emailCode = ''
    form.consent = false
    
    router.push('/login')
    error.value = ''
    alert('注册成功！请使用刚注册的账号进行登录。')
  } catch (e) {
    error.value = e.message || '注册异常'
  } finally {
    loading.value = false
  }
}

const handleResetPassword = async () => {
  if (!canRegister.value) return
  error.value = ''
  loading.value = true
  try {
    await resetPassword(form.username, form.password, form.emailCode)
    form.password = ''
    form.confirmPassword = ''
    form.emailCode = ''
    router.push('/login')
    error.value = '密码重置成功，请重新登录'
  } catch (e) {
    error.value = e.message || '重置异常'
  } finally {
    loading.value = false
  }
}

const handleSendCode = async () => {
  if (!form.username) {
    error.value = '请先输入邮箱'
    return
  }
  if (!form.username.includes('@')) {
    error.value = '请输入正确的邮箱格式'
    return
  }
  try {
    error.value = ''
    isSendingCode.value = true
    const type = activeTab.value === 'register' ? 'register' : (activeTab.value === 'login' ? 'login' : 'reset')
    await sendEmailCode(form.username, type)
    startSendCodeCooldown()
    error.value = '验证码已发送至您的邮箱，如未收到可稍后再试。'
  } catch (e) {
    error.value = e.message || '发送验证码失败'
  } finally {
    isSendingCode.value = false
  }
}

const startSendCodeCooldown = () => {
  sendCodeCooldown.value = 60
  if (sendCodeTimer) clearInterval(sendCodeTimer)
  sendCodeTimer = setInterval(() => {
    sendCodeCooldown.value--
    if (sendCodeCooldown.value <= 0) {
      clearInterval(sendCodeTimer)
      sendCodeTimer = null
    }
  }, 1000)
}

const handleGuestLogin = async () => {
  guestLoading.value = true
  try {
    const success = await guestLogin()
    if (success) router.push('/')
  } finally {
    guestLoading.value = false
  }
}

</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@600;700&display=swap');

.font-sans {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}

.font-display {
  font-family: 'Space Grotesk', 'Inter', sans-serif;
}

input:-webkit-autofill,
input:-webkit-autofill:hover, 
input:-webkit-autofill:focus, 
input:-webkit-autofill:active{
    -webkit-box-shadow: 0 0 0 30px rgba(255,255,255,0.8) inset !important;
    -webkit-text-fill-color: #1e293b !important;
    transition: background-color 5000s ease-in-out 0s;
}

/* 元素悬浮效果的动画定义 */
@keyframes float-q1 {
  0% { transform: translateY(0) scale(1, 1); }
  25% { transform: translateY(-12px) scale(0.96, 1.04); }
  50% { transform: translateY(0) scale(1.03, 0.97); }
  75% { transform: translateY(8px) scale(0.97, 1.03); }
  100% { transform: translateY(0) scale(1, 1); }
}

.jelly-1 { animation: float-q1 4.2s cubic-bezier(0.45, 0.05, 0.55, 0.95) infinite; }
.jelly-2 { animation: float-q1 3.5s cubic-bezier(0.45, 0.05, 0.55, 0.95) infinite 1s; }
.jelly-3 { animation: float-q1 4.8s cubic-bezier(0.45, 0.05, 0.55, 0.95) infinite 0.5s; }

.nova-mark {
  width: 56px;
  height: 56px;
  border-radius: 18px;
  display: inline-grid;
  place-items: center;
  color: white;
  font-size: 22px;
  font-weight: 700;
  background:
    radial-gradient(70% 70% at 25% 25%, rgba(255, 255, 255, 0.45), transparent 50%),
    linear-gradient(135deg, #7c3aed, #ec4899 56%, #22d3ee);
  box-shadow: 0 12px 32px rgba(124, 58, 237, 0.28);
  position: relative;
}

.nova-mark::after {
  content: '';
  position: absolute;
  inset: 4px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.42);
}

.login-bg-soft {
  background: #f5f2ee;
}

.login-bg-video {
  background: #070b12;
}

.login-bg-video-media {
  filter: none;
  transform: none;
}

/*
Legacy background overlays / image modes retained for rollback reference only.

.login-bg-overlay-video {
  background: rgba(8, 10, 16, 0.36);
}

.login-bg-tone-video {
  background:
    radial-gradient(circle at top left, rgba(216, 184, 132, 0.14), transparent 28%),
    linear-gradient(135deg, rgba(9, 13, 22, 0.62), rgba(28, 34, 46, 0.16) 38%, rgba(7, 10, 18, 0.84));
}

.login-bg-image-soft {
  filter: blur(2px) saturate(0.82);
  transform: scale(1.03);
}

.login-bg-overlay-soft {
  background: rgba(245, 242, 238, 0.6);
  backdrop-filter: blur(20px);
}

.login-bg-tone-soft {
  background:
    radial-gradient(circle at top left, rgba(255, 255, 255, 0.55), transparent 32%),
    linear-gradient(135deg, rgba(255, 248, 240, 0.42), rgba(245, 242, 238, 0.18) 42%, rgba(226, 217, 208, 0.32));
}

.login-bg-image-strong {
  filter: saturate(0.96);
}

.login-bg-overlay-strong {
  background: rgba(15, 23, 42, 0.42);
}

.login-bg-tone-strong {
  background:
    radial-gradient(circle at top left, rgba(255, 255, 255, 0.18), transparent 32%),
    linear-gradient(135deg, rgba(15, 23, 42, 0.68), rgba(15, 23, 42, 0.24) 42%, rgba(2, 6, 23, 0.78));
}

.login-panel-soft {
  background: rgba(255, 255, 255, 0.34);
  border: 1px solid rgba(255, 255, 255, 0.46);
  backdrop-filter: blur(18px);
  box-shadow: 0 28px 80px -24px rgba(15, 23, 42, 0.24);
}
*/

.login-panel-video {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(10px);
  box-shadow: 0 28px 80px -24px rgba(2, 6, 23, 0.34);
}

/*
Legacy panel / visual-pane variants retained for rollback reference only.

.login-panel-strong {
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(12px);
  box-shadow: 0 24px 70px -20px rgba(0, 0, 0, 0.45);
}

.login-panel-default {
  background: rgba(255, 255, 255, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(8px);
  box-shadow: 0 20px 60px -15px rgba(0, 0, 0, 0.15);
}

.login-visual-pane-soft {
  background: rgba(15, 23, 42, 0.62);
}
*/

.login-visual-pane-video {
  background: rgba(4, 8, 18, 0.22);
}

/*
Legacy visual-pane variants retained for rollback reference only.

.login-visual-pane-strong {
  background: rgba(2, 6, 23, 0.55);
}

.login-visual-pane-default {
  background: rgba(107, 117, 132, 0.95);
}
*/

.auth-side-readable {
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.76), rgba(255, 255, 255, 0.6));
  border-left: 1px solid rgba(255, 255, 255, 0.24);
}

.auth-side-readable::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.14), rgba(255, 255, 255, 0.02)),
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.2), transparent 34%);
}

.auth-side-readable > * {
  position: relative;
  z-index: 1;
}

.auth-brand-strip {
  padding: 0;
  border-radius: 0;
  background: transparent;
  border: none;
  box-shadow: none;
  backdrop-filter: none;
}

.auth-readable-heading {
  color: #0f172a;
  text-shadow: 0 1px 20px rgba(255, 255, 255, 0.38);
}

.auth-readable-heading span {
  color: #f89aa5 !important;
}

.auth-readable-segment {
  border: 1px solid rgba(255, 255, 255, 0.35);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.45),
    0 10px 26px rgba(15, 23, 42, 0.1);
}

.auth-side-readable label:not(.inline-flex),
.auth-readable-label {
  font-size: 11px;
  letter-spacing: 0.16em;
  color: #334155;
  opacity: 1;
  text-shadow: 0 1px 14px rgba(255, 255, 255, 0.28);
}

.auth-side-readable input:not([type='checkbox']),
.auth-readable-input {
  background: rgba(255, 255, 255, 0.88);
  border-color: rgba(148, 163, 184, 0.38);
  color: #0f172a;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.1);
}

.auth-side-readable input:not([type='checkbox'])::placeholder,
.auth-readable-input::placeholder {
  color: #64748b;
  opacity: 0.95;
}

.auth-readable-link {
  color: #4338ca;
  text-shadow: 0 1px 12px rgba(255, 255, 255, 0.24);
}

.auth-readable-link:hover {
  color: #312e81;
}

.auth-readable-muted {
  color: #475569;
  text-shadow: 0 1px 12px rgba(255, 255, 255, 0.24);
}

.login-submit-video {
  background: linear-gradient(135deg, #0f172a, #1d2c3d 48%, #3f5b78);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.24);
}

.login-submit-video:hover {
  background: linear-gradient(135deg, #0c121b, #172333 48%, #344c65);
}

.login-guest-video {
  background: rgba(245, 249, 253, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.56);
  color: #203244;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.12);
}

.login-guest-video:hover {
  background: rgba(250, 252, 255, 0.92);
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.14);
}

.login-theme-video .auth-side-readable {
  background: linear-gradient(180deg, rgba(246, 249, 252, 0.54), rgba(255, 255, 255, 0.34));
}

.login-theme-video .auth-side-readable::before {
  background: none;
}

.login-theme-video .auth-brand-strip {
  background: transparent;
  border-color: transparent;
  box-shadow: none;
}

.login-theme-video .auth-readable-heading {
  color: #183042;
}

.login-theme-video .auth-readable-heading span {
  color: rgba(0, 0, 0, 0.577) !important;
}

.login-theme-video .auth-readable-segment {
  border-color: rgba(255, 255, 255, 0.44);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.54),
    0 12px 28px rgba(15, 23, 42, 0.08);
}

.login-theme-video .auth-readable-label {
  color: #546678;
}

.login-theme-video .auth-readable-input {
  background: rgba(248, 251, 255, 0.8);
  border-color: rgba(148, 163, 184, 0.24);
}

.login-theme-video .auth-readable-input::placeholder {
  color: #7a8c9e;
}

.login-theme-video .auth-readable-input:focus {
  border-color: #7d95af;
  box-shadow:
    0 0 0 4px rgba(125, 149, 175, 0.16),
    0 16px 34px rgba(15, 23, 42, 0.12);
}

.login-theme-video .auth-readable-link {
  color: #4b647d;
}

.login-theme-video .auth-readable-link:hover {
  color: #324a61;
}

.login-theme-video .auth-readable-muted {
  color: #657383;
}

.login-theme-video .group\/pwd > button:hover,
.login-theme-video .group\/cpwd > button:hover {
  color: #4b647d;
  background: rgba(248, 251, 255, 0.72);
}

</style>
