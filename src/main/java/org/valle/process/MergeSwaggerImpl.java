package org.valle.process;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.valle.process.models.SwaggerNode;
import org.valle.provide.GetSwaggerNode;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.valle.utils.JacksonUtils.readValue;

/**
 * Implémentation de {@link MergeSwagger}.
 *
 * <p>Stratégie de fusion :
 * <ul>
 *   <li>Les références de paths ({@code $ref: "paths/XYZ.yml"}) → le fichier est <b>inliné</b>
 *       directement dans la section {@code paths}.</li>
 *   <li>Les références de composants ({@code $ref: "../components/XYZ.yml"}) →
 *       converties en {@code $ref: "#/components/schemas/XYZ"} ;
 *       le fichier composant est chargé et ajouté sous {@code components.schemas}.</li>
 *   <li>Les valeurs {@code discriminator.mapping} pointant vers des fichiers composants
 *       sont aussi converties en références internes.</li>
 *   <li>Les {@code $ref} directs dans la section {@code components} du swagger principal
 *       (ex. {@code securitySchemes.bearerAuth.$ref}) sont <b>inlinés</b>.</li>
 * </ul>
 */
@Slf4j
public class MergeSwaggerImpl implements MergeSwagger {

    private static final String COMPONENT_PREFIX_FROM_PATH = "../components/";
    private static final String COMPONENT_PREFIX_FROM_MAIN = "components/";
    private static final String INTERNAL_SCHEMA_REF_PREFIX = "#/components/schemas/";
    private static final String KEY_PATHS = "paths";
    private static final String KEY_COMPONENTS = "components";
    private static final String KEY_SCHEMAS = "schemas";

    private final GetSwaggerNode getSwaggerNode;
    private final File baseDir;

    /**
     * @param getSwaggerNode fournisseur du nœud Swagger racine
     * @param baseDir        répertoire de base (celui du fichier swagger principal)
     */
    public MergeSwaggerImpl(GetSwaggerNode getSwaggerNode, File baseDir) {
        this.getSwaggerNode = getSwaggerNode;
        this.baseDir = baseDir;
    }

    @Override
    public SwaggerNode execute() {
        log.info("Début de la fusion du swagger");
        SwaggerNode swaggerNode = getSwaggerNode.provide();
        ObjectNode root = swaggerNode.node().deepCopy();

        Set<String> componentFileNames = new LinkedHashSet<>();

        // 1. Résoudre les références de paths (inline le contenu du fichier path)
        if (root.has(KEY_PATHS)) {
            root.set(KEY_PATHS, resolvePathFileRefs((ObjectNode) root.get(KEY_PATHS), componentFileNames));
        }

        // 2. Résoudre les $ref directs dans la section components du swagger principal
        //    (ex: securitySchemes.bearerAuth.$ref → inliné tel quel sous securitySchemes)
        ObjectNode componentsNode = root.has(KEY_COMPONENTS)
                ? root.get(KEY_COMPONENTS).deepCopy()
                : root.objectNode();
        inlineDirectRefsInComponents(componentsNode);

        // 3. Charger tous les fichiers composants collectés sous components/schemas
        //    (y compris les dépendances transitives)
        ObjectNode schemasNode = componentsNode.has(KEY_SCHEMAS)
                ? componentsNode.get(KEY_SCHEMAS).deepCopy()
                : componentsNode.objectNode();

        Set<String> loaded = new LinkedHashSet<>();
        while (loaded.size() < componentFileNames.size()) {
            Set<String> toLoad = new LinkedHashSet<>(componentFileNames);
            toLoad.removeAll(loaded);
            for (String fileName : toLoad) {
                File componentFile = new File(baseDir, COMPONENT_PREFIX_FROM_MAIN + fileName);
                String schemaKey = stripYmlExtension(fileName);
                if (componentFile.exists()) {
                    log.debug("Chargement du composant: {} → schemas/{}", fileName, schemaKey);
                    JsonNode converted = convertComponentRefs(readValue(componentFile), componentFileNames);
                    schemasNode.set(schemaKey, converted);
                } else {
                    log.warn("Fichier composant non trouvé: {}", componentFile.getAbsolutePath());
                }
                loaded.add(fileName);
            }
        }

        componentsNode.set(KEY_SCHEMAS, schemasNode);
        root.set(KEY_COMPONENTS, componentsNode);

        log.info("Fusion terminée — {} composant(s) chargé(s) sous components/schemas", loaded.size());
        return SwaggerNode.builder()
                .node(root)
                .extension(swaggerNode.extension())
                .build();
    }

    /** Retire l'extension {@code .yml} ou {@code .yaml} d'un nom de fichier. */
    private static String stripYmlExtension(String fileName) {
        if (fileName.endsWith(".yml")) {
            return fileName.substring(0, fileName.length() - 4);
        }
        if (fileName.endsWith(".yaml")) {
            return fileName.substring(0, fileName.length() - 5);
        }
        return fileName;
    }

    // ── Paths ────────────────────────────────────────────────────────────────

    /**
     * Pour chaque entrée de la section {@code paths} dont la valeur est un
     * {@code {$ref: "paths/XYZ.yml"}}, charge le fichier et convertit ses refs de composants.
     */
    private ObjectNode resolvePathFileRefs(ObjectNode paths, Set<String> componentFileNames) {
        ObjectNode result = paths.objectNode();
        paths.fields().forEachRemaining(entry -> {
            String pathKey = entry.getKey();
            JsonNode value = entry.getValue();
            if (value.isObject() && value.has("$ref")) {
                String ref = value.get("$ref").asText();
                if (!ref.startsWith("#")) {
                    File pathFile = new File(baseDir, ref);
                    if (pathFile.exists()) {
                        log.debug("Inlining path file: {}", ref);
                        result.set(pathKey, convertComponentRefs(readValue(pathFile), componentFileNames));
                        return;
                    }
                    log.warn("Fichier path non trouvé: {}", pathFile.getAbsolutePath());
                }
            }
            result.set(pathKey, convertComponentRefs(value, componentFileNames));
        });
        return result;
    }

    // ── Components du fichier principal ─────────────────────────────────────

    /**
     * Inline les {@code $ref} directs dans la section {@code components} du swagger principal.
     * Exemple : {@code securitySchemes.bearerAuth.$ref: "components/bearerAuth.yml"}
     * → le contenu du fichier remplace le nœud {@code $ref}.
     */
    private void inlineDirectRefsInComponents(ObjectNode node) {
        List<String> fields = new ArrayList<>();
        node.fieldNames().forEachRemaining(fields::add);
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (!value.isObject()) {
                continue;
            }
            ObjectNode obj = (ObjectNode) value;
            if (obj.has("$ref")) {
                String ref = obj.get("$ref").asText();
                if (!ref.startsWith("#")) {
                    File refFile = new File(baseDir, ref);
                    if (refFile.exists()) {
                        log.debug("Inlining component ref: {}", ref);
                        node.set(field, readValue(refFile));
                    }
                }
            } else {
                inlineDirectRefsInComponents(obj);
            }
        }
    }

    // ── Conversion des refs composants ───────────────────────────────────────

    /**
     * Parcourt récursivement {@code node} et convertit :
     * <ul>
     *   <li>{@code $ref: "../components/XYZ.yml"} → {@code $ref: "#/components/schemas/XYZ"}</li>
     *   <li>les valeurs de {@code discriminator.mapping} de la même façon</li>
     * </ul>
     * Collecte au passage les noms de fichiers composants dans {@code componentFileNames}.
     */
    private JsonNode convertComponentRefs(JsonNode node, Set<String> componentFileNames) {
        if (node == null) return null;

        if (node.isObject()) {
            ObjectNode source = (ObjectNode) node;
            ObjectNode result = source.objectNode();
            source.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                if ("$ref".equals(key) && value.isTextual()) {
                    result.set(key, convertRefValue(value.asText(), componentFileNames));
                } else if ("mapping".equals(key) && value.isObject()) {
                    result.set(key, convertMappingValues((ObjectNode) value, componentFileNames));
                } else {
                    result.set(key, convertComponentRefs(value, componentFileNames));
                }
            });
            return result;

        } else if (node.isArray()) {
            ArrayNode source = (ArrayNode) node;
            ArrayNode result = source.arrayNode();
            source.forEach(item -> result.add(convertComponentRefs(item, componentFileNames)));
            return result;
        }

        return node;
    }

    /**
     * Convertit une valeur de {@code $ref} si elle pointe vers un fichier composant.
     * {@code "../components/XYZ.yml"} → {@code "#/components/schemas/XYZ"}
     */
    private TextNode convertRefValue(String ref, Set<String> componentFileNames) {
        if (ref.startsWith(COMPONENT_PREFIX_FROM_PATH)) {
            String fileName = ref.substring(COMPONENT_PREFIX_FROM_PATH.length());
            componentFileNames.add(fileName);
            return TextNode.valueOf(INTERNAL_SCHEMA_REF_PREFIX + stripYmlExtension(fileName));
        }
        if (ref.startsWith(COMPONENT_PREFIX_FROM_MAIN)) {
            String fileName = ref.substring(COMPONENT_PREFIX_FROM_MAIN.length());
            componentFileNames.add(fileName);
            return TextNode.valueOf(INTERNAL_SCHEMA_REF_PREFIX + stripYmlExtension(fileName));
        }
        return TextNode.valueOf(ref);
    }

    /**
     * Convertit les valeurs d'un nœud {@code discriminator.mapping}.
     * {@code "../components/XYZ.yml"} → {@code "#/components/schemas/XYZ"}
     */
    private ObjectNode convertMappingValues(ObjectNode mapping, Set<String> componentFileNames) {
        ObjectNode result = mapping.objectNode();
        mapping.fields().forEachRemaining(entry -> {
            String val = entry.getValue().asText();
            if (val.startsWith(COMPONENT_PREFIX_FROM_PATH)) {
                String fileName = val.substring(COMPONENT_PREFIX_FROM_PATH.length());
                componentFileNames.add(fileName);
                result.set(entry.getKey(), TextNode.valueOf(INTERNAL_SCHEMA_REF_PREFIX + stripYmlExtension(fileName)));
            } else {
                result.set(entry.getKey(), entry.getValue());
            }
        });
        return result;
    }
}

