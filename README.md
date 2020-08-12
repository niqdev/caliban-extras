# caliban-extras

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
* [ ] pagination module - issue with `Node` interface
* [ ] filters module
* [x] refined/newtype module
* [ ] migrate from cats to zio
* [ ] mutations
* [ ] subscriptions
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

## Example

A minimalistic version of GraphQL [GitHub](https://developer.github.com/v4/explorer) api with pagination, filters and authentication

```bash
# run example
sbt -jvm-debug 5005 "examples/runMain com.github.niqdev.caliban.Main"
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
