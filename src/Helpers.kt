package com.overleaf.tags

import org.bson.Document
import org.bson.types.ObjectId

fun mongoDocumentToMap(document: Document): Map<String, Any> {
  val asMap: MutableMap<String, Any> = document.toMutableMap()
  if (asMap.containsKey("_id")) {
    val id = asMap.getValue("_id")
    if (id is ObjectId) {
      asMap.set("_id", id.toHexString())
    }
  }
  return asMap
}

