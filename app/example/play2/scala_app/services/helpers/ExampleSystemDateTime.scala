package example.play2.scala_app.services.helpers

import org.joda.time.{DateTime, DateTimeZone}


trait ExampleSystemDateTime {
  def now: DateTime
}

object ExampleDefaultSystemDateTime extends ExampleSystemDateTime {
  def now = DateTime.now(DateTimeZone.UTC)
}
