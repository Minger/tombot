package services

import apis.facebookmessenger.UserProfile

import scala.collection.mutable

/**
  * Created by markmo on 8/08/2016.
  */
class UserService {

  val users = mutable.Map[String, User]()

  def hasUser(id: String) = users.contains(id)

  def getUser(id: String) = users.get(id)

  def getUserIdOrElse(id: String) = users.get(id) match {
    case Some(user) => user.id
    case None => id
  }

  def setUser(id: String, user: User): Unit =
    users(id) = user

  def authenticate(username: String, password: String): Option[User] =
    if (username == "admin" && password == "password") {
      Some(User("1234", "Mark", "Moloney", "https://fbcdn-profile-a.akamaihd.net/hprofile-ak-xfa1/v/t1.0-1/s200x200/31774_1363558046272_6117087_n.jpg?oh=3ed68485384521119ba144322e84f597&oe=5859AEC4&__gda__=1481748621_4b2654a725211c462acff3957522994b", "en_US", 10, "male"))
    } else {
      None
    }

}

case class User(id: String, firstName: String, lastName: String, picture: String, locale: String, timezone: Int, gender: String)

object User {

  def apply(id: String, profile: UserProfile): User =
    User(id, profile.firstName, profile.lastName, profile.picture, profile.locale, profile.timezone, profile.gender)

}
