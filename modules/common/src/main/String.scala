package lila.common

import java.text.Normalizer
import java.util.regex.Matcher.quoteReplacement

object String {

  private val slugR = """[^\w-]""".r

  def slugify(input: String) = {
    val nowhitespace = input.trim.replace(" ", "-")
    val normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD)
    val slug = slugR.replaceAllIn(normalized, "")
    slug.toLowerCase
  }

  def decodeUriPath(input: String): Option[String] = {
    try {
      play.utils.UriEncoding.decodePath(input, "UTF-8").some
    }
    catch {
      case e: play.utils.InvalidUriEncodingException => None
    }
  }

  final class Delocalizer(netDomain: String) {

    private val regex = ("""\w{2}\.""" + quoteReplacement(netDomain)).r

    def apply(url: String) = regex.replaceAllIn(url, netDomain)
  }

  def shorten(text: String, length: Int, sep: String = "…") = {
    val t = text.replace("\n", " ")
    if (t.size > (length + sep.size)) (t take length) ++ sep
    else t
  }

  object base64 {
    import java.util.Base64
    import java.nio.charset.StandardCharsets
    def encode(txt: String) =
      Base64.getEncoder.encodeToString(txt getBytes StandardCharsets.UTF_8)
    def decode(txt: String): Option[String] = try {
      Some(new String(Base64.getDecoder decode txt))
    }
    catch {
      case _: java.lang.IllegalArgumentException => none
    }
  }
}
