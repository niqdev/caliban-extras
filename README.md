# caliban-extras

> WIP

## Example

Re-inventing GraphQL [GitHub](https://developer.github.com/v4/explorer) api pagination, filter and authentication

```bash
# run example
sbt -jvm-debug 5005 "examples/runMain com.github.niqdev.caliban.Main"
```

Sample queries

```graphql
query findRepositoryByName {
  repository(name: "zio") {
    id
    name
    url
    isFork
    createdAt
    updatedAt
  }
}

query getNodeById {
  node(id: "opaqueCursor") {
    id
    ... on UserNodeF {
      id
      name
      createdAt
      updatedAt
    }
    ... on RepositoryNodeF {
      name
      url
      isFork
      createdAt
      updatedAt
    }
  }
}

query getSimpleUser {
  user(name: "typelevel") {
    id
    name
    repositories(first: 10, after: "opaqueCursor") {
      edges {
        cursor
        node {
          id
          name
        }
      }
      pageInfo {
        hasNextPage
      }
    }
  }
}

query getRepositories {
  repositories(first: 2, after: "opaqueCursor") {
    nodes {
      id
      name
    }
  }
}

query getUser {
  user(name: "typelevel") {
    id
    name
    createdAt
    updatedAt
    repositories(first: 10) {
      edges {
        cursor
        node {
          id
          name
          url
          isFork
          createdAt
          updatedAt
        }
      }
      pageInfo {
        hasNextPage
        hasPreviousPage
        startCursor
        endCursor
      }
      totalCount
      nodes {
        id
        name
        url
        isFork
        createdAt
        updatedAt
      }
    }
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
* Caliban
    - [Caliban](https://ghostdogpr.github.io/caliban) (Documentation)
    - [Caliban: Designing a Functional GraphQL Library](https://www.youtube.com/watch?v=OC8PbviYUlQ) by Pierre Ricadat (Video)
    - [GraphQL in Scala with Caliban](https://medium.com/@ghostdogpr/graphql-in-scala-with-caliban-part-1-8ceb6099c3c2)
