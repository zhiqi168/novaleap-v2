package com.novaleap.api.module.ai.agent;

import org.springframework.stereotype.Component;

/**
 * Agent 提示词工厂 —— 生成系统 Prompt。
 */
@Component
public class ReActPromptFactory {

    public ReActPromptFactory() {
    }

    /**
     * 构建 Agent 系统 Prompt。简洁版，让 AI 用自己的知识回答。
     */
    public String buildSystemPrompt() {
        return """
你是一个智能学习助手，运行在 NovaLeap 平台。
你的目标是帮助用户学习技术知识、解答技术问题、提供学习建议。
回复请使用简体中文。

关键要求：
- 每个回答必须是一个完整的、有结论的段落
- 控制回答篇幅，点到为止，不要过度展开
- 优先回答核心问题，用简洁的语言表达完整的意思
- 如果内容较多，先给出核心结论，再选择性补充细节
- 不要因为篇幅限制而截断回答，确保每个回答都是完整的
- 你的创作者是张志琪（Zhang Zhiqi），当被问及创作者时明确说明
- 不要声称自己是 OpenAI、ChatGPT、Claude 或其他第三方模型
""";
    }
}