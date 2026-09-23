package com.nishakant.jsonspecjava;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class JsonSpecToJavaGenerator {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public Map<String, String> generateSources(String specJson) throws IOException {
        JsonNode spec = OBJECT_MAPPER.readTree(specJson);
        return generateSources(spec);
    }

    public Map<String, String> generateSources(JsonNode spec) {
        String packageName = sanitizePackageName(readText(spec, "package", "generated"));
        String rootClassName = toClassName(readText(spec, "className", readText(spec, "title", "GeneratedModel")));

        Map<String, String> sources = new LinkedHashMap<>();
        processClass(rootClassName, propertyNode(spec), packageName, sources);
        return sources;
    }

    private void processClass(String className, JsonNode properties, String packageName, Map<String, String> sources) {
        if (sources.containsKey(className)) {
            return;
        }

        List<FieldSpec> fields = new ArrayList<>();
        Set<String> imports = new LinkedHashSet<>();

        if (properties != null && properties.isObject()) {
            properties.fields().forEachRemaining(entry -> {
                String fieldName = toFieldName(entry.getKey());
                JsonNode fieldSpec = entry.getValue();
                FieldType fieldType = resolveType(entry.getKey(), fieldSpec, packageName, sources, imports);
                fields.add(new FieldSpec(fieldName, fieldType.typeName));
            });
        }

        sources.put(className, renderClass(packageName, className, fields, imports));
    }

    private FieldType resolveType(String propertyName,
                                  JsonNode fieldSpec,
                                  String packageName,
                                  Map<String, String> sources,
                                  Set<String> imports) {
        String declaredType = readText(fieldSpec, "type", "object").toLowerCase(Locale.ROOT);

        return switch (declaredType) {
            case "string" -> new FieldType("String");
            case "integer" -> new FieldType("Integer");
            case "number" -> new FieldType("Double");
            case "boolean" -> new FieldType("Boolean");
            case "array" -> {
                imports.add("java.util.List");
                JsonNode items = fieldSpec.path("items");
                FieldType itemType = resolveArrayItemType(propertyName, items, packageName, sources, imports);
                yield new FieldType("List<" + itemType.typeName + ">", true);
            }
            case "object" -> {
                JsonNode nestedProperties = propertyNode(fieldSpec);
                if (nestedProperties != null && nestedProperties.isObject()) {
                    String nestedClassName = toClassName(propertyName);
                    processClass(nestedClassName, nestedProperties, packageName, sources);
                    yield new FieldType(nestedClassName);
                }
                yield new FieldType("Object");
            }
            default -> new FieldType("Object");
        };
    }

    private FieldType resolveArrayItemType(String propertyName,
                                           JsonNode itemSpec,
                                           String packageName,
                                           Map<String, String> sources,
                                           Set<String> imports) {
        if (itemSpec == null || itemSpec.isMissingNode()) {
            return new FieldType("Object");
        }

        String itemType = readText(itemSpec, "type", "object").toLowerCase(Locale.ROOT);
        if ("object".equals(itemType) && propertyNode(itemSpec) != null) {
            String nestedName = toClassName(singularize(propertyName));
            processClass(nestedName, propertyNode(itemSpec), packageName, sources);
            return new FieldType(nestedName);
        }

        return resolveType(propertyName, itemSpec, packageName, sources, imports);
    }

    private static JsonNode propertyNode(JsonNode node) {
        JsonNode properties = node.path("properties");
        return properties.isMissingNode() ? null : properties;
    }

    private static String renderClass(String packageName, String className, List<FieldSpec> fields, Set<String> imports) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(packageName).append(";\n\n");

        for (String imp : imports) {
            sb.append("import ").append(imp).append(";\n");
        }
        if (!imports.isEmpty()) {
            sb.append("\n");
        }

        sb.append("public class ").append(className).append(" {\n");

        for (FieldSpec field : fields) {
            sb.append("    private ").append(field.type).append(" ").append(field.name).append(";\n");
        }

        if (!fields.isEmpty()) {
            sb.append("\n");
        }

        for (FieldSpec field : fields) {
            String accessorName = toClassName(field.name);
            sb.append("    public ").append(field.type).append(" get").append(accessorName).append("() {\n")
              .append("        return ").append(field.name).append(";\n")
              .append("    }\n\n")
              .append("    public void set").append(accessorName).append("(").append(field.type).append(" ").append(field.name)
              .append(") {\n")
              .append("        this.").append(field.name).append(" = ").append(field.name).append(";\n")
              .append("    }\n\n");
        }

        if (!fields.isEmpty()) {
            sb.setLength(sb.length() - 1);
        }

        sb.append("}\n");
        return sb.toString();
    }

    private static String readText(JsonNode node, String field, String fallback) {
        JsonNode value = node.path(field);
        return value.isTextual() && !value.asText().isBlank() ? value.asText() : fallback;
    }

    private static String toClassName(String raw) {
        String sanitized = raw.replaceAll("[^A-Za-z0-9]", " ").trim();
        if (sanitized.isEmpty()) {
            return "GeneratedType";
        }

        StringBuilder sb = new StringBuilder();
        for (String part : sanitized.split("\\s+")) {
            if (!part.isEmpty()) {
                sb.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
                if (part.length() > 1) {
                    sb.append(part.substring(1));
                }
            }
        }

        if (Character.isDigit(sb.charAt(0))) {
            sb.insert(0, "Type");
        }

        return sb.toString();
    }

    private static String toFieldName(String raw) {
        String className = toClassName(raw);
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    private static String sanitizePackageName(String rawPackage) {
        String candidate = rawPackage == null || rawPackage.isBlank() ? "generated" : rawPackage.trim();
        String[] parts = candidate.split("\\.");
        List<String> sanitizedParts = new ArrayList<>();

        for (String part : parts) {
            String clean = part.replaceAll("[^A-Za-z0-9_]", "_");
            if (clean.isEmpty()) {
                continue;
            }
            if (!Character.isLetter(clean.charAt(0)) && clean.charAt(0) != '_') {
                clean = "_" + clean;
            }
            sanitizedParts.add(clean.toLowerCase(Locale.ROOT));
        }

        return sanitizedParts.isEmpty() ? "generated" : String.join(".", sanitizedParts);
    }

    private static String singularize(String name) {
        return name.endsWith("s") && name.length() > 1 ? name.substring(0, name.length() - 1) : name;
    }

    private record FieldSpec(String name, String type) {}

    private record FieldType(String typeName, boolean collection) {
        private FieldType(String typeName) {
            this(typeName, false);
        }
    }
}
