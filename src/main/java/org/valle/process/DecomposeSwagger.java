package org.valle.process;

import org.valle.process.models.DecomposedSwagger;

public interface DecomposeSwagger {

    /**
     * Décompose un document Swagger en plusieurs parties.
     * Le document principal possède des réferences vers les éléments paths.
     * Les fichiers paths possèdent des références vers les éléments components. Un sidecar
     * {@code component-categories.json} conserve la catégorie OpenAPI de chaque fichier
     * composant ; son absence conserve la convention historique {@code schemas}.
     *
     * @return {@link DecomposedSwagger}
     */
    DecomposedSwagger execute();
}
