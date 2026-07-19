package com.ecommerce.modules.product.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * AI Assistant 服务 HTTP 客户端
 * 调用 Python AI Assistant 的 /api/v1/tools/generate-tags 接口生成商品搜索标签。
 */
@Slf4j
@Component
public class AiAssistantClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String serviceToken;

    public AiAssistantClient(
            @Value("${ai-assistant.base-url:http://localhost:8000}") String baseUrl,
            @Value("${ai-assistant.service-token:ai-service-shared-secret-key}") String serviceToken) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.serviceToken = serviceToken;
    }

    /**
     * 根据商品标题和描述，调用 AI 生成搜索标签
     *
     * @param title       商品标题
     * @param description 商品描述
     * @return 逗号分隔的标签字符串，如 "手机,华为,5G,旗舰,电子产品"；
     *         调用失败时返回 null
     */
    @SuppressWarnings("unchecked")
    public String generateTags(String title, String description) {
        try {
            String url = baseUrl + "/api/v1/tools/generate-tags";

            Map<String, Object> requestBody = Map.of(
                    "title", title,
                    "description", description != null ? description : ""
            );

            Map<String, Object> response = restTemplate.postForObject(
                    url,
                    new org.springframework.http.HttpEntity<>(requestBody, createHeaders()),
                    Map.class
            );

            if (response != null && response.get("code") instanceof Number code && code.intValue() == 0) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null) {
                    String tags = (String) data.get("tags");
                    if (tags != null && !tags.isBlank()) {
                        log.info("ai_tags_generated title={} tags={}", title, tags);
                        return tags;
                    }
                }
            }

            log.warn("ai_tags_response_invalid title={} response={}", title, response);
        } catch (Exception e) {
            log.warn("ai_tags_call_failed title={} error={}", title, e.getMessage());
        }
        return null;
    }

    private org.springframework.http.HttpHeaders createHeaders() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("X-Service-Token", serviceToken);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return headers;
    }
}
