/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.javapoet;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getOnlyElement;

/** A generated annotation on a declaration. */
public final class AnnotationSpec {
  public final Type type;
  public final ImmutableMultimap<String, CodeBlock> members;

  private AnnotationSpec(Builder builder) {
    this.type = checkNotNull(builder.type, "type");
    this.members = ImmutableListMultimap.copyOf(builder.members);
  }

  void emit(CodeWriter codeWriter, boolean inline) throws IOException {
    String whitespace = inline ? "" : "\n";
    String memberSeparator = inline ? ", " : ",\n";
    if (members.isEmpty()) {
      // @Singleton
      codeWriter.emit("@$T", type);
    } else if (members.keySet().equals(ImmutableSet.of("value"))) {
      // @Named("foo")
      codeWriter.emit("@$T(", type);
      emitAnnotationValue(codeWriter, whitespace, memberSeparator, members.values());
      codeWriter.emit(")");
    } else {
      // Inline:
      //   @Column(name = "updated_at", nullable = false)
      //
      // Not inline:
      //   @Column(
      //       name = "updated_at",
      //       nullable = false
      //   )
      codeWriter.emit("@$T(" + whitespace, type);
      codeWriter.indent(2);
      for (Iterator<Map.Entry<String, Collection<CodeBlock>>> i
          = members.asMap().entrySet().iterator(); i.hasNext();) {
        Map.Entry<String, Collection<CodeBlock>> entry = i.next();
        codeWriter.emit("$L = ", entry.getKey());
        emitAnnotationValue(codeWriter, whitespace, memberSeparator, entry.getValue());
        if (i.hasNext()) codeWriter.emit(memberSeparator);
      }
      codeWriter.unindent(2);
      codeWriter.emit(whitespace + ")");
    }
  }

  private void emitAnnotationValue(CodeWriter codeWriter, String whitespace, String memberSeparator,
      Collection<CodeBlock> value) throws IOException {
    if (value.size() == 1) {
      codeWriter.indent(2);
      codeWriter.emit(getOnlyElement(value));
      codeWriter.unindent(2);
      return;
    }

    codeWriter.emit("{" + whitespace);
    codeWriter.indent(2);
    boolean first = true;
    for (CodeBlock codeBlock : value) {
      if (!first) codeWriter.emit(memberSeparator);
      codeWriter.emit(codeBlock);
      first = false;
    }
    codeWriter.unindent(2);
    codeWriter.emit(whitespace + "}");
  }

  public static Builder builder(Type type) {
    return new Builder(type);
  }

  @Override public boolean equals(Object o) {
    return o instanceof AnnotationSpec
        && ((AnnotationSpec) o).type.equals(type)
        && ((AnnotationSpec) o).members.equals(members);
  }

  @Override public int hashCode() {
    return type.hashCode() + 37 * members.hashCode();
  }

  public static final class Builder {
    private final Type type;
    private final Multimap<String, CodeBlock> members = Multimaps.newListMultimap(
        new TreeMap<String, Collection<CodeBlock>>(), AnnotationSpec.<CodeBlock>listSupplier());

    private Builder(Type type) {
      this.type = type;
    }

    public Builder addMember(String name, String format, Object... args) {
      members.put(name, CodeBlock.builder().add(format, args).build());
      return this;
    }

    public AnnotationSpec build() {
      return new AnnotationSpec(this);
    }
  }

  private static <T> Supplier<List<T>> listSupplier() {
    return new Supplier<List<T>>() {
      @Override public List<T> get() {
        return new ArrayList<>();
      }
    };
  }
}
