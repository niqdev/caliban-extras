package com.github.niqdev

import com.github.niqdev.caliban.codecs.SchemaEncoderOps

package object caliban {

  // TODO add constraint on A
  final implicit def schemaEncoderSyntax[A](model: A): SchemaEncoderOps[A] =
    new SchemaEncoderOps[A](model)
}
