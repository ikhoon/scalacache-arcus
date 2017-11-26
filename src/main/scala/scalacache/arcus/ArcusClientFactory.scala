package scalacache.arcus

import net.spy.memcached.{ArcusClient, ConnectionFactoryBuilder}
import net.spy.memcached.ops.ArrayOperationQueueFactory
import net.spy.memcached.transcoders.SerializingTranscoder

/**
  * Created by ikhoon on 2017. 4. 4..
  */
object ArcusClientFactory {

  // TODO prototype중 이건 좀더 편하게 만들수 있는 factory를 생각해보자.
  def apply(serviceCode: String,
            hostPorts: String,
            poolSize: Int = 10,
            timeout: Long = 700L,
            enableFrontCache: Boolean = false,
            frontCacheMaxElements: Int = 10000,
            frontCacheExpireSecond: Int = 5,
            operateQueueSize: Option[Int] = None,
            operateTimeout: Long = 1000,
            maxReconnectDelay: Long = 1,
            readBufferSize: Int = 16384,
            transcoderCharset: String = "UTF-8",
            transcoderCompressionThreshold: Int = 16384): ArcusClient = {

    val connectionFactoryBuilder = getConnectionFactoryBuilder(
      enableFrontCache,
      frontCacheMaxElements,
      frontCacheExpireSecond,
      operateQueueSize,
      operateTimeout,
      maxReconnectDelay,
      readBufferSize,
      transcoderCharset,
      transcoderCompressionThreshold
    )

    ArcusClient
      .createArcusClientPool(hostPorts, serviceCode, connectionFactoryBuilder, poolSize)
      .getClient
  }

  private def getConnectionFactoryBuilder(enableFrontCache: Boolean,
                                          frontCacheMaxElements: Int,
                                          frontCacheExpireSecond: Int,
                                          operateQueueSize: Option[Int],
                                          operateTimeout: Long,
                                          maxReconnectDelay: Long,
                                          readBufferSize: Int,
                                          transcoderCharset: String,
                                          transcodercompressionThreshold: Int) = {
    val cfb = new ConnectionFactoryBuilder

    if (enableFrontCache) {
      cfb
        .setMaxFrontCacheElements(frontCacheMaxElements)
        .setFrontCacheExpireTime(frontCacheExpireSecond)
    }

    operateQueueSize.map(size => cfb.setOpQueueFactory(new ArrayOperationQueueFactory(size)))

    val trans = new SerializingTranscoder
    trans.setCharset(transcoderCharset)
    trans.setCompressionThreshold(transcodercompressionThreshold)
    cfb
      .setTranscoder(trans)
      .setShouldOptimize(false)
      .setOpTimeout(operateTimeout)
      .setMaxReconnectDelay(maxReconnectDelay)
      .setReadBufferSize(readBufferSize)
  }
}
