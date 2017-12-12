package scalacache.arcus

import net.spy.memcached._
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scala.concurrent.duration._
import scala.language.postfixOps
import scalacache.common.{LegacyCodecCheckSupport, Snack}
import scalacache.serialization.Codec
import scalacache.serialization.circe._
import scala.concurrent.ExecutionContext.Implicits.global
import scalacache.CacheConfig
import scalacache.modes.scalaFuture._


class ArcusCacheSpec
    extends FlatSpec
    with Matchers
    with Eventually
    with BeforeAndAfter
    with ScalaFutures
    with IntegrationPatience
    with LegacyCodecCheckSupport {

  val serviceCode = "test"
  val host = "localhost:2181"
  val client = ArcusClientFactory(serviceCode, host)

  def memcachedIsRunning = {
    try {
      client.get("foo")
      true
    } catch { case _: Exception => false }
  }

  def serialise[V](v: V)(implicit codec: Codec[V]): Array[Byte] = codec.encode(v)

  if (!memcachedIsRunning) {
    alert("Skipping tests because Memcached does not appear to be running on localhost.")
  } else {

    before {
      //      client.flush()
    }

    behavior of "get"

    it should "return the value stored in Memcached" in {
      client.set("key1", 0, serialise(123))
      whenReady(ArcusCache[Int](client).get("key1")) { _ should be(Some(123)) }
    }

    it should "return None if the given key does not exist in the underlying cache" in {
      whenReady(ArcusCache[Int](client).get("non-existent-key")) { _ should be(None) }
    }

    behavior of "put"

    it should "store the given key-value pair in the underlying cache" in {
      whenReady(ArcusCache[Int](client).put("key2")(123, None)) { _ =>
        client.get("key2") should be(serialise(123))
      }
    }

    behavior of "put with TTL"

    it should "store the given key-value pair in the underlying cache" in {
      whenReady(ArcusCache[Int](client).put("key3")(123, Some(3 seconds))) { _ =>
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

      whenReady(ArcusCache[Int](client).remove("key1")) { _ =>
        client.get("key1") should be(null)
      }
    }

    import scalacache.serialization.binary._
    legacySupportCheck { (legacySerialization, codec) =>
      new ArcusCache[Snack](
        client = client, useLegacySerialization = legacySerialization)(
        CacheConfig.defaultCacheConfig, codec
      )
    }
  }

}

