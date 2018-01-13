scala_library(
    name = "cli",
    dependencies = [
        "3rdparty/jvm:argonaut-shapeless",
        "3rdparty/jvm:caseapp",
        "cache/src/main/scala:cache",
        "core:core",
        "extra/src/main/scala/coursier/extra:extra",
        "extra/src/main/scala-2.11/coursier/extra:native",
        ":util",
        ":options"
    ],
    sources = globs(
        "coursier/cli/scaladex/*.scala",
        "coursier/cli/spark/*.scala",
        "coursier/cli/*.scala",
        exclude=["coursier/cli/Options.scala"]
    ),
)

scala_library(
    name = "util",
    dependencies = [
        "3rdparty/jvm:argonaut-shapeless",
        "cache/src/main/scala:cache",
        "core:core",
    ],
    sources = globs("coursier/cli/util/*.scala"),
)

scala_library(
    name = "options",
    dependencies = [
        "3rdparty/jvm:caseapp",
        "cache/src/main/scala:cache",
    ],
    sources = globs("coursier/cli/options/*.scala"),
)

scala_library(
    name = "spark",
    dependencies = [
        "cache/src/main/scala:cache",
        ":options",
        ":cli"
    ],
    sources = globs("coursier/cli/spark/*.scala"),
)



jvm_binary(
    name = "coursier-cli",
    basename = "coursier-cli",
    dependencies = [
        ":cli",
    ],
    main = "coursier.cli.Coursier",
)
