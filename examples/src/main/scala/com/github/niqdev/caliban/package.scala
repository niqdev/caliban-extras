package com.github.niqdev

import com.github.niqdev.caliban.codecs.SchemaEncoderOps

package object caliban {

  // TODO add constraint on M
  final implicit def schemaEncoderSyntax[M](model: M): SchemaEncoderOps[M] =
    new SchemaEncoderOps[M](model)
}
