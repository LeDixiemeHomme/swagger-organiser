package org.valle.process;

import org.valle.process.models.SwaggerNode;

/**
 * Interface pour fusionner un document Swagger décomposé en un seul fichier.
 * Résout toutes les références $ref et remplace les fichiers externes par leur contenu.
 */
public interface MergeSwagger {

    /**
     * Fusionne un document Swagger décomposé en un seul fichier unifié.
     *
     * @return {@link SwaggerNode} contenant le swagger fusionné
     */
    SwaggerNode execute();
}

