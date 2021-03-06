/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.build.gradle.internal.dsl;

import com.android.annotations.NonNull;
import com.android.resources.Density;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.gradle.api.Action;
import org.gradle.internal.reflect.Instantiator;

import java.util.Set;

/**
 * Main entry point for all splits related information.
 */
public class Splits {

    private final DensitySplitData density;
    private final AbiSplitData abi;

    private static final Set<String> ABI_LIST = ImmutableSet.of(
            "armeabi", "armeabi-v7a", "arm64-v8a","x86", "x86_64", "mips", "mips64");

    public Splits(@NonNull Instantiator instantiator) {
        density = instantiator.newInstance(DensitySplitData.class);
        abi = instantiator.newInstance(AbiSplitData.class);
    }

    public DensitySplitData getDensity() {
        return density;
    }

    public void density(Action<DensitySplitData> action) {
        action.execute(density);
    }

    public AbiSplitData getAbi() {
        return abi;
    }

    public void abi(Action<AbiSplitData> action) {
        action.execute(abi);
    }

    /**
     * Returns the list of Density filters used for multi-apk.
     *
     * null value is allowed, indicating the need to generate an apk with all densities.
     *
     * @return a set of filters.
     */
    @NonNull
    public Set<String> getDensityFilters() {
        Density[] values = Density.values();
        Set<String> fullList = Sets.newHashSetWithExpectedSize(values.length - 1);
        for (Density value : values) {
            if (value != Density.NODPI) {
                fullList.add(value.getResourceValue());
            }
        }

        return density.getApplicableFilters(fullList);
    }

    /**
     * Returns the list of ABI filters used for multi-apk.
     *
     * null value is allowed, indicating the need to generate an apk with all abis.
     *
     * @return a set of filters.
     */
    @NonNull
    public Set<String> getAbiFilters() {
        return abi.getApplicableFilters(ABI_LIST);
    }
}
