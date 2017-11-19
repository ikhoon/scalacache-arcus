package scalacache.arcus

import net.spy.memcached._
import org.scalatest.concurrent.{ Eventually, IntegrationPatience, ScalaFutures }
import org.scalatest.time.{ Seconds, Span }
import org.scalatest.{ BeforeAndAfter, FlatSpec, Matchers }

import scala.concurrent.duration._
import scala.language.postfixOps
import scalacache.common.LegacyCodecCheckSupport
import scalacache.serialization.Codec

class ArcusCacheSpec
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

    behavior of "get"

    it should "return the value stored in Memcached" in {
      client.set("key1", 0, serialise(123))
      whenReady(ArcusCache(client).get[Int]("key1")) { _ should be(Some(123)) }
    }

    it should "return None if the given key does not exist in the underlying cache" in {
      whenReady(ArcusCache(client).get[Int]("non-existent-key")) { _ should be(None) }
    }

    behavior of "put"

    it should "store the given key-value pair in the underlying cache" in {
      whenReady(ArcusCache(client).put("key2", 123, None)) { _ =>
        client.get("key2") should be(serialise(123))
      }
    }

    behavior of "put with TTL"

    it should "store the given key-value pair in the underlying cache" in {
      whenReady(ArcusCache(client).put("key3", 123, Some(3 seconds))) { _ =>
        client.get("key3") should be(serialise(123))

        // Should expire after 3 seconds
        eventually(timeout(Span(4, Seconds))) {
          client.get("key3") should be(null)
        }
      }
    }

    behavior of "remove"

    it should "delete the given key and its value from the underlying cache" in {
      client.set("key1", 0, 123)
      client.get("key1") should be(123)

      whenReady(ArcusCache(client).remove("key1")) { _ =>
        client.get("key1") should be(null)
      }
    }

    legacySupportCheck { legacySerialization =>
      new ArcusCache(client = client, useLegacySerialization = legacySerialization)
    }
  }

}

