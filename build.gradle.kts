plugins {
    id("java")
    application
    alias(libs.plugins.shadow)
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

application {
    mainClass.set("org.valle.present.picocli.CliApp")
}