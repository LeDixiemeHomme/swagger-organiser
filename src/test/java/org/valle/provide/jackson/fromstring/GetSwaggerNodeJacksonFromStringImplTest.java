package org.valle.provide.jackson.fromstring;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.valle.process.models.Extension;
import org.valle.process.models.SwaggerNode;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.valle.utils.JacksonUtils.readValue;

class GetSwaggerNodeJacksonFromStringImplTest {

    @Test
    void test_provide() {
        // Arrange
        GetSwaggerNodeJacksonFromStringImpl provider = new GetSwaggerNodeJacksonFromStringImpl(SWAGGER_STRING, Extension.JSON);
        JsonNode jsonNode = readValue(new File("src/test/resources/decomposed/swagger-initial.json"));
        // Act
        SwaggerNode actual = provider.provide();
        // Assert
        assertThat(actual.node()).isEqualTo(jsonNode);
    }

    String SWAGGER_STRING = """
                    {
                      "openapi": "3.0.0",
                      "info": {
                        "title": "Exemple API Décomposé",
                        "version": "1.0.0"
                      },
                      "paths": {
                        "/users": {
                          "get": {
                            "summary": "Liste des utilisateurs",
                            "responses": {
                              "200": {
                                "description": "OK",
                                "content": {
                                  "application/json": {
                                    "schema": {
                                      "$ref": "#/components/schemas/User"
                                    }
                                  }
                                }
                              },
                              "400": {
                                "description": "Erreur",
                                "content": {
                                  "application/json": {
                                    "schema": {
                                      "$ref": "#/components/schemas/Error"
                                    }
                                  }
                                }
                              }
                            }
                          }
                        },
                        "/products": {
                          "get": {
                            "summary": "Liste des produits",
                            "responses": {
                              "200": {
                                "description": "OK",
                                "content": {
                                  "application/json": {
                                    "schema": {
                                      "$ref": "#/components/schemas/Product"
                                    }
                                  }
                                }
                              },
                              "400": {
                                "description": "Erreur",
                                "content": {
                                  "application/json": {
                                    "schema": {
                                      "$ref": "#/components/schemas/Error"
                                    }
                                  }
                                }
                              }
                            }
                          }
                        }
                      },
                      "components": {
                        "schemas": {
                          "Error": {
                            "type": "object",
                            "properties": {
                              "code": {
                                "type": "integer"
                              },
                              "message": {
                                "type": "string"
                              }
                            }
                          },
                          "Product": {
                            "type": "object",
                            "properties": {
                              "id": {
                                "type": "integer"
                              },
                              "name": {
                                "type": "string"
                              },
                              "price": {
                                "type": "number"
                              }
                            }
                          },
                          "User": {
                            "type": "object",
                            "properties": {
                              "id": {
                                "type": "integer"
                              },
                              "name": {
                                "type": "string"
                              },
                              "product": {
                                "$ref": "#/components/schemas/Product"
                              }
                            }
                          }
                        }
                      }
                    }
            """;
}