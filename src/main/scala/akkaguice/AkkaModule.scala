package akkaguice

import akka.actor.ActorSystem
import akkaguice.AkkaModule.ActorSystemProvider
import com.google.inject.{AbstractModule, Inject, Injector, Provider}
import com.typesafe.config.Config
import net.codingwell.scalaguice.ScalaModule

/**
  * Created by markmo on 27/07/2016.
  */
object AkkaModule {

  class ActorSystemProvider @Inject()(val config: Config, val injector: Injector) extends Provider[ActorSystem] {
    override def get() = {
      val system = ActorSystem("main-actor-system", config)
      // add the GuiceAkkaExtension to the system, and initialize it with the Guice injector
      GuiceAkkaExtension(system).initialize(injector)
      system
    }
  }

}

class AkkaModule extends AbstractModule with ScalaModule {

  override def configure(): Unit = {
    bind[ActorSystem].toProvider[ActorSystemProvider].asEagerSingleton()
  }

}
