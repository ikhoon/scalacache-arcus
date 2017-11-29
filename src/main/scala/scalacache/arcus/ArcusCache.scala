package scalacache.arcus

import net.spy.memcached._
import net.spy.memcached.internal.GetFuture
import org.slf4j.LoggerFactory

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Promise}
import scala.util.{Success, Try}
import scala.util.control.NonFatal
import scalacache.serialization.Codec
import scalacache._

/**
  * Wrapper around arcus client
  * @param client arcus client
  * @param keySanitizer arcus key sanitizer
  * @param useLegacySerialization set this to true to use Spymemcached's serialization mechanism
  *                               to maintain compatibility with ScalaCache 0.7.x or earlier.
  */
class ArcusCache[V](client: MemcachedClient,
                    keySanitizer: MemcachedKeySanitizer = ReplaceAndTruncateSanitizer(),
                    useLegacySerialization: Boolean = false
                   )(
  implicit val config: CacheConfig,
  codec: Codec[V])
  extends AbstractCache[V]
  with MemcachedTTLConverter {


  override protected final val logger = LoggerFactory.getLogger(getClass.getName)

  override protected def doGet[F[_]](key: String)(implicit mode: Mode[F]): F[Option[V]] = {
    mode.M.async { cb =>
      val f = client.asyncGet(keySanitizer.toValidMemcachedKey(key))
      try {
        val bytes = f.get()
        val value = {
          if(bytes != null) {
            if (useLegacySerialization) Try(bytes.asInstanceOf[V]).toEither.map(Some(_))
            else codec.decode(bytes.asInstanceOf[Array[Byte]]).right.map(Some(_))
          }
          else Right(None)
        }
        cb(value)
      } catch {
        case NonFatal(e) => cb(Left(e))
      }
    }
  }


  override protected def doPut[F[_]](key: String, value: V, ttl: Option[Duration])(implicit mode: Mode[F]): F[Any] = {
    mode.M.async { cb =>
      val valueToSend = if(useLegacySerialization) value else codec.encode(value)
      val f = client.set(keySanitizer.toValidMemcachedKey(key), toMemcachedExpiry(ttl), valueToSend)
      f.get
      logCachePut(key, ttl)
      cb(Right(()))
    }
  }
  /**
    * Remove the given key and its associated value from the cache, if it exists.
    * If the key is not in the cache, do nothing.
    *
    * @param key cache key
    */
  override protected def doRemove[F[_]](key: String)(implicit mode: Mode[F]): F[Any] = {
    mode.M.async { cb =>
      val f = client.delete(key)
      f.get
      cb(Right(()))
    }
  }

  override protected def doRemoveAll[F[_]]()(implicit mode: Mode[F]): F[Any] = {
    mode.M.async { cb =>
      val f = client.flush()
      f.get
      cb(Right(()))
    }
  }

  override def close[F[_]]()(implicit mode: Mode[F]): F[Any] = mode.M.delay(client.shutdown())


}

object ArcusCache {

  /**
    * Create a Arcus client connecting to the given host(s) and use it for caching
    * @param serviceCode arcus cluster servcie code e.g.
    * @param address arcus admin string, with addresses separated by spaces, e.g. "localhost:2181"
    */
  def apply[V](serviceCode: String, address: String, useLegacySerialization: Boolean)(implicit config: CacheConfig, codec: Codec[V]): ArcusCache[V] =
    apply(ArcusClientFactory(serviceCode, address), useLegacySerialization)

  /**
    * Create a cache that uses the given Memcached client
    *
    * @param client Arcus client
    */
  def apply[V](client: MemcachedClient, useLegacySerialization: Boolean = false)(implicit config: CacheConfig, codec: Codec[V]): ArcusCache[V] =
    new ArcusCache[V](client)

}
