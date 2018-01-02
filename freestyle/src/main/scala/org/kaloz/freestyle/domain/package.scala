package org.kaloz

package object freestyle {

  final case class Prisoner(name: String)

  sealed trait Decision
  case object Guilty extends Decision
  case object Silence extends Decision

  final case class Verdict(years: Int)

  final case class Strategy(f: Prisoner => Decision)

  def verdict(prisonerDecision: Decision,
              otherPrisonerDecision: Decision): Verdict = {
    if (prisonerDecision == Silence && otherPrisonerDecision == Silence)
      Verdict(1)
    else if (prisonerDecision == Guilty && otherPrisonerDecision == Silence)
      Verdict(0)
    else
      Verdict(3)
  }

}
