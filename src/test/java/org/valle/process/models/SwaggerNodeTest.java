package org.valle.process.models;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.valle.utils.JacksonUtils;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.valle.process.models.SwaggerNode.findRefs;

class SwaggerNodeTest {

    public static final String INPUT_SWAGGER_BASE_PATH = "src/test/resources";

    @ParameterizedTest
    @MethodSource("provide_test_findRefs_values")
    void test_findRefs(
            String inputFileName,
            String schemaName,
            Set<String> expectedReferences
    ) {
        // Arrange
        JacksonUtils jacksonUtils = new JacksonUtils(new File(INPUT_SWAGGER_BASE_PATH + "/cleared/" + inputFileName));
        JsonNode objectMap = jacksonUtils.readValue();
        // Act
        Set<String> actual = findRefs(objectMap.get("components").get("schemas").get(schemaName), objectMap, new HashSet<>());
        // Assert
        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringCollectionOrder()
                .isEqualTo(expectedReferences);
    }

    private static Stream<Arguments> provide_test_findRefs_values() {
        return Stream.of(
                Arguments.of("swagger-cobaye.yml", "ContractDTOV1", Set.of(
                        "AccountDTOV1",
                        "DestinationUcsDTOV1",
                        "InitialPaymentDTOV1",
                        "DeathGuaranteeDTOV1",
                        "ProductDTOV1",
                        "ProgrammedDepositDTOV1",
                        "PocketDTOV1",
                        "RppDTOV1",
                        "MandateDTOV1",
                        "OfferDTOV1",
                        "BeneficiaryClauseDTOV1",
                        "FinancialOptionDTOV1",
                        "OriginUcsDTOV1",
                        "InvestmentRepartitionDTOV1",
                        "SupportDTOV1")),
                Arguments.of("test-oneof-allof.yaml", "Object1", Set.of("Object2", "Object3", "Object4")),
                Arguments.of("test-oneof-allof.yaml", "Object2", Set.of("Object3", "Object4")),
                Arguments.of("test-oneof-allof.yaml", "Object3", Set.of())
        );
    }

    @ParameterizedTest
    @MethodSource("provide_test_getAllNamedReferencesOfAPath_values")
    void test_getAllNamedReferencesOfAPath(
            String inputFileName,
            EndPoint endPoint,
            Set<String> expectedReferences
    ) {
        // Arrange
        JacksonUtils jacksonUtils = new JacksonUtils(new File(INPUT_SWAGGER_BASE_PATH + "/cleared/" + inputFileName));
        SwaggerNode swaggerNode = jacksonUtils.getSwaggerNode();
        // Act
        Set<String> actual = swaggerNode.getAllNamedReferencesOfAPath(endPoint);
        // Assert
        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringCollectionOrder()
                .isEqualTo(expectedReferences);
    }

    private static Stream<Arguments> provide_test_getAllNamedReferencesOfAPath_values() {
        return Stream.of(
                Arguments.of("swagger-cobaye.yml",
                        EndPoint.builder().method("post").path("/cadh/v1/operations").build(),
                        Set.of("AccountDTOV1",
                                "DestinationUcsDTOV1",
                                "ContractDTOV1",
                                "InitialPaymentDTOV1",
                                "ActorDTOV1",
                                "OperationOutputDTOV1",
                                "RecommendationAssessmentDTOV1",
                                "DeathGuaranteeDTOV1",
                                "FundsOriginDTOV1",
                                "FrcDTOV1",
                                "OperationInputDTOV1",
                                "CustomerDTOV1",
                                "ProductDTOV1",
                                "ProgrammedDepositDTOV1",
                                "PocketDTOV1",
                                "RppDTOV1",
                                "MandateDTOV1",
                                "OfferDTOV1",
                                "BeneficiaryClauseDTOV1",
                                "FinancialOptionDTOV1",
                                "OriginUcsDTOV1",
                                "InvestmentRepartitionDTOV1",
                                "SupportDTOV1",
                                "CaaErrorMessage")
                ),
                Arguments.of("partial_ref_allOf.yml",
                        EndPoint.builder().method("get").path("/employee").build(),
                        Set.of("Person")
                ),
                Arguments.of("in_schema.yml",
                        EndPoint.builder().method("get").path("/user").build(),
                        Set.of("User")
                ),
                Arguments.of("in_parameter.yml",
                        EndPoint.builder().method("get").path("/user").build(),
                        Set.of("UserId")
                ),
                Arguments.of("in_oneOf.yml",
                        EndPoint.builder().method("get").path("/animal").build(),
                        Set.of("Cat", "Dog")
                ),
                Arguments.of("in_example.yml",
                        EndPoint.builder().method("get").path("/product").build(),
                        Set.of("ProductExample")
                ),
                Arguments.of("in_discriminator_oneOf.yml",
                        EndPoint.builder().method("get").path("/vehicle").build(),
                        Set.of("Vehicle", "Car", "Truck")
                ),
                Arguments.of("in_circle_ref.yml",
                        EndPoint.builder().method("get").path("/node").build(),
                        Set.of("Node")
                ),
                Arguments.of("in_allOf.yml",
                        EndPoint.builder().method("get").path("/employee").build(),
                        Set.of("Person")
                ),
                Arguments.of("crossed_ref.yml",
                        EndPoint.builder().method("get").path("/order").build(),
                        Set.of("Order", "Address", "Customer")
                )
        );
    }

    @Test
    void test_decomposePaths_OK() {
        // Arrange
        File swaggerFile = new File("src/test/resources/decomposed/swagger-initial.yml");
        JacksonUtils jacksonUtils = new JacksonUtils(swaggerFile);
        SwaggerNode swaggerNode = jacksonUtils.getSwaggerNode();
        // Act
        SwaggerNode actual = swaggerNode.decomposePaths();
        // Assert
        assertThat(actual.node().properties()).hasSize(2);
    }

    @Test
    void test_decomposeComponent_OK() {
        // Arrange
        File swaggerFile = new File("src/test/resources/decomposed/swagger-initial.yml");
        JacksonUtils jacksonUtils = new JacksonUtils(swaggerFile);
        SwaggerNode swaggerNode = jacksonUtils.getSwaggerNode();
        // Act
        SwaggerNode actual = swaggerNode.decomposeComponent();
        // Assert
        assertThat(actual.node().properties()).hasSize(3);
    }

    public static final String SWAGGER_FILE_PATH = "src/test/resources/cleared/swagger-cobaye.yml";

    @Test
    void test_getAllEndpoints() {
        // Arrange
        JacksonUtils jacksonUtilsCobaye = new JacksonUtils(new File(SWAGGER_FILE_PATH));
        SwaggerNode swaggerNode = jacksonUtilsCobaye.getSwaggerNode();
        // Act
        var endpoints = swaggerNode.getAllEndpoints();
        // Assert
        assertThat(endpoints).hasSize(5);
    }
}
