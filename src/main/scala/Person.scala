import zio.schema.Schema
import zio.schema.codec.BinaryCodec
import zio.schema.codec.ProtobufCodec
import zio.schema.DeriveSchema
import zio.Chunk
import zio.ZIO
import zio.ZIOApp
import zio.ZIOAppDefault
import zio.Scope
import zio.ZIOAppArgs
import zio.json.JsonEncoder
import zio.json.DeriveJsonDecoder
import zio.json.JsonDecoder
import zio.json.DeriveJsonEncoder
import zio.json.EncoderOps
import zio.json.DecoderOps

final case class Person(name: String, age: Int)
object Person {
  implicit val schema: Schema[Person]    = DeriveSchema.gen
  val protobufCodec: BinaryCodec[Person] = ProtobufCodec.protobufCodec

  implicit val encoder: JsonEncoder[Person] = DeriveJsonEncoder.gen[Person]
  implicit val decoder: JsonDecoder[Person] = DeriveJsonDecoder.gen[Person]

}

object PersonApp extends ZIOAppDefault {
  override def run = {
    val person = Person("Alice", 30)

    // Serialize to Protobuf bytes
    val protobufBytes: Chunk[Byte] = Person.protobufCodec.encode(person)

    // Deserialize from Protobuf bytes
    val decodedPerson: Either[String, Person] =
      Person.protobufCodec.decode(protobufBytes).left.map(_.getMessage)

// Encode:
    val json: String = person.toJson // → """{"name":"Alice","age":30}"""

// Decode:
    val decoded: Either[String, Person] =
      """{"name":"Alice","age":30}""".fromJson[Person]

    decodedPerson match {
      case Right(p) => zio.Console.printLine(s"Decoded Person: $p")
      case Left(error) =>
        zio.Console.printLine(s"Failed to decode Person: $error")
    }
  }
}
