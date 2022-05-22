package application

import bean.StartUpLog
import com.alibaba.fastjson.JSON
import common.MallConstants
import handler.DauHandler
import org.apache.spark.SparkConf

import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import myutil.MyKafkaUtil
import org.apache.hadoop.conf.Configuration
import java.text.SimpleDateFormat
import java.util.Date


object DauApp {
  def main(args: Array[String]): Unit = {

    val sparkConf: SparkConf = new SparkConf().setMaster("local[*]").setAppName("dau_app")
    val ssc = new StreamingContext(sparkConf,Seconds(5))

    // 1 消费kafka
    val inputDstream = MyKafkaUtil
      .getKafkaStream(ssc,Set(MallConstants.KAFKA_TOPIC_STARTUP))

    //2 数据流 转换 结构变成case class 补充两个时间字段
    val startuplogDstream: DStream[StartUpLog] = inputDstream.map { it => {

      val startupLog: StartUpLog = JSON.parseObject(it.value(), classOf[StartUpLog])

      val dateTimeStr: String = new SimpleDateFormat("yyyy-MM-dd HH").format(new Date(startupLog.ts))
      val dateArr: Array[String] = dateTimeStr.split(" ")
      startupLog.logDate = dateArr(0)
      startupLog.logHour = dateArr(1)
      startupLog
      }
    }
    //5.跨批次过滤数据
    val filterStartUpDStreamByRedis: DStream[StartUpLog] = DauHandler.filterDataByRedis(startuplogDstream, ssc.sparkContext)

    //6.同批次过滤数据
    val filterStartUpDStreamByGroup: DStream[StartUpLog] = DauHandler.filterDataByMid(filterStartUpDStreamByRedis)

    filterStartUpDStreamByGroup.cache()

    //7.将经过两次过滤的数据集写入Redis，给下一个批次数据过滤使用
    DauHandler.saveDateUserToRedis(filterStartUpDStreamByGroup)

    //通过Phoenix写入HBase
    filterStartUpDStreamByGroup.foreachRDD(rdd => {
      import org.apache.phoenix.spark._
      rdd.saveToPhoenix("GMALL190408_DAU", Seq("MID", "UID", "APPID", "AREA", "OS", "CH", "TYPE", "VS", "LOGDATE", "LOGHOUR", "TS"),
        new Configuration(), Some("device1,device2,device3:2181"))
    })

    ssc.start()
    ssc.awaitTermination()

  }
}
