# claude-code-json-spec-java

Small Java utility that generates Java POJO source files from a JSON schema-like specification.

## Build

```bash
mvn test
```

## Generate Java classes

```bash
mvn -q -DskipTests package
java -jar target/claude-code-json-spec-java-1.0-SNAPSHOT.jar spec.json ./out
```

## Supported spec shape (minimal)

```json
{
  "package": "com.example.generated",
  "className": "Person",
  "type": "object",
  "properties": {
    "name": { "type": "string" },
    "age": { "type": "integer" }
  }
}
```

Supported `type` values: `string`, `integer`, `number`, `boolean`, `object`, `array`.
