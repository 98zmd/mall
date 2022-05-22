package myutil

import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent

import java.util.Properties


object MyKafkaUtil {
  def getKafkaStream(ssc: StreamingContext, topics: Set[String])= {

    val properties: Properties = PropertiesUtil.load("config.properties")

    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> "device1:9092,device2:9092,device3:9092",
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "test-consumer-group",
      "auto.offset.reset" -> "latest",
      "enable.auto.commit" -> (false: java.lang.Boolean)
    )

    val kafkaPara = Map(
      "bootstrap.servers" -> properties.getProperty("kafka.broker.list"),
      "group.id" -> "test-consumer-group"
    )

    //基于Direct方式消费Kafka数据x
    //val kafkaDStream  = KafkaUtils
     // .createDirectStream[String, String, StringDecoder, StringDecoder](ssc,kafkaPara, topics)
    val kafka = KafkaUtils.createDirectStream(
      ssc,
      PreferConsistent,
      Subscribe[String, String](Array("GMALL_STARTUP"),kafkaParams)
    )

    //返回
    kafka
  }
}
