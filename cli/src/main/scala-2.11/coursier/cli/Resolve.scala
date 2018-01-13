package coursier
package cli

import caseapp._
import coursier.cli.options.CommonOptions

final case class Resolve(
  @Recurse
    common: CommonOptions
) extends App {

  // the `val helper = ` part is needed because of DelayedInit it seems
  val helper = new Helper(common, remainingArgs, printResultStdout = true)

}
