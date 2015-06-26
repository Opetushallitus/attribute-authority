import com.earldouglas.xsbtwebplugin.PluginKeys._
import com.earldouglas.xsbtwebplugin.WebPlugin
import org.scalatra.sbt._
import sbt.Keys._
import sbt._
import sbtbuildinfo.Plugin._


object AttributeAuthorityBuild extends Build {
  val Organization = "fi.vm.sade"
  val Name = "attributeauthority"
  val Version = "0.1.0-SNAPSHOT"
  val ScalaVersion = "2.11.1"
  val ScalatraVersion = "2.3.0"
  val artifactory = "https://artifactory.oph.ware.fi/artifactory/"

  lazy val project = Project (
    "attribute-authority",
    file("."),
    settings = ScalatraPlugin.scalatraWithJRebel ++ WebPlugin.webSettings ++ buildInfoSettings ++ Seq(
      organization := Organization,
      name := Name,
      version := Version,
      scalaVersion := ScalaVersion,
      resolvers += Classpaths.typesafeReleases,
      sourceGenerators in Compile <+= buildInfo,
      buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
      buildInfoPackage := "fi.vm.sade.attributeauthority",
      libraryDependencies ++= Seq(
        "org.scalatra" %% "scalatra" % ScalatraVersion,
        "org.scalatra" %% "scalatra-swagger" % ScalatraVersion,
        "org.json4s" %% "json4s-jackson" % "3.2.10",
        "org.scalatra" %% "scalatra-specs2" % ScalatraVersion % "test",
        "com.typesafe" % "config" % "1.2.1",
        "org.scalaj" %% "scalaj-http" % "0.3.15",
        "org.slf4j" % "slf4j-log4j12" % "1.7.7",
        "log4j" % "log4j" % "1.2.17",
        "com.github.cb372" %% "scalacache-guava" % "0.6.3",
        "org.eclipse.jetty" % "jetty-webapp" % "9.1.3.v20140225" % "container",
        "org.eclipse.jetty" % "jetty-plus" % "9.1.3.v20140225" % "container",
        "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016" % "container;provided;test" artifacts (Artifact("javax.servlet", "jar", "jar"))
      ),
      artifactName <<= (name in (Compile, packageWar)) { projectName =>
        (config: ScalaVersion, module: ModuleID, artifact: Artifact) =>
          var newName = projectName
          if (module.revision.nonEmpty) {
            newName += "-" + module.revision
          }
          newName + "." + artifact.extension
      },
      artifactPath in (Compile, packageWar) ~= { defaultPath =>
        file("target") / defaultPath.getName
      },
      credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
      publishTo := {
        if (Version.trim.endsWith("SNAPSHOT"))
          Some("snapshots" at artifactory + "/oph-sade-snapshot-local;build.timestamp=" + new java.util.Date().getTime)
        else
          Some("releases" at artifactory + "/oph-sade-release-local")
      }
    )
  )
}
