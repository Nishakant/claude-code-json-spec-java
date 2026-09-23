package com.nishakant.jsonspecjava;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: java -jar <jar-file> <spec.json> <output-directory>");
            System.exit(1);
        }

        Path specPath = Path.of(args[0]);
        Path outputDir = Path.of(args[1]);

        String spec = Files.readString(specPath, StandardCharsets.UTF_8);
        JsonSpecToJavaGenerator generator = new JsonSpecToJavaGenerator();
        Map<String, String> sources = generator.generateSources(spec);

        String packageName = extractPackageName(sources);
        Path packageDir = outputDir.resolve(packageName.replace('.', '/'));
        Files.createDirectories(packageDir);

        for (Map.Entry<String, String> entry : sources.entrySet()) {
            Path outputFile = packageDir.resolve(entry.getKey() + ".java");
            Files.writeString(outputFile, entry.getValue(), StandardCharsets.UTF_8);
        }

        System.out.printf("Generated %d file(s) in %s%n", sources.size(), packageDir.toAbsolutePath());
    }

    private static String extractPackageName(Map<String, String> sources) {
        return sources.values().stream()
                .map(source -> source.split("\\R", 2)[0])
                .map(line -> line.replace("package", "").replace(";", "").trim())
                .findFirst()
                .orElse("generated");
    }
}
