package com.culpin.team.core

import java.io.File

import com.culpin.team.SbtApidocConfiguration
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ Matchers, FlatSpec }
import sbt.Logger

import scala.util.{ Success => USuccess }

class ApidocSpec extends FlatSpec with Matchers with MockitoSugar {

  "Apidoc" should " parse empty input file and configuration" in {
    assert(Apidoc(Seq(), SbtApidocConfiguration("name", "description", false, "1.0"), mock[Logger]) === USuccess(None))
  }

  "Apidoc" should " parse very basic input file and configuration" in {
    val USuccess(Some((apidata, apiconfig))) = Apidoc(Seq(new File(getClass.getResource("/Application.scala").getFile)), SbtApidocConfiguration("name", "description", false, "1.0"), mock[Logger])
    val expectedApiData = "[ {\n  \"type\" : \"get\",\n  \"url\" : \"/\",\n  \"title\" : \"Home page.\",\n  \"name\" : \"Welcome_Page.\",\n  \"group\" : \"Application\",\n  \"version\" : \"1.0.0\"," +
      "\n  \"description\" : \"Renders the welcome page\",\n  \"success\" : {\n    \"examples\" : [ {\n      \"title\" : \"Success-Response:\",\n      \"content\" : \"HTTP/1.1 200 OK\\nHTML for welcome page\\n{\\n  \\\"emailAvailable\\\": \\\"true\\\"\\n}\"," +
      "\n      \"type\" : \"json\"\n    } ]\n  }\n} ]"
    assert(apidata === expectedApiData)
    assert(apiconfig === "{\n  \"name\":\"name\",\n  \"description\":\"description\",\n  \"sampleUrl\":false,\n  \"version\":\"1.0\"\n}")
  }

  "Apidoc" should " parse basic input file and configuration" in {
    val USuccess(Some((apidata, apiconfig))) = Apidoc(Seq(new File(getClass.getResource("/simple-example.js").getFile)),
      SbtApidocConfiguration("name", "description", false, "1.0"), mock[Logger])
    val expectedApiData = "[ {\n  \"type\" : \"get\",\n  \"url\" : \"/user/:id\",\n  \"title\" : \"Read data of a User\",\n  \"version\" : \"0.3.0\",\n  \"name\" : \"GetUser\"," +
      "\n  \"permission\" : {\n    \"name\" : \"admin\"\n  },\n  \"description\" : \"Compare Verison 0.3.0 with 0.2.0 and you will see the green markers with new items in version 0.3.0 and red markers with removed items since 0.2.0.\"," +
      "\n  \"parameter\" : {\n    \"fields\" : {\n      \"Parameter\" : [ {\n        \"group\" : \"Parameter\",\n        \"type\" : \"Number\",\n        \"optional\" : \"false\"," +
      "\n        \"field\" : \"id\",\n        \"description\" : \"The Users-ID.\"\n      } ]\n    }\n  },\n  \"examples\" : [ {\n    \"title\" : \"Example usage:\"," +
      "\n    \"content\" : \"curl -i http://localhost/user/4711\",\n    \"type\" : \"json\"\n  } ],\n  \"success\" : {\n    \"fields\" : {\n      \"Success 200\" : [ {\n        \"group\" : \"Success 200\"," +
      "\n        \"type\" : \"Number\",\n        \"optional\" : \"false\",\n        \"field\" : \"id\",\n        \"description\" : \"The Users-ID.\"\n      }," +
      " {\n        \"group\" : \"Success 200\",\n        \"type\" : \"Date\",\n        \"optional\" : \"false\",\n        \"field\" : \"registered\"," +
      "\n        \"description\" : \"Registration Date.\"\n      }, {\n        \"group\" : \"Success 200\",\n        \"type\" : \"Date\"," +
      "\n        \"optional\" : \"false\",\n        \"field\" : \"name\",\n        \"description\" : \"Fullname of the User.\"\n      }," +
      " {\n        \"group\" : \"Success 200\",\n        \"type\" : \"String[]\",\n        \"optional\" : \"false\",\n        \"field\" : \"nicknames\",\n        \"description\" : \"List of Users nicknames (Array of Strings).\"\n      }," +
      " {\n        \"group\" : \"Success 200\",\n        \"type\" : \"Object\",\n        \"optional\" : \"false\",\n        \"field\" : \"profile\",\n        \"description\" : \"Profile data (example for an Object)\"\n      }," +
      " {\n        \"group\" : \"Success 200\",\n        \"type\" : \"Number\",\n        \"optional\" : \"false\",\n        \"field\" : \"profile.age\",\n        \"description\" : \"Users age.\"\n      }, {\n        \"group\" : \"Success 200\"," +
      "\n        \"type\" : \"String\",\n        \"optional\" : \"false\",\n        \"field\" : \"profile.image\",\n        \"description\" : \"Avatar-Image.\"\n      }, {\n        \"group\" : \"Success 200\",\n        \"type\" : \"Object[]\"," +
      "\n        \"optional\" : \"false\",\n        \"field\" : \"options\",\n        \"description\" : \"List of Users options (Array of Objects).\"\n      }, {\n        \"group\" : \"Success 200\",\n        \"type\" : \"String\"," +
      "\n        \"optional\" : \"false\",\n        \"field\" : \"options.name\",\n        \"description\" : \"Option Name.\"\n      }, {\n        \"group\" : \"Success 200\",\n        \"type\" : \"String\",\n        \"optional\" : \"false\"," +
      "\n        \"field\" : \"options.value\",\n        \"description\" : \"Option Value.\"\n      } ]\n    }\n  },\n  \"error\" : {\n    \"fields\" : {\n      \"Error 4xx\" : [ {\n        \"group\" : \"Error 4xx\",\n        \"optional\" : \"false\"," +
      "\n        \"field\" : \"NoAccessRight\",\n        \"description\" : \"Only authenticated Admins can access the data.\"\n      }, {\n        \"group\" : \"Error 4xx\",\n        \"optional\" : \"false\",\n        \"field\" : \"UserNotFound\"," +
      "\n        \"description\" : \"The <code>id</code> of the User was not found.\"\n      } ]\n    },\n    \"examples\" : [ {\n      \"title\" : \"Response (example):\",\n      \"content\" : \"HTTP/1.1 401 Not Authenticated\\n{\\n  \\\"error\\\": \\\"NoAccessRight\\\"\\n}\"," +
      "\n      \"type\" : \"json\"\n    } ]\n  }\n} ]"
    assert(apidata === expectedApiData)
    assert(apiconfig === "{\n  \"name\":\"name\",\n  \"description\":\"description\",\n  \"sampleUrl\":false,\n  \"version\":\"1.0\"\n}")
  }

}
