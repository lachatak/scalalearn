package org.kaloz.persistence.infrastructure

import akka.serialization.SerializerWithStringManifest
import org.json4s._
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read, write}

class JsonPersistenceSerializer extends SerializerWithStringManifest {

  implicit val formats = Serialization.formats(NoTypeHints)

  override def identifier: Int = 12345

  def manifest(o: AnyRef): String = o.getClass.getName

  override def toBinary(o: AnyRef): Array[Byte] = {
    val json = write(o)
    json.getBytes()
  }

  def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {
    val m = Manifest.classType[AnyRef](Class.forName(manifest))
    val json = new String(bytes, "utf8")
    read[AnyRef](json)(formats, m)
  }
}
