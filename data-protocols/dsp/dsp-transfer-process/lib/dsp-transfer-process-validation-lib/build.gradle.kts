plugins {
    `java-library`
}

dependencies {
    api(project(":data-protocols:dsp:dsp-spi"))
    implementation(project(":core:common:lib:validator-lib"))
    
    testImplementation(project(":core:common:junit"))
}