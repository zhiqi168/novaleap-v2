<template>
  <div class="space-y-4">
    <div class="bg-admin-card rounded-lg p-4 shadow-sm border border-gray-100 flex flex-wrap items-center gap-3">
      <div class="relative flex-1 min-w-[200px] max-w-[320px]">
        <Search class="w-4 h-4 text-admin-muted absolute left-3 top-1/2 -translate-y-1/2" />
        <input
          v-model="keyword"
          @keyup.enter="loadData(1)"
          type="text"
          placeholder="搜索题目标题"
          class="w-full pl-9 pr-4 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-admin-primary/20 focus:border-admin-primary outline-none transition-all"
        />
      </div>
      <select
        v-model="categoryFilter"
        @change="loadData(1)"
        class="bg-gray-50 border border-gray-200 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-admin-primary/20 outline-none"
      >
        <option value="">全部分类</option>
        <option
          v-for="item in normalizedCategoryOptions"
          :key="item.code"
          :value="item.code"
        >
          {{ item.name }}
        </option>
      </select>
      <select
        v-model="difficultyFilter"
        @change="loadData(1)"
        class="bg-gray-50 border border-gray-200 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-admin-primary/20 outline-none"
      >
        <option value="">全部难度</option>
        <option value="1">简单</option>
        <option value="2">中等</option>
        <option value="3">困难</option>
      </select>
      <button
        @click="openCategoryDialog"
        class="inline-flex items-center gap-1.5 px-3 py-2 border border-gray-200 rounded-lg text-sm text-admin-text hover:bg-gray-50 transition-colors"
      >
        <Plus class="w-4 h-4" />
        新增分类
      </button>

      <button
        @click="openOfficialImportDialog"
        class="ml-auto inline-flex items-center gap-1.5 px-3 py-2 border border-admin-primary/30 text-admin-primary rounded-lg text-sm hover:bg-admin-primary/5 transition-colors"
      >
        <Upload class="w-4 h-4" />
        导入官方题库
      </button>

      <button
        @click="openCreate"
        class="inline-flex items-center gap-1.5 px-3 py-2 bg-admin-primary text-white rounded-lg text-sm hover:bg-admin-primary/90 transition-colors"
      >
        <Plus class="w-4 h-4" />
        新增题目
      </button>
    </div>

    <div class="bg-admin-card rounded-lg p-4 shadow-sm border border-gray-100">
      <div class="flex items-center justify-between gap-3 mb-3">
        <div>
          <h3 class="text-sm font-semibold text-admin-text">分类管理</h3>
          <p class="text-xs text-admin-muted mt-1">内置分类不可删除；已被题目或题库占用的分类也不能删除。</p>
        </div>
        <span class="text-xs text-admin-muted">{{ normalizedCategoryOptions.length }} 个分类</span>
      </div>
      <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3">
        <div
          v-for="item in normalizedCategoryOptions"
          :key="`category-card-${item.code}`"
          class="rounded-lg border border-gray-100 bg-gray-50 px-3 py-3 flex items-start justify-between gap-3"
        >
          <div class="min-w-0">
            <div class="flex items-center gap-2 flex-wrap">
              <span class="text-sm font-medium text-admin-text">{{ item.name }}</span>
              <span class="px-2 py-0.5 rounded-full text-[11px] bg-white border border-gray-200 text-admin-muted">
                {{ item.code }}
              </span>
              <span
                v-if="item.builtin"
                class="px-2 py-0.5 rounded-full text-[11px] bg-slate-100 text-slate-600"
              >
                内置
              </span>
            </div>
            <p class="mt-2 text-xs text-admin-muted">{{ categoryUsageText(item) }}</p>
          </div>
          <button
            @click="handleDeleteCategory(item)"
            :disabled="!item.deletable || categoryDeletingCode === item.code"
            class="shrink-0 inline-flex items-center gap-1 px-2 py-1 text-xs rounded border transition-colors"
            :class="item.deletable
              ? 'border-red-200 text-admin-danger hover:bg-red-50 disabled:opacity-60'
              : 'border-gray-200 text-gray-400 cursor-not-allowed opacity-70'"
          >
            <Trash2 class="w-3.5 h-3.5" />
            {{ categoryDeletingCode === item.code ? '删除中...' : '删除' }}
          </button>
        </div>
      </div>
    </div>

    <div class="bg-admin-card rounded-lg shadow-sm border border-gray-100 overflow-hidden">
      <div v-if="loading" class="p-8 text-center text-admin-muted">
        <div class="w-6 h-6 border-2 border-admin-primary/30 border-t-admin-primary rounded-full animate-spin mx-auto mb-2"></div>
        加载中...
      </div>
      <div v-else-if="error" class="p-8 text-center text-admin-danger">{{ error }}</div>
      <table v-else class="w-full text-sm">
        <thead class="bg-gray-50 text-admin-muted">
          <tr>
            <th class="text-left px-4 py-3 font-medium">ID</th>
            <th class="text-left px-4 py-3 font-medium">标题</th>
            <th class="text-left px-4 py-3 font-medium">来源</th>
            <th class="text-left px-4 py-3 font-medium">分类</th>
            <th class="text-left px-4 py-3 font-medium">难度</th>
            <th class="text-left px-4 py-3 font-medium">标准答案</th>
            <th class="text-left px-4 py-3 font-medium">浏览量</th>
            <th class="text-left px-4 py-3 font-medium">状态</th>
            <th class="text-left px-4 py-3 font-medium">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr v-for="q in questions" :key="q.id" class="hover:bg-gray-50/50 transition-colors">
            <td class="px-4 py-3 text-admin-muted">{{ q.id }}</td>
            <td class="px-4 py-3 font-medium text-admin-text max-w-[360px] truncate">{{ q.title }}</td>
            <td class="px-4 py-3">
              <span
                class="px-2 py-1 rounded-full text-xs font-medium"
                :class="q.sourceType === 'CUSTOM' ? 'bg-sky-100 text-sky-700' : 'bg-slate-100 text-slate-700'"
              >
                {{ q.sourceType === 'CUSTOM' ? '自定义题库' : '官方题库' }}
              </span>
            </td>
            <td class="px-4 py-3 text-admin-muted">{{ categoryLabel(q.category) }}</td>
            <td class="px-4 py-3">
              <span
                class="px-2 py-1 rounded-full text-xs font-medium"
                :class="{
                  'bg-green-100 text-green-700': q.difficulty === 1,
                  'bg-orange-100 text-orange-700': q.difficulty === 2,
                  'bg-red-100 text-red-700': q.difficulty === 3,
                }"
              >
                {{ difficultyText(q.difficulty) }}
              </span>
            </td>
            <td class="px-4 py-3">
              <span
                class="px-2 py-1 rounded-full text-xs font-medium"
                :class="q.standardAnswer ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'"
              >
                {{ q.standardAnswer ? '已配置' : '未配置' }}
              </span>
            </td>
            <td class="px-4 py-3 text-admin-muted">{{ q.viewCount }}</td>
            <td class="px-4 py-3">
              <span class="px-2 py-1 rounded-full text-xs font-medium" :class="q.status === 1 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'">
                {{ q.status === 1 ? '上架' : '下架' }}
              </span>
            </td>
            <td class="px-4 py-3">
              <div class="flex items-center gap-2">
                <button
                  @click="openEdit(q)"
                  class="inline-flex items-center gap-1 px-2 py-1 text-xs rounded border border-gray-200 hover:bg-gray-50 text-admin-text"
                >
                  <Pencil class="w-3.5 h-3.5" />
                  编辑
                </button>
                <button
                  @click="handleDelete(q)"
                  class="inline-flex items-center gap-1 px-2 py-1 text-xs rounded border border-red-200 text-admin-danger hover:bg-red-50"
                >
                  <Trash2 class="w-3.5 h-3.5" />
                  删除
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-if="!loading && !error" class="flex items-center justify-between px-4 py-3 border-t border-gray-100 text-sm text-admin-muted">
        <span>共 {{ total }} 条</span>
        <div class="flex gap-1">
          <button @click="loadData(currentPage - 1)" :disabled="currentPage <= 1" class="px-3 py-1 border border-gray-200 rounded hover:bg-gray-50 disabled:opacity-40 transition-colors">上一页</button>
          <span class="px-3 py-1 bg-admin-primary/10 text-admin-primary rounded font-medium">{{ currentPage }}</span>
          <button @click="loadData(currentPage + 1)" :disabled="currentPage >= totalPages" class="px-3 py-1 border border-gray-200 rounded hover:bg-gray-50 disabled:opacity-40 transition-colors">下一页</button>
        </div>
      </div>
    </div>

    <div class="bg-admin-card rounded-lg shadow-sm border border-gray-100 overflow-hidden">
      <div class="px-4 py-4 border-b border-gray-100 flex flex-wrap items-center gap-3">
        <div>
          <h3 class="text-base font-semibold text-admin-text">自定义题库审核</h3>
          <p class="text-xs text-admin-muted mt-1">用户上传 Word / TXT 后，会先进入这里审核，通过后才正式入库。</p>
        </div>

        <div class="relative flex-1 min-w-[220px] max-w-[320px] ml-auto">
          <Search class="w-4 h-4 text-admin-muted absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            v-model="bankKeyword"
            @keyup.enter="loadBankData(1)"
            type="text"
            placeholder="搜索题库名 / 文件名"
            class="w-full pl-9 pr-4 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-admin-primary/20 focus:border-admin-primary outline-none transition-all"
          />
        </div>

        <select
          v-model="bankStatusFilter"
          @change="loadBankData(1)"
          class="bg-gray-50 border border-gray-200 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-admin-primary/20 outline-none"
        >
          <option value="">全部状态</option>
          <option value="0">待审核</option>
          <option value="1">已通过</option>
          <option value="2">已驳回</option>
        </select>
      </div>

      <div v-if="bankLoading" class="p-8 text-center text-admin-muted">
        <div class="w-6 h-6 border-2 border-admin-primary/30 border-t-admin-primary rounded-full animate-spin mx-auto mb-2"></div>
        加载中...
      </div>
      <div v-else-if="bankError" class="p-8 text-center text-admin-danger">{{ bankError }}</div>
      <table v-else class="w-full text-sm">
        <thead class="bg-gray-50 text-admin-muted">
          <tr>
            <th class="text-left px-4 py-3 font-medium">ID</th>
            <th class="text-left px-4 py-3 font-medium">题库信息</th>
            <th class="text-left px-4 py-3 font-medium">上传用户</th>
            <th class="text-left px-4 py-3 font-medium">分类 / 难度</th>
            <th class="text-left px-4 py-3 font-medium">题目数</th>
            <th class="text-left px-4 py-3 font-medium">状态</th>
            <th class="text-left px-4 py-3 font-medium">摘要</th>
            <th class="text-left px-4 py-3 font-medium">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr v-for="bank in customBanks" :key="bank.id" class="hover:bg-gray-50/50 transition-colors">
            <td class="px-4 py-3 text-admin-muted">{{ bank.id }}</td>
            <td class="px-4 py-3 max-w-[260px]">
              <div class="font-medium text-admin-text truncate">{{ bank.name }}</div>
              <p class="mt-1 text-xs text-admin-muted truncate">{{ bank.originalFileName }}</p>
            </td>
            <td class="px-4 py-3 text-admin-muted">
              <div>{{ bank.ownerNickname || '-' }}</div>
              <p class="text-xs mt-1">{{ bank.ownerUsername || '-' }}</p>
            </td>
            <td class="px-4 py-3 text-admin-muted">
              <div>{{ categoryLabel(bank.category) }}</div>
              <p class="text-xs mt-1">{{ difficultyText(bank.difficulty) }}</p>
            </td>
            <td class="px-4 py-3 text-admin-muted">
              {{ bank.importedQuestionCount || bank.questionCount || 0 }}
            </td>
            <td class="px-4 py-3">
              <span class="px-2 py-1 rounded-full text-xs font-medium" :class="bankAuditStatusClass(bank.status)">
                {{ bankAuditStatusText(bank.status) }}
              </span>
              <p v-if="bank.status === 2 && bank.rejectReason" class="mt-1 text-xs text-rose-600 line-clamp-2">
                {{ bank.rejectReason }}
              </p>
            </td>
            <td class="px-4 py-3 max-w-[320px] text-xs text-admin-muted">
              <p class="line-clamp-3 leading-6">{{ bank.previewText || '暂无解析摘要' }}</p>
            </td>
            <td class="px-4 py-3">
              <div class="flex items-center flex-wrap gap-2">
                <button
                  @click="openBankPreview(bank)"
                  class="inline-flex items-center gap-1 px-2 py-1 text-xs rounded border border-gray-200 hover:bg-gray-50 text-admin-text"
                >
                  <Eye class="w-3.5 h-3.5" />
                  预览
                </button>
                <button
                  v-if="bank.status === 0"
                  @click="handleBankAudit(bank, 1)"
                  class="inline-flex items-center gap-1 px-2 py-1 text-xs rounded border border-emerald-200 text-emerald-700 hover:bg-emerald-50"
                >
                  <Check class="w-3.5 h-3.5" />
                  通过
                </button>
                <button
                  v-if="bank.status === 0"
                  @click="handleBankAudit(bank, 2)"
                  class="inline-flex items-center gap-1 px-2 py-1 text-xs rounded border border-rose-200 text-rose-700 hover:bg-rose-50"
                >
                  <XCircle class="w-3.5 h-3.5" />
                  驳回
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-if="!bankLoading && !bankError" class="flex items-center justify-between px-4 py-3 border-t border-gray-100 text-sm text-admin-muted">
        <span>共 {{ bankTotal }} 条</span>
        <div class="flex gap-1">
          <button @click="loadBankData(bankCurrentPage - 1)" :disabled="bankCurrentPage <= 1" class="px-3 py-1 border border-gray-200 rounded hover:bg-gray-50 disabled:opacity-40 transition-colors">上一页</button>
          <span class="px-3 py-1 bg-admin-primary/10 text-admin-primary rounded font-medium">{{ bankCurrentPage }}</span>
          <button @click="loadBankData(bankCurrentPage + 1)" :disabled="bankCurrentPage >= bankTotalPages" class="px-3 py-1 border border-gray-200 rounded hover:bg-gray-50 disabled:opacity-40 transition-colors">下一页</button>
        </div>
      </div>
    </div>

    <div v-if="dialogVisible" class="fixed inset-0 z-40 bg-black/30 flex items-center justify-center p-4">
      <div class="w-full max-w-2xl bg-white rounded-xl shadow-xl border border-gray-100">
        <div class="px-5 py-4 border-b border-gray-100 flex items-center justify-between">
          <h3 class="text-base font-semibold text-admin-text">{{ dialogMode === 'create' ? '新增题目' : '编辑题目' }}</h3>
          <button @click="closeDialog" class="p-1.5 rounded hover:bg-gray-100 text-admin-muted">
            <X class="w-4 h-4" />
          </button>
        </div>
        <div class="p-5 space-y-4">
          <div>
            <label class="block text-xs text-admin-muted mb-1">标题</label>
            <input v-model="form.title" type="text" class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm outline-none focus:ring-2 focus:ring-admin-primary/20" />
          </div>

          <div class="grid grid-cols-1 md:grid-cols-4 gap-3">
            <div>
              <label class="block text-xs text-admin-muted mb-1">分类</label>
              <select v-model="form.category" class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm outline-none focus:ring-2 focus:ring-admin-primary/20">
                <option
                  v-for="item in normalizedCategoryOptions"
                  :key="item.code"
                  :value="item.code"
                >
                  {{ item.name }}
                </option>
              </select>
            </div>
            <div>
              <label class="block text-xs text-admin-muted mb-1">难度</label>
              <select v-model.number="form.difficulty" class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm outline-none focus:ring-2 focus:ring-admin-primary/20">
                <option :value="1">简单</option>
                <option :value="2">中等</option>
                <option :value="3">困难</option>
              </select>
            </div>
            <div>
              <label class="block text-xs text-admin-muted mb-1">状态</label>
              <select v-model.number="form.status" class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm outline-none focus:ring-2 focus:ring-admin-primary/20">
                <option :value="1">上架</option>
                <option :value="0">下架</option>
              </select>
            </div>
            <div>
              <label class="block text-xs text-admin-muted mb-1">浏览量</label>
              <input v-model.number="form.viewCount" type="number" min="0" class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm outline-none focus:ring-2 focus:ring-admin-primary/20" />
            </div>
          </div>

          <div>
            <label class="block text-xs text-admin-muted mb-1">标签(逗号分隔)</label>
            <input v-model="form.tags" type="text" class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm outline-none focus:ring-2 focus:ring-admin-primary/20" />
          </div>

          <div>
            <label class="block text-xs text-admin-muted mb-1">内容</label>
            <textarea v-model="form.content" rows="5" class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm outline-none focus:ring-2 focus:ring-admin-primary/20 resize-y"></textarea>
          </div>

          <div>
            <label class="block text-xs text-admin-muted mb-1">标准答案（数据库）</label>
            <textarea
              v-model="form.standardAnswer"
              rows="6"
              class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm outline-none focus:ring-2 focus:ring-admin-primary/20 resize-y"
              placeholder="请填写该题标准答案，题库 AI 回答将直接基于此字段。"
            ></textarea>
          </div>
        </div>
        <div class="px-5 py-4 border-t border-gray-100 flex justify-end gap-2">
          <button @click="closeDialog" class="px-3 py-2 rounded-lg border border-gray-200 text-sm hover:bg-gray-50">取消</button>
          <button @click="submitDialog" :disabled="submitting" class="px-3 py-2 rounded-lg bg-admin-primary text-white text-sm hover:bg-admin-primary/90 disabled:opacity-60">
            {{ submitting ? '提交中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="categoryDialogVisible" class="fixed inset-0 z-40 bg-black/30 flex items-center justify-center p-4">
      <div class="w-full max-w-md bg-white rounded-xl shadow-xl border border-gray-100">
        <div class="px-5 py-4 border-b border-gray-100 flex items-center justify-between">
          <h3 class="text-base font-semibold text-admin-text">新增题库分类</h3>
          <button @click="closeCategoryDialog" class="p-1.5 rounded hover:bg-gray-100 text-admin-muted">
            <X class="w-4 h-4" />
          </button>
        </div>
        <div class="p-5 space-y-4">
          <div>
            <label class="block text-xs text-admin-muted mb-1">分类名称</label>
            <input
              v-model="categoryForm.name"
              type="text"
              placeholder="例如：系统设计"
              class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm outline-none focus:ring-2 focus:ring-admin-primary/20"
            />
          </div>
          <div>
            <label class="block text-xs text-admin-muted mb-1">分类编码（可选）</label>
            <input
              v-model="categoryForm.code"
              type="text"
              placeholder="例如：system-design"
              class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm outline-none focus:ring-2 focus:ring-admin-primary/20"
            />
            <p class="mt-1 text-xs text-admin-muted">不填会根据名称自动生成。</p>
          </div>
        </div>
        <div class="px-5 py-4 border-t border-gray-100 flex justify-end gap-2">
          <button @click="closeCategoryDialog" class="px-3 py-2 rounded-lg border border-gray-200 text-sm hover:bg-gray-50">取消</button>
          <button
            @click="submitCategoryDialog"
            :disabled="categorySubmitting"
            class="px-3 py-2 rounded-lg bg-admin-primary text-white text-sm hover:bg-admin-primary/90 disabled:opacity-60"
          >
            {{ categorySubmitting ? '提交中...' : '保存分类' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="officialImportDialogVisible" class="fixed inset-0 z-40 bg-black/30 flex items-center justify-center p-4">
      <div class="w-full max-w-lg bg-white rounded-xl shadow-xl border border-gray-100">
        <div class="px-5 py-4 border-b border-gray-100 flex items-center justify-between">
          <h3 class="text-base font-semibold text-admin-text">导入官方题库</h3>
          <button @click="closeOfficialImportDialog" class="p-1.5 rounded hover:bg-gray-100 text-admin-muted">
            <X class="w-4 h-4" />
          </button>
        </div>
        <div class="p-5 space-y-4">
          <div>
            <label class="block text-xs text-admin-muted mb-1">题库文件（仅支持 TXT）</label>
            <input
              ref="officialFileInputRef"
              type="file"
              accept=".txt,text/plain"
              class="hidden"
              @change="handleOfficialFileChange"
            />
            <button
              @click="selectOfficialImportFile"
              type="button"
              class="inline-flex items-center gap-1.5 px-3 py-2 border border-gray-200 rounded-lg text-sm text-admin-text hover:bg-gray-50 transition-colors"
            >
              <Upload class="w-4 h-4" />
              选择文件
            </button>
            <p class="mt-2 text-xs text-admin-muted">{{ officialImportFile?.name || '未选择文件' }}</p>
          </div>

          <div>
            <label class="block text-xs text-admin-muted mb-1">题库名称（可选）</label>
            <input
              v-model="officialImportForm.name"
              type="text"
              placeholder="不填则使用文件名"
              class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm outline-none focus:ring-2 focus:ring-admin-primary/20"
            />
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div>
              <label class="block text-xs text-admin-muted mb-1">分类</label>
              <select
                v-model="officialImportForm.category"
                class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm outline-none focus:ring-2 focus:ring-admin-primary/20"
              >
                <option
                  v-for="item in normalizedCategoryOptions"
                  :key="item.code"
                  :value="item.code"
                >
                  {{ item.name }}
                </option>
              </select>
            </div>
            <div>
              <label class="block text-xs text-admin-muted mb-1">难度</label>
              <select
                v-model.number="officialImportForm.difficulty"
                class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm outline-none focus:ring-2 focus:ring-admin-primary/20"
              >
                <option :value="1">简单</option>
                <option :value="2">中等</option>
                <option :value="3">困难</option>
              </select>
            </div>
          </div>
        </div>
        <div class="px-5 py-4 border-t border-gray-100 flex justify-end gap-2">
          <button @click="closeOfficialImportDialog" class="px-3 py-2 rounded-lg border border-gray-200 text-sm hover:bg-gray-50">取消</button>
          <button
            @click="submitOfficialImport"
            :disabled="officialImportSubmitting"
            class="px-3 py-2 rounded-lg bg-admin-primary text-white text-sm hover:bg-admin-primary/90 disabled:opacity-60"
          >
            {{ officialImportSubmitting ? '导入中...' : '开始导入' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="bankPreviewVisible" class="fixed inset-0 z-40 bg-black/30 flex items-center justify-center p-4">
      <div class="w-full max-w-4xl bg-white rounded-xl shadow-xl border border-gray-100">
        <div class="px-5 py-4 border-b border-gray-100 flex items-center justify-between">
          <div>
            <h3 class="text-base font-semibold text-admin-text">{{ previewBank?.name || '题库预览' }}</h3>
            <p class="text-xs text-admin-muted mt-1">{{ previewBank?.originalFileName || '' }}</p>
          </div>
          <button @click="closeBankPreview" class="p-1.5 rounded hover:bg-gray-100 text-admin-muted">
            <X class="w-4 h-4" />
          </button>
        </div>
        <div class="p-5 space-y-4">
          <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
            <div class="rounded-lg bg-gray-50 px-3 py-2">
              <p class="text-xs text-admin-muted">上传用户</p>
              <p class="mt-1 text-sm text-admin-text">{{ previewBank?.ownerNickname || '-' }}</p>
            </div>
            <div class="rounded-lg bg-gray-50 px-3 py-2">
              <p class="text-xs text-admin-muted">分类</p>
              <p class="mt-1 text-sm text-admin-text">{{ categoryLabel(previewBank?.category) }}</p>
            </div>
            <div class="rounded-lg bg-gray-50 px-3 py-2">
              <p class="text-xs text-admin-muted">难度</p>
              <p class="mt-1 text-sm text-admin-text">{{ difficultyText(previewBank?.difficulty) }}</p>
            </div>
            <div class="rounded-lg bg-gray-50 px-3 py-2">
              <p class="text-xs text-admin-muted">解析题数</p>
              <p class="mt-1 text-sm text-admin-text">{{ previewBank?.questionCount || 0 }}</p>
            </div>
          </div>

          <div class="rounded-xl border border-gray-100 bg-gray-50 p-4 max-h-[420px] overflow-y-auto">
            <pre class="whitespace-pre-wrap text-sm leading-6 text-admin-text">{{ previewBank?.rawContent || '暂无可预览内容' }}</pre>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { Search, Plus, Pencil, Trash2, X, Eye, Check, XCircle, Upload } from 'lucide-vue-next'
import {
  getQuestionList,
  createQuestion,
  updateQuestion,
  deleteQuestion,
  getQuestionCategoryList,
  createQuestionCategory,
  deleteQuestionCategory,
  getCustomQuestionBankList,
  auditCustomQuestionBank,
  importOfficialQuestionBank,
} from '@/api/admin'

const loading = ref(true)
const error = ref(null)
const questions = ref([])
const keyword = ref('')
const categoryFilter = ref('')
const difficultyFilter = ref('')
const categoryOptions = ref([])
const currentPage = ref(1)
const total = ref(0)
const totalPages = ref(1)

const bankLoading = ref(true)
const bankError = ref(null)
const customBanks = ref([])
const bankKeyword = ref('')
const bankStatusFilter = ref('')
const bankCurrentPage = ref(1)
const bankTotal = ref(0)
const bankTotalPages = ref(1)
const bankPreviewVisible = ref(false)
const previewBank = ref(null)

const dialogVisible = ref(false)
const dialogMode = ref('create')
const editingId = ref(null)
const submitting = ref(false)
const categoryDialogVisible = ref(false)
const categorySubmitting = ref(false)
const categoryDeletingCode = ref('')
const officialImportDialogVisible = ref(false)
const officialImportSubmitting = ref(false)
const officialFileInputRef = ref(null)
const officialImportFile = ref(null)
const officialImportForm = reactive({
  name: '',
  category: '',
  difficulty: 2,
})
const categoryForm = reactive({
  name: '',
  code: '',
})
const form = reactive({
  title: '',
  content: '',
  standardAnswer: '',
  category: '',
  difficulty: 2,
  tags: '',
  status: 1,
  viewCount: 0,
})

const defaultCategoryMap = {
  java: 'Java 核心',
  spring: 'Spring 生态',
  db: '数据存储',
  redis: 'Redis',
  algo: '算法',
  network: '计算机网络',
  'system-design': '系统设计',
  arch: '架构设计',
  linux: 'Linux',
}
const categoryCodeAliasMap = {
  database: 'db',
  '数据库': 'db',
  algorithm: 'algo',
  '算法': 'algo',
  '计算机网络': 'network',
  'system-sign': 'system-design',
  'system_sign': 'system-design',
  'system design': 'system-design',
  '系统设计': 'system-design',
  architecture: 'arch',
  '架构': 'arch',
  '架构设计': 'arch',
}

const normalizeCategoryCode = (code) => {
  const text = String(code || '').trim()
  if (!text) return ''
  const compact = text.replace(/_/g, '-').replace(/\s+/g, '-')
  const lowered = compact.toLowerCase()
  return categoryCodeAliasMap[text] || categoryCodeAliasMap[lowered] || lowered
}

const getDefaultCategoryCode = () => normalizedCategoryOptions.value[0]?.code || 'java'

const normalizeCategoryName = (code, name) => {
  const key = normalizeCategoryCode(code)
  if (!key) return ''
  if (defaultCategoryMap[key]) {
    return defaultCategoryMap[key]
  }
  const text = String(name || '').trim()
  if (!text) return key
  if (text.includes('\uFFFD') || /^\?+$/.test(text)) {
    return key
  }
  return text
}

const normalizedCategoryOptions = computed(() => {
  const source = Array.isArray(categoryOptions.value) ? categoryOptions.value : []
  const rows = source.length
    ? source
    : Object.entries(defaultCategoryMap).map(([code, name]) => ({ code, name }))
  const deduped = rows
    .map((item) => {
      const code = normalizeCategoryCode(item?.code)
      const builtin = item?.builtin ?? Boolean(defaultCategoryMap[code])
      const questionCount = Number(item?.questionCount || 0)
      const bankCount = Number(item?.bankCount || 0)
      return {
        code,
        name: normalizeCategoryName(code, item?.name),
        builtin,
        questionCount,
        bankCount,
        deletable: item?.deletable ?? (!builtin && questionCount === 0 && bankCount === 0),
      }
    })
    .filter((item) => item.code && item.name)
  const seen = new Set()
  return deduped.filter((item) => {
    if (seen.has(item.code)) return false
    seen.add(item.code)
    return true
  })
})

const categoryLabel = (code) => {
  const key = normalizeCategoryCode(code)
  if (!key) return '-'
  if (defaultCategoryMap[key]) return defaultCategoryMap[key]
  const hit = normalizedCategoryOptions.value.find((item) => item.code === key)
  return hit?.name || key
}

const difficultyText = (difficulty) => ({ 1: '简单', 2: '中等', 3: '困难' }[difficulty] || '未知')
const bankAuditStatusText = (status) => {
  if (status === 0) return '待审核'
  if (status === 1) return '已通过'
  if (status === 2) return '已驳回'
  return '未知'
}
const bankAuditStatusClass = (status) => {
  if (status === 0) return 'bg-amber-100 text-amber-700'
  if (status === 1) return 'bg-emerald-100 text-emerald-700'
  if (status === 2) return 'bg-rose-100 text-rose-700'
  return 'bg-gray-100 text-gray-600'
}

const loadCategoryOptions = async () => {
  try {
    const res = await getQuestionCategoryList()
    if (res.code === 200) {
      const rows = Array.isArray(res.data) ? res.data : []
      categoryOptions.value = rows
        .map((item) => ({
          code: normalizeCategoryCode(item?.code),
          name: normalizeCategoryName(item?.code, item?.name),
          builtin: Boolean(item?.builtin),
          questionCount: Number(item?.questionCount || 0),
          bankCount: Number(item?.bankCount || 0),
          deletable: Boolean(item?.deletable),
        }))
        .filter((item) => item.code && item.name)
    } else {
      categoryOptions.value = []
    }
  } catch (e) {
    categoryOptions.value = []
  }

  if (!categoryOptions.value.length) {
    categoryOptions.value = Object.entries(defaultCategoryMap).map(([code, name]) => ({
      code,
      name,
      builtin: true,
      questionCount: 0,
      bankCount: 0,
      deletable: false,
    }))
  }
  if (!form.category || !normalizedCategoryOptions.value.some((item) => item.code === form.category)) {
    form.category = getDefaultCategoryCode()
  }
  if (!officialImportForm.category || !normalizedCategoryOptions.value.some((item) => item.code === officialImportForm.category)) {
    officialImportForm.category = getDefaultCategoryCode()
  }
}

const openCategoryDialog = () => {
  categoryForm.name = ''
  categoryForm.code = ''
  categoryDialogVisible.value = true
}

const closeCategoryDialog = () => {
  categoryDialogVisible.value = false
  categorySubmitting.value = false
  categoryDeletingCode.value = ''
}

const submitCategoryDialog = async () => {
  const name = categoryForm.name.trim()
  const code = categoryForm.code.trim()
  if (!name) {
    alert('分类名称不能为空')
    return
  }

  categorySubmitting.value = true
  try {
    const res = await createQuestionCategory({ name, code })
    if (res.code !== 200) {
      alert(res.msg || '新增分类失败')
      return
    }
    const savedCode = normalizeCategoryCode(res.data?.code)
    await loadCategoryOptions()
    if (savedCode) {
      form.category = savedCode
      categoryFilter.value = savedCode
      await loadData(1)
    }
    closeCategoryDialog()
  } catch (e) {
    alert(e.message || '新增分类失败')
  } finally {
    categorySubmitting.value = false
  }
}

const categoryUsageText = (item) => {
  const questionCount = Number(item?.questionCount || 0)
  const bankCount = Number(item?.bankCount || 0)
  if (questionCount > 0 || bankCount > 0) {
    return `题目 ${questionCount} / 题库 ${bankCount}`
  }
  return '未被使用'
}

const handleDeleteCategory = async (item) => {
  if (!item?.code) return
  if (!item.deletable) {
    if (item.builtin) {
      alert('内置分类不允许删除')
      return
    }
    alert('该分类仍被题目或题库使用，不能删除')
    return
  }
  if (!confirm(`确定删除分类“${item.name}”吗？`)) return

  categoryDeletingCode.value = item.code
  try {
    const res = await deleteQuestionCategory(item.code)
    if (res.code !== 200) {
      alert(res.msg || '删除分类失败')
      return
    }
    if (categoryFilter.value === item.code) {
      categoryFilter.value = ''
    }
    if (form.category === item.code) {
      form.category = getDefaultCategoryCode()
    }
    if (officialImportForm.category === item.code) {
      officialImportForm.category = getDefaultCategoryCode()
    }
    await loadCategoryOptions()
    await loadData(1)
  } catch (e) {
    alert(e.message || '删除分类失败')
  } finally {
    categoryDeletingCode.value = ''
  }
}

const resetOfficialImportForm = () => {
  officialImportForm.name = ''
  officialImportForm.category = getDefaultCategoryCode()
  officialImportForm.difficulty = 2
  officialImportFile.value = null
  if (officialFileInputRef.value) {
    officialFileInputRef.value.value = ''
  }
}

const openOfficialImportDialog = () => {
  resetOfficialImportForm()
  officialImportDialogVisible.value = true
}

const closeOfficialImportDialog = () => {
  officialImportDialogVisible.value = false
  officialImportSubmitting.value = false
}

const selectOfficialImportFile = () => {
  officialFileInputRef.value?.click()
}

const handleOfficialFileChange = (event) => {
  const [file] = event?.target?.files || []
  officialImportFile.value = file || null
}

const submitOfficialImport = async () => {
  if (!officialImportFile.value) {
    alert('请选择要导入的 TXT 文件')
    return
  }

  officialImportSubmitting.value = true
  try {
    const formData = new FormData()
    formData.append('file', officialImportFile.value)
    const trimmedName = officialImportForm.name.trim()
    if (trimmedName) {
      formData.append('name', trimmedName)
    }
    formData.append('category', officialImportForm.category || getDefaultCategoryCode())
    formData.append('difficulty', String(officialImportForm.difficulty || 2))

    const res = await importOfficialQuestionBank(formData)
    if (res.code === 200) {
      const count = Number(res.data?.importedQuestionCount || 0)
      closeOfficialImportDialog()
      await loadData(1)
      alert(`导入成功，共导入 ${count} 道官方题目`)
    } else {
      alert(res.msg || '导入失败')
    }
  } catch (e) {
    alert(e.message || '导入失败')
  } finally {
    officialImportSubmitting.value = false
  }
}

const resetForm = () => {
  form.title = ''
  form.content = ''
  form.standardAnswer = ''
  form.category = getDefaultCategoryCode()
  form.difficulty = 2
  form.tags = ''
  form.status = 1
  form.viewCount = 0
}

const openCreate = () => {
  dialogMode.value = 'create'
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

const openEdit = (question) => {
  dialogMode.value = 'edit'
  editingId.value = question.id
  form.title = question.title || ''
  form.content = question.content || ''
  form.standardAnswer = question.standardAnswer || ''
  const normalizedCategory = normalizeCategoryCode(question.category)
  form.category = normalizedCategory || getDefaultCategoryCode()
  form.difficulty = question.difficulty ?? 2
  form.tags = question.tags || ''
  form.status = question.status ?? 1
  form.viewCount = question.viewCount ?? 0
  dialogVisible.value = true
}

const closeDialog = () => {
  dialogVisible.value = false
  submitting.value = false
}

const submitDialog = async () => {
  if (!form.title.trim()) {
    alert('题目标题不能为空')
    return
  }
  if (!form.standardAnswer.trim()) {
    alert('标准答案不能为空')
    return
  }

  submitting.value = true
  try {
    const payload = {
      title: form.title.trim(),
      content: form.content,
      standardAnswer: form.standardAnswer.trim(),
      category: form.category,
      difficulty: form.difficulty,
      tags: form.tags,
      status: form.status,
      viewCount: Number.isFinite(form.viewCount) ? Math.max(0, form.viewCount) : 0,
    }

    const res = dialogMode.value === 'create'
      ? await createQuestion(payload)
      : await updateQuestion(editingId.value, payload)

    if (res.code === 200) {
      closeDialog()
      await loadData(dialogMode.value === 'create' ? 1 : currentPage.value)
    } else {
      alert(res.msg || '保存失败')
    }
  } catch (e) {
    alert(e.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (question) => {
  if (!confirm(`确定删除题目「${question.title}」吗？`)) return
  try {
    const res = await deleteQuestion(question.id)
    if (res.code === 200) {
      const nextPage = questions.value.length === 1 && currentPage.value > 1 ? currentPage.value - 1 : currentPage.value
      await loadData(nextPage)
    } else {
      alert(res.msg || '删除失败')
    }
  } catch (e) {
    alert(e.message || '删除失败')
  }
}

const openBankPreview = (bank) => {
  previewBank.value = bank
  bankPreviewVisible.value = true
}

const closeBankPreview = () => {
  previewBank.value = null
  bankPreviewVisible.value = false
}

const handleBankAudit = async (bank, targetStatus) => {
  const actionText = targetStatus === 1 ? '通过' : '驳回'
  let rejectReason = ''

  if (targetStatus === 2) {
    const input = window.prompt(`请输入“${bank.name}”的驳回原因`, bank.rejectReason || '')
    if (input === null) return
    rejectReason = input.trim()
    if (!rejectReason) {
      alert('驳回原因不能为空')
      return
    }
  }

  if (!confirm(`确认${actionText}题库《${bank.name}》吗？`)) return

  try {
    const res = await auditCustomQuestionBank(bank.id, {
      status: targetStatus,
      rejectReason,
    })
    if (res.code === 200) {
      if (bankPreviewVisible.value && previewBank.value?.id === bank.id) {
        previewBank.value = res.data
      }
      await loadBankData(bankCurrentPage.value)
      await loadData(currentPage.value)
    } else {
      alert(res.msg || '审核失败')
    }
  } catch (e) {
    alert(e.message || '审核失败')
  }
}

const loadData = async (page = 1) => {
  if (page < 1) return
  loading.value = true
  error.value = null
  try {
    const res = await getQuestionList(page, 10, categoryFilter.value, difficultyFilter.value, keyword.value)
    if (res.code === 200) {
      questions.value = res.data.records || []
      total.value = res.data.total || 0
      totalPages.value = res.data.pages || 1
      currentPage.value = page
    } else {
      error.value = res.msg || '加载失败'
    }
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

const loadBankData = async (page = 1) => {
  if (page < 1) return
  bankLoading.value = true
  bankError.value = null
  try {
    const res = await getCustomQuestionBankList(page, 10, bankStatusFilter.value, bankKeyword.value)
    if (res.code === 200) {
      customBanks.value = res.data.records || []
      bankTotal.value = res.data.total || 0
      bankTotalPages.value = Math.max(1, res.data.pages || 1)
      bankCurrentPage.value = page
    } else {
      bankError.value = res.msg || '加载失败'
    }
  } catch (e) {
    bankError.value = e.message
  } finally {
    bankLoading.value = false
  }
}

onMounted(async () => {
  await loadCategoryOptions()
  await loadData(1)
  await loadBankData(1)
})
</script>
