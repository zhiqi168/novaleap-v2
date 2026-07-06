package com.novaleap.api.module.ai.config;

import com.novaleap.api.module.ai.support.INovaLeapAiService;
import com.novaleap.api.service.AiLimitService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 程序化构建 Spring AI 的底层基础设施 Bean。
 * <p>
 * 不再依赖 {@code spring.ai.openai.*} 自动配置，而是通过 {@link NovaLeapAiProperties}
 * 读取自定义配置（{@code novaleap.ai.*}）来构建 {@link OpenAiApi}、{@link OpenAiChatModel} 和 {@link ChatClient}。
 * <br>
 * 切换模型服务商只需修改 {@code application.yml} 中的 {@code novaleap.ai.base-url} 和 {@code novaleap.ai.model-name}。
 */
@Configuration
public class AiChatClientConfig {

    @Bean
    public OpenAiApi openAiApi(NovaLeapAiProperties props) {
        return new OpenAiApi(props.getBaseUrl(), props.getApiKey());
    }

    @Bean
    public OpenAiChatModel openAiChatModel(OpenAiApi openAiApi, NovaLeapAiProperties props) {
        return new OpenAiChatModel(openAiApi,
                OpenAiChatOptions.builder()
                        .withModel(props.getModelName())
                        .withMaxTokens(props.getMaxTokens())
                        .build());
    }

    @Bean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel).build();
    }
}