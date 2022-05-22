package handler

import bean.StartUpLog
import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.streaming.dstream.DStream
import redis.clients.jedis.Jedis

import java.text.SimpleDateFormat
import java.util
import util.Date
import myutil.RedisUtil

object DauHandler {

  /**
    * 同批次内部去重（分组）
    *
    * @param filterStartUpDStream 按照Redis去重后的数据集
    */
  def filterDataByMid(filterStartUpDStream: DStream[StartUpLog]) = {

    //将数据转换为log=>（mid，log）
    val midToStartUpLogDStream: DStream[(String, StartUpLog)] = filterStartUpDStream.map(log => (log.mid, log))

    //按照mid分组
    val midToStartUpLogDStreamGroup: DStream[(String, Iterable[StartUpLog])] = midToStartUpLogDStream.groupByKey()

    //组内取一条数据
    val filterStartUpDStreamByGroup: DStream[StartUpLog] = midToStartUpLogDStreamGroup.map { case (mid, logIter) =>
      logIter.head
    }

    //返回
    filterStartUpDStreamByGroup

  }


  private val sdf = new SimpleDateFormat("yyyy-MM-dd")

  /**
    * 跨批次过滤数据
    *
    * @param startUpLogDStream 读取Kafka的数据集转换为样例类对象
    */
  def filterDataByRedis(startUpLogDStream: DStream[StartUpLog], sc: SparkContext): DStream[StartUpLog] = {

    startUpLogDStream.transform(rdd => {

      //每一次计算在Driver端获取连接和数据
      val jedisClient: Jedis = RedisUtil.getJedisClient

      //创建RedisKey
      val date: String = sdf.format(new Date(System.currentTimeMillis()))
      val redisKey = s"dau_$date"

      //查询当日登录用户名单
      val midSet: util.Set[String] = jedisClient.smembers(redisKey)

      //关闭连接
      jedisClient.close()

      //广播midSet
      val midSetBC: Broadcast[util.Set[String]] = sc.broadcast(midSet)

      //过滤
      rdd.filter(log => !midSetBC.value.contains(log.mid))

    })

  }


  /**
    * 将经过两次过滤的数据集写入Redis，给下一个批次数据过滤使用
    *
    * @param filterStartUpDStreamByGroup 两次过滤后的数据集
    */
  def saveDateUserToRedis(filterStartUpDStreamByGroup: DStream[StartUpLog]): Unit = {

    filterStartUpDStreamByGroup.foreachRDD(rdd => {

      rdd.foreachPartition(iter => {

        //获取Redis连接
        val jedisClient: Jedis = RedisUtil.getJedisClient

        //处理数据（写入Redis）
        iter.foreach(log => {

          //单日登录用户的RedisKey
          val redisKey = s"dau_${log.logDate}"

          jedisClient.sadd(redisKey, log.mid)
        })

        //关闭Redis连接
        jedisClient.close()

      })

    })

  }

}
