import zio.schema._, zio.schema.codec.{BinaryCodec, ProtobufCodec}
import zio.{Chunk, ZIO}

sealed trait Event
object Event {
  final case class UserCreated(id: String, email: String)          extends Event
  final case class UserDeleted(id: String, reason: Option[String]) extends Event

  implicit val schema: Schema[Event] =
    DeriveSchema.gen[Event] // or write Schema.Enum2 manually for explicit ids
  implicit val codec: BinaryCodec[Event] = ProtobufCodec.protobufCodec[Event]
}

// encode/decode
val event: Event                      = Event.UserCreated("u1", "a@b.com")
val bytes: Chunk[Byte]                = Event.codec.encode(event)
val decoded: Either[Throwable, Event] = Event.codec.decode(bytes)
