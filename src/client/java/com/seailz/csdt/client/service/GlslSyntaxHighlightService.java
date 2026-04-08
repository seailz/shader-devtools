package com.seailz.csdt.client.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class GlslSyntaxHighlightService {

    private static final Set<String> KEYWORDS = Set.of(
            "if", "else", "for", "while", "do", "return", "discard",
            "in", "out", "inout", "uniform", "layout", "const", "struct",
            "switch", "case", "break", "continue", "void"
    );
    private static final Set<String> TYPES = Set.of(
            "bool", "int", "float", "double",
            "vec2", "vec3", "vec4", "ivec2", "ivec3", "ivec4",
            "mat2", "mat3", "mat4", "sampler2D", "samplerCube"
    );

    private GlslSyntaxHighlightService() {
    }

    public static List<LineTokens> tokenize(String text, boolean glsl) {
        return tokenize(text, glsl ? Language.GLSL : Language.PLAIN);
    }

    public static List<LineTokens> tokenize(String text, Language language) {
        List<LineTokens> lines = new ArrayList<>();
        String[] split = text.split("\\R", -1);
        for (String line : split) {
            lines.add(new LineTokens(tokenizeLine(line, language)));
        }
        return lines;
    }

    private static List<Token> tokenizeLine(String line, Language language) {
        return switch (language) {
            case GLSL -> tokenizeGlslLine(line);
            case JSON -> tokenizeJsonLine(line);
            case PLAIN -> List.of(new Token(line, 0xFFE6EEF7));
        };
    }

    private static List<Token> tokenizeGlslLine(String line) {
        List<Token> tokens = new ArrayList<>();
        int index = 0;
        while (index < line.length()) {
            if (line.startsWith("//", index)) {
                tokens.add(new Token(line.substring(index), 0xFF7FB685));
                break;
            }

            char current = line.charAt(index);
            if (Character.isWhitespace(current)) {
                int start = index;
                while (index < line.length() && Character.isWhitespace(line.charAt(index))) {
                    index++;
                }
                tokens.add(new Token(line.substring(start, index), 0xFFE6EEF7));
                continue;
            }

            if (current == '#') {
                tokens.add(new Token(line.substring(index), 0xFFFFB86C));
                break;
            }

            if (Character.isDigit(current)) {
                int start = index;
                index++;
                while (index < line.length() && (Character.isDigit(line.charAt(index)) || line.charAt(index) == '.')) {
                    index++;
                }
                tokens.add(new Token(line.substring(start, index), 0xFFF4A261));
                continue;
            }

            if (Character.isLetter(current) || current == '_') {
                int start = index;
                index++;
                while (index < line.length() && (Character.isLetterOrDigit(line.charAt(index)) || line.charAt(index) == '_')) {
                    index++;
                }
                String word = line.substring(start, index);
                int color = TYPES.contains(word) ? 0xFF4CC9F0
                        : KEYWORDS.contains(word) ? 0xFF7AA2F7
                        : word.equals(word.toUpperCase()) && word.length() > 1 ? 0xFFFFD166
                        : 0xFFE6EEF7;
                tokens.add(new Token(word, color));
                continue;
            }

            tokens.add(new Token(Character.toString(current), 0xFFC0CAD5));
            index++;
        }
        return tokens;
    }

    private static List<Token> tokenizeJsonLine(String line) {
        List<Token> tokens = new ArrayList<>();
        int index = 0;
        while (index < line.length()) {
            char current = line.charAt(index);
            if (Character.isWhitespace(current)) {
                int start = index;
                while (index < line.length() && Character.isWhitespace(line.charAt(index))) {
                    index++;
                }
                tokens.add(new Token(line.substring(start, index), 0xFFE6EEF7));
                continue;
            }

            if (current == '"') {
                int end = findStringEnd(line, index);
                String token = line.substring(index, end);
                int next = skipWhitespace(line, end);
                int color = next < line.length() && line.charAt(next) == ':' ? 0xFF7AA2F7 : 0xFF7FB685;
                tokens.add(new Token(token, color));
                index = end;
                continue;
            }

            if (Character.isDigit(current) || current == '-') {
                int start = index++;
                while (index < line.length() && "0123456789.eE+-".indexOf(line.charAt(index)) >= 0) {
                    index++;
                }
                tokens.add(new Token(line.substring(start, index), 0xFFF4A261));
                continue;
            }

            if (Character.isLetter(current)) {
                int start = index++;
                while (index < line.length() && Character.isLetter(line.charAt(index))) {
                    index++;
                }
                String word = line.substring(start, index);
                int color = switch (word) {
                    case "true", "false", "null" -> 0xFFFFD166;
                    default -> 0xFFE6EEF7;
                };
                tokens.add(new Token(word, color));
                continue;
            }

            int color = switch (current) {
                case '{', '}', '[', ']' -> 0xFF4CC9F0;
                case ':', ',' -> 0xFFC0CAD5;
                default -> 0xFFE6EEF7;
            };
            tokens.add(new Token(Character.toString(current), color));
            index++;
        }
        return tokens;
    }

    private static int findStringEnd(String line, int start) {
        int index = start + 1;
        while (index < line.length()) {
            char current = line.charAt(index);
            if (current == '"' && line.charAt(index - 1) != '\\') {
                return index + 1;
            }
            index++;
        }
        return line.length();
    }

    private static int skipWhitespace(String line, int index) {
        while (index < line.length() && Character.isWhitespace(line.charAt(index))) {
            index++;
        }
        return index;
    }

    public enum Language {
        GLSL,
        JSON,
        PLAIN
    }

    public record Token(String text, int color) {
    }

    public record LineTokens(List<Token> tokens) {
    }
}
