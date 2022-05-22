package application

import myutil.RedisUtil
import redis.clients.jedis.Jedis

object Test {
  def main(args: Array[String]): Unit = {
    val jedisClient: Jedis = RedisUtil.getJedisClient
  }
}
