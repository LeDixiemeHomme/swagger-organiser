plugins {
    id("java")
}

group = "org.valle"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")
    // Utilisation de la version stable de Lanterna avec gui2 inclus
    implementation("com.googlecode.lanterna:lanterna:3.1.2")
    implementation("info.picocli:picocli:4.7.5")
    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("org.slf4j:slf4j-simple:2.0.13")
    implementation("org.projectlombok:lombok:1.18.32")
    // https://mvnrepository.com/artifact/jakarta.validation/jakarta.validation-api
    implementation("jakarta.validation:jakarta.validation-api:3.1.1")
    // https://mvnrepository.com/artifact/jakarta.el/jakarta.el-api
    implementation("jakarta.el:jakarta.el-api:6.0.1")
    // https://mvnrepository.com/artifact/jakarta.annotation/jakarta.annotation-api
    implementation("jakarta.annotation:jakarta.annotation-api:3.0.0")
    implementation("org.hibernate.validator:hibernate-validator:7.0.5.Final")
    implementation("org.glassfish:jakarta.el:4.0.2") // nécessaire pour l’EL

    annotationProcessor("org.projectlombok:lombok:1.18.32")
    annotationProcessor("info.picocli:picocli-codegen:4.7.5")

    testImplementation("org.assertj:assertj-core:3.25.3")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}