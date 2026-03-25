plugins {
    id("java")
    application
    alias(libs.plugins.shadow)
    alias(libs.plugins.openapi.generator)
}

group = "org.valle"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.jackson.databind)
    implementation(libs.jackson.dataformat.yaml)
    // Utilisation de la version stable de Lanterna avec gui2 inclus
    implementation(libs.lanterna)
    implementation(libs.picocli)
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.simple)
    implementation(libs.lombok)
    // https://mvnrepository.com/artifact/jakarta.validation/jakarta.validation-api
    implementation(libs.jakarta.validation.api)
    // https://mvnrepository.com/artifact/jakarta.el/jakarta.el-api
    implementation(libs.jakarta.el.api)
    // https://mvnrepository.com/artifact/jakarta.annotation/jakarta.annotation-api
    implementation(libs.jakarta.annotation.api)
    implementation(libs.hibernate.validator)
    implementation(libs.glassfish.jakarta.el) // nécessaire pour l'EL

    annotationProcessor(libs.lombok)
    annotationProcessor(libs.picocli.codegen)

    testImplementation(libs.assertj.core)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}

// ── OpenAPI Generator ────────────────────────────────────────────────────────
// Génère une documentation HTML interactive (html2) à partir de openapi.yml.
// Résultat : build/generated/swagger-doc/index.html
openApiGenerate {
    generatorName.set("html2")
    inputSpec.set("${rootDir}/src/main/resources/openapi.yml")
    outputDir.set(layout.buildDirectory.dir("generated/swagger-doc").get().asFile.path)
    skipValidateSpec.set(false)
    // Pas de stubs ni de modèles générés — doc uniquement
    generateApiTests.set(false)
    generateModelTests.set(false)
    generateApiDocumentation.set(false)
    generateModelDocumentation.set(false)
}

// Inclure le index.html généré dans le JAR sous swagger-doc/
tasks.named<ProcessResources>("processResources") {
    dependsOn("openApiGenerate")
    from(layout.buildDirectory.dir("generated/swagger-doc")) {
        include("index.html")
        into("swagger-doc")
    }
}

application {
    mainClass.set("org.valle.present.picocli.CliApp")
}

tasks.shadowJar {
    mergeServiceFiles()
}
