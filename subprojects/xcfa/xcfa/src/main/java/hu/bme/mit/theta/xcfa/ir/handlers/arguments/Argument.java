/*
 * Copyright 2021 Budapest University of Technology and Economics
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hu.bme.mit.theta.xcfa.ir.handlers.arguments;

import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.Type;

import java.util.Map;

public abstract class Argument {

    public static Argument of(String type, String name) {
        if (!type.equals("")) {
            return new RegArgument(type, name);
        }

        String[] split = name.split(" ");
        if (split.length == 2) {
            switch (split[0]) {
                case "meta":
                    return new StringArgument(null, split[1]);
                case "label":
                    return new LabelArgument(null, split[1]);
                default:
                    return new ConstantArgument(split[0], split[1]);
            }
        } else return new StringArgument(null, split[0]);
    }

    public abstract Type getType();

    public abstract Expr<?> getExpr(Map<String, Expr<?>> values);

    public abstract String getName();
}
