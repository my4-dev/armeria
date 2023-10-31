/*
 * Copyright 2023 LINE Corporation
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

package com.linecorp.armeria.server;

import static java.util.Objects.requireNonNull;

import java.util.function.Function;

import com.linecorp.armeria.internal.server.RouteDecoratingService;

abstract class AbstractContextPathDecoratingBindingBuilder<T extends ServiceConfigsBuilder>
        extends AbstractBindingBuilder {

    private final AbstractContextPathServicesBuilder<T> builder;

    AbstractContextPathDecoratingBindingBuilder(AbstractContextPathServicesBuilder<T> builder) {
        super(builder.contextPaths());
        this.builder = builder;
    }

    AbstractContextPathServicesBuilder<T> build(
            Function<? super HttpService, ? extends HttpService> decorator) {
        requireNonNull(decorator, "decorator");
        buildRouteList().forEach(
                route -> contextPaths().forEach(contextPath -> builder.addRouteDecoratingService(
                        new RouteDecoratingService(route, contextPath, decorator))));
        return builder;
    }

    AbstractContextPathServicesBuilder<T> build(
            DecoratingHttpServiceFunction decoratingHttpServiceFunction) {
        requireNonNull(decoratingHttpServiceFunction, "decoratingHttpServiceFunction");
        return build(delegate -> new FunctionalDecoratingHttpService(delegate, decoratingHttpServiceFunction));
    }
}
