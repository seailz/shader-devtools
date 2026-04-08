package com.seailz.csdt.client.service;

import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.renderer.PostChainConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StrictJsonParser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public final class PostEffectVisualizationService {

    private PostEffectVisualizationService() {
    }

    public static Visualization visualize(String jsonText) {
        try {
            var json = StrictJsonParser.parse(new StringReader(jsonText));
            PostChainConfig config = PostChainConfig.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(JsonSyntaxException::new);

            List<SectionLine> lines = new ArrayList<>();
            lines.add(SectionLine.header("Internal Targets"));
            if (config.internalTargets().isEmpty()) {
                lines.add(SectionLine.detail("Uses only default frame targets"));
            } else {
                for (var entry : config.internalTargets().entrySet()) {
                    PostChainConfig.InternalTarget target = entry.getValue();
                    String size = "%sx%s".formatted(target.width().map(String::valueOf).orElse("screen"), target.height().map(String::valueOf).orElse("screen"));
                    String flags = (target.persistent() ? "persistent" : "transient") + " | clear 0x" + Integer.toHexString(target.clearColor()).toUpperCase();
                    lines.add(SectionLine.item(entry.getKey().toString()));
                    lines.add(SectionLine.detail(size + " | " + flags));
                }
            }

            lines.add(SectionLine.header("Passes"));
            if (config.passes().isEmpty()) {
                lines.add(SectionLine.detail("No passes"));
            } else {
                int passNumber = 1;
                for (PostChainConfig.Pass pass : config.passes()) {
                    lines.add(SectionLine.item("Pass " + passNumber + ": " + shortId(pass.fragmentShaderId())));
                    lines.add(SectionLine.detail("Vertex " + shortId(pass.vertexShaderId()) + " -> Output " + shortId(pass.outputTarget())));
                    if (pass.inputs().isEmpty()) {
                        lines.add(SectionLine.detail("Inputs: none"));
                    } else {
                        for (PostChainConfig.Input input : pass.inputs()) {
                            lines.add(SectionLine.detail(describeInput(input)));
                        }
                    }
                    if (pass.uniforms().isEmpty()) {
                        lines.add(SectionLine.detail("Uniforms: none"));
                    } else {
                        lines.add(SectionLine.detail("Uniforms: " + pass.uniforms().keySet()));
                    }
                    passNumber++;
                }
            }

            List<TargetInfo> targets = new ArrayList<>();
            for (var entry : config.internalTargets().entrySet()) {
                PostChainConfig.InternalTarget target = entry.getValue();
                String size = "%sx%s".formatted(target.width().map(String::valueOf).orElse("screen"), target.height().map(String::valueOf).orElse("screen"));
                String flags = (target.persistent() ? "persistent" : "transient") + " | clear 0x" + Integer.toHexString(target.clearColor()).toUpperCase();
                targets.add(new TargetInfo(shortId(entry.getKey()), size, flags));
            }

            List<PassInfo> passes = new ArrayList<>();
            int passNumber = 1;
            for (PostChainConfig.Pass pass : config.passes()) {
                List<String> inputs = new ArrayList<>();
                for (PostChainConfig.Input input : pass.inputs()) {
                    inputs.add(describeInput(input));
                }
                List<String> uniforms = pass.uniforms().isEmpty()
                        ? List.of("none")
                        : List.of("Uniforms: " + pass.uniforms().keySet());
                passes.add(new PassInfo(
                        passNumber++,
                        shortId(pass.fragmentShaderId()),
                        shortId(pass.vertexShaderId()),
                        shortId(pass.outputTarget()),
                        inputs,
                        uniforms
                ));
            }

            return new Visualization(true, lines, null, targets, passes);
        } catch (JsonParseException exception) {
            return new Visualization(false, List.of(
                    SectionLine.header("Post Effect"),
                    SectionLine.detail("Parse failed"),
                    SectionLine.detail(exception.getClass().getSimpleName() + ": " + exception.getMessage())
            ), exception.getMessage(), List.of(), List.of());
        }
    }

    private static String describeInput(PostChainConfig.Input input) {
        if (input instanceof PostChainConfig.TextureInput textureInput) {
            return "Texture " + input.samplerName() + " <- " + shortId(textureInput.location())
                    + " (" + textureInput.width() + "x" + textureInput.height()
                    + (textureInput.bilinear() ? ", bilinear)" : ", nearest)");
        }
        if (input instanceof PostChainConfig.TargetInput targetInput) {
            return "Target " + input.samplerName() + " <- " + shortId(targetInput.targetId())
                    + (targetInput.useDepthBuffer() ? " depth" : " color")
                    + (targetInput.bilinear() ? " bilinear" : " nearest");
        }
        return "Input " + input.samplerName() + " <- " + input.referencedTargets();
    }

    private static String shortId(Identifier id) {
        String value = id.toString();
        return value.startsWith("minecraft:") ? value.substring("minecraft:".length()) : value;
    }

    public record Visualization(boolean parsed, List<SectionLine> lines, String error, List<TargetInfo> targets, List<PassInfo> passes) {
    }

    public record TargetInfo(String name, String size, String flags) {
    }

    public record PassInfo(int index, String fragmentShader, String vertexShader, String outputTarget, List<String> inputs, List<String> uniforms) {
    }

    public record SectionLine(String text, int color) {
        private static SectionLine header(String text) {
            return new SectionLine(text, 0xFFFFD166);
        }

        private static SectionLine item(String text) {
            return new SectionLine(text, 0xFFFFFFFF);
        }

        private static SectionLine detail(String text) {
            return new SectionLine(text, 0xFFB8C7D9);
        }
    }
}
