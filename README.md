# caliban-extras

[![Build Status][build-image]][build-url]
[![Sonatype Nexus (Releases)][nexus-image]][nexus-url]
[![Sonatype Nexus (Snapshots)][nexus-snapshot-image]][nexus-snapshot-url]
[![Scala Steward badge][scala-steward-image]][scala-steward-url]

[build-image]: https://travis-ci.org/niqdev/caliban-extras.svg?branch=master
[build-url]: https://travis-ci.org/niqdev/caliban-extras
[nexus-image]: https://img.shields.io/nexus/r/com.github.niqdev/caliban-refined_2.13?color=blueviolet&server=https%3A%2F%2Foss.sonatype.org&style=popout-square
[nexus-url]: https://oss.sonatype.org/content/repositories/releases/com/github/niqdev/caliban-refined_2.13/
[nexus-snapshot-image]: https://img.shields.io/nexus/s/com.github.niqdev/caliban-refined_2.13?label=nexus-snapshot&server=https%3A%2F%2Foss.sonatype.org&style=flat-square
[nexus-snapshot-url]: https://oss.sonatype.org/content/repositories/snapshots/com/github/niqdev/caliban-refined_2.13/
[scala-steward-image]: https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=popout-square&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=
[scala-steward-url]: https://scala-steward.org

* [caliban-refined](#caliban-refined)
* [Example](#example)
* [Resources](#resources)
* [TODO](#todo)

## caliban-refined

A tiny module to add [refined](https://github.com/fthomas/refined) and [newtype](https://github.com/estatico/scala-newtype) support for [Schema](https://ghostdogpr.github.io/caliban/docs/schema.html#schemas) and [ArgBuilder](https://ghostdogpr.github.io/caliban/docs/schema.html#arguments)

Add the following line in `build.sbt`

```sbt
libraryDependencies += "com.github.niqdev" %% "caliban-refined" % "0.1.3"
```

Replace all the custom implementations like
```scala
implicit val nonEmptyStringSchema: Schema[Any, NonEmptyString] =
  Schema.stringSchema.contramap(_.value)

implicit val nonNegIntArgBuilder: ArgBuilder[NonNegInt] = {	
  case value: IntValue =>	
    NonNegInt.from(value.toInt).leftMap(CalibanError.ExecutionError(_))	
  case other =>
    Left(CalibanError.ExecutionError(s"Can't build a NonNegInt from input $other"))	
}
```

with an import

```scala
// add import
import caliban.refined._

@newtype case class Id(int: PosInt)
@newtype case class Name(string: NonEmptyString)
case class User(id: Id, name: Name)
case class UserArg(id: Id)
case class Query(user: UserArg => User)

val resolver = Query(arg => User(arg.id, Name("myName")))
val api      = GraphQL.graphQL(RootResolver(resolver))
```

See the [tests](https://github.com/niqdev/caliban-extras/blob/master/modules/refined/src/test/scala/caliban/refined/RefinedSpec.scala) for a complete example

## Example

A minimalistic version of GraphQL [GitHub](https://developer.github.com/v4/explorer) api with pagination, *filters and authentication (TODO)*

```bash
# run example
sbt -jvm-debug 5005 "examples/runMain com.github.niqdev.caliban.Main"

# verify endpoint
http -v :8080/api/graphql query='{users(first:1){totalCount}}'
```

### Sample queries

```graphql
query counts {
  countUsers: users(first: 1) {
    totalCount
  }
  countRepositories: repositories(first: 1) {
    totalCount
  }
  countIssues: issues(first: 1) {
    totalCount
  }
}
```
```json
{
  "data": {
    "countUsers": {
      "totalCount": 2
    },
    "countRepositories": {
      "totalCount": 40
    },
    "countIssues": {
      "totalCount": 40
    }
  }
}
```

```graphql
query nodes {
  userNodes: users(first: 1) {
    nodes {
      id
      name
      createdAt
      updatedAt
      #repository > issue | issues
      #repositories > issue | issues
    }
  }
  repositoryNodes: repositories(first: 1) {
    nodes {
      id
      name
      url
      isFork
      createdAt
      updatedAt
      #issue
      #issues
    }
  }
  issueNodes: issues(first: 1) {
    nodes {
      id
      number
      status
      title
      body
      createdAt
      updatedAt
    }
  }
}
```
```json
{
  "data": {
    "userNodes": {
      "nodes": [
        {
          "id": "dXNlcjp2MTpmMGZiZTEzMS0zZjY1LTQxNDUtYjM3My01YmJmYzFjOWExYWU=",
          "name": "zio",
          "createdAt": "2020-08-11T18:25:25.411363Z",
          "updatedAt": "2020-08-11T18:25:25.411363Z"
        }
      ]
    },
    "repositoryNodes": {
      "nodes": [
        {
          "id": "cmVwb3NpdG9yeTp2MToxOTQ2MDQ4Ny01ZmZkLTRkMzgtYjBlOS0xNmRiNDQ4NDYxNTU=",
          "name": "zio-s3",
          "url": "https://github.com/zio/zio-s3",
          "isFork": false,
          "createdAt": "2020-08-11T18:25:25.496188Z",
          "updatedAt": "2020-08-11T18:25:25.496188Z"
        }
      ]
    },
    "issueNodes": {
      "nodes": [
        {
          "id": "aXNzdWU6djE6MjMzNTlhMjktZDQxYy00ODAxLWE2MjMtNGM2YzNmMGU5NjMy",
          "number": 27,
          "status": "CLOSE",
          "title": "title6",
          "body": "body6",
          "createdAt": "2020-08-11T18:25:25.571263Z",
          "updatedAt": "2020-08-11T18:25:25.571263Z"
        }
      ]
    }
  }
}
```
```bash
# user:v1:f0fbe131-3f65-4145-b373-5bbfc1c9a1ae
echo "dXNlcjp2MTpmMGZiZTEzMS0zZjY1LTQxNDUtYjM3My01YmJmYzFjOWExYWU=" | base64 --decode

# repository:v1:19460487-5ffd-4d38-b0e9-16db44846155
echo "cmVwb3NpdG9yeTp2MToxOTQ2MDQ4Ny01ZmZkLTRkMzgtYjBlOS0xNmRiNDQ4NDYxNTU=" | base64 --decode

# issue:v1:23359a29-d41c-4801-a623-4c6c3f0e9632
echo "aXNzdWU6djE6MjMzNTlhMjktZDQxYy00ODAxLWE2MjMtNGM2YzNmMGU5NjMy" | base64 --decode
```

```graphql
query findByParameter {
  user(name: "zio") {
    name
    # it doesn't verify user ownership
    repository(name: "shapeless") {
      name
      url
      isFork
      # it doesn't verify repository ownership
      issue(number: 1) {
        number
        status
        title
        body
      }
    }
  }
}
```
```json
{
  "data": {
    "user": {
      "name": "zio",
      "repository": {
        "name": "shapeless",
        "url": "https://github.com/milessabin/shapeless",
        "isFork": false,
        "issue": {
          "number": 1,
          "status": "OPEN",
          "title": "title0",
          "body": "body0"
        }
      }
    }
  }
}
```

```graphql
query findUserPaginated {
  users(first: 10, after: "opaqueCursor") {
    edges {
      cursor
      node {
        name
        repositories(first: 4) {
          nodes {
            name
          }
        }
      }
    }
    pageInfo {
      hasNextPage
      hasPreviousPage
      startCursor
      endCursor
    }
    totalCount
  }
}
```
```json
{
  "data": {
    "users": {
      "edges": [
        {
          "cursor": "Y3Vyc29yOnYxOjI=",
          "node": {
            "name": "zio",
            "repositories": {
              "nodes": [
                {
                  "name": "zio-lambda"
                },
                {
                  "name": "zio-web"
                },
                {
                  "name": "zio-ftp"
                },
                {
                  "name": "zio-actors"
                }
              ]
            }
          }
        },
        {
          "cursor": "Y3Vyc29yOnYxOjE=",
          "node": {
            "name": "typelevel",
            "repositories": {
              "nodes": [
                {
                  "name": "scodec"
                },
                {
                  "name": "ciris"
                },
                {
                  "name": "refined"
                },
                {
                  "name": "shapeless"
                }
              ]
            }
          }
        }
      ],
      "pageInfo": {
        "hasNextPage": false,
        "hasPreviousPage": false,
        "startCursor": "Y3Vyc29yOnYxOjI=",
        "endCursor": "Y3Vyc29yOnYxOjE="
      },
      "totalCount": 2
    }
  }
}
```

```graphql
query findByNodeIds {
  nodes(
    ids: [
      "dXNlcjp2MTpmMGZiZTEzMS0zZjY1LTQxNDUtYjM3My01YmJmYzFjOWExYWU=",
      "cmVwb3NpdG9yeTp2MToyOTBlYzI2NS1lYzkxLTRhOWItYmRkYS03YjA5NzBkYjk5Y2I=",
      "aXNzdWU6djE6MjMzNTlhMjktZDQxYy00ODAxLWE2MjMtNGM2YzNmMGU5NjMy"
    ]
  ) {
    id
    ... on User {
      name
    }
    ... on Repository {
      name
      url
      isFork
    }
    ... on Issue {
      number
      status
      title
      body
    }
  }
}
```
```json
{
  "data": {
    "nodes": [
      {
        "id": "dXNlcjp2MTpmMGZiZTEzMS0zZjY1LTQxNDUtYjM3My01YmJmYzFjOWExYWU=",
        "name": "zio"
      },
      {
        "id": "cmVwb3NpdG9yeTp2MToyOTBlYzI2NS1lYzkxLTRhOWItYmRkYS03YjA5NzBkYjk5Y2I=",
        "name": "refined",
        "url": "https://github.com/fthomas/refined",
        "isFork": false
      },
      {
        "id": "aXNzdWU6djE6MjMzNTlhMjktZDQxYy00ODAxLWE2MjMtNGM2YzNmMGU5NjMy",
        "number": 27,
        "status": "CLOSE",
        "title": "title6",
        "body": "body6"
      }
    ]
  }
}
```

## Resources

* GraphQL
    - [GraphQL](https://graphql.org) (Documentation)
    - [Specification](http://spec.graphql.org)
    - [GraphQL Playground](https://www.graphqlbin.com)
    - [The Fullstack Tutorial for GraphQL](https://www.howtographql.com)
    - [awesome-graphql](https://github.com/chentsulin/awesome-graphql)
    - [Public GraphQL APIs](https://github.com/APIs-guru/graphql-apis)
* GraphiQL
    - [GraphiQL](https://github.com/graphql/graphiql)
    - [Altair GraphQL Client](https://altair.sirmuel.design) (GUI)
    - [GraphiQL.app](https://github.com/skevy/graphiql-app) (GUI)
    - [graphiql](https://github.com/friendsofgo/graphiql) (Docker)
* Pagination
    - Relay [GraphQL Cursor Connections Specification](https://relay.dev/graphql/connections.htm)
    - [GraphQL Pagination](https://graphql.org/learn/pagination)
    - [Global Object Identification](https://graphql.org/learn/global-object-identification)
    - [GraphQL Server Specification](https://relay.dev/docs/en/graphql-server-specification)
    - [GraphQL Pagination best practices](https://medium.com/javascript-in-plain-english/graphql-pagination-using-edges-vs-nodes-in-connections-f2ddb8edffa0)
    - [Evolving API Pagination at Slack](https://slack.engineering/evolving-api-pagination-at-slack-1c1f644f8e12)
* Filter
    - Drupal [Filters](https://drupal-graphql.gitbook.io/graphql/queries/filters)
    - [Droste](https://github.com/higherkindness/droste)
* Caliban
    - [Caliban](https://ghostdogpr.github.io/caliban) (Documentation)
    - [Caliban: Designing a Functional GraphQL Library](https://www.youtube.com/watch?v=OC8PbviYUlQ) by Pierre Ricadat (Video)
    - [GraphQL in Scala with Caliban](https://medium.com/@ghostdogpr/graphql-in-scala-with-caliban-part-1-8ceb6099c3c2)

## TODO

* [x] cats [example](https://github.com/niqdev/scala-fp/pull/85)
    - query `node` and `nodes`
    - query `user` and `users`
    - query `repository` and `repositories`
    - query `issue` and `issues`
* [x] abstract node
* [x] abstract pagination (relay spec)
* [ ] abstract filters (drupal spec with droste) [example](https://github.com/niqdev/scala-fp/pull/96)
    - [doobie](https://tpolecat.github.io/doobie/index.html) support
    - [skunk](https://tpolecat.github.io/skunk) support
    - [slick](https://scala-slick.org) support
* [ ] pagination module - issue with `Node` interface, see possible [solution](https://gist.github.com/paulpdaniels/d8e932b9faee19812d2de8f56dd77a51)
* [ ] filters module
* [x] refined/newtype module
* [ ] migrate from cats to zio
* [ ] mutations example
* [ ] subscriptions example
* [ ] enumeratum module (?)
* [ ] TLS
* [ ] JWT auth
* [ ] static GraphiQL
* [ ] static doc (markdown)
    - https://github.com/2fd/graphdoc
    - https://github.com/wayfair/dociql
    - https://github.com/gjtorikian/graphql-docs
    - https://github.com/edno/docusaurus2-graphql-doc-generator
* [ ] helm chart + argocd deployment (live demo)
* [ ] tests !!!
