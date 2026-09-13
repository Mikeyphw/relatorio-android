#!/usr/bin/env python3
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
errors = []

def read(relative):
    return (ROOT / relative).read_text(encoding="utf-8")

wrapper = read("gradle/wrapper/gradle-wrapper.properties")
devtool = read(".devtool.toml")
app = read("app/build.gradle.kts")
persistence = read("persistence/build.gradle.kts")
core = read("core-model/build.gradle.kts")
gradlew = read("gradlew")

checks = {
    "Gradle wrapper 9.7.1":
        "gradle-9.7.1-bin.zip" in wrapper,

    "Gradle checksum":
        "acd53f1edaf02f1a8ff99879f8a34b302661a057d9b063ae9e35b552f804d20a" in wrapper,

    "Devtool Gradle 9.7.1":
        re.search(r'(?m)^version = "9\.7\.1"$', devtool)
        is not None,

    "app NDK 29.0.14206865":
        'ndkVersion = "29.0.14206865"' in app,

    "persistence NDK 29.0.14206865":
        'ndkVersion = "29.0.14206865"' in persistence,

    "app Java 21 compile":
        app.count("JavaVersion.VERSION_21") >= 2,

    "persistence Java 21 compile":
        persistence.count("JavaVersion.VERSION_21") >= 2,

    "app Java 21 toolchain":
        "JavaLanguageVersion.of(21)" in app,

    "persistence Java 21 toolchain":
        "JavaLanguageVersion.of(21)" in persistence,

    "core JVM 21":
        "jvmToolchain(21)" in core,

    "gradlew Gradle hint":
        "gradle wrapper --gradle-version 9.7.1" in gradlew,
}

for name, passed in checks.items():
    if not passed:
        errors.append(name)

for relative, text in {
    "app/build.gradle.kts": app,
    "persistence/build.gradle.kts": persistence,
    "core-model/build.gradle.kts": core,
    ".devtool.toml": devtool,
    "gradle/wrapper/gradle-wrapper.properties": wrapper,
    "gradlew": gradlew,
}.items():
    for forbidden in (
        "VERSION_17",
        "jvmToolchain(17)",
        "9.4.1",
    ):
        if forbidden in text:
            errors.append(
                f"legacy reference {forbidden!r} remains in {relative}"
            )

if errors:
    print("Toolchain baseline validation FAILED:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Toolchain baseline validation PASSED")
print("Gradle: 9.7.1")
print("Java/JVM: 21")
print("Android NDK: 29.0.14206865")
