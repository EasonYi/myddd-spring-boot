plugins {
    `java-library`
    idea
}

group = "org.myddd.extensions.organisation"
version = rootProject.extra["projectVersion"]!!

dependencies {
    implementation(project(":myddd-domain"))

    implementation(project(":myddd-extensions:security:security-api"))
    implementation("com.google.code.gson:gson:${rootProject.extra["gson_version"]}")

    testImplementation(project(":myddd-ioc:myddd-ioc-spring"))
    testImplementation(project(":myddd-extensions:organisation:organisation-infra"))
    testImplementation(project(":myddd-extensions:security:security-api"))

}
