package scalacache.arcus

import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scalacache.common.LegacyCodecCheckSupport
import scalacache.serialization.Codec
import scalacache._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by ikhoon on 19/11/2017.
  */
class ScalaCacheArcusSpec
  extends FlatSpec
  with Matchers
  with Eventually
  with BeforeAndAfter
  with ScalaFutures
  with IntegrationPatience
  with LegacyCodecCheckSupport {

    val serviceCode = sys.env("ARCUS_SERVICE_CODE")
    val host = sys.env("ARCUS_ADMIN_HOST")
    val client = ArcusClientFactory(serviceCode, host)

    implicit val arcusCache = ScalaCache(ArcusCache(client))

    def memcachedIsRunning = {
      try {
        client.get("foo")
        true
      } catch { case _: Exception => false }
    }

    def serialise[A](v: A)(implicit codec: Codec[A, Array[Byte]]): Array[Byte] = codec.serialize(v)

    if (!memcachedIsRunning) {
      alert("Skipping tests because Memcached does not appear to be running on localhost.")
    } else {

      before {
        //      client.flush()
      }

      behavior of "cache"

      it should "return the value stored in Memcached" in {
        Await.result(remove("key1"), Duration.Inf)
        val eventualInt1 = caching("key1") {
          Future {
            println("sleep 1" + Thread.currentThread())
            Thread.sleep(2000)
            0
          }
        }
        val eventualInt2 = caching("key1") {
          Future {
            println("sleep 2" + Thread.currentThread())
            Thread.sleep(2000)
            0
          }
        }
        Await.result(eventualInt1, Duration.Inf)
        Await.result(eventualInt2, Duration.Inf)

      }


    }
}
