package scalacache.arcus

import net.spy.memcached._
import org.slf4j.LoggerFactory

import scala.concurrent.duration.Duration
import scala.concurrent.{ ExecutionContext, Promise }
import scala.util.Success
import scalacache.serialization.Codec
import scalacache.{ Cache, LoggingSupport }

/**
 * Wrapper around spymemcached
 *
 * @param useLegacySerialization set this to true to use Spymemcached's serialization mechanism
 *                               to maintain compatibility with ScalaCache 0.7.x or earlier.
 */
class ArcusCache(client: MemcachedClient,
                 keySanitizer: MemcachedKeySanitizer = ReplaceAndTruncateSanitizer(),
                 useLegacySerialization: Boolean = false)(implicit execContext: ExecutionContext = ExecutionContext.global)
    extends Cache[Array[Byte]]
    with MemcachedTTLConverter
    with LoggingSupport {

  override protected final val logger = LoggerFactory.getLogger(getClass.getName)

  /**
   * Get the value corresponding to the given key from the cache
   *
   * @param key cache key
   * @tparam V the type of the corresponding value
   * @return the value, if there is one
   */
  override def get[V](key: String)(implicit codec: Codec[V, Array[Byte]]) = {
    val p = Promise[Option[V]]()
    val f = client.asyncGet(keySanitizer.toValidMemcachedKey(key))
    p.complete {
      val baseResult = f.get
      val result = {
        if (baseResult != null) {
          if (useLegacySerialization)
            Some(baseResult.asInstanceOf[V])
          else
            Some(codec.deserialize(baseResult.asInstanceOf[Array[Byte]]))
        } else None
      }
      if (logger.isDebugEnabled)
        logCacheHitOrMiss(key, result)
      Success(result)
    }
    p.future
  }

  /**
   * Insert the given key-value pair into the cache, with an optional Time To Live.
   *
   * @param key cache key
   * @param value corresponding value
   * @param ttl Time To Live
   * @tparam V the type of the corresponding value
   */
  override def put[V](key: String, value: V, ttl: Option[Duration])(implicit codec: Codec[V, Array[Byte]]) = {
    val p = Promise[Unit]()
    val valueToSend = if (useLegacySerialization) value else codec.serialize(value)
    val f = client.set(keySanitizer.toValidMemcachedKey(key), toMemcachedExpiry(ttl), valueToSend)
    p.complete {
      f.get
      logCachePut(key, ttl)
      Success(())
    }
    p.future
  }

  /**
   * Remove the given key and its associated value from the cache, if it exists.
   * If the key is not in the cache, do nothing.
   *
   * @param key cache key
   */
  override def remove(key: String) = {
    val p = Promise[Unit]()
    val f = client.delete(key)
    p.complete {
      f.get
      Success(())
    }
    p.future
  }

  override def removeAll() = {
    val p = Promise[Unit]()
    val f = client.flush()
    p.complete {
      f.get
      Success(())
    }
    p.future
  }

  override def close(): Unit = {
    client.shutdown()
  }

}

object ArcusCache {

  /**
   * Create a Memcached client connecting to the given host(s) and use it for caching
   * @param serviceCode arcus cluster servcie code e.g.
   * @param address arcus admin string, with addresses separated by spaces, e.g. "localhost:2181"
   */
  def apply(serviceCode: String, address: String): ArcusCache =
    apply(ArcusClientFactory(serviceCode, address))

  /**
   * Create a cache that uses the given Memcached client
   *
   * @param client Memcached client
   */
  def apply(client: MemcachedClient, useLegacySerialization: Boolean = false): ArcusCache = new ArcusCache(client, useLegacySerialization = useLegacySerialization)

}

