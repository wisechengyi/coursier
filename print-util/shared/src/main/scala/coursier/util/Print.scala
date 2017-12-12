package coursier.util

import coursier.Artifact
import coursier.core.{Attributes, Dependency, Module, Orders, Project, Resolution}

import scala.io.AnsiColor

object Print {

  def dependency(dep: Dependency): String =
    dependency(dep, printExclusions = false)

  def dependency(dep: Dependency, printExclusions: Boolean): String = {

    def exclusionsStr = dep
      .exclusions
      .toVector
      .sorted
      .map {
        case (org, name) =>
          s"\n  exclude($org, $name)"
      }
      .mkString

    s"${dep.module}:${dep.version}:${dep.configuration}" + (if (printExclusions) exclusionsStr else "")
  }

  def dependenciesUnknownConfigs(deps: Seq[Dependency], projects: Map[(Module, String), Project]): String =
    dependenciesUnknownConfigs(deps, projects, printExclusions = false)

  def dependenciesUnknownConfigs(
    deps: Seq[Dependency],
    projects: Map[(Module, String), Project],
    printExclusions: Boolean
  ): String = {

    val deps0 = deps.map { dep =>
      dep.copy(
        version = projects
          .get(dep.moduleVersion)
          .fold(dep.version)(_.version)
      )
    }

    val minDeps = Orders.minDependencies(
      deps0.toSet,
      _ => Map.empty
    )

    val deps1 = minDeps
      .groupBy(_.copy(configuration = "", attributes = Attributes("", "")))
      .toVector
      .map { case (k, l) =>
        k.copy(configuration = l.toVector.map(_.configuration).sorted.distinct.mkString(";"))
      }
      .sortBy { dep =>
        (dep.module.organization, dep.module.name, dep.module.toString, dep.version)
      }

    deps1.map(dependency(_, printExclusions)).mkString("\n")
  }

  private def compatibleVersions(first: String, second: String): Boolean = {
    // too loose for now
    // e.g. RCs and milestones should not be considered compatible with subsequent non-RC or
    // milestone versions - possibly not with each other either

    first.split('.').take(2).toSeq == second.split('.').take(2).toSeq
  }

  def dependencyTree(
    roots: Seq[Dependency],
    resolution: Resolution,
    printExclusions: Boolean,
    reverse: Boolean,
    jsonPrintRequirement: Option[JsonPrintRequirement] = Option.empty
  ): String =
    dependencyTree(roots, resolution, printExclusions, reverse, colors = true, jsonPrintRequirement)


  case class Elem(dep: Dependency,
                  artifacts: Seq[(Dependency, Artifact)] = Seq(),
                  jsonPrintRequirement: Option[JsonPrintRequirement],
                  resolution: Resolution,
                  colors: Boolean,
                  printExclusions: Boolean,
                  excluded: Boolean) {

    val (red, yellow, reset) =
      if (colors)
        (Console.RED, Console.YELLOW, Console.RESET)
      else
        ("", "", "")

    // This is used to printing json output
    // Seq of (classifier, file path) tuple
    lazy val downloadedFiles: Seq[(String, String)] = {
      jsonPrintRequirement match {
        case Some(req) =>
          req.depToArtifacts.getOrElse(dep, Seq())
            .map(x => (x.classifier, req.fileByArtifact.get(x.url)))
            .filter(_._2.isDefined)
            .map( x => (x._1, x._2.get.getPath))
        case None => Seq()
      }
    }

    lazy val reconciledVersion: String = resolution.reconciledVersions
      .getOrElse(dep.module, dep.version)

    // These are used to printing json output
    val reconciledVersionStr = s"${dep.module}:$reconciledVersion"
    val requestedVersionStr = s"${dep.module}:${dep.version}"

    lazy val repr =
      if (excluded)
        resolution.reconciledVersions.get(dep.module) match {
          case None =>
            s"$yellow(excluded)$reset ${dep.module}:${dep.version}"
          case Some(version) =>
            val versionMsg =
              if (version == dep.version)
                "this version"
              else
                s"version $version"

            s"${dep.module}:${dep.version} " +
              s"$red(excluded, $versionMsg present anyway)$reset"
        }
      else {
        val versionStr =
          if (reconciledVersion == dep.version)
            dep.version
          else {
            val assumeCompatibleVersions = compatibleVersions(dep.version, reconciledVersion)

            (if (assumeCompatibleVersions) yellow else red) +
              s"${dep.version} -> $reconciledVersion" +
              (if (assumeCompatibleVersions || colors) "" else " (possible incompatibility)") +
              reset
          }

        s"${dep.module}:$versionStr"
      }

    lazy val children: Seq[Elem] =
      if (excluded)
        Nil
      else {
        val dep0 = dep.copy(version = reconciledVersion)

        val dependencies = resolution.dependenciesOf(
          dep0,
          withReconciledVersions = false
        ).sortBy { trDep =>
          (trDep.module.organization, trDep.module.name, trDep.version)
        }

        def excluded = resolution
          .dependenciesOf(
            dep0.copy(exclusions = Set.empty),
            withReconciledVersions = false
          )
          .sortBy { trDep =>
            (trDep.module.organization, trDep.module.name, trDep.version)
          }
          .map(_.moduleVersion)
          .filterNot(dependencies.map(_.moduleVersion).toSet).map {
          case (mod, ver) =>
            Elem(
              Dependency(mod, ver, "", Set.empty, Attributes("", ""), false, false),
              artifacts,
              jsonPrintRequirement,
              resolution,
              colors,
              printExclusions,
              excluded = true
            )
        }

        dependencies.map(Elem(_, artifacts, jsonPrintRequirement, resolution, colors, printExclusions, excluded = false)) ++
          (if (printExclusions) excluded else Nil)
      }
  }

  def dependencyTree(
    roots: Seq[Dependency],
    resolution: Resolution,
    printExclusions: Boolean,
    reverse: Boolean,
    colors: Boolean,
    jsonPrintRequirement: Option[JsonPrintRequirement]
  ): String = {

    val (red, yellow, reset) =
      if (colors)
        (Console.RED, Console.YELLOW, Console.RESET)
      else
        ("", "", "")



    if (jsonPrintRequirement.isDefined) {
      // NB: This value has to be eagerly computed, otherwise later it will be called many times to cause OOM.
      val artifacts: Seq[(Dependency, Artifact)] = resolution.dependencyArtifacts
      JsonReport(roots.toVector.map(Elem(_, artifacts, jsonPrintRequirement, resolution, colors, printExclusions, excluded = false)), jsonPrintRequirement.get.conflictResolutionForRoots)(_.children, _.reconciledVersionStr, _.requestedVersionStr, _.downloadedFiles)
    }
    else if (reverse) {

      final case class Parent(
        module: Module,
        version: String,
        dependsOn: Module,
        wantVersion: String,
        gotVersion: String,
        excluding: Boolean
      ) {
        lazy val repr: String =
          if (excluding)
            s"$yellow(excluded by)$reset $module:$version"
          else if (wantVersion == gotVersion)
            s"$module:$version"
          else {
            val assumeCompatibleVersions = compatibleVersions(wantVersion, gotVersion)

            s"$module:$version " +
              (if (assumeCompatibleVersions) yellow else red) +
              s"(wants $dependsOn:$wantVersion, got $gotVersion)" +
              reset
          }
      }

      val parents: Map[Module, Seq[Parent]] = {
        val links = for {
          dep <- resolution.dependencies.toVector
          elem <- Elem(dep, artifacts = Seq(), jsonPrintRequirement = Option.empty, resolution, colors, printExclusions, excluded = false).children
        }
          yield elem.dep.module -> Parent(
            dep.module,
            dep.version,
            elem.dep.module,
            elem.dep.version,
            elem.reconciledVersion,
            elem.excluded
          )

        links
          .groupBy(_._1)
          .mapValues(_.map(_._2).distinct.sortBy(par => (par.module.organization, par.module.name)))
          .iterator
          .toMap
      }

      def children(par: Parent) =
        if (par.excluding)
          Nil
        else
          parents.getOrElse(par.module, Nil)

      Tree(
        resolution
          .dependencies
          .toVector
          .sortBy(dep => (dep.module.organization, dep.module.name, dep.version))
          .map(dep =>
            Parent(dep.module, dep.version, dep.module, dep.version, dep.version, excluding = false)
          )
      )(children, _.repr)
    } else {
      Tree(roots.toVector.map(Elem(_, artifacts=Seq(), jsonPrintRequirement = Option.empty, resolution, colors, printExclusions, excluded = false)))(_.children, _.repr)
    }
  }
}
