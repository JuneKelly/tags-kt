package com.overleaf.tags

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import com.fasterxml.jackson.databind.*
import io.ktor.jackson.*
import io.ktor.features.*

import com.mongodb.client.MongoClients;
import com.mongodb.client.model.Filters.*;
import com.mongodb.client.model.Updates.*;
import org.bson.Document
import org.bson.types.ObjectId

import java.util.Arrays


data class CreateTagBody(
  val name: String
)
data class UpdateTagUserIdsBody(
  val user_id: String
)
data class RenameTagBody(
  val name: String
)

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

  install(ContentNegotiation) {
      jackson {
          enable(SerializationFeature.INDENT_OUTPUT)
      }
  }

  // Mongo stuff
  val connection = "mongodb://mongo:27017"
  // val connection = "mongodb://localhost:27018"
  val mongoClient = MongoClients.create(connection)
  val db = mongoClient.getDatabase("sharelatex")
  val tags = db.getCollection("tags")

  routing {

    get("/") {
      call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
    }

    get("/status") {
      call.respondText("tags sharelatex up", contentType = ContentType.Text.Plain)
    }

    get("/health_check") {
      call.respondText("", contentType = ContentType.Text.Plain)
    }

    get("/user/{userId}/tag") {
      val userId = call.parameters["userId"]
      val result = ArrayList<Map<String, Any>>()
      tags.find(eq("user_id", userId))
        .forEach {
          val asMap: Map<String, Any> = mongoDocumentToMap(it)
          result.add(asMap)
        }
      call.respond(result)
    }

    post("/user/{userId}/tag") {
      val userId = call.parameters["userId"]
      val body = call.receive<CreateTagBody>()
      val doc = Document()
        .append("name", body.name)
        .append("user_id", userId)
        .append("project_ids", Arrays.asList<String>())
      tags.insertOne(doc)
      call.respond(mongoDocumentToMap(doc))
    }

    put("/user/{userId}/tag") {
      val userId = call.parameters["userId"]
      val body = call.receive<UpdateTagUserIdsBody>()
      tags.updateMany(
        eq("user_id", userId),
        set("user_id", body.user_id)
      )
      call.response.status(HttpStatusCode.NoContent)
    }

    post("/user/{userId}/tag/{tagId}/rename") {
      val userId = call.parameters["userId"]
      val tagId = call.parameters["tagId"]
      val body = call.receive<RenameTagBody>()
      tags.updateOne(
        and(
          eq("user_id", userId),
          eq("_id", ObjectId(tagId))
        ),
        set("name", body.name)
      )
      call.response.status(HttpStatusCode.NoContent)
    }

    delete("/user/{userId}/tag/{tagId}") {
      val userId = call.parameters["userId"]
      val tagId = call.parameters["tagId"]
      tags.deleteOne(
        and(
          eq("user_id", userId),
          eq("_id", ObjectId(tagId))
        )
      )
      call.response.status(HttpStatusCode.NoContent)
    }

    post("/user/{userId}/tag/{tagId}/project/{projectId}") {
      val userId = call.parameters["userId"]
      val tagId = call.parameters["tagId"]
      val projectId = call.parameters["projectId"]
      tags.updateOne(
        and(eq("user_id", userId), eq("_id", ObjectId(tagId))),
        addToSet("project_ids", projectId)
      )
      call.response.status(HttpStatusCode.NoContent)
    }
  }
}
