import net.ingtra.nriyfacebook.Setting
import net.ingtra.nriyfacebook.algolcheck.AlgolCheck

/**
  * Created by ic on 2016-07-11.
  */
object AlgolCheckTest {
  def main(args: Array[String]) {
    val algolcheck = new AlgolCheck(Setting.graphApiKey)
    print(algolcheck.request())


  }
}
