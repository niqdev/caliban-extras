package com.github.niqdev

import com.github.niqdev.caliban.codecs.{ SchemaDecoderOps, SchemaEncoderOps }

package object caliban {

  final implicit def schemaEncoderSyntax[M](model: M): SchemaEncoderOps[M] =
    new SchemaEncoderOps[M](model)

  final implicit def schemaDecoderSyntax[S](schema: S): SchemaDecoderOps[S] =
    new SchemaDecoderOps[S](schema)
}
