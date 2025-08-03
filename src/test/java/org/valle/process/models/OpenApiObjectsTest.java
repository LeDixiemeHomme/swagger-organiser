package org.valle.process.models;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.valle.provide.jackson.JacksonUtils;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiObjectsTest {

    public static final String INPUT_SWAGGER_BASE_PATH = "src/test/resources";

    @ParameterizedTest
    @MethodSource("provide_test_getAllNamedReferencesOfASchema_values")
    void test_getAllNamedReferencesOfASchema(
            String inputFileName,
            String schemaName,
            Set<String> expectedReferences
    ) {
        // Arrange
        JacksonUtils jacksonUtils = new JacksonUtils(new File(INPUT_SWAGGER_BASE_PATH + "/" + inputFileName));

        JsonNode objectMap = jacksonUtils.readValue();
        Map<String, Object> rawValue = jacksonUtils.readRawValue();

        OpenApiObjects openApiObjects = new OpenApiObjects(objectMap, rawValue);

        JsonNode node = openApiObjects.getSchemas().get(schemaName);

        // Act
        Set<String> actual = openApiObjects.findRefs(node);

        // Assert
        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringCollectionOrder()
                .isEqualTo(expectedReferences);
    }

    private static Stream<Arguments> provide_test_getAllNamedReferencesOfASchema_values() {
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
                        "SupportDTOV1"))
//                Arguments.of("in_schema.yml", "User", Set.of()),
//                Arguments.of("in_parameter.yml", "User", Set.of())
        );
    }

    @Test
    void test_getAllNamedReferencesOfAPath() {
        // Arrange
        JacksonUtils jacksonUtils = new JacksonUtils(new File(INPUT_SWAGGER_BASE_PATH + "/" + "swagger-cobaye.yml"));

        JsonNode objectMap = jacksonUtils.readValue();
        Map<String, Object> rawValue = jacksonUtils.readRawValue();

        OpenApiObjects openApiObjects = new OpenApiObjects(objectMap, rawValue);

        EndPoint endPoint = EndPoint.builder()
                .method("post")
                .path("/cadh/v1/operations")
                .build();

        Set<String> expected = Set.of("AccountDTOV1",
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
                "CaaErrorMessage");
        // Act
        Set<String> actual = openApiObjects.getAllNamedReferencesOfAPath(endPoint);
        // Assert
        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringCollectionOrder()
                .isEqualTo(expected);
    }
}