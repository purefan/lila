package lila.tutor

import chess.Color
import scala.concurrent.ExecutionContext

import lila.common.LilaOpeningFamily
import lila.insight.{ Filter, InsightApi, InsightDimension, Metric, Phase, Question }

case class TutorColorOpenings(
    families: List[TutorOpeningFamily]
)

case class TutorOpeningFamily(
    family: LilaOpeningFamily,
    games: TutorMetric[TutorRatio],
    performance: TutorMetric[Double],
    acpl: TutorMetricOption[Double]
)

private case object TutorOpening {

  import TutorBuilder._

  def compute(user: TutorUser)(implicit
      insightApi: InsightApi,
      ec: ExecutionContext
  ): Fu[Color.Map[TutorColorOpenings]] = for {
    whiteOpenings <- computeOpenings(user, Color.White)
    blackOpenings <- computeOpenings(user, Color.Black)
  } yield Color.Map(whiteOpenings, blackOpenings)

  def computeOpenings(user: TutorUser, color: Color)(implicit
      insightApi: InsightApi,
      ec: ExecutionContext
  ): Fu[TutorColorOpenings] = {
    for {
      myPerfs   <- insightApi.ask(perfQuestion(color), user.user, withPovs = false) map Answer.apply
      peerPerfs <- insightApi.askPeers(myPerfs.alignedQuestion, user.rating) map Answer.apply
      performances = Answers(myPerfs, peerPerfs)
      acplQuestion = myPerfs.alignedQuestion
        .copy(metric = Metric.MeanCpl)
        .add(Filter(InsightDimension.Phase, List(Phase.Opening, Phase.Middle)))
      acpls <- answers(acplQuestion, user)
    } yield TutorColorOpenings {
      performances.mine.list.map { case (family, myValue, myCount) =>
        TutorOpeningFamily(
          family,
          games = performances.countMetric(family, myCount),
          performance = performances.valueMetric(family, myValue),
          acpl = acpls valueMetric family
        )
      }
    }
  }

  def perfQuestion(color: Color) = Question(
    InsightDimension.OpeningFamily,
    Metric.Performance,
    List(standardFilter, colorFilter(color))
  )
}