package com.seailz.csdt.client.service;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ShaderInventoryService {

    private ShaderInventoryService() {
    }

    public static List<ShaderResourceEntry> listCoreShaders() {
        return listShaders(ShaderCategory.CORE);
    }

    public static List<ShaderResourceEntry> listPostShaders() {
        return listShaders(ShaderCategory.POST);
    }

    private static List<ShaderResourceEntry> listShaders(ShaderCategory category) {
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        Map<Identifier, List<Resource>> stacks = category.collect(resourceManager);
        return stacks.entrySet().stream()
                .map(entry -> toEntry(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(ShaderResourceEntry::overridden).reversed().thenComparing(ShaderResourceEntry::path))
                .toList();
    }

    private static ShaderResourceEntry toEntry(Identifier id, List<Resource> stack) {
        List<ShaderResourceVersion> versions = stack.stream().map(resource -> toVersion(id, resource)).toList();
        List<String> packs = versions.stream().map(ShaderResourceVersion::packId).distinct().toList();
        String activePack = ShaderResourceOverrideService.selectedPack(id.toString());
        if (activePack == null) {
            activePack = packs.isEmpty() ? "<unknown>" : packs.getLast();
        }
        return new ShaderResourceEntry(id.toString(), activePack, packs, packs.size() > 1, versions);
    }

    private static ShaderResourceVersion toVersion(Identifier id, Resource resource) {
        Path path = resolvePath(resource.source(), id);
        boolean editable = path != null && Files.exists(path) && Files.isRegularFile(path) && Files.isWritable(path);
        String displaySource = path == null ? resource.sourcePackId() : path.toString();
        return new ShaderResourceVersion(resource.sourcePackId(), displaySource, path, editable, resource);
    }

    public static String loadText(ShaderResourceVersion version) throws IOException {
        try (var reader = version.resource().openAsReader()) {
            return reader.lines().collect(java.util.stream.Collectors.joining("\n"));
        }
    }

    public static void saveText(ShaderResourceVersion version, String contents) throws IOException {
        if (!version.editable() || version.path() == null) {
            throw new IOException("This resource is read only");
        }
        Files.writeString(version.path(), contents);
    }

    public static void deleteText(ShaderResourceVersion version) throws IOException {
        if (!version.editable() || version.path() == null) {
            throw new IOException("This resource is read only");
        }
        Files.deleteIfExists(version.path());
    }

    public static List<ShaderResourceVersion> listVersionsForLocation(ResourceManager resourceManager, Identifier location) {
        String root = location.getPath().startsWith("post_effect/") ? "post_effect" : "shaders";
        return resourceManager.listResourceStacks(root, id -> id.equals(location))
                .getOrDefault(location, List.of())
                .stream()
                .map(resource -> toVersion(location, resource))
                .collect(Collectors.toList());
    }

    public static @Nullable ShaderResourceEntry findEntry(String resourcePath) {
        Identifier location = Identifier.parse(resourcePath);
        List<ShaderResourceVersion> versions = listVersionsForLocation(Minecraft.getInstance().getResourceManager(), location);
        if (versions.isEmpty()) {
            return null;
        }

        List<String> packs = versions.stream().map(ShaderResourceVersion::packId).distinct().toList();
        String activePack = ShaderResourceOverrideService.selectedPack(resourcePath);
        if (activePack == null) {
            activePack = packs.isEmpty() ? "<unknown>" : packs.getLast();
        }
        return new ShaderResourceEntry(resourcePath, activePack, packs, packs.size() > 1, versions);
    }

    public static OverrideCreationResult createOverrideInPreferredPack(String resourcePath, String contents) throws IOException {
        Identifier location = Identifier.parse(resourcePath);
        OverrideTarget target = findPreferredOverrideTarget();
        if (target == null) {
            throw new IOException("No writable directory resource pack is selected");
        }

        Path output = target.root()
                .resolve("assets")
                .resolve(location.getNamespace())
                .resolve(location.getPath());
        Files.createDirectories(output.getParent());
        Files.writeString(output, contents);
        return new OverrideCreationResult(target.packId(), output);
    }

    public static String displayPath(String resourcePath) {
        Identifier location = Identifier.parse(resourcePath);
        String path = location.getPath();
        if (path.startsWith("shaders/")) {
            return path.substring("shaders/".length());
        }
        return path;
    }

    public static @Nullable ShaderResourceEntry findPipelineShaderEntry(Object shaderReference, boolean fragment) {
        String raw = String.valueOf(shaderReference);
        String namespace = "minecraft";
        String path = raw;
        int separator = raw.indexOf(':');
        if (separator >= 0) {
            namespace = raw.substring(0, separator);
            path = raw.substring(separator + 1);
        }

        String extension = fragment ? ".fsh" : ".vsh";
        String withExtension = path.endsWith(extension) ? path : path + extension;
        List<String> candidates = new ArrayList<>();
        if (path.startsWith("shaders/")) {
            candidates.add(namespace + ":" + withExtension);
        } else {
            candidates.add(namespace + ":shaders/core/" + withExtension);
            candidates.add(namespace + ":shaders/post/" + withExtension);
            candidates.add(namespace + ":shaders/" + withExtension);
            candidates.add(namespace + ":" + withExtension);
        }
        if (path.startsWith("core/") || path.startsWith("post/") || path.startsWith("include/")) {
            candidates.add(namespace + ":shaders/" + withExtension);
        }

        return candidates.stream()
                .distinct()
                .map(ShaderInventoryService::findEntry)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private static @Nullable Path resolvePath(PackResources source, Identifier id) {
        Path root = resolvePackRoot(source);
        return root == null ? null : root.resolve("assets").resolve(id.getNamespace()).resolve(id.getPath());
    }

    private static @Nullable Path resolvePackRoot(PackResources source) {
        if (source instanceof PathPackResources pathPackResources) {
            return resolvePathPackRoot(pathPackResources);
        }

        try {
            Field rootField = source.getClass().getDeclaredField("root");
            rootField.setAccessible(true);
            Object value = rootField.get(source);
            if (value instanceof Path rootPath) {
                return rootPath;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return null;
    }

    private static @Nullable Path resolvePathPackRoot(PathPackResources packResources) {
        try {
            Field rootField = PathPackResources.class.getDeclaredField("root");
            rootField.setAccessible(true);
            return (Path) rootField.get(packResources);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static @Nullable OverrideTarget findPreferredOverrideTarget() {
        PackRepository repository = Minecraft.getInstance().getResourcePackRepository();
        OverrideTarget preferred = null;
        for (Pack pack : repository.getSelectedPacks()) {
            try (PackResources resources = pack.open()) {
                Path root = resolvePackRoot(resources);
                if (root != null && Files.isDirectory(root) && Files.isWritable(root)) {
                    preferred = new OverrideTarget(pack.getId(), root);
                }
            } catch (Exception ignored) {
            }
        }
        return preferred;
    }

    public enum ShaderCategory {
        CORE {
            @Override
            Map<Identifier, List<Resource>> collect(ResourceManager resourceManager) {
                return resourceManager.listResourceStacks("shaders", id -> matches(id.getPath()));
            }

            @Override
            boolean matches(String path) {
                return path.startsWith("shaders/core/") || path.startsWith("shaders/include/");
            }
        },
        POST {
            @Override
            Map<Identifier, List<Resource>> collect(ResourceManager resourceManager) {
                Map<Identifier, List<Resource>> resources = new java.util.HashMap<>();
                resources.putAll(resourceManager.listResourceStacks("post_effect", id -> matches(id.getPath())));
                resources.putAll(resourceManager.listResourceStacks("shaders", id -> matches(id.getPath())));
                return resources;
            }

            @Override
            boolean matches(String path) {
                return path.startsWith("post_effect/") || path.startsWith("shaders/post/");
            }
        };

        abstract Map<Identifier, List<Resource>> collect(ResourceManager resourceManager);
        abstract boolean matches(String path);
    }

    public record ShaderResourceEntry(String path, String activePack, List<String> packStack, boolean overridden, List<ShaderResourceVersion> versions) {
    }

    public record ShaderResourceVersion(String packId, String displaySource, @Nullable Path path, boolean editable, Resource resource) {
    }

    public record OverrideCreationResult(String packId, Path path) {
    }

    private record OverrideTarget(String packId, Path root) {
    }
}
