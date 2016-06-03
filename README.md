# scala-channel
```
import dellcliff.channel.Channel
import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global


object Main {

  def main(args: Array[String]): Unit = {

    val channel1 = Channel[Int]()

    async {
      while (true) {
        println(await(channel1.take()))
      }
    }

    async {
      var i = 0
      while (true) {
        await(channel1.put(i))
        i += 1
      }
    }

    Thread.sleep(4000)
  }
}
```
