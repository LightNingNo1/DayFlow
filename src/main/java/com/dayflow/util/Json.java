package com.dayflow.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 极简 JSON 解析器，覆盖 Canvas LMS API 返回的扁平结构（数组、对象、
 * 字符串、数字、null、布尔值）。不做严格错误恢复，仅满足本应用需求。
 *
 * <p>线程安全；每个 parse 返回全新对象。
 */
public final class Json {

    private final String src;
    private int pos;

    private Json(String src) {
        this.src = src;
        this.pos = 0;
    }

    public static Object parse(String text) {
        return new Json(text).parseValue();
    }

    /** 把 JSON 解析结果安全地转换成 List，失败返回空列表。 */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> asList(Object obj) {
        if (obj instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    result.add((Map<String, Object>) map);
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asMap(Object obj) {
        if (obj instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return new HashMap<>();
    }

    private Object parseValue() {
        skipWs();

        if (pos >= src.length()) {
            return null;
        }

        char c = src.charAt(pos);

        return switch (c) {
            case '{' -> parseObject();
            case '[' -> parseArray();
            case '"' -> parseString();
            case 't', 'f' -> parseBool();
            case 'n' -> parseNull();
            default -> parseNumber();
        };
    }

    private Map<String, Object> parseObject() {
        Map<String, Object> obj = new HashMap<>();
        pos++; // 跳过 {
        skipWs();

        if (pos < src.length() && src.charAt(pos) == '}') {
            pos++;
            return obj;
        }

        while (pos < src.length()) {
            skipWs();
            String key = parseString();
            skipWs();

            if (pos < src.length() && src.charAt(pos) == ':') {
                pos++;
            }

            Object value = parseValue();
            obj.put(key, value);

            skipWs();
            if (pos < src.length() && src.charAt(pos) == ',') {
                pos++;
                continue;
            }
            if (pos < src.length() && src.charAt(pos) == '}') {
                pos++;
                return obj;
            }
        }
        return obj;
    }

    private List<Object> parseArray() {
        List<Object> list = new ArrayList<>();
        pos++; // 跳过 [
        skipWs();

        if (pos < src.length() && src.charAt(pos) == ']') {
            pos++;
            return list;
        }

        while (pos < src.length()) {
            list.add(parseValue());
            skipWs();
            if (pos < src.length() && src.charAt(pos) == ',') {
                pos++;
                continue;
            }
            if (pos < src.length() && src.charAt(pos) == ']') {
                pos++;
                return list;
            }
        }
        return list;
    }

    private String parseString() {
        StringBuilder sb = new StringBuilder();
        pos++; // 跳过起始 "

        while (pos < src.length()) {
            char c = src.charAt(pos++);

            if (c == '"') {
                return sb.toString();
            }

            if (c == '\\' && pos < src.length()) {
                char escaped = src.charAt(pos++);
                switch (escaped) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (pos + 4 <= src.length()) {
                            int code = Integer.parseInt(
                                    src.substring(pos, pos + 4), 16);
                            sb.append((char) code);
                            pos += 4;
                        }
                    }
                    default -> sb.append(escaped);
                }
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    private Number parseNumber() {
        int start = pos;
        if (pos < src.length() && src.charAt(pos) == '-') {
            pos++;
        }
        while (pos < src.length()
                && Character.isDigit(src.charAt(pos))) {
            pos++;
        }

        boolean isFloat = false;

        if (pos < src.length() && src.charAt(pos) == '.') {
            isFloat = true;
            pos++;
            while (pos < src.length()
                    && Character.isDigit(src.charAt(pos))) {
                pos++;
            }
        }

        if (pos < src.length()
                && (src.charAt(pos) == 'e' || src.charAt(pos) == 'E')) {
            isFloat = true;
            pos++;
            if (pos < src.length()
                    && (src.charAt(pos) == '+' || src.charAt(pos) == '-')) {
                pos++;
            }
            while (pos < src.length()
                    && Character.isDigit(src.charAt(pos))) {
                pos++;
            }
        }

        String text = src.substring(start, pos);
        return isFloat
                ? Double.parseDouble(text)
                : Long.parseLong(text);
    }

    private Boolean parseBool() {
        if (src.startsWith("true", pos)) {
            pos += 4;
            return Boolean.TRUE;
        }
        if (src.startsWith("false", pos)) {
            pos += 5;
            return Boolean.FALSE;
        }
        pos++;
        return Boolean.FALSE;
    }

    private Object parseNull() {
        if (src.startsWith("null", pos)) {
            pos += 4;
        } else {
            pos++;
        }
        return null;
    }

    private void skipWs() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
            pos++;
        }
    }
}
