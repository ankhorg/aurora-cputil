dependencies {
    api(project(":"))

    compileOnly("org.apache.logging.log4j:log4j-api:2.23.1")
    // https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-slf4j-impl
    testImplementation("org.apache.logging.log4j:log4j-slf4j-impl:2.23.1")

}