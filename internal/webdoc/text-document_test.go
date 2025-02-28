// ORIGINAL: javatest/TextDocumentStatisticsTest.java

// Copyright (c) 2020 Markus Mobius
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package webdoc_test

import (
	"testing"

	"github.com/omnivore-app/go-domdistiller/internal/stringutil"
	"github.com/omnivore-app/go-domdistiller/internal/testutil"
	"github.com/stretchr/testify/assert"
)

const ThreeWords = "I love statistics"

func Test_WebDoc_TextDocument_OnlyContent(t *testing.T) {
	builder := testutil.NewTextDocumentBuilder(stringutil.FastWordCounter{})
	builder.AddContentBlock(ThreeWords)
	builder.AddContentBlock(ThreeWords)
	builder.AddContentBlock(ThreeWords)

	doc := builder.Build()
	assert.Equal(t, 9, doc.CountWordsInContent())
}

func Test_WebDoc_TextDocument_OnlyNonContent(t *testing.T) {
	builder := testutil.NewTextDocumentBuilder(stringutil.FastWordCounter{})
	builder.AddNonContentBlock(ThreeWords)
	builder.AddNonContentBlock(ThreeWords)
	builder.AddNonContentBlock(ThreeWords)

	doc := builder.Build()
	assert.Equal(t, 0, doc.CountWordsInContent())
}

func Test_WebDoc_TextDocument_MixedContent(t *testing.T) {
	builder := testutil.NewTextDocumentBuilder(stringutil.FastWordCounter{})
	builder.AddContentBlock(ThreeWords)
	builder.AddNonContentBlock(ThreeWords)
	builder.AddContentBlock(ThreeWords)
	builder.AddNonContentBlock(ThreeWords)

	doc := builder.Build()
	assert.Equal(t, 6, doc.CountWordsInContent())
}
