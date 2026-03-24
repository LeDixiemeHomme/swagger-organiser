package org.valle.present.picocli;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.valle.persist.PersistDecomposedSwagger;
import org.valle.persist.PersistResult;
import org.valle.process.ClearEndpointOnDemand;
import org.valle.process.DecomposeSwagger;
import org.valle.process.GetAndShowEndpoints;
import org.valle.process.models.DecomposedSwagger;
import org.valle.process.models.EndPoint;
import org.valle.process.models.SwaggerNode;
import org.valle.provide.GetSwaggerNode;
import picocli.CommandLine;

import java.io.File;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CliAppTest {

    // --- Mocks des services ---
    @Mock GetSwaggerNode        mockProvider;
    @Mock GetAndShowEndpoints   mockShow;
    @Mock ClearEndpointOnDemand mockClear;
    @Mock SwaggerNode           mockClearedNode;
    @Mock GetSwaggerNode        mockClearedProvider;
    @Mock DecomposeSwagger      mockDecompose;
    @Mock DecomposedSwagger     mockDecomposedSwagger;
    @Mock PersistDecomposedSwagger mockPersistDecomposed;
    @Mock ObjectNode            mockObjectNode;

    @SuppressWarnings("unchecked")
    PersistResult<ObjectNode> mockPersistResult = mock(PersistResult.class);

    CliApp cliApp = new CliApp();

    @BeforeEach
    void setUp() {
        // Injection des factories (package-private) avec les mocks
        cliApp.swaggerNodeFactory      = f    -> mockProvider;
        cliApp.showFactory             = gsn  -> mockShow;
        cliApp.clearFactory            = gsn  -> mockClear;
        cliApp.nodeProviderFactory     = node -> mockClearedProvider;
        cliApp.decomposeFactory        = gsn  -> mockDecompose;
        cliApp.persistDecomposedFactory = path -> mockPersistDecomposed;
        cliApp.persistResultFactory    = file -> mockPersistResult;

        // Stubs communs (lenient = pas d'erreur si non utilises dans certains tests)
        lenient().when(mockClear.execute(any())).thenReturn(mockClearedNode);
        lenient().when(mockDecompose.execute()).thenReturn(mockDecomposedSwagger);
        lenient().when(mockClearedNode.node()).thenReturn(mockObjectNode);
    }

    private CommandLine cli() {
        return new CommandLine(cliApp)
                .registerConverter(EndPoint.class, EndPoint::fromString);
    }

    // =========================================================================
    // Tests d'orchestration : ordre d'appel des services
    // =========================================================================

    @Nested
    class Orchestration {

        @Test
        void should_always_call_show_then_clear_in_order() {
            cli().execute("-sf", "any.yml", "-toRm", "post:/test");

            InOrder order = inOrder(mockShow, mockClear);
            order.verify(mockShow).execute();
            order.verify(mockClear).execute(any());
        }

        @Test
        void should_call_decompose_after_clear_when_d_flag_is_set() {
            cli().execute("-sf", "any.yml", "-toRm", "post:/test", "-d");

            InOrder order = inOrder(mockClear, mockDecompose);
            order.verify(mockClear).execute(any());
            order.verify(mockDecompose).execute();
        }

        @Test
        void should_persist_decomposed_after_decompose_when_d_and_pf_flags_are_set() {
            cli().execute("-sf", "any.yml", "-toRm", "post:/test", "-d", "-pf");

            InOrder order = inOrder(mockDecompose, mockPersistDecomposed);
            order.verify(mockDecompose).execute();
            order.verify(mockPersistDecomposed).persist(mockDecomposedSwagger);
        }

        @Test
        void should_execute_full_pipeline_in_order_when_all_flags_are_set() {
            cli().execute("-sf", "any.yml", "-toRm", "post:/test", "-d", "-pf");

            InOrder order = inOrder(mockShow, mockClear, mockDecompose, mockPersistDecomposed);
            order.verify(mockShow).execute();
            order.verify(mockClear).execute(any());
            order.verify(mockDecompose).execute();
            order.verify(mockPersistDecomposed).persist(mockDecomposedSwagger);
        }
    }

    // =========================================================================
    // Tests des flags : comportement conditionnel selon les options CLI
    // =========================================================================

    @Nested
    class Flags {

        @Test
        void should_not_call_decompose_when_d_flag_is_absent() {
            cli().execute("-sf", "any.yml", "-toRm", "post:/test");

            verify(mockDecompose, never()).execute();
        }

        @Test
        void should_not_persist_anything_when_pf_flag_is_absent() {
            cli().execute("-sf", "any.yml", "-toRm", "post:/test", "-d");

            verify(mockPersistDecomposed, never()).persist(any());
            verify(mockPersistResult, never()).persist(any());
        }

        @Test
        void should_persist_cleared_node_when_pf_is_set_but_d_is_absent() {
            cli().execute("-sf", "any.yml", "-toRm", "post:/test", "-pf");

            verify(mockPersistResult).persist(mockObjectNode);
            verify(mockPersistDecomposed, never()).persist(any());
        }

        @Test
        void should_persist_decomposed_and_not_cleared_node_when_both_flags_are_set() {
            cli().execute("-sf", "any.yml", "-toRm", "post:/test", "-d", "-pf");

            verify(mockPersistDecomposed).persist(mockDecomposedSwagger);
            verify(mockPersistResult, never()).persist(any());
        }
    }

    // =========================================================================
    // Tests des arguments : transmission correcte des valeurs CLI aux services
    // =========================================================================

    @Nested
    class Arguments {

        @Test
        void should_pass_file_path_to_swagger_node_factory() {
            AtomicReference<File> capturedFile = new AtomicReference<>();
            cliApp.swaggerNodeFactory = file -> {
                capturedFile.set(file);
                return mockProvider;
            };

            cli().execute("-sf", "src/main/resources/swagger-cobaye.yml", "-toRm", "post:/test");

            assertThat(capturedFile.get().getPath())
                    .isEqualTo(new File("src/main/resources/swagger-cobaye.yml").getPath());
        }

        @Test
        void should_pass_single_endpoint_to_clear() {
            cli().execute("-sf", "any.yml", "-toRm", "post:/cadh/v1/operations");

            verify(mockClear).execute(Set.of(
                    EndPoint.builder().method("post").path("/cadh/v1/operations").build()
            ));
        }

        @Test
        void should_pass_multiple_endpoints_to_clear_when_comma_separated() {
            cli().execute("-sf", "any.yml", "-toRm", "post:/users,get:/items");

            verify(mockClear).execute(Set.of(
                    EndPoint.builder().method("post").path("/users").build(),
                    EndPoint.builder().method("get").path("/items").build()
            ));
        }

        @Test
        void should_use_decomposed_path_constant_when_persisting() {
            AtomicReference<String> capturedPath = new AtomicReference<>();
            cliApp.persistDecomposedFactory = path -> {
                capturedPath.set(path);
                return mockPersistDecomposed;
            };

            cli().execute("-sf", "any.yml", "-toRm", "post:/test", "-d", "-pf");

            assertThat(capturedPath.get()).isEqualTo(CliApp.DECOMPOSED_PATH);
        }

        @Test
        void should_use_result_path_constant_when_persisting_cleared_node() {
            AtomicReference<File> capturedFile = new AtomicReference<>();
            cliApp.persistResultFactory = file -> {
                capturedFile.set(file);
                return mockPersistResult;
            };

            cli().execute("-sf", "any.yml", "-toRm", "post:/test", "-pf");

            assertThat(capturedFile.get().getPath())
                    .isEqualTo(new File(CliApp.RESULT_PATH).getPath());
        }
    }
}

