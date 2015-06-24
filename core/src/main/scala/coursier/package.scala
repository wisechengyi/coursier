import scalaz.EitherT
import scalaz.concurrent.Task

/**
 * Pulls definitions from coursier.core, with default arguments.
 */
package object coursier {

  type Dependency = core.Dependency
  object Dependency {
    def apply(module: Module,
              version: String,
              scope: Scope = Scope.Other(""), // Substituted by Resolver with its own default scope (compile)
              attributes: Attributes = Attributes(),
              exclusions: Set[(String, String)] = Set.empty,
              optional: Boolean = false): Dependency =
      core.Dependency(module, version, scope, attributes, exclusions, optional)
  }

  type Attributes = core.Attributes
  object Attributes {
    def apply(`type`: String = "jar",
              classifier: String = ""): Attributes =
      core.Attributes(`type`, classifier)
  }

  type Project = core.Project
  object Project {
    def apply(module: Module,
              version: String,
              dependencies: Seq[Dependency] = Seq.empty,
              parent: Option[ModuleVersion] = None,
              dependencyManagement: Seq[Dependency] = Seq.empty,
              properties: Map[String, String] = Map.empty,
              profiles: Seq[Profile] = Seq.empty,
              versions: Option[core.Versions] = None): Project =
      core.Project(module, version, dependencies, parent, dependencyManagement, properties, profiles, versions)
  }

  type Profile = core.Profile
  object Profile {
    type Activation = core.Activation
    object Activation {
      def apply(properties: Seq[(String, Option[String])] = Nil): Activation =
        core.Activation(properties)
    }

    def apply(id: String,
              activeByDefault: Option[Boolean] = None,
              activation: Activation = Activation(),
              dependencies: Seq[Dependency] = Nil,
              dependencyManagement: Seq[Dependency] = Nil,
              properties: Map[String, String] = Map.empty) =
      core.Profile(id, activeByDefault, activation, dependencies, dependencyManagement, properties)
  }

  type Module = core.Module
  object Module {
    def apply(organization: String, name: String): Module =
      core.Module(organization, name)
  }

  type ModuleVersion = (core.Module, String)

  type Scope = core.Scope
  val Scope: core.Scope.type = core.Scope

  type Repository = core.Repository

  def fetchFrom(repositories: Seq[Repository]): ModuleVersion => EitherT[Task, List[String], (Repository, Project)] =
    modVersion => core.Resolution.find(repositories, modVersion._1, modVersion._2)

  type Resolution = core.Resolution
  object Resolution {
    val empty = apply()
    def apply(rootDependencies: Set[Dependency] = Set.empty,
              dependencies: Set[Dependency] = Set.empty,
              conflicts: Set[Dependency] = Set.empty,
              projectCache: Map[ModuleVersion, (Repository, Project)] = Map.empty,
              errorCache: Map[ModuleVersion, Seq[String]] = Map.empty,
              filter: Option[Dependency => Boolean] = None,
              profileActivation: Option[(String, Profile.Activation, Map[String, String]) => Boolean] = None): Resolution =
      core.Resolution(rootDependencies, dependencies, conflicts, projectCache, errorCache, filter, profileActivation)
  }

  def resolve(dependencies: Set[Dependency],
              fetch: ModuleVersion => EitherT[Task, List[String], (Repository, Project)],
              maxIterations: Option[Int] = Some(200),
              filter: Option[Dependency => Boolean] = None,
              profileActivation: Option[(String, Profile.Activation, Map[String, String]) => Boolean] = None): Task[Resolution] = {

    val startResolution = Resolution(
      dependencies, Set.empty, Set.empty,
      Map.empty, Map.empty,
      filter,
      profileActivation
    )

    startResolution.last(fetch, maxIterations.getOrElse(-1))
  }

  type Artifact = core.Artifact
  object Artifact {
    def apply(url: String,
              extra: Map[String, String] = Map.empty,
              attributes: Attributes = Attributes()): Artifact =
      core.Artifact(url, extra, attributes)
  }

  type MavenRepository[G <: core.FetchMetadata] = core.MavenRepository[G]
  val MavenRepository: core.MavenRepository.type = core.MavenRepository

}