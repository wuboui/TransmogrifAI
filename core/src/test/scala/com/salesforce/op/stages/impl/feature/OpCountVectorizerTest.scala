/*
 * Copyright (c) 2017, Salesforce.com, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.salesforce.op.stages.impl.feature

import com.salesforce.op._
import com.salesforce.op.features.types._
import com.salesforce.op.test.TestOpVectorColumnType.{IndCol, IndVal}
import com.salesforce.op.test.{TestFeatureBuilder, TestOpVectorMetadataBuilder, TestSparkContext}
import com.salesforce.op.utils.spark.OpVectorMetadata
import com.salesforce.op.utils.spark.RichDataset._
import org.apache.spark.ml.linalg.Vectors
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Assertions, FlatSpec, Matchers}


@RunWith(classOf[JUnitRunner])
class OpCountVectorizerTest extends FlatSpec with TestSparkContext {

  val data = Seq[(Real, TextList)](
    (Real(0), Seq("a", "b", "c").toTextList),
    (Real(1), Seq("a", "b", "b", "b", "a", "c").toTextList)
  )

  lazy val (ds, f1, f2) = TestFeatureBuilder(data)

  lazy val expected = Array[(Real, OPVector)](
    (Real(0), Vectors.sparse(3, Array(0, 1, 2), Array(1.0, 1.0, 1.0)).toOPVector),
    (Real(1), Vectors.sparse(3, Array(0, 1, 2), Array(3.0, 2.0, 1.0)).toOPVector)
  )

  val f2vec = new OpCountVectorizer().setInput(f2).setVocabSize(3).setMinDF(2)

  Spec[OpCountVectorizerTest] should "convert array of strings into count vector" in {
    val transformedData = f2vec.fit(ds).transform(ds)
    val output = f2vec.getOutput()
    transformedData.orderBy(f1.name).collect(f1, output) should contain theSameElementsInOrderAs expected
  }

  it should "return the a fitted vectorizer with the correct parameters" in {
    val fitted = f2vec.fit(ds)
    val vectorMetadata = fitted.getMetadata()
    val expectedMeta = TestOpVectorMetadataBuilder(
      f2vec,
      f2 -> List(IndVal(Some("b")), IndVal(Some("a")), IndVal(Some("c")))
    )
    // cannot just do equals because fitting is nondeterministic
    OpVectorMetadata(f2vec.getOutputFeatureName, vectorMetadata).columns should contain theSameElementsAs
      expectedMeta.columns
  }

  it should "convert array of strings into count vector (shortcut version)" in {
    val output = f2.countVec(minDF = 2, vocabSize = 3)
    val f2vec = output.originStage.asInstanceOf[OpCountVectorizer]
    val transformedData = f2vec.fit(ds).transform(ds)
    transformedData.orderBy(f1.name).collect(f1, output) should contain theSameElementsInOrderAs expected
  }
}
