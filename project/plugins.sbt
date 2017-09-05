
plugins_(
  "io.get-coursier"   % "sbt-coursier"    % coursierVersion,
  "com.typesafe"      % "sbt-mima-plugin" % "0.1.15",
  "org.xerial.sbt"    % "sbt-pack"        % "0.9.0",
  "com.jsuereth"      % "sbt-pgp"         % "1.1.0-M1",
  "com.lightbend.sbt" % "sbt-proguard"    % "0.3.0",
  "com.github.gseitz" % "sbt-release"     % "1.0.6",
  "org.scala-js"      % "sbt-scalajs"     % "0.6.19",
  "io.get-coursier"   % "sbt-shading"     % coursierVersion,
  "org.xerial.sbt"    % "sbt-sonatype"    % "2.0",
  "com.timushev.sbt"  % "sbt-updates"     % "0.3.1",
  "org.tpolecat"      % "tut-plugin"      % "0.6.0"
)

libs ++= Seq(
  "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value,
  compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full), // for shapeless / auto type class derivations
  "com.github.alexarchambault" %% "argonaut-shapeless_6.2" % "1.2.0-M5"
)

// important: this line is matched / substituted during releases (via sbt-release)
def coursierVersion = "1.0.0-RC11"

// required for just released things
resolvers += Resolver.sonatypeRepo("releases")


def plugins_(modules: ModuleID*) = modules.map(addSbtPlugin)
def libs = libraryDependencies
