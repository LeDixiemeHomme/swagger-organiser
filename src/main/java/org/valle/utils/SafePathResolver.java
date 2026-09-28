package org.valle.utils;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * Resolves user- or document-provided relative paths without leaving an allowed root.
 */
public final class SafePathResolver {

    private SafePathResolver() {
    }

    public static Path resolveWithin(Path root, String relativePath, String description) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException(description + " ne peut pas être vide.");
        }

        try {
            Path normalizedRoot = root.toAbsolutePath().normalize();
            Path requestedPath = Path.of(relativePath);
            if (requestedPath.isAbsolute()) {
                throw invalidPath(description, relativePath);
            }

            Path resolvedPath = normalizedRoot.resolve(requestedPath).normalize();
            if (!resolvedPath.startsWith(normalizedRoot)) {
                throw invalidPath(description, relativePath);
            }
            return resolvedPath;
        } catch (InvalidPathException exception) {
            throw new IllegalArgumentException(
                    description + " invalide : " + relativePath, exception);
        }
    }

    private static IllegalArgumentException invalidPath(String description, String path) {
        return new IllegalArgumentException(
                description + " sort du répertoire autorisé : " + path);
    }
}
