./gradlew shadowJar
java -jar build/libs/swagger-organiser-1.0-SNAPSHOT-all.jar -sf src/main/resources/swagger-cobaye.yml -toRm post:/cadh/v1/operations -pf -d
