package com.github.niqdev

import com.github.niqdev.caliban.codecs.SchemaEncoderOps

package object caliban {

  final implicit def schemaEncoderSyntax[A](model: A): SchemaEncoderOps[A] =
    new SchemaEncoderOps[A](model)
}
