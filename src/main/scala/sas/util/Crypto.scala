package sas.util

import java.nio.charset.{Charset, StandardCharsets}
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Crypto {

  val Base64Encoder: Base64.Encoder = Base64.getEncoder
  val Charset: Charset = StandardCharsets.UTF_8

  def sha1(input: String): Array[Byte] = sha1(bytes(input))

  def sha1(input: Array[Byte]): Array[Byte] = MessageDigest.getInstance("SHA-1").digest(input)

  def base64(input: Array[Byte]): String = new String(Base64Encoder.encode(input), Charset)

  def base64sha1(input: String): String = base64(sha1(input))

  def base64hmacsha1(publicKey: String, privateKey: String): String = {
    val mac = Mac.getInstance("HmacSHA1")
    val key = new SecretKeySpec(bytes(privateKey), mac.getAlgorithm)
    mac.init(key)
    val digest = mac.doFinal(bytes(publicKey))
    base64(digest)
  }

  private def bytes(input: String) = input.getBytes(Charset)
}
