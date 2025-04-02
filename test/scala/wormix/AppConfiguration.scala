package wormix

import org.springframework.context.annotation.{Bean, Configuration, ImportResource}

@Configuration
@ImportResource(Array("classpath:beans.xml"))
class AppConfiguration {

  @Bean
  def person() = Person("Вася", "Пупкин")

}

case class Person(firstName: String, lastName: String)