package com.seailz.csdt.client.service;

import com.mojang.logging.LogUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class McpControlServerService {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 34783;

    private static HttpServer server;

    private McpControlServerService() {
    }

    public static synchronized void start() {
        if (server != null) {
            return;
        }

        String host = System.getProperty("csdt.control.host", DEFAULT_HOST);
        int port = Integer.getInteger("csdt.control.port", DEFAULT_PORT);
        try {
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(host, port), 0);
            httpServer.createContext("/health", exchange -> handle(exchange, McpControlServerService::handleHealth));
            httpServer.createContext("/refresh-shaders", exchange -> handle(exchange, McpControlServerService::handleRefreshShaders));
            httpServer.createContext("/screenshot", exchange -> handle(exchange, McpControlServerService::handleScreenshot));
            httpServer.createContext("/lightmap-readback", exchange -> handle(exchange, McpControlServerService::handleLightmapReadback));
            httpServer.createContext("/atlas-sprite", exchange -> handle(exchange, McpControlServerService::handleAtlasSprite));
            httpServer.createContext("/atlas-bank-readback", exchange -> handle(exchange, McpControlServerService::handleAtlasBankReadback));
            httpServer.createContext("/fog-frame-history", exchange -> handle(exchange, McpControlServerService::handleFogFrameHistory));
            httpServer.createContext("/forced-post-effect", exchange -> handle(exchange, McpControlServerService::handleForcedPostEffect));
            httpServer.setExecutor(Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "Shader DevTools MCP control");
                thread.setDaemon(true);
                return thread;
            }));
            httpServer.start();
            server = httpServer;
            LOGGER.info("Shader DevTools MCP control server listening on http://{}:{}", host, port);
        } catch (Exception exception) {
            LOGGER.error("Failed to start Shader DevTools MCP control server", exception);
        }
    }

    private static void handle(HttpExchange exchange, Handler handler) throws IOException {
        try {
            handler.handle(exchange);
        } catch (Exception exception) {
            LOGGER.error("Shader DevTools MCP control request failed", exception);
            sendJson(exchange, 500, "{\"ok\":false,\"error\":\"" + jsonEscape(exception.toString()) + "\"}");
        } finally {
            exchange.close();
        }
    }

    private static void handleHealth(HttpExchange exchange) throws IOException {
        if (!acceptsGetOrPost(exchange)) {
            sendMethodNotAllowed(exchange);
            return;
        }

        sendJson(exchange, 200, "{\"ok\":true}");
    }

    private static void handleRefreshShaders(HttpExchange exchange) throws IOException {
        if (!acceptsGetOrPost(exchange)) {
            sendMethodNotAllowed(exchange);
            return;
        }

        ShaderReloadService.reloadAllShaders();
        sendJson(exchange, 202, "{\"ok\":true,\"status\":\"queued\"}");
    }

    private static void handleScreenshot(HttpExchange exchange) throws IOException {
        if (!acceptsGetOrPost(exchange)) {
            sendMethodNotAllowed(exchange);
            return;
        }

        try {
            Path path = ScreenshotCaptureService.captureScreenshot().get(30, TimeUnit.SECONDS);
            sendJson(exchange, 200, "{\"ok\":true,\"path\":\"" + jsonEscape(path.toString()) + "\"}");
        } catch (TimeoutException exception) {
            LOGGER.error("Timed out while taking screenshot for MCP request", exception);
            sendJson(exchange, 504, "{\"ok\":false,\"error\":\"Timed out while taking screenshot\"}");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.error("Interrupted while taking screenshot for MCP request", exception);
            sendJson(exchange, 500, "{\"ok\":false,\"error\":\"Interrupted while taking screenshot\"}");
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            LOGGER.error("Failed to take screenshot for MCP request", cause);
            sendJson(exchange, 500, "{\"ok\":false,\"error\":\"" + jsonEscape(cause.toString()) + "\"}");
        } catch (Exception exception) {
            LOGGER.error("Failed to take screenshot for MCP request", exception);
            sendJson(exchange, 500, "{\"ok\":false,\"error\":\"" + jsonEscape(exception.toString()) + "\"}");
        }
    }

    private static void handleLightmapReadback(HttpExchange exchange) throws Exception {
        if (!acceptsGetOrPost(exchange)) {
            sendMethodNotAllowed(exchange);
            return;
        }

        SamplerInspectionService.ReadbackResult result =
                LightmapReadbackService.capture().get(5, TimeUnit.SECONDS);
        sendJson(exchange, 200,
                "{\"ok\":true,\"dump\":\""
                        + jsonEscape(result.dumpText()) + "\"}");
    }

    private static void handleAtlasSprite(HttpExchange exchange) throws Exception {
        if (!acceptsGetOrPost(exchange)) {
            sendMethodNotAllowed(exchange);
            return;
        }

        String sprite = "minecraft:missingno";
        String query = exchange.getRequestURI().getRawQuery();
        if (query != null) {
            for (String pair : query.split("&")) {
                String[] parts = pair.split("=", 2);
                String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                String value = parts.length == 2
                        ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                        : "";
                if ("sprite".equals(key) && !value.isBlank()) {
                    sprite = value;
                }
            }
        }

        AtlasInspectionService.Result result = AtlasInspectionService
                .inspectBlockSprite(sprite).get(5, TimeUnit.SECONDS);
        sendJson(exchange, 200,
                "{\"ok\":true,\"id\":\"" + jsonEscape(result.id())
                        + "\",\"atlasWidth\":" + result.atlasWidth()
                        + ",\"atlasHeight\":" + result.atlasHeight()
                        + ",\"x\":" + result.x()
                        + ",\"y\":" + result.y()
                        + ",\"width\":" + result.width()
                        + ",\"height\":" + result.height()
                        + ",\"u0\":" + result.u0()
                        + ",\"v0\":" + result.v0()
                        + ",\"u1\":" + result.u1()
                        + ",\"v1\":" + result.v1()
                        + ",\"animated\":" + result.animated() + "}");
    }

    private static void handleAtlasBankReadback(HttpExchange exchange) throws Exception {
        if (!acceptsGetOrPost(exchange)) {
            sendMethodNotAllowed(exchange);
            return;
        }

        SamplerInspectionService.ReadbackResult result =
                AtlasBankReadbackService.capture().get(5, TimeUnit.SECONDS);
        sendJson(exchange, 200,
                "{\"ok\":true,\"dump\":\""
                        + jsonEscape(result.dumpText()) + "\"}");
    }

    private static void handleFogFrameHistory(HttpExchange exchange) throws IOException {
        if (!acceptsGetOrPost(exchange)) {
            sendMethodNotAllowed(exchange);
            return;
        }

        List<FogFrameInspectionService.Sample> samples =
                FogFrameInspectionService.snapshot();
        StringBuilder json = new StringBuilder("{\"ok\":true,\"samples\":[");
        for (int index = 0; index < samples.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            FogFrameInspectionService.Sample sample = samples.get(index);
            json.append("{\"sequence\":").append(sample.sequence())
                    .append(",\"nanoTime\":").append(sample.nanoTime())
                    .append(",\"environmentalStart\":")
                    .append(Float.toString(sample.environmentalStart()))
                    .append(",\"environmentalEnd\":")
                    .append(Float.toString(sample.environmentalEnd()))
                    .append(",\"renderDistanceStart\":")
                    .append(Float.toString(sample.renderDistanceStart()))
                    .append(",\"renderDistanceEnd\":")
                    .append(Float.toString(sample.renderDistanceEnd()))
                    .append('}');
        }
        json.append("]}");
        sendJson(exchange, 200, json.toString());
    }

    private static void handleForcedPostEffect(HttpExchange exchange) throws IOException {
        if (!acceptsGetOrPost(exchange)) {
            sendMethodNotAllowed(exchange);
            return;
        }

        String resource = null;
        boolean enabled = true;
        String query = exchange.getRequestURI().getRawQuery();
        if (query != null) {
            for (String pair : query.split("&")) {
                String[] parts = pair.split("=", 2);
                String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                String value = parts.length == 2
                        ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                        : "";
                if ("resource".equals(key)) {
                    resource = value;
                } else if ("enabled".equals(key)) {
                    enabled = Boolean.parseBoolean(value);
                }
            }
        }

        if (enabled && (resource == null || resource.isBlank())) {
            sendJson(exchange, 400,
                    "{\"ok\":false,\"error\":\"Missing resource query parameter\"}");
            return;
        }

        ForcedPostEffectService.setForcedPostEffect(resource, enabled);
        sendJson(exchange, 200, "{\"ok\":true,\"enabled\":" + enabled + "}");
    }

    private static boolean acceptsGetOrPost(HttpExchange exchange) {
        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        return "GET".equals(method) || "POST".equals(method);
    }

    private static void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Allow", "GET, POST");
        sendJson(exchange, 405, "{\"ok\":false,\"error\":\"Method not allowed\"}");
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream body = exchange.getResponseBody()) {
            body.write(bytes);
        }
    }

    private static String jsonEscape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    @FunctionalInterface
    private interface Handler {
        void handle(HttpExchange exchange) throws Exception;
    }
}
