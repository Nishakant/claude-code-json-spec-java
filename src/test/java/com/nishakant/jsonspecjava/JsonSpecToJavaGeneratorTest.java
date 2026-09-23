package com.nishakant.jsonspecjava;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonSpecToJavaGeneratorTest {

    private final JsonSpecToJavaGenerator generator = new JsonSpecToJavaGenerator();

    @Test
    void generatesSimpleClassFromSpec() throws IOException {
        String spec = """
                {
                  "package": "com.example.generated",
                  "className": "Person",
                  "type": "object",
                  "properties": {
                    "name": {"type": "string"},
                    "age": {"type": "integer"},
                    "active": {"type": "boolean"}
                  }
                }
                """;

        Map<String, String> sources = generator.generateSources(spec);

        assertEquals(1, sources.size());
        String personSource = sources.get("Person");
        assertTrue(personSource.contains("package com.example.generated;"));
        assertTrue(personSource.contains("private String name;"));
        assertTrue(personSource.contains("private Integer age;"));
        assertTrue(personSource.contains("private Boolean active;"));
        assertTrue(personSource.contains("public String getName()"));
        assertTrue(personSource.contains("public void setAge(Integer age)"));
    }

    @Test
    void generatesNestedAndArrayTypes() throws IOException {
        String spec = """
                {
                  "package": "com.example.generated",
                  "title": "Order",
                  "type": "object",
                  "properties": {
                    "id": {"type": "string"},
                    "customer": {
                      "type": "object",
                      "properties": {
                        "email": {"type": "string"}
                      }
                    },
                    "items": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "properties": {
                          "sku": {"type": "string"},
                          "quantity": {"type": "integer"}
                        }
                      }
                    }
                  }
                }
                """;

        Map<String, String> sources = generator.generateSources(spec);

        assertEquals(3, sources.size());
        assertTrue(sources.get("Order").contains("import java.util.List;"));
        assertTrue(sources.get("Order").contains("private Customer customer;"));
        assertTrue(sources.get("Order").contains("private List<Item> items;"));
        assertTrue(sources.get("Customer").contains("private String email;"));
        assertTrue(sources.get("Item").contains("private Integer quantity;"));
    }

    @Test
    void defaultsAndSanitizesPackageName() throws IOException {
        String specWithoutPackage = """
                {
                  "className": "Sample",
                  "type": "object",
                  "properties": {
                    "id": {"type": "string"}
                  }
                }
                """;

        Map<String, String> defaultPackageSources = generator.generateSources(specWithoutPackage);
        assertTrue(defaultPackageSources.get("Sample").contains("package generated;"));

        String specWithInvalidPackage = """
                {
                  "package": "com.example.generated-v1",
                  "className": "Sample",
                  "type": "object",
                  "properties": {
                    "id": {"type": "string"}
                  }
                }
                """;

        Map<String, String> sanitizedPackageSources = generator.generateSources(specWithInvalidPackage);
        assertTrue(sanitizedPackageSources.get("Sample").contains("package com.example.generated_v1;"));
    }

    @Test
    void generatesUniqueNamesForCollidingNestedTypes() throws IOException {
        String spec = """
                {
                  "className": "Order",
                  "type": "object",
                  "properties": {
                    "billing": {
                      "type": "object",
                      "properties": {
                        "address": {
                          "type": "object",
                          "properties": {
                            "line1": {"type": "string"}
                          }
                        }
                      }
                    },
                    "shipping": {
                      "type": "object",
                      "properties": {
                        "address": {
                          "type": "object",
                          "properties": {
                            "postalCode": {"type": "string"}
                          }
                        }
                      }
                    }
                  }
                }
                """;

        Map<String, String> sources = generator.generateSources(spec);

        assertTrue(sources.get("Billing").contains("private Address address;"));
        assertTrue(sources.get("Shipping").contains("private ShippingAddress address;"));
        assertTrue(sources.get("Address").contains("private String line1;"));
        assertTrue(sources.get("ShippingAddress").contains("private String postalCode;"));
    }
}
