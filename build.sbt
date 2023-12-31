val V = new {
  val Scala      = "3.3.0"
  val ScalaGroup = "3.3"

  val catsEffect = "3.5.1"
  val tapir      = "1.6.4"
  val sttp       = "3.8.16"
  val sttp4      = "4.0.0-M1"

  val pureconfig = "0.17.4"
  val jasync     = "2.2.2"
  val sttpOpenAi = "0.0.7"
  val scodecBits = "1.1.37"

  val tyrian        = "0.7.1"
  val scalaJavaTime = "2.5.0"

  val organiseImports = "0.6.0"
  val zerowaste       = "0.2.7"

  val scribe          = "3.11.9"
  val hedgehog        = "0.10.1"
  val munitCatsEffect = "2.0.0-M3"
}

val Dependencies = new {

  lazy val backend = Seq(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-armeria-server-cats" % V.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe"          % V.tapir,
      "com.outr"                    %% "scribe-slf4j"              % V.scribe,
      "com.github.pureconfig"         %% "pureconfig-core"      % V.pureconfig,
      "com.github.jasync-sql"          % "jasync-postgresql"    % V.jasync,
      "com.softwaremill.sttp.client3" %% "armeria-backend-cats" % V.sttp,
      "com.softwaremill.sttp.openai"  %% "core"                 % V.sttpOpenAi,
      "com.softwaremill.sttp.client4" %% "armeria-backend-cats" % V.sttp4,
      "org.scodec"                    %% "scodec-bits"          % V.scodecBits,
    ),
    excludeDependencies ++= Seq(
      "com.lihaoyi" % "geny_2.13",
    ),
  )

  lazy val frontend = Seq(
    libraryDependencies ++= Seq(
      "io.indigoengine"               %%% "tyrian-io"         % V.tyrian,
      "com.softwaremill.sttp.tapir"   %%% "tapir-sttp-client" % V.tapir,
      "com.softwaremill.sttp.client3" %%% "cats"              % V.sttp,
      "io.github.cquiroz"             %%% "scala-java-time"   % V.scalaJavaTime,
      "com.outr"                      %%% "scribe"            % V.scribe,
    ),
  )

  lazy val common = Seq(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %%% "tapir-json-circe" % V.tapir,
    ),
  )

  lazy val commonJVM = Seq(
  )

  lazy val commonJS = Seq(
  )

  lazy val exampleBasedTest = Def.settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "munit-cats-effect" % V.munitCatsEffect % Test,
    ),
    Test / fork := true,
  )

  lazy val propertyBasedTest = Def.settings(
    libraryDependencies ++= Seq(
      "qa.hedgehog" %%% "hedgehog-munit" % V.hedgehog % Test,
    ),
    Test / fork := true,
  )
}

ThisBuild / organization := "nocold1211.github.io"
ThisBuild / version      := "0.0.1-SNAPSHOT"
ThisBuild / scalaVersion := V.Scala
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % V.organiseImports
ThisBuild / semanticdbEnabled := true
Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val root = (project in file("."))
  .aggregate(frontend, backend)

lazy val common = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/common"))
  .settings(Dependencies.common)
  .settings(Dependencies.exampleBasedTest)
  .jvmSettings(Dependencies.commonJVM)
  .jsSettings(Dependencies.commonJS)
  .jsSettings(
    useYarn := true,
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.CommonJSModule)
    },
    scalacOptions ++= Seq(
      "-scalajs",
      "-Wunused:all",
    ),
    Test / fork := false,
    Compile / compile / wartremoverErrors ++= Warts
      .allBut(Wart.SeqApply, Wart.SeqUpdated, Wart.Any),
  )
  .jsConfigure { project =>
    project
      .enablePlugins(ScalablyTypedConverterPlugin)
  }

lazy val backend = (project in file("modules/backend"))
  .settings(Dependencies.backend)
  .settings(Dependencies.exampleBasedTest)
  .settings(
    name := "study-assistant-backend",
    assemblyMergeStrategy := {
      case x if x `contains` "io.netty.versions.properties" =>
        MergeStrategy.first
      case x if x `contains` "module-info.class" => MergeStrategy.discard
      case PathList("io", "getquill", xs @ _*)   => MergeStrategy.first
      case x =>
        val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
        oldStrategy(x)
    },
    scalacOptions ++= Seq(
//      "-explain",
      "-Wunused:all",
    ),
    Compile / compile / wartremoverErrors ++= Warts.allBut(
      Wart.TripleQuestionMark,
      Wart.DefaultArguments,
    ),
  )
  .dependsOn(common.jvm, dbAccess)

lazy val frontend = (project in file("modules/frontend"))
  .enablePlugins(ScalaJSPlugin)
  .enablePlugins(ScalablyTypedConverterExternalNpmPlugin)
  .settings(Dependencies.frontend)
  .settings(Dependencies.exampleBasedTest)
  .settings(
    name                := "study-assistant-frontend",
    Compile / mainClass := Some("dev.nocold.assistant.frontend.FrontendMain"),
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
    scalaJSLinkerConfig ~= { _.withSourceMap(false) },
    externalNpm := {
      scala.sys.process.Process("yarn", baseDirectory.value).!
      baseDirectory.value
    },
    scalacOptions ++= Seq(
      "-scalajs",
      "-Wunused:privates",
      "-Wunused:locals",
      "-Wunused:explicits",
      "-Wunused:implicits",
      "-Wunused:params",
    ),
    stIgnore ++= List("parcel"),
  )
  .dependsOn(common.js)
