package scalacache.common

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Future
import scalacache._
import scalacache.serialization.Codec
import scalacache.serialization.Codec.DecodingResult
import scalacache.serialization.circe._

object Snack {
  val Jagabee: Snack = Snack("Jagabee")
}

case class Snack(name: String)

class DummySnackCodec extends Codec[Snack] {
  var serialiserUsed = false
  var deserialiserUsed = false

  override def encode(value: Snack): Array[Byte] = {
    serialiserUsed = true
    Array.empty
  }

  override def decode(bytes: Array[Byte]): DecodingResult[Snack] = {
    deserialiserUsed = true
    Right(Snack.Jagabee)
  }

}

trait LegacyCodecCheckSupport { this: FlatSpec with Matchers with ScalaFutures with IntegrationPatience =>

  /**
   * Given a function that returns a [[Cache]] based on whether or not Codec-based serialisation should be skipped,
   * performs a basic check to verify usage/non-usage of in-scope Codecs.
   *
   * @param buildCache function that takes a boolean indicating whether not the cache returned should make use of
   *                   in-scope Codecs
   */
  def legacySupportCheck(buildCache: (Boolean, Codec[Snack]) => Cache[Snack]): Unit = {

    behavior of "useLegacySerialization"

    import scalacache.modes.scalaFuture._
    import scala.concurrent.ExecutionContext.Implicits.global
    it should "use the in-scope Codec if useLegacySerialization is false" in {
      val codec = new DummySnackCodec
      implicit val cache = buildCache(false, codec)
      whenReady(cache.put[Future]("snack")(Snack.Jagabee, None)) { _ =>
        codec.serialiserUsed shouldBe true
        whenReady(cache.get[Future]("snack")) { _ =>
          codec.deserialiserUsed shouldBe true
        }
      }
    }

    it should "use not the in-scope Codec if useLegacySerialization is true" in {
      implicit val codec = new DummySnackCodec
      val cache = buildCache(true, codec)
      whenReady(cache.put("snack")(Snack.Jagabee, None)) { _ =>
        codec.serialiserUsed shouldBe false
        whenReady(cache.get("snack")) { _ =>
          codec.deserialiserUsed shouldBe false
        }
      }
    }
  }
}
