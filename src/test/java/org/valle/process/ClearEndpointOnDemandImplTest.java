package org.valle.process;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.valle.process.models.EndPoint;
import org.valle.provide.jackson.JacksonUtils;

import java.io.File;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.valle.process.ClearEndpointOnDemandImpl.findRefs;
import static org.valle.process.ClearEndpointOnDemandImpl.getAllNamedReferencesOfAPath;

class ClearEndpointOnDemandImplTest {

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

        JsonNode allComponents = objectMap.get("components").get("schemas");

        // Act
        Set<String> actual = findRefs(allComponents.get(schemaName), allComponents);

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
        JacksonUtils jacksonUtils = new JacksonUtils(new File(INPUT_SWAGGER_BASE_PATH + "/" + inputFileName));

        JsonNode objectMap = jacksonUtils.readValue();

        JsonNode allPaths = objectMap.get("paths");

        JsonNode allComponents = objectMap.get("components").get("schemas");
        // Act
        Set<String> actual = getAllNamedReferencesOfAPath(endPoint, allPaths, allComponents);
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
                )
        );
    }
}