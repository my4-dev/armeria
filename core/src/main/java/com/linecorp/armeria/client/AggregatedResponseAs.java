/*
 * Copyright 2022 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.linecorp.armeria.client;

import java.io.IOException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.ResponseEntity;
import com.linecorp.armeria.common.util.Exceptions;
import com.linecorp.armeria.internal.common.JacksonUtil;

final class AggregatedResponseAs {

    static ResponseAs<AggregatedHttpResponse, ResponseEntity<byte[]>> bytes() {
        return response -> ResponseEntity.of(response.headers(), response.content().array(),
                                             response.trailers());
    }

    static ResponseAs<AggregatedHttpResponse, ResponseEntity<String>> string() {
        return response -> ResponseEntity.of(response.headers(), response.contentUtf8(), response.trailers());
    }

    static <T> ResponseAs<AggregatedHttpResponse, ResponseEntity<T>> json(Class<? extends T> clazz) {
        return response -> newJsonResponseEntity(response, bytes -> JacksonUtil.readValue(bytes, clazz));
    }

    static <T> ResponseAs<AggregatedHttpResponse, ResponseEntity<T>> json(Class<? extends T> clazz,
                                                                          HttpStatusPredicate predicate) {
        return response -> newJsonResponseEntity(response, bytes -> JacksonUtil.readValue(bytes, clazz),
                                                 predicate);
    }

    static <T> ResponseAs<AggregatedHttpResponse, ResponseEntity<T>> json(Class<? extends T> clazz,
                                                                          HttpStatusClassPredicate predicate) {
        return response -> newJsonResponseEntity(response, bytes -> JacksonUtil.readValue(bytes, clazz),
                                                 predicate);
    }

    static <T> ResponseAs<AggregatedHttpResponse, ResponseEntity<T>> json(Class<? extends T> clazz,
                                                                          ObjectMapper mapper) {
        return response -> newJsonResponseEntity(response, bytes -> mapper.readValue(bytes, clazz));
    }

    static <T> ResponseAs<AggregatedHttpResponse, ResponseEntity<T>> json(Class<? extends T> clazz,
                                                                          ObjectMapper mapper,
                                                                          HttpStatusPredicate predicate) {
        return response -> newJsonResponseEntity(response, bytes -> mapper.readValue(bytes, clazz),
                                                 predicate);
    }

    static <T> ResponseAs<AggregatedHttpResponse, ResponseEntity<T>> json(Class<? extends T> clazz,
                                                                          ObjectMapper mapper,
                                                                          HttpStatusClassPredicate predicate) {
        return response -> newJsonResponseEntity(response, bytes -> mapper.readValue(bytes, clazz),
                                                 predicate);
    }

    static <T> ResponseAs<AggregatedHttpResponse, ResponseEntity<T>> json(TypeReference<? extends T> typeRef) {
        return response -> newJsonResponseEntity(response, bytes -> JacksonUtil.readValue(bytes, typeRef));
    }

    static <T> ResponseAs<AggregatedHttpResponse, ResponseEntity<T>> json(TypeReference<? extends T> typeRef,
                                                                          HttpStatusPredicate predicate) {
        return response -> newJsonResponseEntity(response, bytes -> JacksonUtil.readValue(bytes, typeRef),
                                                 predicate);
    }

    static <T> ResponseAs<AggregatedHttpResponse, ResponseEntity<T>> json(TypeReference<? extends T> typeRef,
                                                                          HttpStatusClassPredicate predicate) {
        return response -> newJsonResponseEntity(response, bytes -> JacksonUtil.readValue(bytes, typeRef),
                                                 predicate);
    }

    static <T> ResponseAs<AggregatedHttpResponse, ResponseEntity<T>> json(TypeReference<? extends T> typeRef,
                                                                          ObjectMapper mapper) {
        return response -> newJsonResponseEntity(response, bytes -> mapper.readValue(bytes, typeRef));
    }

    static <T> ResponseAs<AggregatedHttpResponse, ResponseEntity<T>> json(TypeReference<? extends T> typeRef,
                                                                          ObjectMapper mapper,
                                                                          HttpStatusPredicate predicate) {
        return response -> newJsonResponseEntity(response, bytes -> mapper.readValue(bytes, typeRef),
                                                 predicate);
    }

    static <T> ResponseAs<AggregatedHttpResponse, ResponseEntity<T>> json(TypeReference<? extends T> typeRef,
                                                                          ObjectMapper mapper,
                                                                          HttpStatusClassPredicate predicate) {
        return response -> newJsonResponseEntity(response, bytes -> mapper.readValue(bytes, typeRef),
                                                 predicate);
    }

    private static <T> ResponseEntity<T> newJsonResponseEntity(AggregatedHttpResponse response,
                                                               JsonDecoder<T> decoder) {
        if (!response.status().isSuccess()) {
            throw newInvalidHttpResponseException(response);
        }
        return createJsonResponseEntity(response, decoder);
    }

    private static <T> ResponseEntity<T> newJsonResponseEntity(AggregatedHttpResponse response,
                                                               JsonDecoder<T> decoder,
                                                               HttpStatusPredicate predicate) {
        if (!predicate.test(response.status())) {
            throw newInvalidHttpStatusResponseException(response, predicate);
        }
        return createJsonResponseEntity(response, decoder);
    }

    private static <T> ResponseEntity<T> newJsonResponseEntity(AggregatedHttpResponse response,
                                                               JsonDecoder<T> decoder,
                                                               HttpStatusClassPredicate predicate) {
        if (!predicate.test(response.status().codeClass())) {
            throw newInvalidHttpStatusClassResponseException(response, predicate);
        }
        return createJsonResponseEntity(response, decoder);
    }

    private static <T> ResponseEntity<T> createJsonResponseEntity(AggregatedHttpResponse response,
                                                                  JsonDecoder<T> decoder) {
        try {
            return ResponseEntity.of(response.headers(), decoder.decode(response.content().array()),
                                     response.trailers());
        } catch (IOException e) {
            return Exceptions.throwUnsafely(new InvalidHttpResponseException(response, e));
        }
    }

    private static InvalidHttpResponseException newInvalidHttpResponseException(
            AggregatedHttpResponse response) {
        return new InvalidHttpResponseException(
                response, "status: " + response.status() +
                          " (expect: the success class (2xx). response: " + response, null);
    }

    private static InvalidHttpResponseException newInvalidHttpStatusResponseException(
            AggregatedHttpResponse response, HttpStatusPredicate predicate) {
        return new InvalidHttpResponseException(
                response, "status: " + response.status() +
                          " (expect: the " + predicate.status().reasonPhrase() + " class (" +
                          predicate.status().codeAsText() + "). response: " + response, null);
    }

    private static InvalidHttpResponseException newInvalidHttpStatusClassResponseException(
            AggregatedHttpResponse response, HttpStatusClassPredicate predicate) {
        return new InvalidHttpResponseException(
                response, "status: " + response.status() +
                          " (expect: the " + predicate.statusClass() + " class response: " + response, null);
    }

    @FunctionalInterface
    private interface JsonDecoder<T> {
        T decode(byte[] bytes) throws IOException;
    }

    private AggregatedResponseAs() {}
}
