package io.scalastic.aws

import io.scalastic.aws.AwsDocumentation.documentationPath

import java.io._
import scala.util.Try

trait Serializer {

  def deserialize[A](name: String, c: A): Any = {
    Try {
      val ois = new ObjectInputStreamWithCustomClassLoader(new FileInputStream(documentationPath + name + ".ser"))
      val obj = ois.readObject.asInstanceOf[A]
      ois.close
      println("Load previously generated file " + documentationPath + name + ".ser")
      obj
    }
  }

  def serialize(name: String, objet: Any): Unit = {
    val oos = new ObjectOutputStream(new FileOutputStream(documentationPath + name + ".ser"))
    oos.writeObject(objet)
    oos.close
    println("Serialize " + objet.getClass + " to file " + name + ".ser")
  }

}

class ObjectInputStreamWithCustomClassLoader(
                                              fileInputStream: FileInputStream
                                            ) extends ObjectInputStream(fileInputStream) {
  override def resolveClass(desc: java.io.ObjectStreamClass): Class[_] = {
    try {
      Class.forName(desc.getName, false, getClass.getClassLoader)
    }
    catch {
      case ex: ClassNotFoundException => super.resolveClass(desc)
    }
  }
}

trait IO {

  def write(name: String, objet: ujson.Value): Unit = {
    val file = new File(documentationPath + name + ".json")
    val pw = new PrintWriter(file)
    ujson.writeTo(objet, pw, 2, true)
    println("Save JSON-formatted AWS documentation into " + documentationPath + name + ".json")
  }
}
