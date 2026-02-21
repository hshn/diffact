ThisBuild / scalaVersion := "3.8.1"
useReadableConsoleGit
ThisBuild / organization := "dev.hshn"
ThisBuild / homepage     := Some(url("https://github.com/hshn/diffact"))
ThisBuild / licenses     := List("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / developers   := List(
  Developer("hshn", "Shota Hoshino", "sht.hshn@gmail.com", url("https://github.com/hshn"))
)
ThisBuild / description   := "Structural diff detection library for Scala 3"
ThisBuild / versionScheme := Some("early-semver")

val zioVersion = "2.1.22"

lazy val diffact = (project in file(".") withId "diffact")
  .aggregate(diffactCore, diffactZio, diffactSlick, diffactZioSlick)
  .settings(
    publish / skip := true
  )

lazy val diffactCore = (project in file("diffact-core"))
  .settings(
    name := "diffact",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-test"     % zioVersion % Test,
      "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  )

lazy val diffactZio = (project in file("diffact-zio"))
  .settings(
    name := "diffact-zio",
    libraryDependencies ++= Seq(
      "dev.zio"       %% "zio-prelude"  % "1.0.0-RC45",
      "org.typelevel" %% "cats-core"    % "2.13.0",
      "dev.zio"       %% "zio-test"     % zioVersion % Test,
      "dev.zio"       %% "zio-test-sbt" % zioVersion % Test,
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  )
  .dependsOn(diffactCore)

lazy val diffactSlick = (project in file("diffact-slick"))
  .settings(
    name := "diffact-slick",
    libraryDependencies ++= Seq(
      "com.typesafe.slick" %% "slick"        % "3.6.1",
      "org.typelevel"      %% "cats-core"    % "2.13.0",
      "dev.zio"            %% "zio-test"     % zioVersion % Test,
      "dev.zio"            %% "zio-test-sbt" % zioVersion % Test,
      "com.h2database"      % "h2"           % "2.2.224"  % Test,
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  )
  .dependsOn(diffactCore)

lazy val diffactZioSlick = (project in file("diffact-zio-slick"))
  .settings(
    name := "diffact-zio-slick",
    libraryDependencies ++= Seq(
      "com.typesafe.slick" %% "slick"        % "3.6.1",
      "dev.zio"            %% "zio-test"     % zioVersion % Test,
      "dev.zio"            %% "zio-test-sbt" % zioVersion % Test,
      "com.h2database"      % "h2"           % "2.2.224"  % Test,
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  )
  .dependsOn(diffactZio)
