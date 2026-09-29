package org.valle.process;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.valle.process.models.DecomposedSwagger;
import org.valle.process.models.SwaggerNode;
import org.valle.provide.GetSwaggerNode;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.valle.utils.JacksonUtils.readValue;
import static org.valle.utils.SafePathResolver.resolveWithin;

/**
 * Implémentation de {@link MergeSwagger}.
 *
 * <p>Stratégie de fusion :
 * <ul>
 *   <li>Les références de paths ({@code $ref: "paths/XYZ.yml"}) → le fichier est <b>inliné</b>
 *       directement dans la section {@code paths}.</li>
 *   <li>Les références de composants ({@code $ref: "../components/XYZ.yml"}) →
 *       converties en références internes dans leur catégorie d'origine. Les sorties
 *       récentes portent cette information dans {@code component-categories.json}.</li>
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
    private static final String INTERNAL_COMPONENT_REF_PREFIX = "#/components/";
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
        Map<String, String> componentCategories = readComponentCategories();

        Set<String> componentFileNames = new LinkedHashSet<>();
        String extension = swaggerNode.extension().toString().toLowerCase();
        componentCategories.keySet().stream()
                .map(name -> name + "." + extension)
                .forEach(componentFileNames::add);

        // 1. Résoudre les références de paths (inline le contenu du fichier path)
        if (root.has(KEY_PATHS)) {
            root.set(KEY_PATHS, resolvePathFileRefs(
                    (ObjectNode) root.get(KEY_PATHS), componentFileNames, componentCategories));
        }

        // 2. Résoudre les $ref directs dans la section components du swagger principal
        //    (ex: securitySchemes.bearerAuth.$ref → inliné tel quel sous securitySchemes)
        ObjectNode componentsNode = root.has(KEY_COMPONENTS)
                ? root.get(KEY_COMPONENTS).deepCopy()
                : root.objectNode();
        inlineDirectRefsInComponents(componentsNode);

        // 3. Charger tous les fichiers composants collectés sous leur catégorie
        //    (y compris les dépendances transitives)
        ObjectNode schemasNode = componentsNode.has(KEY_SCHEMAS)
                ? componentsNode.get(KEY_SCHEMAS).deepCopy()
                : componentsNode.objectNode();

        Set<String> loaded = new LinkedHashSet<>();
        while (loaded.size() < componentFileNames.size()) {
            Set<String> toLoad = new LinkedHashSet<>(componentFileNames);
            toLoad.removeAll(loaded);
            for (String fileName : toLoad) {
                File componentFile = resolveWithin(baseDir.toPath(),
                        COMPONENT_PREFIX_FROM_MAIN + fileName, "Référence de composant").toFile();
                String schemaKey = stripYmlExtension(fileName);
                if (componentFile.exists()) {
                    String category = componentCategories.getOrDefault(schemaKey, KEY_SCHEMAS);
                    log.debug("Chargement du composant: {} → {}/{}", fileName, category, schemaKey);
                    JsonNode converted = convertComponentRefs(
                            readValue(componentFile), componentFileNames, componentCategories);
                    ObjectNode categoryNode;
                    if (KEY_SCHEMAS.equals(category)) {
                        categoryNode = schemasNode;
                    } else {
                        JsonNode existingCategory = componentsNode.get(category);
                        if (existingCategory != null && !existingCategory.isObject()) {
                            throw new IllegalArgumentException(
                                    "Swagger invalide : la catégorie de composant '%s' doit être un objet."
                                            .formatted(category));
                        }
                        categoryNode = existingCategory == null
                                ? componentsNode.objectNode()
                                : existingCategory.deepCopy();
                    }
                    categoryNode.set(schemaKey, converted);
                    if (!KEY_SCHEMAS.equals(category)) {
                        componentsNode.set(category, categoryNode);
                    }
                } else {
                    throw new IllegalArgumentException(
                            "Fichier composant référencé introuvable : " + fileName);
                }
                loaded.add(fileName);
            }
        }

        componentsNode.set(KEY_SCHEMAS, schemasNode);
        root.set(KEY_COMPONENTS, componentsNode);

        log.info("Fusion terminée — {} composant(s) chargé(s)", loaded.size());
        return SwaggerNode.builder()
                .node(root)
                .extension(swaggerNode.extension())
                .comments(swaggerNode.comments())
                .preserveComments(swaggerNode.preserveComments())
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
    private ObjectNode resolvePathFileRefs(
            ObjectNode paths,
            Set<String> componentFileNames,
            Map<String, String> componentCategories) {
        ObjectNode result = paths.objectNode();
        paths.fields().forEachRemaining(entry -> {
            String pathKey = entry.getKey();
            JsonNode value = entry.getValue();
            if (value.isObject() && value.has("$ref")) {
                String ref = value.get("$ref").asText();
                if (!ref.startsWith("#")) {
                    File pathFile = resolveWithin(baseDir.toPath(), ref, "Référence de path").toFile();
                    if (pathFile.exists()) {
                        log.debug("Inlining path file: {}", ref);
                        result.set(pathKey, convertComponentRefs(
                                readValue(pathFile), componentFileNames, componentCategories));
                        return;
                    }
                    throw new IllegalArgumentException(
                            "Fichier path référencé introuvable : " + ref);
                }
            }
            result.set(pathKey, convertComponentRefs(value, componentFileNames, componentCategories));
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
                    File refFile = resolveWithin(baseDir.toPath(),
                            ref, "Référence de composant").toFile();
                    if (refFile.exists()) {
                        log.debug("Inlining component ref: {}", ref);
                        node.set(field, readValue(refFile));
                    } else {
                        throw new IllegalArgumentException(
                                "Fichier composant référencé introuvable : " + ref);
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
     *   <li>{@code $ref: "../components/XYZ.yml"} → une référence interne dans la
     *       catégorie indiquée par les métadonnées (ou {@code schemas} historiquement)</li>
     *   <li>les valeurs de {@code discriminator.mapping} de la même façon</li>
     * </ul>
     * Collecte au passage les noms de fichiers composants dans {@code componentFileNames}.
     */
    private JsonNode convertComponentRefs(
            JsonNode node,
            Set<String> componentFileNames,
            Map<String, String> componentCategories) {
        if (node == null) return null;

        if (node.isObject()) {
            ObjectNode source = (ObjectNode) node;
            ObjectNode result = source.objectNode();
            source.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                if ("$ref".equals(key) && value.isTextual()) {
                    result.set(key, convertRefValue(value.asText(), componentFileNames, componentCategories));
                } else if ("mapping".equals(key) && value.isObject()) {
                    result.set(key, convertMappingValues(
                            (ObjectNode) value, componentFileNames, componentCategories));
                } else {
                    result.set(key, convertComponentRefs(value, componentFileNames, componentCategories));
                }
            });
            return result;

        } else if (node.isArray()) {
            ArrayNode source = (ArrayNode) node;
            ArrayNode result = source.arrayNode();
            source.forEach(item -> result.add(
                    convertComponentRefs(item, componentFileNames, componentCategories)));
            return result;
        }

        return node;
    }

    /**
     * Convertit une valeur de {@code $ref} si elle pointe vers un fichier composant.
     * La catégorie est lue dans le sidecar, avec repli vers {@code schemas}.
     */
    private TextNode convertRefValue(
            String ref,
            Set<String> componentFileNames,
            Map<String, String> componentCategories) {
        if (ref.startsWith(COMPONENT_PREFIX_FROM_PATH)) {
            String fileName = ref.substring(COMPONENT_PREFIX_FROM_PATH.length());
            componentFileNames.add(fileName);
            return TextNode.valueOf(INTERNAL_COMPONENT_REF_PREFIX
                    + componentCategories.getOrDefault(stripYmlExtension(fileName), KEY_SCHEMAS)
                    + "/" + stripYmlExtension(fileName));
        }
        if (ref.startsWith(COMPONENT_PREFIX_FROM_MAIN)) {
            String fileName = ref.substring(COMPONENT_PREFIX_FROM_MAIN.length());
            componentFileNames.add(fileName);
            return TextNode.valueOf(INTERNAL_COMPONENT_REF_PREFIX
                    + componentCategories.getOrDefault(stripYmlExtension(fileName), KEY_SCHEMAS)
                    + "/" + stripYmlExtension(fileName));
        }
        return TextNode.valueOf(ref);
    }

    /**
     * Convertit les valeurs d'un nœud {@code discriminator.mapping}.
     * La catégorie est lue dans le sidecar, avec repli vers {@code schemas}.
     */
    private ObjectNode convertMappingValues(
            ObjectNode mapping,
            Set<String> componentFileNames,
            Map<String, String> componentCategories) {
        ObjectNode result = mapping.objectNode();
        mapping.fields().forEachRemaining(entry -> {
            String val = entry.getValue().asText();
            if (val.startsWith(COMPONENT_PREFIX_FROM_PATH)
                    || val.startsWith(COMPONENT_PREFIX_FROM_MAIN)) {
                String prefix = val.startsWith(COMPONENT_PREFIX_FROM_PATH)
                        ? COMPONENT_PREFIX_FROM_PATH
                        : COMPONENT_PREFIX_FROM_MAIN;
                String fileName = val.substring(prefix.length());
                componentFileNames.add(fileName);
                String componentName = stripYmlExtension(fileName);
                result.set(entry.getKey(), TextNode.valueOf(INTERNAL_COMPONENT_REF_PREFIX
                        + componentCategories.getOrDefault(componentName, KEY_SCHEMAS)
                        + "/" + componentName));
            } else {
                result.set(entry.getKey(), entry.getValue());
            }
        });
        return result;
    }

    private Map<String, String> readComponentCategories() {
        File metadataFile = resolveWithin(
                baseDir.toPath(),
                DecomposedSwagger.COMPONENT_CATEGORIES_FILE,
                "Métadonnées des composants").toFile();
        if (!metadataFile.exists()) {
            return Map.of();
        }

        JsonNode metadata = readValue(metadataFile);
        if (!metadata.isObject()) {
            throw new IllegalArgumentException(
                    "Métadonnées des composants invalides : un objet est attendu.");
        }
        Map<String, String> categories = new LinkedHashMap<>();
        metadata.fields().forEachRemaining(entry -> {
            if (entry.getKey().isBlank() || !entry.getValue().isTextual()
                    || entry.getValue().asText().isBlank()) {
                throw new IllegalArgumentException(
                        "Métadonnées des composants invalides pour '%s'."
                                .formatted(entry.getKey()));
            }
            categories.put(entry.getKey(), entry.getValue().asText());
        });
        return categories;
    }
}
