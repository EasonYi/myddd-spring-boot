plugins {
    `java-library`
}

group = "org.myddd"
version = rootProject.extra["projectVersion"]!!

extra["publishJar"] = true


dependencies {
    api("com.google.guava:guava:${rootProject.extra["guava.version"]}")


    api("org.apache.commons:commons-lang3:${rootProject.extra["commons-lang3.version"]}")
    api("com.google.code.gson:gson:${rootProject.extra["gson.version"]}")
    api("org.slf4j:slf4j-api:${rootProject.extra["slf4jVersion"]}")


    testImplementation("org.slf4j:slf4j-log4j12:${rootProject.extra["slf4jVersion"]}")
}