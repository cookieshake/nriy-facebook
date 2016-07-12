import net.ingtra.nriyfacebook.Setting
import net.ingtra.nriyfacebook.algolcheck.AlgolCheck

object AlgolCheckTest {
  def main(args: Array[String]) {
    val algolcheck = new AlgolCheck(Setting.graphApiKey)
    print(algolcheck.request())


  }
}
