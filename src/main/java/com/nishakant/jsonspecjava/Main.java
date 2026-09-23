package com.nishakant.jsonspecjava;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: java -jar <jar-file> <spec.json> <output-directory>");
            System.err.println("       (arguments passed to this program: <spec.json> <output-directory>)");
            System.exit(1);
        }

        Path specPath = Path.of(args[0]);
        Path outputDir = Path.of(args[1]);

        String spec = Files.readString(specPath, StandardCharsets.UTF_8);
        JsonSpecToJavaGenerator generator = new JsonSpecToJavaGenerator();
        JsonSpecToJavaGenerator.GenerationResult generation = generator.generate(spec);

        Path packageDir = outputDir;
        for (String segment : generation.packageName().split("\\.")) {
            packageDir = packageDir.resolve(segment);
        }
        Files.createDirectories(packageDir);

        for (var entry : generation.sources().entrySet()) {
            Path outputFile = packageDir.resolve(entry.getKey() + ".java");
            Files.writeString(outputFile, entry.getValue(), StandardCharsets.UTF_8);
        }

        System.out.printf("Generated %d file(s) in %s%n", generation.sources().size(), packageDir.toAbsolutePath());
    }
}
