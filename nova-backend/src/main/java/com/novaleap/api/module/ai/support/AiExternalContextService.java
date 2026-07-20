package com.novaleap.api.module.ai.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AiExternalContextService {

    private static final Pattern WEATHER_CITY_PATTERN = Pattern.compile("([\\p{IsHan}A-Za-z路]{2,20})(?:今天|今日|明天|后天)?(?:天气|气温|温度|weather)");
    private static final Set<String> WEATHER_QUERY_HINTS = Set.of("天气", "气温", "温度", "降雨", "下雨", "weather");

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final boolean webEnabled;
    private final boolean weatherWebEnabled;
    private final boolean searchWebEnabled;
    private final int webTimeoutMs;

    /** 自定义搜索引擎 URL。配置后优先使用此地址，为空则使用 DuckDuckGo + Wikipedia 回退 */
    private final String searchUrl;

    public AiExternalContextService(
            ObjectMapper objectMapper,
            @Value("${nova.ai.web.enabled:true}") boolean webEnabled,
            @Value("${nova.ai.web.weather-enabled:true}") boolean weatherWebEnabled,
            @Value("${nova.ai.web.search-enabled:true}") boolean searchWebEnabled,
            @Value("${nova.ai.web.request-timeout-ms:2500}") int webTimeoutMs,
            @Value("${nova.ai.web.search-url:}") String searchUrl
    ) {
        this.objectMapper = objectMapper;
        this.webEnabled = webEnabled;
        this.weatherWebEnabled = weatherWebEnabled;
        this.searchWebEnabled = searchWebEnabled;
        this.webTimeoutMs = Math.max(800, Math.min(webTimeoutMs, 8_000));
        this.searchUrl = safe(searchUrl);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(this.webTimeoutMs))
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    public String buildExternalContext(String message) {
        if (!webEnabled) {
            return "";
        }
        String clean = safe(message);
        if (clean.isBlank()) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        if (searchWebEnabled) {
            String searchSummary = fetchGeneralSearchSummary(clean);
            if (StringUtils.hasText(searchSummary)) {
                parts.add(searchSummary);
            }
        }
        if (weatherWebEnabled && isWeatherQuery(clean)) {
            String weatherSummary = fetchWeatherSummary(extractWeatherLocation(clean));
            if (StringUtils.hasText(weatherSummary)) {
                parts.add(weatherSummary);
            }
        }
        if (parts.isEmpty()) {
            return "";
        }
        return String.join("\n", parts);
    }

    private boolean isWeatherQuery(String message) {
        String lower = safe(message).toLowerCase();
        for (String hint : WEATHER_QUERY_HINTS) {
            if (lower.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    private String extractWeatherLocation(String message) {
        String text = safe(message);
        if (text.isBlank()) {
            return "";
        }
        Matcher matcher = WEATHER_CITY_PATTERN.matcher(text);
        if (matcher.find()) {
            return safe(matcher.group(1));
        }
        return text;
    }

    private String fetchWeatherSummary(String location) {
        String city = safe(location);
        if (city.isBlank()) {
            return "";
        }
        try {
            String geocodeUrl = "https://geocoding-api.open-meteo.com/v1/search?count=1&language=zh&name="
                    + URLEncoder.encode(city, StandardCharsets.UTF_8);
            JsonNode geo = fetchJson(geocodeUrl);
            JsonNode results = geo.path("results");
            if (!results.isArray() || results.isEmpty()) {
                return "";
            }

            JsonNode first = results.get(0);
            double latitude = first.path("latitude").asDouble();
            double longitude = first.path("longitude").asDouble();
            String resolvedName = safe(first.path("name").asText(""));
            String country = safe(first.path("country").asText(""));

            String weatherUrl = "https://api.open-meteo.com/v1/forecast?current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m"
                    + "&timezone=Asia%2FShanghai"
                    + "&latitude=" + latitude
                    + "&longitude=" + longitude;
            JsonNode weather = fetchJson(weatherUrl);
            JsonNode current = weather.path("current");
            if (current.isMissingNode() || current.isNull()) {
                return "";
            }

            String cityLabel = resolvedName.isBlank() ? city : resolvedName;
            String countryPart = country.isBlank() ? "" : ("（" + country + "）");
            String time = safe(current.path("time").asText(""));
            double temperature = current.path("temperature_2m").asDouble(Double.NaN);
            double apparentTemperature = current.path("apparent_temperature").asDouble(Double.NaN);
            double humidity = current.path("relative_humidity_2m").asDouble(Double.NaN);
            double wind = current.path("wind_speed_10m").asDouble(Double.NaN);
            int weatherCode = current.path("weather_code").asInt(-1);

            return String.format(
                    "天气实时数据来源：Open-Meteo；城市：%s%s；观测时间：%s；天气：%s；气温：%.1f°C；体感：%.1f°C；湿度：%.0f%%；风速：%.1fkm/h。",
                    cityLabel,
                    countryPart,
                    time,
                    weatherCodeToText(weatherCode),
                    temperature,
                    apparentTemperature,
                    humidity,
                    wind
            );
        } catch (Exception e) {
            log.debug("fetch weather summary failed: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 使用自定义搜索引擎 URL 检索。searchUrl 模板中的 {query} 会被替换为实际搜索词。
     * 期望返回 JSON 格式，至少包含 results 数组，每项有 title、content、url 字段。
     * 兼容 SearXNG JSON 格式。
     */
    private String fetchCustomSearch(String query) {
        String url = searchUrl.replace("{query}", URLEncoder.encode(query, StandardCharsets.UTF_8));
        try {
            JsonNode node = fetchJson(url);
            // 兼容 SearXNG 格式: { results: [ { title:..., content:..., url:... } ] }
            JsonNode results = node.path("results");
            if (results.isArray() && !results.isEmpty()) {
                StringBuilder sb = new StringBuilder("联网检索来源：自定义搜索引擎。");
                int count = Math.min(5, results.size());
                for (int i = 0; i < count; i++) {
                    JsonNode item = results.get(i);
                    String title = safe(item.path("title").asText(""));
                    String content = safe(item.path("content").asText(""));
                    String resultUrl = safe(item.path("url").asText(""));
                    sb.append(" ").append(i + 1).append(") ").append(title);
                    if (!content.isBlank()) {
                        sb.append("：").append(content.length() > 150 ? content.substring(0, 150) + "..." : content);
                    }
                    if (!resultUrl.isBlank()) {
                        sb.append("（").append(resultUrl).append("）");
                    }
                    sb.append("。");
                }
                return sb.length() > 1200 ? sb.substring(0, 1200) : sb.toString();
            }
            // 兼容通用 JSON 格式: { items: [ { title:..., snippet:..., link:... } ] }
            JsonNode items = node.path("items");
            if (items.isArray() && !items.isEmpty()) {
                StringBuilder sb = new StringBuilder("联网检索来源：自定义搜索引擎。");
                int count = Math.min(5, items.size());
                for (int i = 0; i < count; i++) {
                    JsonNode item = items.get(i);
                    String title = safe(item.path("title").asText(""));
                    String snippet = safe(item.path("snippet").asText(""));
                    String link = safe(item.path("link").asText(""));
                    sb.append(" ").append(i + 1).append(") ").append(title);
                    if (!snippet.isBlank()) {
                        sb.append("：").append(snippet.length() > 150 ? snippet.substring(0, 150) + "..." : snippet);
                    }
                    if (!link.isBlank()) {
                        sb.append("（").append(link).append("）");
                    }
                    sb.append("。");
                }
                return sb.length() > 1200 ? sb.substring(0, 1200) : sb.toString();
            }
            return "";
        } catch (Exception e) {
            log.debug("custom search failed: {}", e.getMessage());
            return "";
        }
    }

    private String fetchGeneralSearchSummary(String query) {
        String q = safe(query);
        if (q.isBlank()) {
            return "";
        }

        // 如果配置了自定义搜索引擎 URL，优先使用
        if (!searchUrl.isBlank()) {
            String customResult = fetchCustomSearch(q);
            if (!customResult.isBlank()) {
                return customResult;
            }
        }

        // 默认走 DuckDuckGo + Wikipedia 回退
        try {
            String url = "https://api.duckduckgo.com/?format=json&no_html=1&no_redirect=1&q="
                    + URLEncoder.encode(q, StandardCharsets.UTF_8);
            JsonNode node = fetchJson(url);
            String heading = safe(node.path("Heading").asText(""));
            String abstractText = safe(node.path("AbstractText").asText(""));
            String abstractUrl = safe(node.path("AbstractURL").asText(""));

            List<String> bullet = new ArrayList<>();
            JsonNode relatedTopics = node.path("RelatedTopics");
            if (relatedTopics.isArray()) {
                for (JsonNode item : relatedTopics) {
                    if (bullet.size() >= 3) {
                        break;
                    }
                    if (item.hasNonNull("Text")) {
                        String text = safe(item.path("Text").asText(""));
                        if (!text.isBlank()) {
                            bullet.add(text);
                        }
                        continue;
                    }
                    JsonNode topics = item.path("Topics");
                    if (topics.isArray()) {
                        for (JsonNode child : topics) {
                            if (bullet.size() >= 3) {
                                break;
                            }
                            String text = safe(child.path("Text").asText(""));
                            if (!text.isBlank()) {
                                bullet.add(text);
                            }
                        }
                    }
                }
            }

            StringBuilder sb = new StringBuilder("联网检索来源：DuckDuckGo 即时检索。");
            if (!heading.isBlank()) {
                sb.append(" 标题：").append(heading).append("。");
            }
            if (!abstractText.isBlank()) {
                sb.append(" 摘要：").append(abstractText).append("。");
            }
            if (!bullet.isEmpty()) {
                sb.append(" 相关信息：");
                for (int i = 0; i < bullet.size(); i++) {
                    sb.append(i + 1).append(")").append(bullet.get(i));
                    if (i < bullet.size() - 1) {
                        sb.append("；");
                    }
                }
                sb.append("。");
            }
            if (!abstractUrl.isBlank()) {
                sb.append(" 参考链接：").append(abstractUrl).append("。");
            }
            String result = sb.toString();
            if ("联网检索来源：DuckDuckGo 即时检索。".equals(result)) {
                return fetchWikiOpenSearchSummary(q);
            }
            return result.length() > 900 ? result.substring(0, 900) : result;
        } catch (Exception e) {
            log.debug("fetch search summary failed: {}", e.getMessage());
            return fetchWikiOpenSearchSummary(q);
        }
    }

    private String fetchWikiOpenSearchSummary(String query) {
        String q = safe(query);
        if (q.isBlank()) {
            return "";
        }
        try {
            String url = "https://zh.wikipedia.org/w/api.php?action=opensearch&limit=3&namespace=0&format=json&search="
                    + URLEncoder.encode(q, StandardCharsets.UTF_8);
            JsonNode node = fetchJson(url);
            if (!node.isArray() || node.size() < 4) {
                return "";
            }

            JsonNode titles = node.get(1);
            JsonNode descriptions = node.get(2);
            JsonNode links = node.get(3);
            if (!titles.isArray() || titles.isEmpty()) {
                return "";
            }

            StringBuilder sb = new StringBuilder("联网检索来源：Wikipedia OpenSearch。");
            int count = Math.min(3, titles.size());
            for (int i = 0; i < count; i++) {
                String title = safe(titles.path(i).asText(""));
                if (title.isBlank()) {
                    continue;
                }
                String desc = descriptions != null && descriptions.isArray() ? safe(descriptions.path(i).asText("")) : "";
                String link = links != null && links.isArray() ? safe(links.path(i).asText("")) : "";
                sb.append(" ").append(i + 1).append(") ").append(title);
                if (!desc.isBlank()) {
                    sb.append("（").append(desc).append("）");
                }
                if (!link.isBlank()) {
                    sb.append("（").append(link).append("）");
                }
                sb.append("。");
            }
            String result = sb.toString();
            return result.length() > 900 ? result.substring(0, 900) : result;
        } catch (Exception e) {
            log.debug("fetch wiki open search failed: {}", e.getMessage());
            return "";
        }
    }

    private JsonNode fetchJson(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(webTimeoutMs))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("后端API服务请求异常: " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }

    private String weatherCodeToText(int code) {
        return switch (code) {
            case 0 -> "晴";
            case 1 -> "大部晴";
            case 2 -> "局部多云";
            case 3 -> "阴";
            case 45, 48 -> "雾";
            case 51, 53, 55, 56, 57 -> "毛毛雨";
            case 61, 63, 65, 66, 67, 80, 81, 82 -> "雨";
            case 71, 73, 75, 77, 85, 86 -> "雪";
            case 95, 96, 99 -> "雷暴";
            default -> "未知";
        };
    }

    /**
     * 对外公开：联网搜索。Agent 工具通过此方法调用，而非基于启发式匹配。
     */
    public String searchWeb(String query) {
        if (!webEnabled || !searchWebEnabled) {
            return "";
        }
        return fetchGeneralSearchSummary(query);
    }

    /**
     * 对外公开：查询天气。Agent 工具通过此方法调用，而非基于启发式匹配。
     */
    public String getWeather(String location) {
        if (!webEnabled || !weatherWebEnabled) {
            return "";
        }
        return fetchWeatherSummary(location);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
